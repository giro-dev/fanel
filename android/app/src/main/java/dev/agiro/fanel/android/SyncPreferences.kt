package dev.agiro.fanel.android

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class SyncPreferences(context: Context) {
    private val baseDir = context.noBackupFilesDir

    @Synchronized
    fun getLastSuccessfulSync(householdId: String): Long {
        val atomicFile = atomicFile(householdId)
        if (!atomicFile.baseFile.exists()) return 0L
        return atomicFile.openRead().use { input ->
            input.readBytes().toString(StandardCharsets.UTF_8).toLongOrNull()
        } ?: 0L
    }

    @Synchronized
    fun setLastSuccessfulSync(householdId: String, timestampMillis: Long) {
        val atomicFile = atomicFile(householdId)
        val output = atomicFile.startWrite()
        try {
            output.write(timestampMillis.toString().toByteArray(StandardCharsets.UTF_8))
            atomicFile.finishWrite(output)
        } catch (exception: Exception) {
            atomicFile.failWrite(output)
            throw exception
        }
    }

    internal fun syncFile(householdId: String): File =
        File(baseDir, "last-successful-sync-${householdKey(householdId)}.txt")

    private fun atomicFile(householdId: String): AtomicFile = AtomicFile(syncFile(householdId))

    private fun householdKey(householdId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(householdId.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(16)
    }
}
