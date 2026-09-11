package dev.agiro.fanel.android

import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import java.util.UUID

class SyncPreferencesTest {
    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun returnsZeroWhenNoSyncFileExists() {
        val householdId = UUID.randomUUID().toString()
        val prefs = SyncPreferences(context)

        assertEquals(0L, prefs.getLastSuccessfulSync(householdId))
    }

    @Test
    fun persistsSyncTimestampPerHousehold() {
        val householdId = UUID.randomUUID().toString()
        val prefs = SyncPreferences(context)

        prefs.setLastSuccessfulSync(householdId, 42L)

        assertEquals(42L, prefs.getLastSuccessfulSync(householdId))
    }

    @Test
    fun returnsZeroWhenFileContentsAreInvalid() {
        val householdId = UUID.randomUUID().toString()
        val prefs = SyncPreferences(context)
        val file = prefs.syncFile(householdId)
        file.writeText("invalid")

        assertEquals(0L, prefs.getLastSuccessfulSync(householdId))
        file.delete()
    }
}
