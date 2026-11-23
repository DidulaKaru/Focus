package com.focusflow.app

import android.app.Application
import com.focusflow.app.repository.LocalStore
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FocusFlowApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LocalStore.init(this)
    }
}
