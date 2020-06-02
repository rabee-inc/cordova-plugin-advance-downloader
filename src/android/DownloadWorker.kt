package jp.rabee

import android.content.Context
import androidx.work.*

class DownloadWorker(cxt: Context, params: WorkerParameters) : Worker(cxt, params) {

    override fun doWork(): Result {
        //TODO: inputDataからParamsを取得する
//        val url = inputData.getString("url")
        val data = inputData
        return Result.success()
    }
}