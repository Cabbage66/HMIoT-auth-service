package com.hmiot.app.core.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hmiot.app.HmiotApplication
import com.hmiot.app.core.network.model.ApiResult

class TerminalCollectWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as HmiotApplication
        val repo = app.appContainer.terminalMonitorRepository
        val trustGuardRepository = app.appContainer.trustGuardRepository
        trustGuardRepository.evaluateAndUpdate("BACKGROUND_WORKER")
        return when (val flush = repo.flushQueue()) {
            is ApiResult.Success -> Result.success()
            is ApiResult.Failure -> Result.retry()
        }
    }
}
