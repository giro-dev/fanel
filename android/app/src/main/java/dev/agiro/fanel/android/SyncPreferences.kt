package dev.agiro.fanel.android

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.nio.charset.StandardCharsets

class SyncPreferences(context: Context) {
    private val atomicFile = AtomicFile(File(context.noBackupFilesDir, "last-successful-sync.txt"))

    @Synchronized
    fun getLastSuccessfulSync(): Long {
        if (!atomicFile.baseFile.exists()) return 0L
        return atomicFile.openRead().use { input ->
            input.readBytes().toString(StandardCharsets.UTF_8).toLongOrNull()
        } ?: 0L
    }

    @Synchronized
    fun setLastSuccessfulSync(timestampMillis: Long) {
        val output = atomicFile.startWrite()
        try {
            output.write(timestampMillis.toString().toByteArray(StandardCharsets.UTF_8))
            atomicFile.finishWrite(output)
        } catch (exception: Exception) {
            atomicFile.failWrite(output)
            throw exception
        }
    }
}
