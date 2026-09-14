package dev.agiro.fanel.android.data.local

import androidx.room.Entity

/**
 * Generic JSON snapshot of a remote collection (members, recipes, shopping lists, ...)
 * keyed by household, domain and an optional sub-key (e.g. the ISO week for menus).
 */
@Entity(tableName = "cached_snapshots", primaryKeys = ["householdId", "domain", "key"])
data class CachedSnapshotEntity(
    val householdId: String,
    val domain: String,
    val key: String,
    val json: String,
    val updatedAtEpochMs: Long
)
