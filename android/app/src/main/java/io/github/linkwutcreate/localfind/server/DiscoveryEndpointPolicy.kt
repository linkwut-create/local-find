package io.github.linkwutcreate.localfind.server

/**
 * Policy shared by discovery paths before a LAN endpoint is surfaced to a user.
 * Local Find intentionally accepts only private IPv4 addresses and valid TCP
 * ports; identity is verified separately with /device-info.
 */
object DiscoveryEndpointPolicy {
    fun isPrivateIpv4(host: String): Boolean {
        val octets = host.trim().split('.').map { it.toIntOrNull() }
        if (octets.size != 4 || octets.any { it == null || it !in 0..255 }) return false

        val first = octets[0] ?: return false
        val second = octets[1] ?: return false
        return first == 10 ||
            (first == 172 && second in 16..31) ||
            (first == 192 && second == 168)
    }

    fun isValidPort(candidate: Int): Boolean = candidate in 1..65535

    fun validPort(candidate: Int, fallback: Int = 8888): Int? {
        val port = if (isValidPort(candidate)) candidate else fallback
        return port.takeIf { isValidPort(it) }
    }
}
