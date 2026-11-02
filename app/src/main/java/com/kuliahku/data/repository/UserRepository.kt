
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

                Log.e(
                    TAG,
                    "Error getting user: ${e.message}",
                    e
                )

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
        semester: Int?
    ): Result<Unit> = withContext(Dispatchers.IO) {

        runCatching {

            require(uid.isNotBlank()) {
                "UID pengguna tidak boleh kosong"
            }

            Log.d(TAG, "Updating profile for UID: $uid")

            // Periksa perubahan NIM
            if (!nim.isNullOrBlank()) {

                val userDoc = firestore
                    .collection(Constants.COLLECTION_USERS)
                    .document(uid)
                    .get()
                    .await()

                val oldNim = userDoc.getString("nim")

                if (oldNim != nim) {

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
                        .set(
                            mapOf("email" to currentEmail)
                        )
                        .await()
                }
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

            updates["updatedAt"] = FieldValue.serverTimestamp()

            // Simpan perubahan ke Firestore
            firestore
                .collection(Constants.COLLECTION_USERS)
                .document(uid)
                .set(updates, SetOptions.merge())
                .await()

            Log.d(TAG, "Profile updated successfully")

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

            // Validasi input
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

            // Ambil path penyimpanan avatar
            val path = Constants.STORAGE_PROFILE_PATH
                .format(uid)
                .removePrefix("/")

            require(path.isNotBlank()) {
                "Path avatar tidak valid"
            }

            Log.d(R2_TAG, "================================")
            Log.d(R2_TAG, "MEMULAI UPLOAD AVATAR")
            Log.d(R2_TAG, "Bucket: $bucket")
            Log.d(R2_TAG, "Object Key: $path")
            Log.d(R2_TAG, "Ukuran gambar: ${bytes.size} bytes")

            // Buat file sementara
            tempFile = File.createTempFile(
                "kuliahku_avatar_",
                ".jpg",
                context.cacheDir
            )

            // Simpan byte gambar ke file
            tempFile.writeBytes(bytes)

            require(tempFile.exists() && tempFile.length() > 0) {
                "File avatar gagal dibuat"
            }

            // Metadata gambar
            val metadata = ObjectMetadata().apply {
                contentType = "image/jpeg"
                contentLength = tempFile.length()
                contentDisposition = "inline"
                cacheControl = "public, max-age=86400"
            }

            // Buat request upload berbasis File
            val request = PutObjectRequest(
                bucket,
                path,
                tempFile
            ).withMetadata(metadata)

            Log.d(R2_TAG, "File sementara berhasil dibuat")
            Log.d(R2_TAG, "Mengirim file ke Cloudflare R2...")

            // Upload file ke Cloudflare R2
            val uploadResult = s3Client.putObject(request)

            Log.i(R2_TAG, "UPLOAD R2 BERHASIL")
            Log.i(R2_TAG, "ETag: ${uploadResult.eTag}")

            // Buat URL publik avatar
            val photoUrl = "$publicUrl/$path"

            Log.d(R2_TAG, "Public URL berhasil dibuat")

            // Simpan URL foto ke Firestore
            firestore
                .collection(Constants.COLLECTION_USERS)
                .document(uid)
                .set(
                    mapOf(
                        "photoUrl" to photoUrl,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                .await()

            Log.i(
                R2_TAG,
                "URL avatar berhasil disimpan ke Firestore"
            )

            Log.d(R2_TAG, "================================")

            Result.success(photoUrl)

        } catch (e: Exception) {

            Log.e(
                R2_TAG,
                "UPLOAD AVATAR GAGAL: ${e.message}",
                e
            )

            if (e is AmazonS3Exception) {

                Log.e(
                    R2_TAG,
                    "HTTP Status: ${e.statusCode}"
                )

                Log.e(
                    R2_TAG,
                    "Error Code: ${e.errorCode}"
                )

                Log.e(
                    R2_TAG,
                    "Error Message: ${e.errorMessage}"
                )
            }

            Result.failure(e)

        } finally {

            // Hapus file sementara
            tempFile?.let { file ->

                if (file.exists()) {
                    val deleted = file.delete()

                    Log.d(
                        R2_TAG,
                        "Temporary file deleted: $deleted"
                    )
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
