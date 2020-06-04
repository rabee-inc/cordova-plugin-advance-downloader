package jp.rabee

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import rx.AsyncEmitter
import rx.Observable
import java.util.*

class RxDownloader(
        private val context: Context,
        private val requests: ArrayList<DownloadManager.Request> = ArrayList<DownloadManager.Request>()) {

    companion object {
        const val TAG = "RxDownloader"
        const val KEY = "AdvanceDownloadTasks"
    }

    private val mPrefs = context.getSharedPreferences(KEY, Context.MODE_PRIVATE)
    private val typeToken = object : TypeToken<MutableMap<String, AdvanceDownloadTask>>() {}

    private val manager: DownloadManager =
            context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private val queuedRequests: HashMap<Long, DownloadManager.Request> =
            HashMap<Long, DownloadManager.Request>()

    private var receiver: BroadcastReceiver? = null

    fun enqueue(request: DownloadManager.Request): RxDownloader = apply {
        requests.add(request)
    }

    fun execute(task: AdvanceDownloadTask): Observable<DownloadStatus> =
            if (requests.isEmpty()) Observable.empty()
            else Observable.fromEmitter({ emitter ->
                receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        intent ?: return
                        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.action)) {
                            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                            if (!queuedRequests.contains(downloadId)) {
                                return
                            }
                            resolveDownloadStatus(task, downloadId, emitter)
                            queuedRequests.remove(downloadId)
                            if (queuedRequests.isEmpty()) {
                                emitter.onCompleted()
                                context ?: return

                                mPrefs.edit().clear().apply()
                            }
                        }
                    }
                }
                context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

                requests.forEach {
                    val downloadId = manager.enqueue(it)
                    // このObservableの処理待ちのRequestとして追加する
                    queuedRequests.put(downloadId, it)
                    Log.d(TAG, "DownloadID: ${downloadId}, START")
                }

                emitter.setCancellation {
                    queuedRequests.forEach {
                        manager.remove(it.key)
                    }
                    receiver?.let {
                        context.unregisterReceiver(it)
                    }
                    mPrefs.edit().clear().apply()
                }
            }, AsyncEmitter.BackpressureMode.BUFFER)

    fun removeRequest(downloadId: Long) {
        manager.remove(downloadId)
    }

    private fun resolveDownloadStatus(task: AdvanceDownloadTask, downloadId: Long, emitter: AsyncEmitter<in DownloadStatus>) {
        val query = DownloadManager.Query().apply {
            setFilterById(downloadId)
        }

        val tasks = Gson().fromJson<MutableMap<String, AdvanceDownloadTask>>(mPrefs.getString(KEY, "{}"), typeToken.type)
        task.downloadId = downloadId
        tasks[task.id] = task
        mPrefs.edit().putString(KEY, Gson().toJson(tasks)).apply()

        val cursor = manager.query(query)
        if (cursor.moveToFirst()) {
            val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            val reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
            val requestResult: RequestResult = createRequestResult(downloadId, cursor)
            Log.d(TAG, "RESULT: ${requestResult.toString()}")
            when (status) {
                DownloadManager.STATUS_FAILED -> {
                    val failedReason = when (reason) {
                        DownloadManager.ERROR_CANNOT_RESUME -> "ERROR_CANNOT_RESUME"
                        DownloadManager.ERROR_DEVICE_NOT_FOUND -> "ERROR_DEVICE_NOT_FOUND"
                        DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "ERROR_FILE_ALREADY_EXISTS"
                        DownloadManager.ERROR_FILE_ERROR -> "ERROR_FILE_ERROR"
                        DownloadManager.ERROR_HTTP_DATA_ERROR -> "ERROR_HTTP_DATA_ERROR"
                        DownloadManager.ERROR_INSUFFICIENT_SPACE -> "ERROR_INSUFFICIENT_SPACE"
                        DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "ERROR_TOO_MANY_REDIRECTS"
                        DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "ERROR_UNHANDLED_HTTP_CODE"
                        DownloadManager.ERROR_UNKNOWN -> "ERROR_UNKNOWN"
                        else -> ""
                    }
                    Log.e(TAG, "DownloadID: ${downloadId}, Failed: ${failedReason}")
                    emitter.onNext(DownloadStatus.Failed(requestResult, failedReason))
                    emitter.onError(DownloadFailedException(failedReason, queuedRequests[downloadId]))
                }
                DownloadManager.STATUS_PAUSED -> {
                    val pausedReason = when (reason) {
                        DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "PAUSED_QUEUED_FOR_WIFI"
                        DownloadManager.PAUSED_UNKNOWN -> "PAUSED_UNKNOWN"
                        DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "PAUSED_WAITING_FOR_NETWORK"
                        DownloadManager.PAUSED_WAITING_TO_RETRY -> "PAUSED_WAITING_TO_RETRY"
                        else -> ""
                    }
                    Log.d(TAG, "DownloadID: ${downloadId}, Paused: ${pausedReason}")
                    emitter.onNext(DownloadStatus.Paused(requestResult, pausedReason))
                }
                DownloadManager.STATUS_PENDING -> {
                    Log.d(TAG, "DownloadID: ${downloadId}, Waiting")
                    emitter.onNext(DownloadStatus.Waiting(requestResult))
                }
                DownloadManager.STATUS_RUNNING -> {
                    val total = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    if (total >= 0) {
                        val downloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val progress = (downloaded * 100 / total)
                        Log.d(TAG, "DownloadID: ${downloadId}, Processing: ${progress}")
                        emitter.onNext(DownloadStatus.Processing(requestResult, progress))
                    }
                }
                DownloadManager.STATUS_SUCCESSFUL -> {
                    Log.d(TAG, "DownloadID: ${downloadId}, Complete")
                    emitter.onNext(DownloadStatus.Complete(requestResult))
                }
            }
        }
        cursor.close()
    }

    fun createRequestResult(downloadId: Long, cursor: Cursor): RequestResult =
            RequestResult(
                    downloadId = downloadId,
                    remoteUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI)),
                    localUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)),
                    mediaType = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE)),
                    totalSize = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                    title = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE))
            )

    sealed class DownloadStatus(val result: RequestResult, val progress: Long = 0, val reason: String = "") {
        class Waiting(result: RequestResult) : DownloadStatus(result)
        class Processing(result: RequestResult, progress: Long) : DownloadStatus(result, progress)
        class Paused(result: RequestResult, reason: String) : DownloadStatus(result, reason = reason)
        class Failed(result: RequestResult, reason: String) : DownloadStatus(result, reason = reason)
        class Complete(result: RequestResult) : DownloadStatus(result)
    }

    class DownloadFailedException(message: String, val request: DownloadManager.Request?) : Throwable(message)
}

fun DownloadManager.Request.execute(context: Context, task: AdvanceDownloadTask): Observable<RxDownloader.DownloadStatus> =
        RxDownloader(context).enqueue(this).execute(task)