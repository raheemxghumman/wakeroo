package com.wakeroo.app

import android.app.Application
import android.content.Context
import com.wakeroo.app.alarm.AlarmScheduler
import com.wakeroo.app.alarm.Notifications
import com.wakeroo.app.data.AlarmRepository
import com.wakeroo.app.data.DirectBootStore
import com.wakeroo.app.data.SettingsStore
import com.wakeroo.app.data.db.WakerooDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers

/**
 * Manual dependency container. The app also starts before the first unlock after a reboot
 * (direct boot), so anything backed by credential storage, Room and DataStore, is lazy.
 */
class AppContainer(context: Context) {
    val directBoot = DirectBootStore(context)
    val scheduler = AlarmScheduler(context, directBoot)
    val database = lazy { WakerooDatabase.create(context) }
    val settings by lazy { SettingsStore(context) }
    val repository = AlarmRepository(context, database, directBoot, scheduler)
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

class WakerooApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
    }
}

val Context.container: AppContainer get() = (applicationContext as WakerooApp).container
