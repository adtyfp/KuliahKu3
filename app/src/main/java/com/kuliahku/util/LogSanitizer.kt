package com.kuliahku.util

import android.util.Log
import com.kuliahku.BuildConfig

/**
 * Logger yang otomatis menyembunyikan log di release build.
 * Jangan pernah log: email, amount, teks materi, token, atau URL lengkap dengan query.
 */
object LogSanitizer {
    private const val TAG = "KuliahKu"

    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d("$TAG/$tag", message)
    }

    fun e(tag: String, message: String, tr: Throwable? = null) {
        if (BuildConfig.DEBUG) Log.e("$TAG/$tag", message, tr)
        // Di release, kirim ke Crashlytics saja (tanpa pesan sensitif)
    }

    /** Redact nominal uang untuk log. */
    fun redactAmount(amount: Long): String = "Rp.***"

    /** Redact email: user@domain.com -> u***@d***.com */
    fun redactEmail(email: String): String {
        val at = email.indexOf('@')
        if (at <= 0) return "***"
        val user = email.take(1) + "***"
        val domain = email.substring(at + 1)
        val dot = domain.indexOf('.')
        return if (dot > 0) "$user@${domain.take(1)}***${domain.substring(dot)}" else "$user@***"
    }
}