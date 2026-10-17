package dev.agiro.fanel.android.wireguard

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

data class WgEnrollRequest(val token: String, val publicKey: String, val deviceName: String)
data class WgEnrollResponse(
    val peerId: String,
    val assignedAddress: String,
    val serverPublicKey: String,
    val presharedKey: String?,
    val endpoint: String,
    val allowedIps: List<String>,
    val persistentKeepalive: Int,
    val tunnelBaseUrl: String,
    val lanBaseUrl: String,
)
data class WgServerIdentity(val serverId: String)

interface WgApi {
    @POST("api/v1/wg/enroll")
    suspend fun enroll(@Body request: WgEnrollRequest): WgEnrollResponse

    @GET("api/v1/health/identity")
    suspend fun identity(): WgServerIdentity

    @DELETE("api/v1/wg/peers/me")
    suspend fun revokeMe(@Header("X-Fanel-Wg-Public-Key") publicKey: String): retrofit2.Response<Unit>
}

fun pinnedWgApi(baseUrl: String, pin: String): WgApi {
    val client = okhttp3.OkHttpClient.Builder().pinned(pin).build()
    return Retrofit.Builder()
        .baseUrl(baseUrl.ensureTrailingSlash())
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WgApi::class.java)
}

fun routedWgApi(sharedClient: okhttp3.OkHttpClient): WgApi =
    Retrofit.Builder()
        .baseUrl(LOGICAL_BASE_URL)
        .client(sharedClient.newBuilder().build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WgApi::class.java)

private fun String.ensureTrailingSlash() = if (endsWith('/')) this else "$this/"
