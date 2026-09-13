package com.metro.hub

import android.app.Application
import com.google.firebase.FirebaseApp

class HubApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
