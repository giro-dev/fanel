package dev.agiro.fanel.android.sync

import dev.agiro.fanel.android.data.CalendarRepositoryContract
import dev.agiro.fanel.android.data.offline.OfflineSyncable
import kotlinx.coroutines.CancellationException

interface HouseholdSyncContract {
    /** Pushes every pending local change and refreshes all offline caches of the household. */
    suspend fun syncAll(householdId: String, from: String, to: String)
}

/**
 * Runs every offline-first domain in turn so a failure in one does not prevent the others from
 * refreshing; the first failure is rethrown afterwards so WorkManager can retry.
 */
class HouseholdSync(
    private val calendarRepository: CalendarRepositoryContract,
    private val domains: List<OfflineSyncable>
) : HouseholdSyncContract {
    override suspend fun syncAll(householdId: String, from: String, to: String) {
        var failure: Exception? = null
        for (domain in domains) {
            try {
                domain.sync(householdId)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                failure = failure ?: exception
            }
        }
        try {
            calendarRepository.fullSync(householdId, from, to)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            failure = failure ?: exception
        }
        failure?.let { throw it }
    }
}
