package jp.rabee

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.work.*
import java.util.concurrent.TimeUnit

class DownloadWorkerManager {

    companion object {
        protected val TAG = "DownloadWorkerManager"

        //WorkManagerが動く上での制約
        private fun createConstraints() =
                Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .setRequiredNetworkType(NetworkType.UNMETERED) //その他の値（NOT_REQUIRED、CONNECTED、NOT_ROAMING、METERED）
                        .setRequiresBatteryNotLow(true) //電池残量が少なくない場合
                        .setRequiresStorageNotLow(true) //ストレージが不足していない場合
                        .build()

        private fun createWorkRequest() =
                OneTimeWorkRequestBuilder<DownloadWorker>()
                        .setConstraints(createConstraints())
                        //作業をやり直す必要がある場合に備えてバックオフを設定する
                        .setBackoffCriteria(BackoffPolicy.LINEAR, OneTimeWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)

        //ジョブのスタート関数。LiveDataを扱わない場合、LifeCycleOwnerは不要。必要な場合、コールバックを高階関数で設定しておく。
        fun startWork(lifecycleOwner: LifecycleOwner, data: Data, callback: () -> Unit) {
            val work = createWorkRequest().setInputData(data).build()
            val workManager = WorkManager.getInstance()

            workManager
                    .enqueueUniqueWork(
                            "Download Work", ExistingWorkPolicy.APPEND, work)

            //WorkManager自体の実行結果をLiveDataで、Observeする。
            workManager
                    .getWorkInfoByIdLiveData(work.id)
                    .observe(lifecycleOwner, Observer { workInfo ->
                        if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {
                            callback()
                            Log.d(TAG, "WorkManagerInfo:Success${work.id}:Info: " +  workInfo.outputData.toString())
                        } else {
                            Log.d(TAG,"WorkManagerInfo:Failed${work.id}:State: " + workInfo.state.toString())
                        }
                    })
        }
    }
}