package io.github.linkwutcreate.localfind.onboarding

/**
 * OEM battery-management / auto-start restrictions are the #1 real-world cause of the
 * background finder service getting killed. This maps a manufacturer/brand string to a
 * small set of known vendor settings screens, so the first-run onboarding flow can jump
 * the user straight to the relevant page instead of a generic "background permission"
 * paragraph. Every vendor screen is reached through a public, unprivileged Intent (no new
 * permission is requested) and every jump has a working fallback.
 */
enum class Vendor {
    XIAOMI,
    HUAWEI_HONOR,
    OPPO,
    VIVO,
    MEIZU,
    SAMSUNG,
    AOSP,
}

/** One candidate Activity to try launching, in preference order. */
data class IntentSpec(
    val packageName: String,
    val className: String,
)

/** One onboarding step: guidance text plus (optionally empty) vendor jump candidates. */
data class OnboardingStep(
    val id: String,
    val titleKey: String,
    val descriptionKey: String,
    /** Tried in order; first one that resolves is launched. Empty means "no direct jump". */
    val candidates: List<IntentSpec>,
)

object VendorDetector {
    /**
     * Buckets a manufacturer/brand pair into one of the OEMs the onboarding flow has
     * copy and (where possible) a direct settings jump for. Sub-brands are folded into
     * their parent OEM's settings surface: Redmi/POCO -> Xiaomi (MIUI/HyperOS security
     * center), Honor -> Huawei (they only forked apart after 2020 and largely still ship
     * the same "protected apps" surface under com.huawei.systemmanager or its
     * com.hihonor.* successor), Realme -> OPPO (ColorOS/ColorOS-derived), iQOO -> vivo
     * (same Funtouch/OriginOS security center). Unrecognized manufacturers (Pixel,
     * generic AOSP builds, emulators, ...) fall back to AOSP, which has no vendor-specific
     * step because stock Android's Doze/App Standby is the only mechanism involved.
     */
    fun detect(manufacturer: String?, brand: String?): Vendor {
        val m = manufacturer.orEmpty().lowercase()
        val b = brand.orEmpty().lowercase()
        val hay = "$m $b"
        return when {
            hay.contains("xiaomi") || hay.contains("redmi") || hay.contains("poco") -> Vendor.XIAOMI
            hay.contains("huawei") || hay.contains("honor") -> Vendor.HUAWEI_HONOR
            hay.contains("oppo") || hay.contains("realme") -> Vendor.OPPO
            hay.contains("vivo") || hay.contains("iqoo") -> Vendor.VIVO
            hay.contains("meizu") -> Vendor.MEIZU
            hay.contains("samsung") -> Vendor.SAMSUNG
            else -> Vendor.AOSP
        }
    }
}

object VendorGuide {
    private const val STEP_AUTOSTART = "autostart"
    private const val STEP_BATTERY = "battery"

    /** Battery-optimization exemption applies to every vendor; always the last step. */
    private val batteryStep = OnboardingStep(
        id = STEP_BATTERY,
        titleKey = "onboard_battery_title",
        descriptionKey = "onboard_battery_desc",
        candidates = emptyList(), // launched via the existing ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS flow
    )

    fun stepsFor(vendor: Vendor): List<OnboardingStep> {
        val autostart = autostartStep(vendor)
        return if (autostart != null) listOf(autostart, batteryStep) else listOf(batteryStep)
    }

    private fun autostartStep(vendor: Vendor): OnboardingStep? = when (vendor) {
        Vendor.XIAOMI -> OnboardingStep(
            id = STEP_AUTOSTART,
            titleKey = "onboard_autostart_xiaomi_title",
            descriptionKey = "onboard_autostart_xiaomi_desc",
            candidates = listOf(
                IntentSpec("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
                IntentSpec("com.miui.securitycenter", "com.miui.appmanager.ApplicationsDetailsActivity"),
            ),
        )
        Vendor.HUAWEI_HONOR -> OnboardingStep(
            id = STEP_AUTOSTART,
            titleKey = "onboard_autostart_huawei_title",
            descriptionKey = "onboard_autostart_huawei_desc",
            candidates = listOf(
                IntentSpec("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                IntentSpec("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
                IntentSpec("com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            ),
        )
        Vendor.OPPO -> OnboardingStep(
            id = STEP_AUTOSTART,
            titleKey = "onboard_autostart_oppo_title",
            descriptionKey = "onboard_autostart_oppo_desc",
            candidates = listOf(
                IntentSpec("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                IntentSpec("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
                IntentSpec("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
            ),
        )
        Vendor.VIVO -> OnboardingStep(
            id = STEP_AUTOSTART,
            titleKey = "onboard_autostart_vivo_title",
            descriptionKey = "onboard_autostart_vivo_desc",
            candidates = listOf(
                IntentSpec("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
                IntentSpec("com.iqoo.secure", "com.iqoo.secure.safeguard.PurviewTabActivity"),
            ),
        )
        Vendor.MEIZU -> OnboardingStep(
            id = STEP_AUTOSTART,
            titleKey = "onboard_autostart_meizu_title",
            descriptionKey = "onboard_autostart_meizu_desc",
            candidates = listOf(
                IntentSpec("com.meizu.safe", "com.meizu.safe.permission.SmartBGActivity"),
                IntentSpec("com.meizu.safe", "com.meizu.safe.security.SHOW_APPSEC"),
            ),
        )
        // Samsung's OneUI "Sleeping apps" list has no stable public component across One
        // UI versions/regions; guessing one risks silently opening the wrong screen
        // (which would resolve and so never trigger the app-details fallback). Ship
        // honest copy instead, with an empty candidate list so the jump button lands
        // safely on the app-details page (the documented, always-correct fallback).
        Vendor.SAMSUNG -> OnboardingStep(
            id = STEP_AUTOSTART,
            titleKey = "onboard_autostart_samsung_title",
            descriptionKey = "onboard_autostart_samsung_desc",
            candidates = emptyList(),
        )
        Vendor.AOSP -> null
    }
}
