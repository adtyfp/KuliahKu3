
package com.kuliahku.di

import android.util.Log
import com.amazonaws.ClientConfiguration
import com.amazonaws.Request
import com.amazonaws.auth.AWS4Signer
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.SignerFactory
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.S3ClientOptions
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

/**
 * Custom signer untuk Cloudflare R2.
 *
 * Menambahkan x-amz-content-sha256 sebelum signing
 * dan menggunakan UNSIGNED-PAYLOAD.
 */
class CloudflareR2Signer : AWS4Signer() {

    override fun sign(
        request: Request<*>?,
        credentials: AWSCredentials?
    ) {
        // Pastikan header tersedia sebelum proses signing
        request?.addHeader(
            "x-amz-content-sha256",
            "UNSIGNED-PAYLOAD"
        )

        // Jalankan AWS Signature V4
        super.sign(request, credentials)
    }

    override fun calculateContentHash(
        request: Request<*>?
    ): String {
        return "UNSIGNED-PAYLOAD"
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // =====================================================
    // JSON
    // =====================================================

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    // =====================================================
    // OKHTTP
    // =====================================================

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .build()

    // =====================================================
    // OPENALEX API
    // =====================================================

    @Provides
    @Singleton
    fun provideOpenAlexApi(
        client: OkHttpClient,
        json: Json
    ): OpenAlexApi =
        Retrofit.Builder()
            .baseUrl("https://api.openalex.org/")
            .client(client)
            .addConverterFactory(
                json.asConverterFactory(
                    "application/json".toMediaType()
                )
            )
            .build()
            .create(OpenAlexApi::class.java)

    // =====================================================
    // CLOUDFLARE R2
    // =====================================================

    @Provides
    @Singleton
    fun provideS3Client(): AmazonS3 {

        val accessKey = BuildConfig.R2_ACCESS_KEY
        val secretKey = BuildConfig.R2_SECRET_KEY
        val accountId = BuildConfig.R2_ACCOUNT_ID

        require(accessKey.isNotBlank()) {
            "R2_ACCESS_KEY belum dikonfigurasi"
        }

        require(secretKey.isNotBlank()) {
            "R2_SECRET_KEY belum dikonfigurasi"
        }

        require(accountId.isNotBlank()) {
            "R2_ACCOUNT_ID belum dikonfigurasi"
        }

        val credentials = BasicAWSCredentials(
            accessKey,
            secretKey
        )

        // Registrasi custom signer
        SignerFactory.registerSigner(
            "CloudflareR2Signer",
            CloudflareR2Signer::class.java
        )

        val configuration = ClientConfiguration().apply {
            maxErrorRetry = 3
            connectionTimeout = 30000
            socketTimeout = 30000
            signerOverride = "CloudflareR2Signer"
        }

        // Inisialisasi S3 Client
        val s3Client = AmazonS3Client(
            credentials,
            configuration
        )

        // Konfigurasi S3
        val options = S3ClientOptions.builder()
            .setPathStyleAccess(true)
            .disableChunkedEncoding()
            .setPayloadSigningEnabled(false)
            .build()

        s3Client.setS3ClientOptions(options)

        // Gunakan region yang dikenali AWS SDK
        s3Client.setRegion(
            Region.getRegion(Regions.US_EAST_1)
        )

        // Endpoint Cloudflare R2
        s3Client.setEndpoint(
            "https://${accountId}.r2.cloudflarestorage.com"
        )

        Log.d(
            "R2_CONFIG",
            "Cloudflare R2 client berhasil dikonfigurasi"
        )

        return s3Client
    }
}
