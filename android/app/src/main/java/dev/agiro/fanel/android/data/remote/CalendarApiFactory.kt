package dev.agiro.fanel.android.data.remote

import dev.agiro.fanel.android.AuthStore
import dev.agiro.fanel.android.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiFactory {
    fun calendarApi(baseUrl: String, authStore: AuthStore, sharedClient: OkHttpClient? = null): CalendarApi =
        retrofit(baseUrl, authStore, sharedClient).create(CalendarApi::class.java)

    fun householdApi(baseUrl: String, authStore: AuthStore, sharedClient: OkHttpClient? = null): HouseholdApi =
        retrofit(baseUrl, authStore, sharedClient).create(HouseholdApi::class.java)

    fun recipesApi(baseUrl: String, authStore: AuthStore, sharedClient: OkHttpClient? = null): RecipesApi =
        retrofit(baseUrl, authStore, sharedClient).create(RecipesApi::class.java)

    fun menuApi(baseUrl: String, authStore: AuthStore, sharedClient: OkHttpClient? = null): MenuApi =
        retrofit(baseUrl, authStore, sharedClient).create(MenuApi::class.java)

    fun shoppingApi(baseUrl: String, authStore: AuthStore, sharedClient: OkHttpClient? = null): ShoppingApi =
        retrofit(baseUrl, authStore, sharedClient).create(ShoppingApi::class.java)

    fun choresApi(baseUrl: String, authStore: AuthStore, sharedClient: OkHttpClient? = null): ChoresApi =
        retrofit(baseUrl, authStore, sharedClient).create(ChoresApi::class.java)

    fun assistantApi(baseUrl: String, authStore: AuthStore, sharedClient: OkHttpClient? = null): AssistantApi =
        retrofit(
            baseUrl,
            authStore,
            (sharedClient ?: okHttpClient(authStore)).newBuilder()
                .readTimeout(ASSISTANT_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(ASSISTANT_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build()
        ).create(AssistantApi::class.java)

    fun okHttpClient(authStore: AuthStore): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val builder = chain.request().newBuilder()
            authStore.apply(builder)
            chain.proceed(builder.build())
        }
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
        )
        .build()

    fun authInterceptor(authStore: AuthStore): okhttp3.Interceptor = okhttp3.Interceptor { chain ->
        val builder = chain.request().newBuilder()
        authStore.apply(builder)
        chain.proceed(builder.build())
    }

    fun loggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

    private fun retrofit(
        baseUrl: String,
        authStore: AuthStore,
        sharedClient: OkHttpClient? = null,
        client: OkHttpClient = sharedClient?.newBuilder()?.build() ?: okHttpClient(authStore)
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private const val ASSISTANT_READ_TIMEOUT_SECONDS = 120L
    private const val ASSISTANT_WRITE_TIMEOUT_SECONDS = 60L
}
