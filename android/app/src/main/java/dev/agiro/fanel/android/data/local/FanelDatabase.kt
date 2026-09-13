package dev.agiro.fanel.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CalendarEventEntity::class, OutboxEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FanelDatabase : RoomDatabase() {
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun outboxDao(): OutboxDao
}
