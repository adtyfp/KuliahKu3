package com.kuliahku.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.kuliahku.data.model.Note
import com.kuliahku.util.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private fun col(uid: String) =
        firestore.collection(Constants.COLLECTION_USERS).document(uid)
            .collection("notes")

    fun observeAll(): Flow<List<Note>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) { trySend(emptyList()); close(); return@callbackFlow }
        val listener = col(uid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull { it.toObject(Note::class.java) } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun observe(noteId: String): Flow<Note?> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(null); close(); return@callbackFlow }
        val listener = col(uid).document(noteId)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.toObject(Note::class.java))
            }
        awaitClose { listener.remove() }
    }

    suspend fun upsert(note: Note): Result<String> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Sesi tidak ditemukan.")
        val id = note.id.ifBlank { col(uid).document().id }
        val data = mapOf(
            "title" to note.title,
            "content" to note.content,
            "source" to note.source,
            "materialId" to note.materialId,
            "createdAt" to (note.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        col(uid).document(id).set(data, SetOptions.merge()).await()
        id
    }

    suspend fun delete(noteId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Sesi tidak ditemukan.")
        col(uid).document(noteId).delete().await()
        Unit
    }
}