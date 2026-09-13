package dev.agiro.fanel.android.sync

import dev.agiro.fanel.android.AuthStore
import dev.agiro.fanel.android.data.remote.ApiFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class SseHouseholdEvents(baseUrl: String, authStore: AuthStore) : HouseholdEvents {
    private val eventsUrl = "${baseUrl.trimEnd('/')}/api/v1/events"
    private val client = ApiFactory.okHttpClient(authStore).newBuilder()
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    override fun observe(householdId: String): Flow<String> = channelFlow {
        val callRef = AtomicReference<Call?>(null)
        val job = launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val call = client.newCall(
                        Request.Builder()
                            .url("$eventsUrl?household=$householdId")
                            .header("Accept", "text/event-stream")
                            .build()
                    )
                    callRef.set(call)
                    call.execute().use { response ->
                        val parser = SseLineParser()
                        val source = response.body?.source() ?: return@use
                        while (isActive) {
                            val line = source.readUtf8Line() ?: break
                            parser.accept(line)?.let { send(it) }
                        }
                    }
                } catch (_: IOException) {
                    // Dropped connection or cancelled call; reconnect unless closing.
                } finally {
                    callRef.set(null)
                }
                delay(RETRY_DELAY_MS)
            }
        }
        awaitClose {
            callRef.get()?.cancel()
            job.cancel()
        }
    }

    private companion object {
        const val RETRY_DELAY_MS = 3_000L
    }
}
