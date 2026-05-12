package com.astfreelancer.flowchat2.data.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    fun createOkHttpClient(
        deviceIdProvider: () -> String,
        timeoutMs: Long = 35_000
    ): OkHttpClient {
        val deviceIdInterceptor = Interceptor { chain ->
            val request = chain.request()
                .newBuilder()
                .addHeader("X-Device-Id", deviceIdProvider())
                .build()
            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .addInterceptor(deviceIdInterceptor)
            .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .build()
    }

    fun createRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://kotlincoroutines.ru/chatapi/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
}
