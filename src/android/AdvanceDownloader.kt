package jp.rabee

import android.app.DownloadManager
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.apache.cordova.*
import org.json.JSONException
import org.json.*
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers.io

class AdvanceDownloader : CordovaPlugin() {
    lateinit var cContext: CallbackContext
    lateinit var mPrefs: SharedPreferences

    private val typeToken = object : TypeToken<MutableMap<String, AdvanceDownloadTask>>() {}

    // アプリ起動時に呼ばれる
    override public fun initialize(cordova: CordovaInterface,  webView: CordovaWebView) {
        mPrefs = cordova.activity.applicationContext.getSharedPreferences(TAG, Context.MODE_PRIVATE)

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
            "getTasks" -> {
                val id = value.getString("id")
                result = this.getTasks(id, cContext)
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
            "stop" -> {
                val id = value.getString("id")
                result = this.stop(id, cContext)
            }
            "setOnChangedStatus" -> {
                val id = value.getString("id")
                result = this.setOnChangedStatus(id, cContext)
            }
            "removeOnChangedStatus" -> {
                val id = value.getString("id")
                result = this.removeOnChangedStatus(id, cContext)
            }
            "setOnProgress" -> {
                val id = value.getString("id")
                result = this.setOnProgress(id, cContext)
            }
            "removeOnProgress" -> {
                val id = value.getString("id")
                result = this.removeOnProgress(id, cContext)
            }
            "setOnFailed" -> {
                val id = value.getString("id")
                result = this.setOnFailed(id, cContext)
            }
            "removeOnFailed" -> {
                val id = value.getString("id")
                result = this.removeOnFailed(id, cContext)
            }
            else -> {
                // TODO error
            }
        }

        return result
    }

    private fun list(callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val output = Gson().toJson(tasks)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)
        return true
    }

    private fun getTasks(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)
        return true
    }

    private fun add(advanceDownloadTask: AdvanceDownloadTask, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        tasks[advanceDownloadTask.id] = advanceDownloadTask
        mPrefs.edit().putString(TAG, Gson().toJson(tasks)).apply()

        val output = Gson().toJson(advanceDownloadTask)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun start(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

//        val uri = Uri.parse(task.url)
//        val request = DownloadManager.Request(uri).apply {
//            setTitle(task.fileName)
//            task.headers.forEach { (k, v) ->
//                addRequestHeader(k, v)
//            }
//            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
//            setDestinationInExternalFilesDir(cordova.activity.applicationContext, Environment.DIRECTORY_DOWNLOADS, task.fileName)
//        }
//
//        request.execute(cordova.activity.applicationContext, task)
//                .subscribeOn(io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe({ status ->
//                    when (status) {
//                        is RxDownloader.DownloadStatus.Complete -> {
//                            Log.d(TAG, "Successful Result: ${status.result.title}")
//                        }
//                        is RxDownloader.DownloadStatus.Processing -> {
//                            Log.d(TAG, "Processing Progress: ${status.progress}")
//                        }
//                        is RxDownloader.DownloadStatus.Paused -> {
//                            Log.d(TAG, "Paused Reason: ${status.reason}")
//                        }
//                        is RxDownloader.DownloadStatus.Waiting -> {
//                            Log.d(TAG, "Waiting Result: ${status.result.title}")
//                        }
//                        is RxDownloader.DownloadStatus.Failed -> {
//                            Log.d(TAG, "Failed Reason: ${status.reason}")
//                        }
//                    }
//                }, { error ->
//                    error.stackTrace
//                }, {
//                    Log.d(TAG, "Complete downloads.")
//                })

        return true
    }

    private fun pause(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun resume(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun stop(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun setOnChangedStatus(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun removeOnChangedStatus(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun setOnProgress(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun removeOnProgress(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun setOnFailed(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun removeOnFailed(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun getTasks(): MutableMap<String, AdvanceDownloadTask> {
        return Gson().fromJson(mPrefs.getString(TAG, "{}"), typeToken.type)
    }

    companion object {
        val TAG = "AdvanceDownloader"
    }
}