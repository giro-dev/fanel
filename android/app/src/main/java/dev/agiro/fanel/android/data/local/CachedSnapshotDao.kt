package dev.agiro.fanel.android.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedSnapshotDao {
    @Query("SELECT * FROM cached_snapshots WHERE householdId = :householdId AND domain = :domain AND `key` = :key")
    fun observe(householdId: String, domain: String, key: String): Flow<CachedSnapshotEntity?>

    @Query("SELECT * FROM cached_snapshots WHERE householdId = :householdId AND domain = :domain AND `key` = :key")
    suspend fun find(householdId: String, domain: String, key: String): CachedSnapshotEntity?

    @Query("SELECT * FROM cached_snapshots WHERE householdId = :householdId AND domain = :domain")
    suspend fun findAll(householdId: String, domain: String): List<CachedSnapshotEntity>

    @Upsert
    suspend fun upsert(snapshot: CachedSnapshotEntity)

    @Query("DELETE FROM cached_snapshots WHERE householdId = :householdId AND domain = :domain AND `key` = :key")
    suspend fun delete(householdId: String, domain: String, key: String)
}
