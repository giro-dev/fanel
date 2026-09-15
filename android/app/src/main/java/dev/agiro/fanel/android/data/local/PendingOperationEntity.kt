package dev.agiro.fanel.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Domain-agnostic outbox entry. [type] is interpreted by the owning repository ([domain]);
 * [targetId] may reference a locally generated id that gets remapped once the server assigns a real one.
 */
@Entity(tableName = "pending_operations")
data class PendingOperationEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val domain: String,
    val type: String,
    val targetId: String?,
    val payloadJson: String?,
    val createdAtEpochMs: Long
)
