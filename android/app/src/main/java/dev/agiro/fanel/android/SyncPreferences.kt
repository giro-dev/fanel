package dev.agiro.fanel.android

import android.content.Context
import java.io.File

class SyncPreferences(context: Context) {
    private val file = File(context.noBackupFilesDir, "last-successful-sync.txt")

    fun getLastSuccessfulSync(): Long = file.takeIf(File::exists)
        ?.readText()
        ?.toLongOrNull()
        ?: 0L

    fun setLastSuccessfulSync(timestampMillis: Long) {
        file.writeText(timestampMillis.toString())
    }
}
