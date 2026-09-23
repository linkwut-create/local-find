package io.github.linkwutcreate.localfind.server

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryEndpointPolicyTest {
    @Test
    fun onlyListeningStatusIsPresentedAsReachable() {
        assertFalse(ServerStatus.STOPPED.isListening())
        assertFalse(ServerStatus.STARTING.isListening())
        assertTrue(ServerStatus.LISTENING.isListening())
        assertFalse(ServerStatus.FAILED.isListening())
    }

    @Test
    fun acceptsOnlyPrivateIpv4Ranges() {
        assertTrue(DiscoveryEndpointPolicy.isPrivateIpv4("10.0.0.8"))
        assertTrue(DiscoveryEndpointPolicy.isPrivateIpv4("172.31.255.254"))
        assertTrue(DiscoveryEndpointPolicy.isPrivateIpv4("192.168.1.20"))
        assertFalse(DiscoveryEndpointPolicy.isPrivateIpv4("172.32.0.1"))
        assertFalse(DiscoveryEndpointPolicy.isPrivateIpv4("8.8.8.8"))
        assertFalse(DiscoveryEndpointPolicy.isPrivateIpv4("::1"))
    }

    @Test
    fun keepsValidPortOrUsesSafeFallback() {
        assertTrue(DiscoveryEndpointPolicy.isValidPort(8888))
        assertFalse(DiscoveryEndpointPolicy.isValidPort(0))
        assertFalse(DiscoveryEndpointPolicy.isValidPort(65536))
        assertEquals(8888, DiscoveryEndpointPolicy.validPort(8888))
        assertEquals(9999, DiscoveryEndpointPolicy.validPort(0, 9999))
        assertEquals(null, DiscoveryEndpointPolicy.validPort(0, 0))
        assertEquals(null, DiscoveryEndpointPolicy.validPort(65536, 70000))
    }

    @Test
    fun sameVerifiedDeviceIdAllowsAddressRecovery() {
        assertTrue(IdentityBindingPolicy.canTransferTrust("phone-a", " phone-a ", true))
    }

    @Test
    fun sameVerifiedDeviceIdAllowsAChangedValidPortOnlyAfterProof() {
        assertTrue(DiscoveryEndpointPolicy.validPort(8899) != null)
        assertTrue(IdentityBindingPolicy.canTransferTrust("phone-a", "phone-a", true))
        assertFalse(IdentityBindingPolicy.canTransferTrust("phone-a", "phone-a", false))
    }

    @Test
    fun sameDeviceIdWithoutFreshVerificationCannotTransferTrust() {
        assertFalse(IdentityBindingPolicy.canTransferTrust("phone-a", "phone-a", false))
    }

    @Test
    fun differentDeviceIdNeverTransfersTrustAtOldOrNewAddress() {
        assertFalse(IdentityBindingPolicy.canTransferTrust("phone-a", "phone-b", true))
        assertFalse(IdentityBindingPolicy.canTransferTrust("phone-a", "phone-b", false))
    }

    @Test
    fun blankOrUnknownIdentityCannotTransferTrust() {
        assertFalse(IdentityBindingPolicy.canTransferTrust("phone-a", "", true))
        assertFalse(IdentityBindingPolicy.canTransferTrust("", "phone-a", true))
        assertFalse(IdentityBindingPolicy.canTransferTrust(null, "phone-a", true))
    }

    @Test
    fun legacyHostPortRecordHasNoIdentityToBind() {
        assertEquals(null, IdentityBindingPolicy.normalizeDeviceId("  "))
        assertFalse(IdentityBindingPolicy.matches(null, "phone-a"))
    }
}
