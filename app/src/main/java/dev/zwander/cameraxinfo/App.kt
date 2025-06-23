package dev.zwander.cameraxinfo

import android.app.Application
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.performance.BugsnagPerformance
import com.google.firebase.Firebase
import com.google.firebase.initialize

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        Firebase.initialize(context = this)

        Bugsnag.start(this)
        BugsnagPerformance.start(this)
    }
}