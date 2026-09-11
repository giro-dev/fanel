package dev.agiro.fanel.android

import android.content.Context

class SyncPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("sync", Context.MODE_PRIVATE)

    fun getLastSuccessfulSync(): Long = prefs.getLong(KEY_LAST_SYNC, 0L)

    fun setLastSuccessfulSync(timestampMillis: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC, timestampMillis).apply()
    }

    private companion object {
        const val KEY_LAST_SYNC = "last_successful_sync"
    }
}
