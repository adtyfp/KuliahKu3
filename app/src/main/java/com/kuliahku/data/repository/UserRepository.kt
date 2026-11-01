package com.kuliahku.data.repository

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kuliahku.data.model.User
import com.kuliahku.data.model.UserSettings
import com.kuliahku.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val s3Client: AmazonS3
) {

    fun observeUser(uid: String): Flow<User?> = callbackFlow {
        val ref = firestore.collection(Constants.COLLECTION_USERS).document(uid)
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) { 
                android.util.Log.e("UserRepository", "Error observing user: ${error.message}", error)
                close(error); 
                return@addSnapshotListener 
            }
            trySend(snapshot?.toObject(User::class.java))
        }
        awaitClose { listener.remove() }
    }

    suspend fun getUser(uid: String): User? = withContext(Dispatchers.IO) {
        try {
            firestore.collection(Constants.COLLECTION_USERS).document(uid)
                .get().await().toObject(User::class.java)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error getting user $uid: ${e.message}", e)
            null
        }
    }

    suspend fun updateProfile(
        uid: String,
        name: String?,
        nim: String?,
        prodi: String?,
        email: String?,
        semester: Int?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            android.util.Log.d("UserRepository", "Updating profile for $uid: nim=$nim, name=$name")
            
            // Cek NIM unik bila diubah atau baru diset
            if (!nim.isNullOrBlank()) {
                val userDoc = firestore.collection(Constants.COLLECTION_USERS).document(uid).get().await()
                val oldNim = userDoc.getString("nim")
                
                if (oldNim != nim) {
                    android.util.Log.d("UserRepository", "NIM changed from $oldNim to $nim, checking uniqueness")
                    // Pastikan NIM baru belum ada yang pakai
                    val nimDoc = firestore.collection(Constants.COLLECTION_NIM_INDEX).document(nim).get().await()
                    if (nimDoc.exists()) {
                        throw Exception("NIM $nim sudah terdaftar oleh pengguna lain.")
                    }
                    
                    val currentEmail = email ?: userDoc.getString("email") ?: ""
                    
                    // Hapus index lama jika ada, dan buat index baru
                    if (!oldNim.isNullOrBlank()) {
                        firestore.collection(Constants.COLLECTION_NIM_INDEX).document(oldNim).delete().await()
                    }
                    firestore.collection(Constants.COLLECTION_NIM_INDEX).document(nim)
                        .set(mapOf("email" to currentEmail)).await()
                }
            }

            val updates = mutableMapOf<String, Any?>()
            name?.let { updates["name"] = it }
            nim?.let { updates["nim"] = it }
            prodi?.let { updates["prodi"] = it }
            email?.let { updates["email"] = it }
            semester?.let { updates["semester"] = it }
            updates["updatedAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()

            firestore.collection(Constants.COLLECTION_USERS).document(uid)
                .set(updates, SetOptions.merge()).await()
            android.util.Log.d("UserRepository", "Profile updated successfully")
            Unit
        }.onFailure {
            android.util.Log.e("UserRepository", "Failed to update profile: ${it.message}", it)
        }
    }

    suspend fun uploadAvatar(uid: String, bytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            android.util.Log.d("UserRepository", "Uploading avatar for $uid, size: ${bytes.size}")
            val path = Constants.STORAGE_PROFILE_PATH.format(uid)
            val metadata = ObjectMetadata().apply {
                contentType = "image/jpeg"
                contentLength = bytes.size.toLong()
            }
            
            s3Client.putObject("kuliahku-storage", path, bytes.inputStream(), metadata)
            
            // Sesuaikan dengan URL publik R2 Anda
            val url = "https://b07ec8aa7ea58d1d879db15183d19f70.r2.cloudflarestorage.com/kuliahku-storage/$path"

            firestore.collection(Constants.COLLECTION_USERS).document(uid)
                .set(mapOf("photoUrl" to url, "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()),
                    SetOptions.merge()).await()
            android.util.Log.d("UserRepository", "Avatar uploaded and URL saved: $url")
            url
        }.onFailure {
            android.util.Log.e("UserRepository", "Failed to upload avatar: ${it.message}", it)
        }
    }

    suspend fun updateSettings(uid: String, settings: UserSettings): Result<Unit> = runCatching {
        firestore.collection(Constants.COLLECTION_USERS).document(uid)
            .set(mapOf("settings" to settings), SetOptions.merge()).await()
        Unit
    }
}