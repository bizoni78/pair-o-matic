package com.pairomatic

import android.app.Application
import com.pairomatic.notifications.NotificationHelper

class PairOMaticApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannel(this)
    }
}
