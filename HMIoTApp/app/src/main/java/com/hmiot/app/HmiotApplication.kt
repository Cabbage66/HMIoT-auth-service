package com.hmiot.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.hmiot.app.core.AppContainer
import com.hmiot.app.core.work.PolicySyncWorker
import com.hmiot.app.core.work.TerminalCollectWorker
import java.util.concurrent.TimeUnit

class HmiotApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        enqueueBaselineWorkers()
    }

    private fun enqueueBaselineWorkers() {
        val policyWork = PeriodicWorkRequestBuilder<PolicySyncWorker>(30, TimeUnit.MINUTES).build()
        val terminalWork = PeriodicWorkRequestBuilder<TerminalCollectWorker>(15, TimeUnit.MINUTES).build()
        val manager = WorkManager.getInstance(this)
        manager.enqueueUniquePeriodicWork("policy-sync", ExistingPeriodicWorkPolicy.UPDATE, policyWork)
        manager.enqueueUniquePeriodicWork("terminal-collect", ExistingPeriodicWorkPolicy.UPDATE, terminalWork)
    }
}
