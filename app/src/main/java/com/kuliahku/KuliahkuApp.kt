package com.kuliahku

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KuliahkuApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Force disable chunked encoding for AWS S3/Cloudflare R2 compatibility
        System.setProperty("com.amazonaws.services.s3.enforceV4ChunkedEncoding", "false")
        
        // Initialize Firebase before App Check
        FirebaseApp.initializeApp(this)

        val appCheck = FirebaseAppCheck.getInstance()
        
        if (BuildConfig.DEBUG) {
            // Use Debug Provider for local development
            appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
            Log.d("AppCheck", "Debug mode: Use this token in Firebase Console: ba79d721-533c-4cc2-b63c-26127b6db6f6")
        } else {
            // Use Play Integrity for release
            appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
        }
    }
}
