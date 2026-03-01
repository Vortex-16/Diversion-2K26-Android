package com.example.chaintorquenative.di

import com.example.chaintorquenative.mobile.data.api.ChainTorqueApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Primary backend URL (original Render account)
    private const val PRIMARY_URL = "https://chaintorque-backend.onrender.com/"
    // Fallback backend URL (new Render account)
    private const val FALLBACK_URL = "https://chain-torque-backend.onrender.com/"
    
    // Currently active URL (can switch to fallback if primary fails)
    @Volatile
    private var activeBaseUrl: String = PRIMARY_URL

    /**
     * Interceptor that handles automatic fallback to secondary backend
     * if the primary backend is unavailable (connection errors, 5xx errors)
     */
    class FallbackInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val originalUrl = originalRequest.url.toString()
            
            // If we've already switched to fallback, proactively rewrite ALL requests
            val requestToExecute = if (activeBaseUrl == FALLBACK_URL && originalUrl.contains(PRIMARY_URL)) {
                val newUrl = originalUrl.replace(PRIMARY_URL, FALLBACK_URL)
                originalRequest.newBuilder().url(newUrl).build()
            } else {
                originalRequest
            }
            
            return try {
                val response = chain.proceed(requestToExecute)
                
                // If we get a server error (5xx) on primary, try fallback
                if (response.code >= 500 && activeBaseUrl == PRIMARY_URL) {
                    response.close()
                    activeBaseUrl = FALLBACK_URL
                    android.util.Log.d("NetworkModule", "🔄 Switching to fallback backend: $FALLBACK_URL")
                    
                    // Rebuild URL with fallback
                    val newUrl = requestToExecute.url.toString()
                        .replace(PRIMARY_URL, FALLBACK_URL)
                    val fallbackRequest = requestToExecute.newBuilder()
                        .url(newUrl)
                        .build()
                    
                    chain.proceed(fallbackRequest)
                } else {
                    response
                }
            } catch (e: IOException) {
                // Connection error on primary, try fallback
                if (activeBaseUrl == PRIMARY_URL) {
                    activeBaseUrl = FALLBACK_URL
                    android.util.Log.d("NetworkModule", "🔄 Connection failed. Switching to fallback backend: $FALLBACK_URL")
                    
                    val newUrl = requestToExecute.url.toString()
                        .replace(PRIMARY_URL, FALLBACK_URL)
                    val fallbackRequest = requestToExecute.newBuilder()
                        .url(newUrl)
                        .build()
                    
                    chain.proceed(fallbackRequest)
                } else {
                    throw e
                }
            }
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(FallbackInterceptor())
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(PRIMARY_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ChainTorqueApiService {
        return retrofit.create(ChainTorqueApiService::class.java)
    }
}
