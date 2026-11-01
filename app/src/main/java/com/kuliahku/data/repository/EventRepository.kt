package com.kuliahku.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.kuliahku.data.model.Event
import com.kuliahku.util.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private fun col(uid: String) =
        firestore.collection(Constants.COLLECTION_USERS).document(uid).collection("events")

    fun observeAll(): Flow<List<Event>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) { trySend(emptyList()); close(); return@callbackFlow }
        val listener = col(uid)
            .orderBy("start", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull { it.toObject(Event::class.java) } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun upsert(event: Event): Result<String> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Sesi tidak ditemukan.")
        val id = event.id.ifBlank { col(uid).document().id }
        val data = mapOf(
            "type" to event.type,
            "title" to event.title,
            "allDay" to event.allDay,
            "start" to event.start,
            "end" to event.end,
            "repeat" to event.repeat,
            "reminderStyle" to event.reminderStyle,
            "reminderOffsetMinutes" to event.reminderOffsetMinutes,
            "exceptions" to event.exceptions,
            "createdAt" to (event.createdAt ?: FieldValue.serverTimestamp()),
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

    /**
     * Tandai satu kemunculan berulang sebagai dikecualikan.
     * @param occurrenceDateKey format yyyy-MM-dd
     */
    suspend fun excludeOccurrence(eventId: String, occurrenceDateKey: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Sesi tidak ditemukan.")
        col(uid).document(eventId)
            .set(mapOf("exceptions" to FieldValue.arrayUnion(occurrenceDateKey)), SetOptions.merge())
            .await()
        Unit
    }

    /**
     * Cek apakah event terjadi pada tanggal tertentu (memperhitungkan repeat + exceptions).
     */
    fun occursOn(event: Event, date: Calendar): Boolean {
        val start = event.start?.toDate() ?: return false
        val eventStart = Calendar.getInstance().apply { time = start }
        val dateKey = keyOf(date)
        if (event.exceptions.contains(dateKey)) return false

        // Normalize ke hari
        val eStart = Calendar.getInstance().apply {
            time = eventStart.time
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val eEnd = Calendar.getInstance().apply {
            time = event.end?.toDate() ?: start
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val d = Calendar.getInstance().apply {
            time = date.time
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        // Multi-day event (rentang awal-akhir)
        if (d.timeInMillis in eStart.timeInMillis..eEnd.timeInMillis) return true

        // Jika multi-day sudah ditangani di atas; untuk repeat kita cek dari start
        if (d.timeInMillis < eStart.timeInMillis) return false
        return when (event.repeatEnum) {
            com.kuliahku.data.model.EventRepeat.NONE -> false
            com.kuliahku.data.model.EventRepeat.DAILY -> true
            com.kuliahku.data.model.EventRepeat.WEEKLY -> {
                eStart.get(Calendar.DAY_OF_WEEK) == d.get(Calendar.DAY_OF_WEEK)
            }
            com.kuliahku.data.model.EventRepeat.MONTHLY -> {
                eStart.get(Calendar.DAY_OF_MONTH) == d.get(Calendar.DAY_OF_MONTH)
            }
            com.kuliahku.data.model.EventRepeat.YEARLY -> {
                eStart.get(Calendar.DAY_OF_MONTH) == d.get(Calendar.DAY_OF_MONTH) &&
                        eStart.get(Calendar.MONTH) == d.get(Calendar.MONTH)
            }
        }
    }

    companion object {
        private val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        fun keyOf(cal: Calendar): String = keyFmt.format(cal.time)
        fun keyOf(date: Date): String = keyFmt.format(date)
    }
}