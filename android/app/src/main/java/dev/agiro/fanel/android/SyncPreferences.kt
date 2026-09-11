package dev.agiro.fanel.android

import android.content.Context
import java.io.File

class SyncPreferences(context: Context) {
    private val file = File(context.noBackupFilesDir, "last-successful-sync.txt")
    private val tempFile = File(context.noBackupFilesDir, "last-successful-sync.txt.tmp")

    @Synchronized
    fun getLastSuccessfulSync(): Long = file.takeIf(File::exists)
        ?.readText()
        ?.toLongOrNull()
        ?: 0L

    @Synchronized
    fun setLastSuccessfulSync(timestampMillis: Long) {
        tempFile.writeText(timestampMillis.toString())
        if (!tempFile.renameTo(file)) {
            file.writeText(timestampMillis.toString())
            tempFile.delete()
        }
    }
}
