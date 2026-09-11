package dev.agiro.fanel.android

import android.content.Context
import androidx.room.Room
import dev.agiro.fanel.android.data.CalendarRepository
import dev.agiro.fanel.android.data.local.FanelDatabase
import dev.agiro.fanel.android.data.remote.CalendarApiFactory

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        FanelDatabase::class.java,
        "fanel-android.db"
    ).build()

    val authStore = AuthStore()
    private val syncPreferences = SyncPreferences(context.applicationContext)
    private val calendarApi = CalendarApiFactory.create(authStore)

    val calendarRepository = CalendarRepository(
        api = calendarApi,
        eventDao = database.calendarEventDao(),
        outboxDao = database.outboxDao(),
        syncPreferences = syncPreferences
    )
}
