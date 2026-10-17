package dev.agiro.fanel.android

import android.content.Context
import androidx.room.Room
import dev.agiro.fanel.android.data.CalendarRepository
import dev.agiro.fanel.android.data.CalendarRepositoryContract
import dev.agiro.fanel.android.data.local.FanelDatabase
import dev.agiro.fanel.android.data.offline.ChoresRepository
import dev.agiro.fanel.android.data.offline.MembersRepository
import dev.agiro.fanel.android.data.offline.MenuRepository
import dev.agiro.fanel.android.data.offline.OutboxPusher
import dev.agiro.fanel.android.data.offline.RecipesRepository
import dev.agiro.fanel.android.data.offline.ShoppingRepository
import dev.agiro.fanel.android.data.remote.ApiFactory
import dev.agiro.fanel.android.data.remote.AssistantApi
import dev.agiro.fanel.android.data.remote.ChoresApi
import dev.agiro.fanel.android.data.remote.HouseholdApi
import dev.agiro.fanel.android.data.remote.MenuApi
import dev.agiro.fanel.android.data.remote.RecipesApi
import dev.agiro.fanel.android.data.remote.ShoppingApi
import dev.agiro.fanel.android.sync.HouseholdEvents
import dev.agiro.fanel.android.sync.HouseholdSync
import dev.agiro.fanel.android.sync.HouseholdSyncContract
import dev.agiro.fanel.android.sync.SseHouseholdEvents
import dev.agiro.fanel.android.wireguard.AndroidNetworkMonitor
import dev.agiro.fanel.android.wireguard.BaseUrlInterceptor
import dev.agiro.fanel.android.wireguard.ConnectionMode
import dev.agiro.fanel.android.wireguard.ConnectionModeResolver
import dev.agiro.fanel.android.wireguard.ConnectionState
import dev.agiro.fanel.android.wireguard.ConnectionStateHolder
import dev.agiro.fanel.android.wireguard.IdentityProber
import dev.agiro.fanel.android.wireguard.LOGICAL_BASE_URL
import dev.agiro.fanel.android.wireguard.RetryOnModeChangeInterceptor
import dev.agiro.fanel.android.wireguard.TunnelController
import dev.agiro.fanel.android.wireguard.WgConfigRepository
import dev.agiro.fanel.android.wireguard.WgTunnelManager
import dev.agiro.fanel.android.wireguard.DataStoreWgConfigRepository
import dev.agiro.fanel.android.wireguard.routedWgApi
import dev.agiro.fanel.android.wireguard.pinnedWgApi
import dev.agiro.fanel.android.wireguard.pinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) : AppContainerContract {
    override val sessionStore = SessionStore(context.applicationContext)
    override val authStore = AuthStore(context.applicationContext)
    override val wgConfigRepository: WgConfigRepository = DataStoreWgConfigRepository(context.applicationContext)
    override val connectionStateHolder = ConnectionStateHolder()
    override val tunnelController: TunnelController = WgTunnelManager(context.applicationContext)
    private val applicationContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var resolver: ConnectionModeResolver? = null
    private var sharedPairedClient: OkHttpClient? = null

    init {
        if (wgConfigRepository.current != null) {
            sessionStore.enterPairedMode()
            startResolver()
        }
        appScope.launch {
            var previousMode = connectionStateHolder.state.value.mode
            connectionStateHolder.state.collect { state ->
                if (state.mode != previousMode) {
                    sharedPairedClient?.dispatcher?.cancelAll()
                    sharedPairedClient?.connectionPool?.evictAll()
                    previousMode = state.mode
                }
            }
        }
    }

    private val database = Room.databaseBuilder(
        context.applicationContext,
        FanelDatabase::class.java,
        "fanel-android.db"
    ).addMigrations(FanelDatabase.MIGRATION_1_2, FanelDatabase.MIGRATION_2_3, FanelDatabase.MIGRATION_3_4).build()

    private val syncPreferences = SyncPreferences(context.applicationContext)

    private val pusher = OutboxPusher(database.pendingOperationDao())

    private var _householdApi: HouseholdApi = buildHouseholdApi()
    override val householdApi: HouseholdApi
        get() = _householdApi

    private var _recipesApi: RecipesApi = buildRecipesApi()
    override val recipesApi: RecipesApi
        get() = _recipesApi

    private var _menuApi: MenuApi = buildMenuApi()
    override val menuApi: MenuApi
        get() = _menuApi

    private var _shoppingApi: ShoppingApi = buildShoppingApi()
    override val shoppingApi: ShoppingApi
        get() = _shoppingApi

    private var _choresApi: ChoresApi = buildChoresApi()
    override val choresApi: ChoresApi
        get() = _choresApi

    private var _assistantApi: AssistantApi = buildAssistantApi()
    override val assistantApi: AssistantApi
        get() = _assistantApi

    private var _householdEvents: HouseholdEvents = buildHouseholdEvents()
    override val householdEvents: HouseholdEvents
        get() = _householdEvents

    private var _calendarRepository: CalendarRepositoryContract = buildCalendarRepository()
    override val calendarRepository: CalendarRepositoryContract
        get() = _calendarRepository

    private var offline: OfflineRepositories = buildOfflineRepositories()
    override val membersRepository: MembersRepository
        get() = offline.members
    override val recipesRepository: RecipesRepository
        get() = offline.recipes
    override val menuRepository: MenuRepository
        get() = offline.menu
    override val shoppingRepository: ShoppingRepository
        get() = offline.shopping
    override val choresRepository: ChoresRepository
        get() = offline.chores
    override val householdSync: HouseholdSyncContract
        get() = offline.sync

    override fun refreshConnection() {
        _householdApi = buildHouseholdApi()
        _recipesApi = buildRecipesApi()
        _menuApi = buildMenuApi()
        _shoppingApi = buildShoppingApi()
        _choresApi = buildChoresApi()
        _assistantApi = buildAssistantApi()
        _householdEvents = buildHouseholdEvents()
        _calendarRepository = buildCalendarRepository()
        offline = buildOfflineRepositories()
    }

    override fun enterPairedMode() {
        sessionStore.enterPairedMode()
        refreshConnection()
    }

    override fun startConnectionResolution() {
        startResolver()
    }

    override fun unpair() {
        resolver?.stop()
        resolver = null
        sharedPairedClient?.dispatcher?.cancelAll()
        sharedPairedClient?.connectionPool?.evictAll()
        sharedPairedClient = null
        sessionStore.exitPairedMode()
        connectionStateHolder.set(ConnectionState(ConnectionMode.UNKNOWN))
        refreshConnection()
    }

    override fun onForeground() {
        resolver?.onForeground()
    }

    override fun onBackground() {
        resolver?.onBackground()
    }

    override suspend fun revokeWgPeer(publicKey: String) {
        val client = pairedClient() ?: return
        routedWgApi(client).revokeMe(publicKey)
    }

    private fun startResolver() {
        if (resolver != null) return
        resolver = ConnectionModeResolver(
            network = AndroidNetworkMonitor(applicationContext),
            prober = IdentityProber { url, pin -> pinnedWgApi(url, pin).identity().serverId },
            tunnel = tunnelController,
            config = { wgConfigRepository.current },
            holder = connectionStateHolder,
            scope = appScope,
        )
    }

    @Synchronized
    private fun pairedClient(): OkHttpClient? {
        if (sessionStore.serverUrl != LOGICAL_BASE_URL) return null
        sharedPairedClient?.let { return it }
        val config = wgConfigRepository.current ?: return null
        return OkHttpClient.Builder()
            .addInterceptor(RetryOnModeChangeInterceptor(connectionStateHolder))
            .addInterceptor(BaseUrlInterceptor(connectionStateHolder) { wgConfigRepository.current })
            .addInterceptor(ApiFactory.authInterceptor(authStore))
            .addInterceptor(ApiFactory.loggingInterceptor())
            .connectTimeout(5, TimeUnit.SECONDS)
            .pinned(config.spkiSha256)
            .build()
            .also { sharedPairedClient = it }
    }

    private class OfflineRepositories(
        val members: MembersRepository,
        val recipes: RecipesRepository,
        val menu: MenuRepository,
        val shopping: ShoppingRepository,
        val chores: ChoresRepository,
        val sync: HouseholdSyncContract
    )

    private fun buildOfflineRepositories(): OfflineRepositories {
        val snapshotDao = database.cachedSnapshotDao()
        val pendingDao = database.pendingOperationDao()
        val members = MembersRepository(_householdApi, snapshotDao, pendingDao, pusher)
        val recipes = RecipesRepository(_recipesApi, snapshotDao, pendingDao, pusher)
        val menu = MenuRepository(_menuApi, snapshotDao, pendingDao, pusher)
        val shopping = ShoppingRepository(_shoppingApi, snapshotDao, pendingDao, pusher)
        val chores = ChoresRepository(_choresApi, snapshotDao, pendingDao, pusher)
        return OfflineRepositories(
            members, recipes, menu, shopping, chores,
            HouseholdSync(_calendarRepository, listOf(members, recipes, menu, shopping, chores))
        )
    }

    private fun buildHouseholdApi(): HouseholdApi =
        ApiFactory.householdApi(sessionStore.serverUrl, authStore, pairedClient())

    private fun buildRecipesApi(): RecipesApi =
        ApiFactory.recipesApi(sessionStore.serverUrl, authStore, pairedClient())

    private fun buildMenuApi(): MenuApi =
        ApiFactory.menuApi(sessionStore.serverUrl, authStore, pairedClient())

    private fun buildShoppingApi(): ShoppingApi =
        ApiFactory.shoppingApi(sessionStore.serverUrl, authStore, pairedClient())

    private fun buildChoresApi(): ChoresApi =
        ApiFactory.choresApi(sessionStore.serverUrl, authStore, pairedClient())

    private fun buildAssistantApi(): AssistantApi =
        ApiFactory.assistantApi(sessionStore.serverUrl, authStore, pairedClient())

    private fun buildHouseholdEvents(): HouseholdEvents =
        SseHouseholdEvents(sessionStore.serverUrl, authStore, pairedClient())

    private fun buildCalendarRepository(): CalendarRepositoryContract = CalendarRepository(
        api = ApiFactory.calendarApi(sessionStore.serverUrl, authStore, pairedClient()),
        eventDao = database.calendarEventDao(),
        pusher = pusher,
        syncPreferences = syncPreferences
    )
}
