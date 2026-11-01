package com.kuliahku.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.kuliahku.data.model.Bookmark
import com.kuliahku.data.model.JournalReference
import com.kuliahku.util.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private fun col(uid: String) =
        firestore.collection(Constants.COLLECTION_USERS).document(uid).collection("bookmarks")

    fun observeAll(): Flow<List<Bookmark>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(emptyList()); close(); return@callbackFlow }
        val listener = col(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull { it.toObject(Bookmark::class.java) } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun toggle(reference: JournalReference): Result<Boolean> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Sesi tidak ditemukan.")
        val id = reference.id
        val doc = col(uid).document(id).get().await()
        if (doc.exists()) {
            doc.reference.delete().await()
            false
        } else {
            val bookmark = mapOf(
                "title" to reference.title,
                "doi" to reference.doi,
                "authors" to reference.authors,
                "year" to reference.year,
                "citedByCount" to reference.citedByCount,
                "isOpenAccess" to reference.isOpenAccess,
                "url" to reference.doi?.let { "https://doi.org/$it" },
                "source" to "openalex",
                "createdAt" to FieldValue.serverTimestamp()
            )
            col(uid).document(id).set(bookmark, SetOptions.merge()).await()
            true
        }
    }

    suspend fun isBookmarked(uid: String, referenceId: String): Boolean {
        return col(uid).document(referenceId).get().await().exists()
    }
}