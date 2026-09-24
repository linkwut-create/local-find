package io.github.linkwutcreate.localfind.onboarding

import android.content.Context

/**
 * Tracks whether the vendor-settings onboarding guide has already been shown once, so it
 * auto-opens only the first time the user turns "Keep This Phone Findable" on, while
 * staying reachable afterwards through its own button. Deliberately separate from
 * [io.github.linkwutcreate.localfind.service.ServiceRunState]: that flag controls
 * whether the service should keep running, this one only controls a one-time UI nudge.
 */
object OnboardingPrefs {
    private const val PREFS_NAME = "onboarding_preferences"
    private const val KEY_HAS_SHOWN = "has_shown_vendor_guide"

    fun hasShownGuide(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_HAS_SHOWN, false)

    fun markGuideShown(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HAS_SHOWN, true)
            .apply()
    }
}
