
package com.kuliahku.data.repository

import android.content.Context
import android.util.Log
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kuliahku.BuildConfig
import com.kuliahku.data.model.User
import com.kuliahku.data.model.UserSettings
import com.kuliahku.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val s3Client: AmazonS3,
    private val context: Context
) {

    companion object {
        private const val TAG = "UserRepository"
        private const val R2_TAG = "R2_UPLOAD"
    }

    // =====================================================
    // OBSERVE USER
    // =====================================================

    fun observeUser(uid: String): Flow<User?> = callbackFlow {
        val ref = firestore
            .collection(Constants.COLLECTION_USERS)
            .document(uid)

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error observing user: ${error.message}", error)
                trySend(null)
                return@addSnapshotListener
            }

            trySend(snapshot?.toObject(User::class.java))
        }

        awaitClose {
            listener.remove()
        }
    }

    // =====================================================
    // GET USER
    // =====================================================

    suspend fun getUser(uid: String): User? =
        withContext(Dispatchers.IO) {
            try {
                firestore
                    .collection(Constants.COLLECTION_USERS)
                    .document(uid)
                    .get()
                    .await()
                    .toObject(User::class.java)

            } catch (e: Exception) {
                Log.e(TAG, "Error getting user: ${e.message}", e)
                null
            }
        }

    // =====================================================
    // UPDATE PROFILE
    // =====================================================

    suspend fun updateProfile(
        uid: String,
        name: String?,
        nim: String?,
        prodi: String?,
        email: String?,
        semester: Int?,
        photoUrl: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {

        runCatching {
            require(uid.isNotBlank()) {
                "UID pengguna tidak boleh kosong"
            }

            Log.d(TAG, "Updating profile for UID: $uid")

            val userRef = firestore
                .collection(Constants.COLLECTION_USERS)
                .document(uid)

            // Ambil data pengguna saat ini
            val userDoc = userRef.get().await()
            val oldNim = userDoc.getString("nim")

            // Periksa perubahan NIM
            if (!nim.isNullOrBlank() && oldNim != nim) {

                val nimDoc = firestore
                    .collection(Constants.COLLECTION_NIM_INDEX)
                    .document(nim)
                    .get()
                    .await()

                if (nimDoc.exists()) {
                    throw Exception(
                        "NIM $nim sudah terdaftar oleh pengguna lain."
                    )
                }

                val currentEmail =
                    email ?: userDoc.getString("email") ?: ""

                // Hapus index NIM lama
                if (!oldNim.isNullOrBlank()) {
                    firestore
                        .collection(Constants.COLLECTION_NIM_INDEX)
                        .document(oldNim)
                        .delete()
                        .await()
                }

                // Simpan index NIM baru
                firestore
                    .collection(Constants.COLLECTION_NIM_INDEX)
                    .document(nim)
                    .set(mapOf("email" to currentEmail))
                    .await()
            }

            // Data profil yang akan diperbarui
            val updates = mutableMapOf<String, Any?>()

            name?.let {
                updates["name"] = it
            }

            nim?.let {
                updates["nim"] = it
            }

            prodi?.let {
                updates["prodi"] = it
            }

            email?.let {
                updates["email"] = it
            }

            semester?.let {
                updates["semester"] = it
            }

            // Simpan URL foto baru jika tersedia
            photoUrl?.let {
                updates["photoUrl"] = it
            }

            updates["updatedAt"] = FieldValue.serverTimestamp()

            // Simpan perubahan ke Firestore
            userRef.set(updates, SetOptions.merge()).await()

            Log.d(TAG, "Profile updated successfully")
            Log.d(TAG, "Photo URL: ${photoUrl ?: "Tidak ada perubahan"}")

            Unit
        }.onFailure { error ->
            Log.e(
                TAG,
                "Failed to update profile: ${error.message}",
                error
            )
        }
    }

    // =====================================================
    // UPLOAD AVATAR TO CLOUDFLARE R2
    // =====================================================

    suspend fun uploadAvatar(
        uid: String,
        bytes: ByteArray
    ): Result<String> = withContext(Dispatchers.IO) {

        var tempFile: File? = null

        try {
            require(uid.isNotBlank()) {
                "UID pengguna tidak boleh kosong"
            }

            require(bytes.isNotEmpty()) {
                "Foto avatar tidak boleh kosong"
            }

            val bucket = BuildConfig.R2_BUCKET_NAME.trim()

            val publicUrl = BuildConfig.R2_PUBLIC_URL
                .trim()
                .removeSuffix("/")

            require(bucket.isNotBlank()) {
                "R2_BUCKET_NAME belum dikonfigurasi"
            }

            require(publicUrl.isNotBlank()) {
                "R2_PUBLIC_URL belum dikonfigurasi"
            }

            // Path dasar dari Constants
            val basePath = Constants.STORAGE_PROFILE_PATH
                .format(uid)
                .removePrefix("/")

            require(basePath.isNotBlank()) {
                "Path avatar tidak valid"
            }

            // Buat nama file unik setiap upload.
            // Tujuannya agar URL berubah dan cache foto lama tidak digunakan.
            val uniqueId = UUID.randomUUID().toString()

            val dotIndex = basePath.lastIndexOf('.')
            val slashIndex = basePath.lastIndexOf('/')

            val objectKey = if (dotIndex > slashIndex) {
                basePath.substring(0, dotIndex) +
                        "_$uniqueId" +
                        basePath.substring(dotIndex)
            } else {
                "${basePath}_$uniqueId.jpg"
            }

            Log.d(R2_TAG, "================================")
            Log.d(R2_TAG, "MEMULAI UPLOAD AVATAR")
            Log.d(R2_TAG, "Bucket: $bucket")
            Log.d(R2_TAG, "Object Key: $objectKey")
            Log.d(R2_TAG, "Ukuran gambar: ${bytes.size} bytes")

            // Buat file sementara
            tempFile = File.createTempFile(
                "kuliahku_avatar_",
                ".jpg",
                context.cacheDir
            )

            tempFile.writeBytes(bytes)

            require(tempFile.exists() && tempFile.length() > 0) {
                "File avatar gagal dibuat"
            }

            // Metadata gambar
            val metadata = ObjectMetadata().apply {
                contentType = "image/jpeg"
                contentLength = tempFile.length()
                contentDisposition = "inline"

                // URL menggunakan object key unik,
                // sehingga cache foto sebelumnya tidak digunakan.
                cacheControl = "public, max-age=86400"
            }

            // Request upload
            val request = PutObjectRequest(
                bucket,
                objectKey,
                tempFile
            ).withMetadata(metadata)

            Log.d(R2_TAG, "Mengirim file ke Cloudflare R2...")

            // Upload ke R2
            val uploadResult = s3Client.putObject(request)

            Log.i(R2_TAG, "UPLOAD R2 BERHASIL")
            Log.i(R2_TAG, "ETag: ${uploadResult.eTag}")

            // Buat URL publik dengan object key baru
            val photoUrl = "$publicUrl/$objectKey"

            Log.i(R2_TAG, "URL foto baru: $photoUrl")
            Log.d(R2_TAG, "================================")

            // Kembalikan URL agar disimpan melalui updateProfile()
            Result.success(photoUrl)

        } catch (e: Exception) {

            Log.e(
                R2_TAG,
                "UPLOAD AVATAR GAGAL: ${e.message}",
                e
            )

            if (e is AmazonS3Exception) {
                Log.e(R2_TAG, "HTTP Status: ${e.statusCode}")
                Log.e(R2_TAG, "Error Code: ${e.errorCode}")
                Log.e(R2_TAG, "Error Message: ${e.errorMessage}")
            }

            Result.failure(e)

        } finally {
            // Hapus file sementara
            tempFile?.let { file ->
                if (file.exists()) {
                    val deleted = file.delete()
                    Log.d(R2_TAG, "Temporary file deleted: $deleted")
                }
            }
        }
    }

    // =====================================================
    // UPDATE SETTINGS
    // =====================================================

    suspend fun updateSettings(
        uid: String,
        settings: UserSettings
    ): Result<Unit> = withContext(Dispatchers.IO) {

        runCatching {
            firestore
                .collection(Constants.COLLECTION_USERS)
                .document(uid)
                .set(
                    mapOf("settings" to settings),
                    SetOptions.merge()
                )
                .await()

            Unit
        }
    }
}
