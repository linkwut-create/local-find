"use strict";

const dgram = require("node:dgram");
const http = require("node:http");
const net = require("node:net");
const os = require("node:os");

const MDNS_ADDRESS = "224.0.0.251";
const MDNS_PORT = 5353;
const SERVICE_TYPE = "_localfind._tcp.local";
const BRIDGE_HOST = "127.0.0.1";
const BRIDGE_PORT = 43789;
const BEACON_PORT = 43790;
const DISCOVERY_TIMEOUT_MS = 1800;
const DEVICE_INFO_TIMEOUT_MS = 1200;
const ADDRESS_SCAN_TIMEOUT_MS = 500;
const ADDRESS_SCAN_CONCURRENCY = 512;
const ADDRESS_SCAN_MAX_HOSTS = 65534;
const ADDRESS_SCAN_MAX_DURATION_MS = 6000;
const BEACON_MAX_AGE_MS = 15000;
const BEACON_WAIT_MS = 1800;

const beaconCache = new Map();

function normalizeDnsName(name) {
  return String(name || "").replace(/\.$/, "").toLowerCase();
}

function encodeDnsName(name) {
  const labels = String(name).replace(/\.$/, "").split(".");
  const chunks = [];
  for (const label of labels) {
    const bytes = Buffer.from(label, "utf8");
    if (bytes.length > 63) throw new Error("DNS label too long");
    chunks.push(Buffer.from([bytes.length]), bytes);
  }
  chunks.push(Buffer.from([0]));
  return Buffer.concat(chunks);
}

function createPtrQuery(serviceType = SERVICE_TYPE) {
  const name = encodeDnsName(serviceType);
  const packet = Buffer.alloc(12 + name.length + 4);
  packet.writeUInt16BE(0, 0);
  packet.writeUInt16BE(0, 2);
  packet.writeUInt16BE(1, 4);
  packet.writeUInt16BE(0, 6);
  packet.writeUInt16BE(0, 8);
  packet.writeUInt16BE(0, 10);
  name.copy(packet, 12);
  packet.writeUInt16BE(12, 12 + name.length);
  packet.writeUInt16BE(1, 14 + name.length);
  return packet;
}

function getIpv4Interfaces() {
  const interfaces = [];
  for (const entries of Object.values(os.networkInterfaces())) {
    for (const entry of entries || []) {
      const family = typeof entry.family === "string" ? entry.family : String(entry.family);
      if (family === "IPv4" && !entry.internal) {
        interfaces.push({
          address: entry.address,
          netmask: entry.netmask,
          prefixLength: prefixLengthFromNetmask(entry.netmask)
        });
      }
    }
  }
  return Array.from(new Map(interfaces.map((entry) => [entry.address, entry])).values());
}

function prefixLengthFromNetmask(netmask) {
  const octets = String(netmask || "").split(".").map(Number);
  if (octets.length !== 4 || octets.some((octet) => !Number.isInteger(octet) || octet < 0 || octet > 255)) {
    return 0;
  }
  let mask = 0;
  for (const octet of octets) mask = (mask << 8) | octet;
  let prefixLength = 0;
  for (let bit = 31; bit >= 0 && ((mask >>> bit) & 1) === 1; bit -= 1) prefixLength += 1;
  return prefixLength;
}

function readDnsName(packet, startOffset) {
  let offset = startOffset;
  let nextOffset = startOffset;
  let jumped = false;
  const labels = [];

  for (let jumps = 0; jumps < 50; jumps += 1) {
    if (offset >= packet.length) return null;
    const length = packet[offset];
    if (length === 0) {
      if (!jumped) nextOffset = offset + 1;
      return { name: labels.join(".") + ".", nextOffset };
    }

    if ((length & 0xc0) === 0xc0) {
      if (offset + 1 >= packet.length) return null;
      const pointer = ((length & 0x3f) << 8) | packet[offset + 1];
      if (!jumped) nextOffset = offset + 2;
      jumped = true;
      offset = pointer;
      continue;
    }

    if (length > 63 || offset + 1 + length > packet.length) return null;
    labels.push(packet.toString("utf8", offset + 1, offset + 1 + length));
    offset += 1 + length;
    if (!jumped) nextOffset = offset;
  }

  return null;
}

function parseMdnsPacket(packet, records) {
  if (packet.length < 12) return;
  const questionCount = packet.readUInt16BE(4);
  const answerCount = packet.readUInt16BE(6);
  const authorityCount = packet.readUInt16BE(8);
  const additionalCount = packet.readUInt16BE(10);
  let offset = 12;

  for (let index = 0; index < questionCount; index += 1) {
    const questionName = readDnsName(packet, offset);
    if (!questionName || questionName.nextOffset + 4 > packet.length) return;
    offset = questionName.nextOffset + 4;
  }

  const recordCount = answerCount + authorityCount + additionalCount;
  for (let index = 0; index < recordCount; index += 1) {
    const parsedName = readDnsName(packet, offset);
    if (!parsedName || parsedName.nextOffset + 10 > packet.length) return;
    offset = parsedName.nextOffset;

    const type = packet.readUInt16BE(offset);
    const recordClass = packet.readUInt16BE(offset + 2);
    const dataLength = packet.readUInt16BE(offset + 8);
    const dataOffset = offset + 10;
    const dataEnd = dataOffset + dataLength;
    if (dataEnd > packet.length) return;
    offset = dataEnd;

    const name = normalizeDnsName(parsedName.name);
    if ((recordClass & 0x7fff) !== 1) continue;

    if (type === 12) {
      const target = readDnsName(packet, dataOffset);
      if (target) records.ptrs.add(normalizeDnsName(target.name));
    } else if (type === 33 && dataLength >= 7) {
      const target = readDnsName(packet, dataOffset + 6);
      if (target) {
        records.srvs.set(name, {
          host: normalizeDnsName(target.name),
          port: packet.readUInt16BE(dataOffset + 4)
        });
      }
    } else if (type === 1 && dataLength === 4) {
      records.addresses.set(name, Array.from(packet.subarray(dataOffset, dataEnd)).join("."));
    }
  }
}

function queryMdns(serviceType = SERVICE_TYPE) {
  return new Promise((resolve) => {
    const records = { ptrs: new Set(), srvs: new Map(), addresses: new Map() };
    let packetCount = 0;
    const socket = dgram.createSocket({ type: "udp4", reuseAddr: true });
    const query = createPtrQuery(serviceType);
    const ipv4Interfaces = getIpv4Interfaces();
    const timers = [];
    let finished = false;

    const finish = () => {
      if (finished) return;
      finished = true;
      for (const timer of timers) clearTimeout(timer);
      try { socket.close(); } catch {}
      const candidates = [];
      for (const [instance, srv] of records.srvs.entries()) {
        const host = records.addresses.get(srv.host);
        if (!host || !srv.port) continue;
        candidates.push({ instance, host, port: srv.port });
      }
      resolve({
        candidates,
        packetCount,
        ipv4Interfaces,
        ptrs: Array.from(records.ptrs),
        srvs: Array.from(records.srvs.entries()).map(([name, value]) => ({ name, ...value })),
        addresses: Array.from(records.addresses.entries()).map(([name, address]) => ({ name, address }))
      });
    };

    socket.on("message", (message) => {
      packetCount += 1;
      try { parseMdnsPacket(message, records); } catch {}
    });
    socket.on("error", finish);
    socket.bind(0, "0.0.0.0", () => {
      try {
        for (const entry of ipv4Interfaces) {
          socket.addMembership(MDNS_ADDRESS, entry.address);
        }
        socket.setMulticastTTL(255);
        const send = () => {
          for (const entry of ipv4Interfaces) {
            socket.setMulticastInterface(entry.address);
            socket.send(query, 0, query.length, MDNS_PORT, MDNS_ADDRESS);
          }
        };
        send();
        timers.push(setTimeout(send, 350), setTimeout(send, 900), setTimeout(finish, DISCOVERY_TIMEOUT_MS));
      } catch {
        finish();
      }
    });
  });
}

async function fetchDeviceInfo(candidate) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), DEVICE_INFO_TIMEOUT_MS);
  try {
    const response = await fetch(`http://${candidate.host}:${candidate.port}/device-info`, {
      cache: "no-store",
      signal: controller.signal
    });
    if (!response.ok) return null;
    const body = await response.json();
    return {
      id: String(body?.id || ""),
      name: body?.name || "",
      type: body?.type || "android_phone",
      host: candidate.host,
      port: String(body?.port || candidate.port),
      networkPrefixLength: Number(body?.networkPrefixLength) || 0
    };
  } catch {
    return null;
  } finally {
    clearTimeout(timer);
  }
}

async function discoverDevice(deviceId, options = {}) {
  const cached = await getVerifiedBeacon(deviceId);
  if (cached) return [cached];

  const discovery = await queryMdns();
  const devices = await Promise.all(discovery.candidates.map(fetchDeviceInfo));
  const matchedDevices = devices.filter((device) => device && device.id === deviceId);
  if (matchedDevices.length > 0) return matchedDevices;

  const beacon = await waitForVerifiedBeacon(deviceId);
  if (beacon) return [beacon];

  const referenceHost = isIpv4Address(options.host) ? options.host : "";
  if (!referenceHost) return [];

  const prefixLength = resolveScanPrefix(referenceHost, options.prefixLength);
  const port = Number(options.port) || 8888;
  const scanned = await scanSubnetForDevice(deviceId, referenceHost, prefixLength, port);
  return scanned ? [scanned] : [];
}

function isIpv4Address(value) {
  const octets = String(value || "").split(".").map(Number);
  return octets.length === 4 && octets.every((octet) => Number.isInteger(octet) && octet >= 0 && octet <= 255);
}

function isPrivateIpv4(value) {
  if (!isIpv4Address(value)) return false;
  const [first, second] = String(value).split(".").map(Number);
  return first === 10
    || (first === 172 && second >= 16 && second <= 31)
    || (first === 192 && second === 168);
}

async function getVerifiedBeacon(deviceId) {
  const announcement = beaconCache.get(deviceId);
  if (!announcement || Date.now() - announcement.receivedAt > BEACON_MAX_AGE_MS) return null;
  const device = await fetchDeviceInfo({ host: announcement.host, port: announcement.port });
  return device && device.id === deviceId ? device : null;
}

async function waitForVerifiedBeacon(deviceId) {
  const deadline = Date.now() + BEACON_WAIT_MS;
  while (Date.now() < deadline) {
    const device = await getVerifiedBeacon(deviceId);
    if (device) return device;
    await new Promise((resolve) => setTimeout(resolve, 150));
  }
  return null;
}

function resolveScanPrefix(referenceHost, requestedPrefixLength) {
  const requested = Number(requestedPrefixLength);
  if (Number.isInteger(requested) && requested >= 16 && requested <= 30) return requested;

  const reference = ipv4ToNumber(referenceHost);
  for (const entry of getIpv4Interfaces()) {
    const local = ipv4ToNumber(entry.address);
    const prefix = Number(entry.prefixLength);
    if (reference !== null && local !== null && prefix >= 16 && prefix <= 30) {
      const mask = (0xffffffff << (32 - prefix)) >>> 0;
      if ((reference & mask) === (local & mask)) return prefix;
    }
  }
  return 24;
}

function ipv4ToNumber(host) {
  const octets = String(host || "").split(".").map(Number);
  if (!isIpv4Address(host)) return null;
  return (((octets[0] << 24) >>> 0)
    + (octets[1] << 16)
    + (octets[2] << 8)
    + octets[3]) >>> 0;
}

function numberToIpv4(value) {
  return [
    (value >>> 24) & 0xff,
    (value >>> 16) & 0xff,
    (value >>> 8) & 0xff,
    value & 0xff
  ].join(".");
}

function buildSubnetHosts(referenceHost, prefixLength) {
  const ipNumber = ipv4ToNumber(referenceHost);
  const normalizedPrefix = Math.min(30, Math.max(16, Number(prefixLength) || 24));
  if (ipNumber === null) return [];
  const mask = (0xffffffff << (32 - normalizedPrefix)) >>> 0;
  const network = (ipNumber & mask) >>> 0;
  const broadcast = (network | (~mask >>> 0)) >>> 0;
  const hostCount = broadcast - network - 1;
  if (hostCount > ADDRESS_SCAN_MAX_HOSTS) return [];

  const hosts = [];
  for (let candidate = network + 1; candidate < broadcast; candidate += 1) {
    if (candidate !== ipNumber) hosts.push(numberToIpv4(candidate));
  }
  return hosts;
}

function fetchDeviceInfoNative(host, port) {
  return new Promise((resolve) => {
    let settled = false;
    let responseBody = "";
    let statusCode = 0;
    const finish = (value) => {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      socket.destroy();
      resolve(value);
    };
    const socket = net.createConnection({ host, port });
    const timer = setTimeout(() => finish(null), ADDRESS_SCAN_TIMEOUT_MS);
    socket.on("connect", () => {
      socket.write("GET /device-info HTTP/1.1\r\nHost: local-find\r\nConnection: close\r\n\r\n");
    });
    socket.on("data", (chunk) => {
      responseBody += chunk.toString("utf8");
      if (responseBody.length > 16384) return finish(null);
      const headerEnd = responseBody.indexOf("\r\n\r\n");
      if (headerEnd < 0) return;
      if (statusCode === 0) {
        const statusLine = responseBody.slice(0, responseBody.indexOf("\r\n"));
        statusCode = Number(statusLine.split(" ")[1]) || 0;
      }
      if (statusCode !== 200) return finish(null);
      try {
        const body = JSON.parse(responseBody.slice(headerEnd + 4));
        finish(body);
      } catch {
        // Wait for the rest of the response body.
      }
    });
    socket.on("end", () => {
      if (statusCode !== 200) return finish(null);
      const headerEnd = responseBody.indexOf("\r\n\r\n");
      try { finish(JSON.parse(responseBody.slice(headerEnd + 4))); } catch { finish(null); }
    });
    socket.on("error", () => finish(null));
  });
}

async function scanSubnetForDevice(deviceId, referenceHost, prefixLength, port) {
  const hosts = buildSubnetHosts(referenceHost, prefixLength);
  if (hosts.length === 0) return null;
  let nextIndex = 0;
  let found = null;
  let cancelled = false;
  const deadline = Date.now() + ADDRESS_SCAN_MAX_DURATION_MS;

  async function worker() {
    while (!cancelled && Date.now() < deadline) {
      const host = hosts[nextIndex++];
      if (!host) return;
      const info = await fetchDeviceInfoNative(host, port);
      if (String(info?.id || "") !== deviceId) continue;
      found = {
        id: deviceId,
        name: info.name || "",
        type: info.type || "android_phone",
        host,
        port: String(info.port || port),
        networkPrefixLength: Number(info.networkPrefixLength) || prefixLength
      };
      cancelled = true;
      return;
    }
  }

  await Promise.all(Array.from({ length: Math.min(ADDRESS_SCAN_CONCURRENCY, hosts.length) }, worker));
  return found;
}

function sendJson(response, statusCode, body) {
  response.writeHead(statusCode, {
    "Content-Type": "application/json; charset=utf-8",
    "Access-Control-Allow-Origin": "*",
    "Cache-Control": "no-store"
  });
  response.end(JSON.stringify(body));
}

const server = http.createServer(async (request, response) => {
  response.setHeader("Access-Control-Allow-Origin", "*");
  if (request.method === "OPTIONS") {
    response.writeHead(204, {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type"
    });
    response.end();
    return;
  }

  const url = new URL(request.url || "/", `http://${BRIDGE_HOST}:${BRIDGE_PORT}`);
  if (request.method !== "GET") {
    sendJson(response, 405, { ok: false, error: "method_not_allowed" });
    return;
  }

  if (url.pathname === "/health") {
    sendJson(response, 200, { ok: true, service: "local-find-discovery-bridge" });
    return;
  }

  if (url.pathname === "/debug") {
    try {
      const serviceType = String(url.searchParams.get("serviceType") || SERVICE_TYPE);
      sendJson(response, 200, {
        ok: true,
        ...(await queryMdns(serviceType)),
        beacons: Array.from(beaconCache.values())
      });
    } catch (error) {
      sendJson(response, 500, { ok: false, error: error.message || "discovery_failed" });
    }
    return;
  }


  if (url.pathname !== "/discover") {
    sendJson(response, 404, { ok: false, error: "not_found" });
    return;
  }

  const deviceId = String(url.searchParams.get("deviceId") || "").trim();
  if (!deviceId || deviceId.length > 128) {
    sendJson(response, 400, { ok: false, error: "deviceId_required" });
    return;
  }

  try {
    const devices = await discoverDevice(deviceId, {
      host: url.searchParams.get("host"),
      port: url.searchParams.get("port"),
      prefixLength: url.searchParams.get("prefixLength")
    });
    sendJson(response, 200, { ok: true, devices });
  } catch (error) {
    sendJson(response, 500, { ok: false, error: error.message || "discovery_failed" });
  }
});

const beaconSocket = dgram.createSocket("udp4");
beaconSocket.on("message", (message) => {
  try {
    const announcement = JSON.parse(message.toString("utf8"));
    const id = String(announcement?.id || "").trim();
    const host = String(announcement?.host || "").trim();
    const port = Number(announcement?.port) || 0;
    if (!id || id.length > 128 || !isPrivateIpv4(host) || port < 1 || port > 65535) return;
    beaconCache.set(id, {
      id,
      name: String(announcement?.name || ""),
      host,
      port,
      networkPrefixLength: Number(announcement?.networkPrefixLength) || 0,
      receivedAt: Date.now()
    });
  } catch {}
});
beaconSocket.on("error", (error) => {
  console.error(JSON.stringify({ ok: false, service: "local-find-discovery-beacon", error: error.message }));
});
beaconSocket.bind(BEACON_PORT, "0.0.0.0");

server.on("error", (error) => {
  console.error(JSON.stringify({ ok: false, error: error.message }));
  process.exitCode = 1;
});

server.listen(BRIDGE_PORT, BRIDGE_HOST, () => {
  console.log(JSON.stringify({ ok: true, service: "local-find-discovery-bridge", host: BRIDGE_HOST, port: BRIDGE_PORT }));
});

process.on("SIGINT", () => {
  beaconSocket.close();
  server.close(() => process.exit(0));
});
process.on("SIGTERM", () => {
  beaconSocket.close();
  server.close(() => process.exit(0));
});
