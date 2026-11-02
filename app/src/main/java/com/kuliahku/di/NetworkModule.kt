package com.kuliahku.di

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.kuliahku.BuildConfig
import com.kuliahku.data.remote.openalex.OpenAlexApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    @Provides
    @Singleton
    fun provideOpenAlexApi(client: OkHttpClient, json: Json): OpenAlexApi =
        Retrofit.Builder()
            .baseUrl("https://api.openalex.org/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenAlexApi::class.java)

    @Provides
    @Singleton
    fun provideS3Client(): AmazonS3 {
        val accessKey = BuildConfig.R2_ACCESS_KEY
        val secretKey = BuildConfig.R2_SECRET_KEY
        val accountId = BuildConfig.R2_ACCOUNT_ID

        android.util.Log.d("NetworkModule", "S3 Init - Account ID: $accountId")

        val credentials = BasicAWSCredentials(accessKey, secretKey)
        
        // Konfigurasi untuk menonaktifkan fitur AWS yang tidak didukung R2
        val configuration = ClientConfiguration().apply {
            maxErrorRetry = 3
            connectionTimeout = 30000
            socketTimeout = 30000
        }
        
        // Mematikan 'Expect: 100-continue' secara global untuk SDK AWS
        System.setProperty("com.amazonaws.sdk.disableExpectContinue", "true")

        val client = AmazonS3Client(credentials, configuration)

        // R2 Wajib: PathStyleAccess dan DisableChunkedEncoding
        client.setS3ClientOptions(
            com.amazonaws.services.s3.S3ClientOptions.builder()
                .setPathStyleAccess(true)
                .disableChunkedEncoding()
                .build()
        )

        if (!accountId.isNullOrBlank()) {
            val endpoint = "https://$accountId.r2.cloudflarestorage.com"
            try {
                // R2 menggunakan us-east-1 untuk kompatibilitas signature
                client.setRegion(Region.getRegion(Regions.US_EAST_1))
                client.setEndpoint(endpoint)
                android.util.Log.d("NetworkModule", "S3 R2 Configured: $endpoint")
            } catch (e: Exception) {
                android.util.Log.e("NetworkModule", "S3 Endpoint Error", e)
            }
        }

        return client
    }
}