const test = require("node:test");
const assert = require("node:assert/strict");
const {
  ipv4ToNumber,
  numberToIpv4,
  buildRecoveryHosts,
  normalizeNetworkPrefixLength,
  isPrivateIPv4,
  isValidPort,
} = require("./net-utils.js");

test("ipv4ToNumber/numberToIpv4 round-trip valid addresses", () => {
  assert.equal(numberToIpv4(ipv4ToNumber("192.168.1.42")), "192.168.1.42");
  assert.equal(numberToIpv4(ipv4ToNumber("10.0.0.1")), "10.0.0.1");
  assert.equal(numberToIpv4(ipv4ToNumber("255.255.255.255")), "255.255.255.255");
});

test("ipv4ToNumber rejects malformed addresses", () => {
  assert.equal(ipv4ToNumber("not-an-ip"), null);
  assert.equal(ipv4ToNumber("1.2.3"), null);
  assert.equal(ipv4ToNumber("1.2.3.4.5"), null);
  assert.equal(ipv4ToNumber("1.2.3.256"), null);
  assert.equal(ipv4ToNumber("1.2.3.-1"), null);
  assert.equal(ipv4ToNumber(""), null);
  assert.equal(ipv4ToNumber(undefined), null);
});

test("isPrivateIPv4 accepts only RFC1918 ranges", () => {
  assert.equal(isPrivateIPv4("10.0.0.8"), true);
  assert.equal(isPrivateIPv4("10.255.255.255"), true);
  assert.equal(isPrivateIPv4("172.16.0.1"), true);
  assert.equal(isPrivateIPv4("172.31.255.254"), true);
  assert.equal(isPrivateIPv4("192.168.1.20"), true);

  assert.equal(isPrivateIPv4("172.15.255.255"), false, "just below the 172.16/12 range");
  assert.equal(isPrivateIPv4("172.32.0.1"), false, "just above the 172.16/12 range");
  assert.equal(isPrivateIPv4("192.169.0.1"), false, "not 192.168/16");
  assert.equal(isPrivateIPv4("8.8.8.8"), false, "public address");
  assert.equal(isPrivateIPv4("::1"), false, "IPv6 must be rejected, not crash");
  assert.equal(isPrivateIPv4(""), false);
  assert.equal(isPrivateIPv4(undefined), false);
});

test("isValidPort enforces the 1-65535 range", () => {
  assert.equal(isValidPort(1), true);
  assert.equal(isValidPort(8888), true);
  assert.equal(isValidPort(65535), true);
  assert.equal(isValidPort(0), false);
  assert.equal(isValidPort(65536), false);
  assert.equal(isValidPort(-1), false);
  assert.equal(isValidPort("8888"), true, "numeric strings from form inputs must work");
  assert.equal(isValidPort("not-a-port"), false);
});

test("normalizeNetworkPrefixLength clamps to a sane subnet range", () => {
  assert.equal(normalizeNetworkPrefixLength(24), 24);
  assert.equal(normalizeNetworkPrefixLength(8), 8);
  assert.equal(normalizeNetworkPrefixLength(30), 30);
  assert.equal(normalizeNetworkPrefixLength(7), 0, "below the accepted floor");
  assert.equal(normalizeNetworkPrefixLength(31), 0, "above the accepted ceiling");
  assert.equal(normalizeNetworkPrefixLength("24"), 24, "numeric strings from device-info JSON");
  assert.equal(normalizeNetworkPrefixLength("not-a-number"), 0);
  assert.equal(normalizeNetworkPrefixLength(undefined), 0);
});

test("buildRecoveryHosts covers exactly the /24 usable range, excluding self/network/broadcast", () => {
  const hosts = buildRecoveryHosts("192.168.1.50", 24);
  assert.equal(hosts.length, 253, "254 usable /24 addresses minus the known host itself");
  assert.ok(!hosts.includes("192.168.1.50"), "must not probe the already-known address");
  assert.ok(!hosts.includes("192.168.1.0"), "must not probe the network address");
  assert.ok(!hosts.includes("192.168.1.255"), "must not probe the broadcast address");
  assert.ok(hosts.includes("192.168.1.1"));
  assert.ok(hosts.includes("192.168.1.254"));
});

test("buildRecoveryHosts stays inside the subnet across the /24 boundary", () => {
  const hosts = buildRecoveryHosts("192.168.1.254", 24);
  assert.ok(!hosts.includes("192.168.2.0"), "must not cross into the next /24");
  assert.ok(!hosts.includes("192.168.0.255"), "must not cross into the previous /24");
  assert.equal(hosts.length, 253);
});

test("buildRecoveryHosts widens correctly for a /16 prefix without exploding scope", () => {
  const hosts = buildRecoveryHosts("10.0.0.5", 16);
  // A full /16 sweep (65534 usable hosts) would be far too slow for a LAN
  // recovery probe; the caller is expected to pass a prefix >= 16, and the
  // function must at least stay within that /16 rather than defaulting wider.
  assert.ok(hosts.every((host) => host.startsWith("10.0.")));
  assert.ok(!hosts.includes("10.1.0.1"));
});

test("buildRecoveryHosts returns an empty list for an unparseable host", () => {
  assert.deepEqual(buildRecoveryHosts("not-an-ip", 24), []);
});
