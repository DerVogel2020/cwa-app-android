package de.rki.coronawarnapp.diagnosiskeys.execution

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import de.rki.coronawarnapp.bugreporting.reportProblem
import de.rki.coronawarnapp.diagnosiskeys.download.DownloadDiagnosisKeysTask
import de.rki.coronawarnapp.task.TaskController
import de.rki.coronawarnapp.task.common.DefaultTaskRequest
import de.rki.coronawarnapp.task.submitBlocking
import de.rki.coronawarnapp.util.worker.InjectedWorkerFactory
import timber.log.Timber

class DiagnosisKeyRetrievalWorker @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskController: TaskController
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = try {
        Timber.tag(TAG).d("$id: doWork() started. Run attempt: $runAttemptCount")

        val taskState = taskController.submitBlocking(
            DefaultTaskRequest(
                DownloadDiagnosisKeysTask::class,
                DownloadDiagnosisKeysTask.Arguments(),
                originTag = "DiagnosisKeyRetrievalWorker"
            )
        )

        when {
            taskState.isSuccessful -> {
                Timber.tag(TAG).d("$id: DownloadDiagnosisKeysTask finished successfully.")
                Result.success()
            }
            else -> {
                taskState.error?.let {
                    Timber.tag(TAG).w(it, "$id: Error during DownloadDiagnosisKeysTask.")
                }
                Result.retry()
            }
        }
    } catch (e: Exception) {
        e.reportProblem(TAG, "DownloadDiagnosisKeysTask failed exceptionally, will retry.")
        Result.retry()
    }

    @AssistedFactory
    interface Factory : InjectedWorkerFactory<DiagnosisKeyRetrievalWorker>

    companion object {
        private val TAG = DiagnosisKeyRetrievalWorker::class.java.simpleName
    }
}
