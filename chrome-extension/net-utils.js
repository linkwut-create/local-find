// Pure network-address helpers shared by popup.js. Kept dependency-free (no
// DOM/chrome.* APIs) so they can be unit tested with node:test without a
// browser or bundler, and loaded as a plain classic <script> before popup.js
// in popup.html so both share one global scope.

function ipv4ToNumber(host) {
  const octets = String(host || "").split(".").map(Number);
  if (octets.length !== 4 || octets.some((octet) => !Number.isInteger(octet) || octet < 0 || octet > 255)) {
    return null;
  }

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

function buildRecoveryHosts(host, prefixLength) {
  const ipNumber = ipv4ToNumber(host);
  if (ipNumber === null) {
    return [];
  }

  const normalizedPrefixLength = Math.min(30, Math.max(16, Number(prefixLength) || 16));
  const mask = (0xffffffff << (32 - normalizedPrefixLength)) >>> 0;
  const network = (ipNumber & mask) >>> 0;
  const broadcast = (network | (~mask >>> 0)) >>> 0;
  const hosts = [];

  for (let candidate = network + 1; candidate < broadcast; candidate += 1) {
    if (candidate !== ipNumber) {
      hosts.push(numberToIpv4(candidate));
    }
  }

  return hosts;
}

function normalizeNetworkPrefixLength(value) {
  const prefixLength = Number(value);
  if (!Number.isInteger(prefixLength) || prefixLength < 8 || prefixLength > 30) {
    return 0;
  }
  return prefixLength;
}

function isPrivateIPv4(host) {
  const octets = String(host || "").split(".").map(Number);
  if (octets.length !== 4 || octets.some((octet) => !Number.isInteger(octet) || octet < 0 || octet > 255)) {
    return false;
  }

  return octets[0] === 10
    || (octets[0] === 192 && octets[1] === 168)
    || (octets[0] === 172 && octets[1] >= 16 && octets[1] <= 31);
}

function isValidPort(port) {
  const numericPort = Number(port);
  return Number.isInteger(numericPort) && numericPort >= 1 && numericPort <= 65535;
}

// `module` does not exist in the extension's browser context, so this is a
// no-op there; nothing here changes runtime behavior in Chrome.
if (typeof module !== "undefined" && module.exports) {
  module.exports = {
    ipv4ToNumber,
    numberToIpv4,
    buildRecoveryHosts,
    normalizeNetworkPrefixLength,
    isPrivateIPv4,
    isValidPort,
  };
}
