package io.github.linkwutcreate.localfind.util

import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

data class LocalNetworkInfo(
    val address: String,
    val prefixLength: Int,
    val interfaceName: String = ""
)

object NetworkUtil {
    /**
     * 获取当前手机优先使用的局域网 IPv4 地址和实际子网前缀长度。
     * Wi-Fi 优先于移动数据，避免控制端把可变的移动数据地址当成局域网地址。
     */
    fun getLocalNetworkInfo(): LocalNetworkInfo? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                .filter { it.isUp && !it.isLoopback }
                .sortedBy { interfacePriority(it.name) }

            for (networkInterface in interfaces) {
                if (!networkInterface.isUp || networkInterface.isLoopback) continue
                for (interfaceAddress in networkInterface.interfaceAddresses) {
                    val address = interfaceAddress.address
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        val hostAddress = address.hostAddress ?: continue
                        return LocalNetworkInfo(
                            address = hostAddress,
                            prefixLength = interfaceAddress.networkPrefixLength.toInt(),
                            interfaceName = networkInterface.name
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun getLocalIpAddress(): String? = getLocalNetworkInfo()?.address

    private fun interfacePriority(name: String): Int = when {
        name == "wlan0" || name.startsWith("wlan") -> 0
        name.startsWith("eth") -> 1
        name.startsWith("rmnet") -> 3
        else -> 2
    }
}
