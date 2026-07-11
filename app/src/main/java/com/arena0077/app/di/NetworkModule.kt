package com.arena0077.app.di

import com.arena0077.app.BuildConfig
import com.arena0077.app.data.api.ArenaApi
import com.arena0077.app.data.auth.AuthManager
import com.arena0077.app.data.local.SessionInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt DI module for networking + JSON.
 *
 * Provides:
 *   - Json (kotlinx.serialization config matching arena.ai's expected format)
 *   - OkHttpClient with session cookies and logging
 *   - Retrofit instance pointed at https://arena.ai/
 *   - ArenaApi (the REST surface)
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        coerceInputValues = true
        prettyPrint = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authManager: AuthManager
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(SessionInterceptor(authManager))
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.ARENA_BASE_URL + "/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideArenaApi(retrofit: Retrofit): ArenaApi =
        retrofit.create(ArenaApi::class.java)
}
