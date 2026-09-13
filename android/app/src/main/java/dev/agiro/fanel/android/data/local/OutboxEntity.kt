package dev.agiro.fanel.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outbox")
data class OutboxEntity(
    @PrimaryKey val localId: String,
    val householdId: String,
    val remoteId: String?,
    val operation: String,
    val payloadJson: String?,
    val createdAtEpochMs: Long
) {
    companion object {
        const val OP_CREATE = "CREATE"
        const val OP_UPDATE = "UPDATE"
        const val OP_DELETE = "DELETE"
    }
}
