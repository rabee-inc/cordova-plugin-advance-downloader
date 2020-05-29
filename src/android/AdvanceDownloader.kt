package jp.rabee

import android.net.Uri
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
        when(action) {
            "list" -> {
                result = this.list(cContext)
            }
            "get" -> {
                val value = data.getJSONObject(0)
                val id = value.getString("id")
                result = this.get(id, cContext)
            }
            "add" -> {
                val value = data.getJSONObject(0)
                val task = AdvanceDownloadTask(
                        id = value.getString("id"),
                        url = Uri.parse(value.getString("url")),
//                        headers = value.getJSONObject("headers"),
                        filePath = value.getString("path")

                )
                result = this.add(task, cContext)
            }
            "start" -> {
                val value = data.getJSONObject(0)
                val id = value.getString("id")
                result = this.start(id, cContext)
            }
            "pause" -> {
                val value = data.getJSONObject(0)
                val id = value.getString("id")
                result = this.pause(id, cContext)
            }
            "resume" -> {
                val value = data.getJSONObject(0)
                val id = value.getString("id")
                result = this.resume(id, cContext)
            }
            "cancel" -> {
                val value = data.getJSONObject(0)
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
//        callback を何回も呼び出したい場合は以下を既述する(ダウンロードの進捗状況を返したい時など)
//        result.keepCallback = true
//        時間のかかる処理とか非同期処理は cordova threadPool を使って下しあ
//            cordova.threadPool.run {
//        }
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

        //FIXME: 以下は不要なので削除すること
        val data = Data.Builder().apply {
            putString("id", advanceDownloadTask.id)
            putString("url", advanceDownloadTask.url.toString())
            putString("path", advanceDownloadTask.filePath)
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
            putString("url", task.url.toString())
            putString("path", task.filePath)
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