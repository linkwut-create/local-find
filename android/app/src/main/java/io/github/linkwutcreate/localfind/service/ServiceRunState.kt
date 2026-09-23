package io.github.linkwutcreate.localfind.service

import android.content.Context

/**
 * Persists the user's explicit choice to keep the finder service available.
 *
 * This is intentionally separate from the pairing stores: pairing authorizes a
 * controller, while this flag controls whether the phone should keep trying to
 * host the local service. Tapping Stop Service clears it.
 */
object ServiceRunState {
    private const val PREFS_NAME = "service_preferences"
    private const val KEY_SHOULD_RUN = "should_run"

    fun shouldRun(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHOULD_RUN, false)

    fun setShouldRun(context: Context, shouldRun: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SHOULD_RUN, shouldRun)
            .apply()
    }
}
