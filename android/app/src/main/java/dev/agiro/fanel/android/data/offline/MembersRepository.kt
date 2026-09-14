package dev.agiro.fanel.android.data.offline

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.agiro.fanel.android.data.local.CachedSnapshotDao
import dev.agiro.fanel.android.data.local.PendingOperationDao
import dev.agiro.fanel.android.data.local.PendingOperationEntity
import dev.agiro.fanel.android.data.remote.HouseholdApi
import dev.agiro.fanel.android.data.remote.MemberDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Read-only offline cache of the household members; members are managed from the web UI. */
class MembersRepository(
    private val api: HouseholdApi,
    snapshotDao: CachedSnapshotDao,
    pendingDao: PendingOperationDao,
    pusher: OutboxPusher,
    gson: Gson = Gson()
) : SnapshotRepository<List<MemberDto>>(
    DOMAIN, snapshotDao, pendingDao, pusher, gson, object : TypeToken<List<MemberDto>>() {}
) {

    fun observeMembers(householdId: String): Flow<List<MemberDto>> =
        observe(householdId).map { it.orEmpty() }

    /** Refreshes from the server when reachable and falls back to the cached snapshot otherwise. */
    suspend fun members(householdId: String): List<MemberDto> {
        val cached = cached(householdId)
        return try {
            pull(householdId)
            cached(householdId).orEmpty()
        } catch (exception: Exception) {
            cached ?: throw exception
        }
    }

    override suspend fun pull(householdId: String) {
        write(householdId, api.members(householdId))
    }

    override suspend fun execute(operation: PendingOperationEntity): IdRemap? = null

    companion object {
        const val DOMAIN = "members"
    }
}
