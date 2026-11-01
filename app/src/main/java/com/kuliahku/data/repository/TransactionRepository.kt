package com.kuliahku.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.kuliahku.data.model.TxSource
import com.kuliahku.data.model.TxType
import com.kuliahku.data.model.Transaction
import com.kuliahku.util.Constants
import com.kuliahku.util.MonthKey
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private fun col(uid: String) =
        firestore.collection(Constants.COLLECTION_USERS).document(uid)
            .collection(Constants.SUBCOLLECTION_TRANSACTIONS)

    fun observeAll(): Flow<List<Transaction>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(emptyList()); close(); return@callbackFlow }
        val listener = col(uid)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull { it.toObject(Transaction::class.java) } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun observeByMonth(monthKey: String): Flow<List<Transaction>> =
        observeAll().map { list -> list.filter { it.monthKey == monthKey } }

    fun observeByYear(yearKey: String): Flow<List<Transaction>> =
        observeAll().map { list -> list.filter { it.yearKey == yearKey } }

    suspend fun upsert(tx: Transaction): Result<String> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Sesi tidak ditemukan.")
        val id = tx.id.ifBlank { col(uid).document().id }
        val dateCal = Calendar.getInstance().apply { time = tx.date?.toDate() ?: java.util.Date() }
        val monthKey = tx.monthKey.ifBlank { MonthKey.of(dateCal) }
        val yearKey = tx.yearKey.ifBlank { MonthKey.yearOf(dateCal.time) }
        val data = mapOf(
            "type" to tx.type,
            "category" to tx.category,
            "amount" to tx.amount,
            "description" to tx.description,
            "date" to tx.date,
            "monthKey" to monthKey,
            "yearKey" to yearKey,
            "source" to tx.source,
            "dedupHash" to tx.dedupHash,
            "createdAt" to (tx.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        col(uid).document(id).set(data, SetOptions.merge()).await()
        id
    }

    suspend fun delete(id: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Sesi tidak ditemukan.")
        col(uid).document(id).delete().await()
        Unit
    }

    suspend fun restore(tx: Transaction): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Sesi tidak ditemukan.")
        val data = mapOf(
            "type" to tx.type, "category" to tx.category, "amount" to tx.amount,
            "description" to tx.description, "date" to tx.date,
            "monthKey" to tx.monthKey, "yearKey" to tx.yearKey,
            "source" to tx.source, "dedupHash" to tx.dedupHash,
            "createdAt" to (tx.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        col(uid).document(tx.id).set(data, SetOptions.merge()).await()
        Unit
    }

    /** Cek duplikat dari notifikasi: hash unik per (paket, jumlah, waktu 5 menit). */
    suspend fun existsByHash(hash: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val snap = col(uid).whereEqualTo("dedupHash", hash).limit(1).get().await()
        return !snap.isEmpty
    }

    /**
     * Cek apakah hash sudah ada di cache lokal (menghindari baca network berulang).
     * Dipakai oleh NotificationListenerService.
     */
    suspend fun addFromNotification(tx: Transaction): Result<String> = runCatching {
        if (tx.dedupHash != null && existsByHash(tx.dedupHash)) {
            error("Duplikat terdeteksi, tidak disimpan.")
        }
        upsert(tx.copy(source = TxSource.NOTIFICATION.value)).getOrThrow()
    }
}