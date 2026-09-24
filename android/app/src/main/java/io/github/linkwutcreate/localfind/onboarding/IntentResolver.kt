package io.github.linkwutcreate.localfind.onboarding

/** Where the "jump to settings" button for a step actually lands. */
sealed class LaunchTarget {
    data class VendorScreen(val spec: IntentSpec) : LaunchTarget()
    object AppDetails : LaunchTarget()
}

/**
 * Decides which candidate Activity (if any) to launch for a step, kept independent of
 * Android's PackageManager/Context so this fallback decision is plain-JVM unit testable.
 * The caller (Activity layer) supplies `canResolve`, backed by
 * `Intent.resolveActivity(packageManager)` or `queryIntentActivities`, and separately
 * performs the actual `startActivity` call, catching `ActivityNotFoundException` /
 * `SecurityException` as a second, defense-in-depth fallback even when `canResolve`
 * claimed a target existed (some OEMs make a component resolvable but still refuse to
 * launch it from a third-party app).
 */
object IntentResolver {
    fun pickLaunchTarget(
        candidates: List<IntentSpec>,
        canResolve: (IntentSpec) -> Boolean,
    ): LaunchTarget {
        val match = candidates.firstOrNull(canResolve)
        return if (match != null) LaunchTarget.VendorScreen(match) else LaunchTarget.AppDetails
    }
}

/** Result of the "I've set it up, test it now" self-check button. */
data class SelfCheckResult(
    val serviceRunning: Boolean,
    val batteryUnrestricted: Boolean,
) {
    val allGood: Boolean get() = serviceRunning && batteryUnrestricted
}
