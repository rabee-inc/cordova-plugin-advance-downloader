package jp.rabee

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.apache.cordova.*
import org.json.JSONException
import org.json.*
import zlc.season.rxdownload4.manager.*
import zlc.season.rxdownload4.notification.SimpleNotificationCreator


class AdvanceDownloader : CordovaPlugin() {
    lateinit var cContext: CallbackContext
    lateinit var prefsTasks: SharedPreferences
    lateinit var prefsChangeStatusCallback: SharedPreferences
    lateinit var prefsProgressCallback: SharedPreferences
    lateinit var prefsCompleteCallback: SharedPreferences
    lateinit var prefsFailedCallback: SharedPreferences

    private val typeToken = object : TypeToken<MutableMap<String, AdvanceDownloadTask>>() {}

    // アプリ起動時に呼ばれる
    override public fun initialize(cordova: CordovaInterface,  webView: CordovaWebView) {
        prefsTasks = cordova.activity.applicationContext.getSharedPreferences(TASK_KEY, Context.MODE_PRIVATE)
        prefsChangeStatusCallback = cordova.activity.applicationContext.getSharedPreferences(STATUS_KEY, Context.MODE_PRIVATE)
        prefsProgressCallback = cordova.activity.applicationContext.getSharedPreferences(PROGRESS_KEY, Context.MODE_PRIVATE)
        prefsCompleteCallback = cordova.activity.applicationContext.getSharedPreferences(COMPLETE_KEY, Context.MODE_PRIVATE)
        prefsFailedCallback = cordova.activity.applicationContext.getSharedPreferences(FAILED_KEY, Context.MODE_PRIVATE)

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
            "setOnComplete" -> {
                val id = value.getString("id")
                result = this.setOnComplete(id, cContext)
            }
            "removeOnComplete" -> {
                val id = value.getString("id")
                result = this.removeOnComplete(id, cContext)
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
        editTasks(tasks)

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

        task.manager = task.url.manager(header = task.headers, notificationCreator = SimpleNotificationCreator())
        task.tag = task.manager.subscribe { status ->
            when(status) {
                is Normal -> {
                    //do nothing.
                }
                is Started,
                is Paused -> {
                    if (prefsChangeStatusCallback.getBoolean(STATUS_KEY, true)) {
                        val r = PluginResult(PluginResult.Status.OK, status.toString())
                        callbackContext.sendPluginResult(r)
                    }
                }
                is Downloading -> {
                    if (prefsProgressCallback.getBoolean(PROGRESS_KEY, true)) {
                        val r = PluginResult(PluginResult.Status.OK, status.progress.percentStr())
                        callbackContext.sendPluginResult(r)
                    }
                }
                is Completed -> {
                    if (prefsCompleteCallback.getBoolean(COMPLETE_KEY, true)) {
                        val r = PluginResult(PluginResult.Status.OK, task.filePath + "/" + task.fileName)
                        callbackContext.sendPluginResult(r)
                    }
                }
                is Failed -> {
                    if (prefsFailedCallback.getBoolean(FAILED_KEY, true)) {
                        val r = PluginResult(PluginResult.Status.OK, status.throwable.printStackTrace().toString())
                        callbackContext.sendPluginResult(r)
                    }
                }
                is Deleted -> {}
            }
        }
        task.manager.start()

        tasks[task.id] = task
        editTasks(tasks)

        return true
    }

    private fun pause(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        task.manager.stop()

        return true
    }

    private fun resume(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        task.manager.start()

        return true
    }

    private fun stop(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        task.manager.delete()
        task.manager.dispose(task.tag)

        tasks.remove(task.id)
        editTasks(tasks)

        return true
    }

    private fun setOnChangedStatus(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        prefsChangeStatusCallback.edit().putBoolean(STATUS_KEY, true).apply()

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun removeOnChangedStatus(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        prefsChangeStatusCallback.edit().putBoolean(STATUS_KEY, false).apply()

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun setOnProgress(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        prefsProgressCallback.edit().putBoolean(PROGRESS_KEY, true).apply()

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun removeOnProgress(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        prefsProgressCallback.edit().putBoolean(PROGRESS_KEY, false).apply()

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun setOnComplete(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        prefsCompleteCallback.edit().putBoolean(COMPLETE_KEY, true).apply()

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun removeOnComplete(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        prefsCompleteCallback.edit().putBoolean(COMPLETE_KEY, false).apply()

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun setOnFailed(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        prefsFailedCallback.edit().putBoolean(FAILED_KEY, true).apply()

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun removeOnFailed(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        prefsFailedCallback.edit().putBoolean(FAILED_KEY, false).apply()

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun getTasks(): MutableMap<String, AdvanceDownloadTask> {
        return Gson().fromJson(prefsTasks.getString(TASK_KEY, "{}"), typeToken.type)
    }

    private fun editTasks(tasks: MutableMap<String, AdvanceDownloadTask>) {
        prefsTasks.edit().putString(TASK_KEY, Gson().toJson(tasks)).apply()
    }

    companion object {
        val TAG = "AdvanceDownloader"

        val TASK_KEY = "prefsTasks"
        val STATUS_KEY = "prefsChangeStatusCallback"
        val PROGRESS_KEY = "prefsProgressCallback"
        val COMPLETE_KEY = "prefsCompleteCallback"
        val FAILED_KEY = "prefsFailedCallback"
    }
}