package io.github.linkwutcreate.localfind.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VendorGuideTest {

    // --- Vendor detection -------------------------------------------------

    @Test
    fun `detects xiaomi from manufacturer or redmi poco brand`() {
        assertEquals(Vendor.XIAOMI, VendorDetector.detect("Xiaomi", "Xiaomi"))
        assertEquals(Vendor.XIAOMI, VendorDetector.detect("Xiaomi", "Redmi"))
        assertEquals(Vendor.XIAOMI, VendorDetector.detect("Xiaomi", "POCO"))
    }

    @Test
    fun `detects huawei and honor into the same bucket`() {
        assertEquals(Vendor.HUAWEI_HONOR, VendorDetector.detect("HUAWEI", "HUAWEI"))
        assertEquals(Vendor.HUAWEI_HONOR, VendorDetector.detect("HONOR", "HONOR"))
    }

    @Test
    fun `detects oppo including realme sub-brand`() {
        assertEquals(Vendor.OPPO, VendorDetector.detect("OPPO", "OPPO"))
        assertEquals(Vendor.OPPO, VendorDetector.detect("realme", "realme"))
    }

    @Test
    fun `detects vivo including iqoo sub-brand`() {
        assertEquals(Vendor.VIVO, VendorDetector.detect("vivo", "vivo"))
        assertEquals(Vendor.VIVO, VendorDetector.detect("vivo", "iQOO"))
    }

    @Test
    fun `detects meizu`() {
        assertEquals(Vendor.MEIZU, VendorDetector.detect("Meizu", "meizu"))
    }

    @Test
    fun `detects samsung`() {
        assertEquals(Vendor.SAMSUNG, VendorDetector.detect("samsung", "samsung"))
    }

    @Test
    fun `unknown or stock manufacturer falls back to AOSP`() {
        assertEquals(Vendor.AOSP, VendorDetector.detect("Google", "Pixel"))
        assertEquals(Vendor.AOSP, VendorDetector.detect(null, null))
        assertEquals(Vendor.AOSP, VendorDetector.detect("", ""))
    }

    @Test
    fun `detection is case-insensitive`() {
        assertEquals(Vendor.XIAOMI, VendorDetector.detect("XIAOMI", "XIAOMI"))
        assertEquals(Vendor.XIAOMI, VendorDetector.detect("xiaomi", "xiaomi"))
    }

    // --- Step lists ---------------------------------------------------------

    @Test
    fun `vendors with a known autostart screen get two steps ending in battery`() {
        for (vendor in listOf(Vendor.XIAOMI, Vendor.HUAWEI_HONOR, Vendor.OPPO, Vendor.VIVO, Vendor.MEIZU)) {
            val steps = VendorGuide.stepsFor(vendor)
            assertEquals("$vendor should have 2 steps", 2, steps.size)
            assertEquals("autostart", steps[0].id)
            assertTrue("$vendor autostart step should offer at least one jump candidate", steps[0].candidates.isNotEmpty())
            assertEquals("battery", steps[1].id)
        }
    }

    @Test
    fun `samsung gets an autostart step with no jump candidates (safe fallback only)`() {
        val steps = VendorGuide.stepsFor(Vendor.SAMSUNG)
        assertEquals(2, steps.size)
        assertEquals("autostart", steps[0].id)
        assertTrue(steps[0].candidates.isEmpty())
        assertEquals("battery", steps[1].id)
    }

    @Test
    fun `aosp only gets the battery step`() {
        val steps = VendorGuide.stepsFor(Vendor.AOSP)
        assertEquals(1, steps.size)
        assertEquals("battery", steps[0].id)
    }

    // --- Intent fallback logic ----------------------------------------------

    @Test
    fun `picks the first resolvable candidate`() {
        val a = IntentSpec("pkg.a", "A")
        val b = IntentSpec("pkg.b", "B")
        val target = IntentResolver.pickLaunchTarget(listOf(a, b)) { it == b }
        assertEquals(LaunchTarget.VendorScreen(b), target)
    }

    @Test
    fun `falls back to app details when no candidate resolves`() {
        val a = IntentSpec("pkg.a", "A")
        val b = IntentSpec("pkg.b", "B")
        val target = IntentResolver.pickLaunchTarget(listOf(a, b)) { false }
        assertEquals(LaunchTarget.AppDetails, target)
    }

    @Test
    fun `falls back to app details when candidate list is empty`() {
        val target = IntentResolver.pickLaunchTarget(emptyList()) { true }
        assertEquals(LaunchTarget.AppDetails, target)
    }

    @Test
    fun `stops at the first match and ignores later resolvable candidates`() {
        val a = IntentSpec("pkg.a", "A")
        val b = IntentSpec("pkg.b", "B")
        // Both resolve; must still pick the first one (preference order matters).
        val target = IntentResolver.pickLaunchTarget(listOf(a, b)) { true }
        assertEquals(LaunchTarget.VendorScreen(a), target)
    }

    // --- Self-check -----------------------------------------------------------

    @Test
    fun `self check passes only when both service and battery are good`() {
        assertTrue(SelfCheckResult(serviceRunning = true, batteryUnrestricted = true).allGood)
        assertFalse(SelfCheckResult(serviceRunning = false, batteryUnrestricted = true).allGood)
        assertFalse(SelfCheckResult(serviceRunning = true, batteryUnrestricted = false).allGood)
        assertFalse(SelfCheckResult(serviceRunning = false, batteryUnrestricted = false).allGood)
    }
}
