package dev.agiro.fanel.android.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingOperationDao {
    @Upsert
    suspend fun upsert(entry: PendingOperationEntity)

    @Query("SELECT * FROM pending_operations WHERE householdId = :householdId AND domain = :domain ORDER BY createdAtEpochMs, rowid")
    suspend fun pendingFor(householdId: String, domain: String): List<PendingOperationEntity>

    @Query("SELECT * FROM pending_operations WHERE householdId = :householdId ORDER BY createdAtEpochMs, rowid")
    suspend fun pendingForHousehold(householdId: String): List<PendingOperationEntity>

    @Query("SELECT COUNT(*) FROM pending_operations WHERE householdId = :householdId AND domain = :domain")
    fun observePendingCount(householdId: String, domain: String): Flow<Int>

    @Query("SELECT * FROM pending_operations WHERE id = :id")
    suspend fun find(id: String): PendingOperationEntity?

    @Query("DELETE FROM pending_operations WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM pending_operations WHERE householdId = :householdId AND domain = :domain AND targetId = :targetId")
    suspend fun deleteByTarget(householdId: String, domain: String, targetId: String)
}
