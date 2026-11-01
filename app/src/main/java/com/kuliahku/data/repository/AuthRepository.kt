package com.kuliahku.data.repository

import com.amazonaws.services.s3.AmazonS3
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kuliahku.data.model.AuthResult
import com.kuliahku.data.model.NimIndex
import com.kuliahku.data.model.User
import com.kuliahku.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val s3Client: AmazonS3
) {

    val currentUser: FirebaseUser? get() = auth.currentUser

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        nim: String,
        semester: Int,
        prodi: String
    ): AuthResult = withContext(Dispatchers.IO) {
        var createdUid: String? = null
        var nimIndexCreated = false
        try {
            withTimeout(Constants.NETWORK_TIMEOUT) {
                // 1. Cek NIM unik
                val nimDoc = firestore.collection(Constants.COLLECTION_NIM_INDEX).document(nim).get().await()
                if (nimDoc.exists()) {
                    return@withTimeout AuthResult.Error("NIM $nim sudah terdaftar.")
                }

                // 2. Buat Auth user
                val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                createdUid = authResult.user?.uid ?: return@withTimeout AuthResult.Error("Gagal membuat akun.")

                // 3. Update display name
                authResult.user?.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(name).build()
                )?.await()

                // 4. Buat dokumen user & nimIndex secara paralel (opsional, tapi kita buat sequensial demi keamanan rollback)
                val user = User(
                    uid = createdUid,
                    name = name,
                    email = email.trim(),
                    nim = nim,
                    prodi = prodi,
                    semester = semester,
                    createdAt = com.google.firebase.Timestamp.now(),
                    updatedAt = com.google.firebase.Timestamp.now()
                )
                
                firestore.collection(Constants.COLLECTION_USERS).document(createdUid)
                    .set(user).await()

                firestore.collection(Constants.COLLECTION_NIM_INDEX).document(nim)
                    .set(NimIndex(email = email.trim())).await()
                
                nimIndexCreated = true
                AuthResult.Success(createdUid, isNewUser = true)
            }
        } catch (e: Exception) {
            // Rollback jika gagal
            if (createdUid != null) {
                try {
                    if (nimIndexCreated) firestore.collection(Constants.COLLECTION_NIM_INDEX).document(nim).delete().await()
                    firestore.collection(Constants.COLLECTION_USERS).document(createdUid!!).delete().await()
                    auth.currentUser?.delete()?.await()
                } catch (_: Exception) { }
            }
            AuthResult.Error(mapAuthError(e), e)
        }
    }

    suspend fun login(identifier: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            withTimeout(Constants.NETWORK_TIMEOUT) {
                val email = if (identifier.contains("@")) {
                    identifier.trim()
                } else {
                    val nimDoc = firestore.collection(Constants.COLLECTION_NIM_INDEX)
                        .document(identifier.trim()).get().await()
                    if (!nimDoc.exists()) return@withTimeout AuthResult.Error("NIM tidak ditemukan.")
                    nimDoc.getString("email") ?: return@withTimeout AuthResult.Error("Data NIM tidak valid.")
                }

                val result = auth.signInWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: return@withTimeout AuthResult.Error("Login gagal.")
                AuthResult.Success(uid, isNewUser = false)
            }
        } catch (e: Exception) {
            AuthResult.Error(mapAuthError(e), e)
        }
    }

    suspend fun logout() {
        auth.signOut()
    }

    suspend fun resetPassword(email: String): AuthResult {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            AuthResult.Success(uid = "", isNewUser = false)
        } catch (e: Exception) {
            AuthResult.Error(mapAuthError(e), e)
        }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): AuthResult {
        val user = auth.currentUser ?: return AuthResult.Error("Tidak ada sesi aktif.")
        val email = user.email ?: return AuthResult.Error("Email tidak tersedia.")
        return try {
            // Re-autentikasi
            val credential = com.google.firebase.auth.EmailAuthProvider
                .getCredential(email, currentPassword)
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()
            AuthResult.Success(user.uid, false)
        } catch (e: Exception) {
            AuthResult.Error(mapAuthError(e), e)
        }
    }

    suspend fun deleteAccount(password: String): AuthResult {
        val user = auth.currentUser ?: return AuthResult.Error("Tidak ada sesi aktif.")
        val email = user.email ?: return AuthResult.Error("Email tidak tersedia.")
        val uid = user.uid
        return try {
            // Re-autentikasi
            val credential = com.google.firebase.auth.EmailAuthProvider
                .getCredential(email, password)
            user.reauthenticate(credential).await()

            // Hapus data Firestore (user doc + subcollections)
            val userRef = firestore.collection(Constants.COLLECTION_USERS).document(uid)
            val subcollections = listOf(
                "tasks", "notes", "events", "transactions",
                "materials", "quizzes", "quizAttempts",
                "flashcardDecks", "bookmarks", "notifications"
            )
            subcollections.forEach { col ->
                try {
                    val docs = userRef.collection(col).get().await()
                    docs.documents.forEach { it.reference.delete().await() }
                } catch (_: Exception) { }
            }

            // Hapus nimIndex
            val userDoc = userRef.get().await()
            val nim = userDoc.getString("nim")
            if (nim != null) {
                firestore.collection(Constants.COLLECTION_NIM_INDEX).document(nim).delete().await()
            }
            userRef.delete().await()

            // Hapus foto profil dari R2 (jika ada)
            try {
                val path = Constants.STORAGE_PROFILE_PATH.format(uid)
                s3Client.deleteObject("kuliahku-storage", path)
            } catch (_: Exception) { }

            // Hapus Auth user
            user.delete().await()
            AuthResult.Success(uid, false)
        } catch (e: Exception) {
            AuthResult.Error(mapAuthError(e), e)
        }
    }

    private fun mapAuthError(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("password is invalid", true) ||
                    msg.contains("credential is incorrect", true) -> "Email/NIM atau password salah."
            msg.contains("no user record", true) -> "Akun tidak ditemukan."
            msg.contains("email address is already in use", true) -> "Email sudah terdaftar."
            msg.contains("network", true) -> "Koneksi internet bermasalah."
            msg.contains("timed out", true) || e is kotlinx.coroutines.TimeoutCancellationException -> "Waktu koneksi habis. Coba lagi."
            msg.contains("too many requests", true) -> "Terlalu banyak percobaan. Coba lagi nanti."
            msg.contains("weak password", true) -> "Password terlalu lemah (min. 8 karakter)."
            else -> msg.ifBlank { "Terjadi kesalahan. Coba lagi." }
        }
    }
}