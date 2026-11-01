package com.kuliahku.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kuliahku.data.model.AppNotification
import com.kuliahku.util.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private fun col(uid: String) =
        firestore.collection(Constants.COLLECTION_USERS).document(uid).collection("notifications")

    fun observeAll(): Flow<List<AppNotification>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(emptyList()); close(); return@callbackFlow }
        val listener = col(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull { it.toObject(AppNotification::class.java) } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun observeUnreadCount(): Flow<Int> =
        observeAll().map { list -> list.count { !it.read } }

    suspend fun push(
        title: String,
        body: String,
        type: String,
        deepLink: String? = null
    ): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching
        col(uid).add(
            mapOf(
                "title" to title,
                "body" to body,
                "type" to type,
                "deepLink" to deepLink,
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }

    suspend fun markRead(id: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching
        col(uid).document(id).update("read", true).await()
        Unit
    }

    suspend fun markAllRead(): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching
        val docs = col(uid).whereEqualTo("read", false).get().await()
        docs.documents.forEach { it.reference.update("read", true) }
        Unit
    }

    suspend fun delete(id: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching
        col(uid).document(id).delete().await()
        Unit
    }
}