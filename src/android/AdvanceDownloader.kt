package jp.rabee

import androidx.work.*
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.apache.cordova.*
import org.json.JSONException
import org.json.*

class AdvanceDownloader : CordovaPlugin() {
    lateinit var cContext: CallbackContext
    private val downloadLifecycleOwner = DownloadLifecycleOwner()
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

        val output = Gson().toJson(tasks)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)
        return true
    }

    private fun add(advanceDownloadTask: AdvanceDownloadTask, callbackContext: CallbackContext): Boolean {
        runBlocking {
            advanceDownloadTask.status = AdvanceDownloadStatus.WAITING
            tasks[advanceDownloadTask.id] = advanceDownloadTask
        }

        val output = Gson().toJson(advanceDownloadTask)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        //TODO: DEMO用なので以下は削除すること
        val data = Data.Builder().apply {
            putString("id", advanceDownloadTask.id)
            putString("url", advanceDownloadTask.url)
            putString("path", advanceDownloadTask.filePath)
            putString("name", advanceDownloadTask.fileName)
            putInt("size", advanceDownloadTask.size)
            putDouble("progress", advanceDownloadTask.progress)
            advanceDownloadTask.headers.forEach { (k, v) ->
                putString(k, v)
            }
        }.build()

        cordova.activity.runOnUiThread {
            downloadLifecycleOwner.start()
            DownloadWorkerManager.startWork(downloadLifecycleOwner, data, {
                //TODO: callback
            })
        }

        return true
    }

    private fun start(id: String, callbackContext: CallbackContext): Boolean {
        val task = tasks[id]
        task ?: return false

        runBlocking {
            task.status = AdvanceDownloadStatus.PROCESSING
        }

        val output = Gson().toJson(tasks)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        val data = Data.Builder().apply {
            putString("id", task.id)
            putString("url", task.url)
            putString("path", task.filePath)
            putString("name", task.fileName)
            putInt("size", task.size)
            putDouble("progress", task.progress)
            task.headers.forEach { (k, v) ->
                putString(k, v)
            }
        }.build()

        cordova.activity.runOnUiThread {
            downloadLifecycleOwner.start()
            DownloadWorkerManager.startWork(downloadLifecycleOwner, data, {
                //TODO: callback
            })
        }

        return true
    }

    private fun pause(id: String, callbackContext: CallbackContext): Boolean {
        val task = tasks[id]
        task ?: return false

        runBlocking {
            task.status = AdvanceDownloadStatus.PAUSED
        }

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun resume(id: String, callbackContext: CallbackContext): Boolean {
        val task = tasks[id]
        task ?: return false

        runBlocking {
            task.status = AdvanceDownloadStatus.PROCESSING
        }

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun cancel(id: String, callbackContext: CallbackContext): Boolean {
        val task = tasks[id]
        task ?: return false

        runBlocking {
            task.status = AdvanceDownloadStatus.CANCELED
        }

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    companion object {
        protected val TAG = "AdvanceDownloader"
    }

}