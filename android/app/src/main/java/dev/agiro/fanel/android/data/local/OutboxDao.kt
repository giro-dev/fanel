package dev.agiro.fanel.android.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface OutboxDao {
    @Upsert
    suspend fun upsert(entry: OutboxEntity)

    @Query("SELECT * FROM outbox WHERE householdId = :householdId ORDER BY createdAtEpochMs")
    suspend fun pendingForHousehold(householdId: String): List<OutboxEntity>

    @Query("DELETE FROM outbox WHERE localId = :localId")
    suspend fun delete(localId: String)
}
