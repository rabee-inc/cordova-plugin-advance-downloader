package jp.rabee

import android.app.DownloadManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.apache.cordova.*
import org.json.JSONException
import org.json.*
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers.io

class AdvanceDownloader : CordovaPlugin() {
    lateinit var cContext: CallbackContext
    private val tasks = mutableMapOf<String, AdvanceDownloadTask>()

    // アプリ起動時に呼ばれる
    override public fun initialize(cordova: CordovaInterface,  webView: CordovaWebView) {
        println("hi! This is AdvanceDownloader. Now intitilaizing ...")
    }

    // js 側で関数が実行されるとこの関数がまず発火する
    @Throws(JSONException::class)
    override fun execute(action: String, data: JSONArray, callbackContext: CallbackContext): Boolean {
        cContext = callbackContext

        var result = true
        val value = data.getJSONObject(0)
        when(action) {
            "list" -> {
                result = this.list(cContext)
            }
            "get" -> {
                val id = value.getString("id")
                result = this.get(id, cContext)
            }
            "add" -> {
                val task = Gson().fromJson(value.toString(), AdvanceDownloadTask::class.java)
                result = this.add(task, cContext)
            }
            "start" -> {
                val id = value.getString("id")
                result = this.start(id, cContext)
            }
            "pause" -> {
                val id = value.getString("id")
                result = this.pause(id, cContext)
            }
            "resume" -> {
                val id = value.getString("id")
                result = this.resume(id, cContext)
            }
            "cancel" -> {
                val id = value.getString("id")
                result = this.cancel(id, cContext)
            }
            else -> {
                // TODO error
            }
        }

        return result
    }

    private fun list(callbackContext: CallbackContext): Boolean {
        val output = Gson().toJson(tasks)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)
        return true
    }

    private fun get(id: String, callbackContext: CallbackContext): Boolean {
        val task = tasks[id]
        task ?: return false

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)
        return true
    }

    private fun add(advanceDownloadTask: AdvanceDownloadTask, callbackContext: CallbackContext): Boolean {
        runBlocking {
            tasks[advanceDownloadTask.id] = advanceDownloadTask
        }

        val output = Gson().toJson(advanceDownloadTask)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun start(id: String, callbackContext: CallbackContext): Boolean {
        val task = tasks[id]
        task ?: return false

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        val uri = Uri.parse(task.url)
        val request = DownloadManager.Request(uri).apply {
            setTitle(task.fileName)
            task.headers.forEach { (k, v) ->
                addRequestHeader(k, v)
            }
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
            setDestinationInExternalFilesDir(cordova.activity.applicationContext, Environment.DIRECTORY_DOWNLOADS, task.fileName)
        }
        request.execute(cordova.activity.applicationContext)
                .subscribeOn(io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ status ->
                    when (status) {
                        is RxDownloader.DownloadStatus.Complete -> {
                            Toast.makeText(cordova.activity.applicationContext, "Successful Result: ${status.result.title}", Toast.LENGTH_SHORT).show()
                        }
                        is RxDownloader.DownloadStatus.Processing -> {
                            Toast.makeText(cordova.activity.applicationContext, "Processing Result: ${status.result.title}", Toast.LENGTH_SHORT).show()
                        }
                        is RxDownloader.DownloadStatus.Paused -> {
                            Toast.makeText(cordova.activity.applicationContext, "Paused Result: ${status.result.title}", Toast.LENGTH_SHORT).show()
                        }
                        is RxDownloader.DownloadStatus.Waiting -> {
                            Toast.makeText(cordova.activity.applicationContext, "Waiting Result: ${status.result.title}", Toast.LENGTH_SHORT).show()
                        }
                        is RxDownloader.DownloadStatus.Failed -> {
                            Toast.makeText(cordova.activity.applicationContext, "Failed Result: ${status.result.title}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }, { error ->
                    error.stackTrace
                }, {
                    Toast.makeText(cordova.activity.applicationContext, "Complete downloads.", Toast.LENGTH_SHORT).show()
                })

        return true
    }

    private fun pause(id: String, callbackContext: CallbackContext): Boolean {
        val task = tasks[id]
        task ?: return false

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun resume(id: String, callbackContext: CallbackContext): Boolean {
        val task = tasks[id]
        task ?: return false

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun cancel(id: String, callbackContext: CallbackContext): Boolean {
        val task = tasks[id]
        task ?: return false

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    companion object {
        protected val TAG = "AdvanceDownloader"
    }

}