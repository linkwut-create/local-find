package com.example.localfind.util

import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtil {
    /**
     * 获取当前手机局域网中的 IP 地址 (IPv4)
     * 过滤正在正常工作且非 Localhost 回环的 Wi-Fi 实网卡 IP
     */
    fun getLocalIpAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (networkInterface in interfaces) {
                if (!networkInterface.isUp || networkInterface.isLoopback) continue
                val addresses = Collections.list(networkInterface.inetAddresses)
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
