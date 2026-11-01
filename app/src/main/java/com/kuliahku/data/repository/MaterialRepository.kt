package com.kuliahku.data.repository

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.kuliahku.data.model.Material
import com.kuliahku.util.Constants
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaterialRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val s3Client: AmazonS3,
    @ApplicationContext private val context: Context
) {
    private fun col(uid: String) =
        firestore.collection(Constants.COLLECTION_USERS).document(uid).collection("materials")

    fun observeAll(): Flow<List<Material>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(emptyList()); close(); return@callbackFlow }
        val listener = col(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { 
                    android.util.Log.e("MaterialRepository", "Error observing materials: ${err.message}")
                    trySend(emptyList())
                    return@addSnapshotListener 
                }
                trySend(snap?.documents?.mapNotNull { it.toObject(Material::class.java) } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun observe(id: String): Flow<Material?> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(null); close(); return@callbackFlow }
        val listener = col(uid).document(id)
            .addSnapshotListener { snap, err ->
                if (err != null) { 
                    android.util.Log.e("MaterialRepository", "Error observing material $id: ${err.message}")
                    trySend(null)
                    return@addSnapshotListener 
                }
                trySend(snap?.toObject(Material::class.java))
            }
        awaitClose { listener.remove() }
    }

    suspend fun create(material: Material): Result<String> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Sesi tidak ditemukan.")
        val id = material.id.ifBlank { col(uid).document().id }
        val data = mapOf(
            "title" to material.title,
            "sourceType" to material.sourceType,
            "sourceRef" to material.sourceRef,
            "extractedText" to material.extractedText,
            "summary" to material.summary,
            "createdAt" to (material.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        col(uid).document(id).set(data, SetOptions.merge()).await()
        id
    }

    suspend fun updateSummary(id: String, summary: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Sesi tidak ditemukan.")
        col(uid).document(id).set(
            mapOf("summary" to summary, "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        ).await()
        Unit
    }

    suspend fun uploadFile(uri: android.net.Uri, mime: String, name: String): Result<String> = 
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val uid = auth.currentUser?.uid ?: error("Sesi tidak ditemukan.")
                val ext = name.substringAfterLast('.', "bin")
                val path = "users/$uid/materials/${System.currentTimeMillis()}.$ext"
                
                val inputStream = context.contentResolver.openInputStream(uri) ?: error("Gagal membuka file.")
                val bytes = inputStream.readBytes()
                val metadata = ObjectMetadata().apply {
                    contentType = mime
                    contentLength = bytes.size.toLong()
                }

                s3Client.putObject("kuliahku-storage", path, bytes.inputStream(), metadata)
                path
            }
        }

    suspend fun saveQuizAttempt(attemptId: String, data: Map<String, Any?>): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Sesi tidak ditemukan.")
        firestore.collection(Constants.COLLECTION_USERS).document(uid)
            .collection("quizAttempts").document(attemptId).set(data).await()
        Unit
    }
}