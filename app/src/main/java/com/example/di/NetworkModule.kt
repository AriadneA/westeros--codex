package com.example.di

import com.example.BuildConfig
import com.example.data.remote.api.IceAndFireApi
import com.example.data.remote.api.ThronesApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IceAndFireRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ThronesRetrofit

/**
 * Módulo Hilt responsável por fornecer clientes HTTP (OkHttp), serialização JSON (Moshi)
 * e instâncias Retrofit separadas para An API of Ice and Fire e ThronesAPI.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

  private const val TIMEOUT_SECONDS = 15L

  @Provides
  @Singleton
  fun provideMoshi(): Moshi {
    return Moshi.Builder()
      .addLast(KotlinJsonAdapterFactory())
      .build()
  }

  @Provides
  @Singleton
  fun provideOkHttpClient(): OkHttpClient {
    val loggingInterceptor =
      HttpLoggingInterceptor().apply {
        level =
          if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
          } else {
            HttpLoggingInterceptor.Level.NONE
          }
      }
    return OkHttpClient.Builder()
      .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
      .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
      .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
      .addInterceptor(loggingInterceptor)
      .build()
  }

  @Provides
  @Singleton
  @IceAndFireRetrofit
  fun provideIceAndFireRetrofit(
    okHttpClient: OkHttpClient,
    moshi: Moshi,
  ): Retrofit {
    return Retrofit.Builder()
      .baseUrl(IceAndFireApi.BASE_URL)
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
  }

  @Provides
  @Singleton
  @ThronesRetrofit
  fun provideThronesRetrofit(
    okHttpClient: OkHttpClient,
    moshi: Moshi,
  ): Retrofit {
    return Retrofit.Builder()
      .baseUrl(ThronesApi.BASE_URL)
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
  }

  @Provides
  @Singleton
  fun provideIceAndFireApi(
    @IceAndFireRetrofit retrofit: Retrofit,
  ): IceAndFireApi = retrofit.create(IceAndFireApi::class.java)

  @Provides
  @Singleton
  fun provideThronesApi(
    @ThronesRetrofit retrofit: Retrofit,
  ): ThronesApi = retrofit.create(ThronesApi::class.java)
}
