package jp.rabee

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2core.DownloadBlock
import com.tonyodev.fetch2core.Downloader
import org.apache.cordova.*
import org.json.JSONException
import org.json.*

class AdvanceDownloader : CordovaPlugin() {
    companion object {
        const val TAG = "AdvanceDownloader"
        const val TASK_KEY = "prefsTasks"
    }

    private lateinit var cContext: CallbackContext
    private lateinit var fetch: Fetch

    private var onChangedStatusCallbacks : MutableMap<String, MutableList<CallbackContext>> = mutableMapOf()
    private var onProgressCallbacks : MutableMap<String, MutableList<CallbackContext>> = mutableMapOf()
    private var onCompleteCallbacks : MutableMap<String, MutableList<CallbackContext>> = mutableMapOf()
    private var onFailedCallbacks : MutableMap<String, MutableList<CallbackContext>> = mutableMapOf()

    @Suppress("UNUSED_PARAMETER")
    private var prefsTasks: SharedPreferences
        get() = cordova.activity.applicationContext.getSharedPreferences(TASK_KEY, Context.MODE_PRIVATE)
        set(value) {}

    private val typeToken = object : TypeToken<MutableMap<String, AdvanceDownloadTask>>() {}

    // アプリ起動時に呼ばれる
    override public fun initialize(cordova: CordovaInterface,  webView: CordovaWebView) {
        val fetchConfiguration = FetchConfiguration.Builder(cordova.activity)
                .setNamespace(TAG)
                .setDownloadConcurrentLimit(3)
                .setHttpDownloader(HttpUrlConnectionDownloader(Downloader.FileDownloaderType.PARALLEL))
                .setNotificationManager(object : DefaultFetchNotificationManager(cordova.activity) {
                    override fun getFetchInstanceForNamespace(namespace: String): Fetch {
                        return fetch
                    }
                })
                .build()
        fetch = Fetch.Impl.getInstance(fetchConfiguration)


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
                cordova.threadPool.execute {
                    result = this.list(cContext)
                }
            }
            "getTasks" -> {
                val id = value.getString("id")
                cordova.threadPool.execute {
                    result = this.getTasks(id, cContext)
                }
            }
            "add" -> {
                val task = Gson().fromJson(value.toString(), AdvanceDownloadTask::class.java)
                task.request = Request(task.url, getFilePath(task.url))
                task.request?.apply {
                    priority = Priority.HIGH
                    networkType = NetworkType.ALL
                    for (header in task.headers) {
                        addHeader(header.key, header.value)
                    }
                }

                cordova.threadPool.execute {
                    result = this.add(task, cContext)
                }
            }
            "start" -> {
                val id = value.getString("id")
                cordova.threadPool.execute {
                    result = this.start(id, cContext)
                }
            }
            "pause" -> {
                val id = value.getString("id")
                cordova.threadPool.execute {
                    result = this.pause(id, cContext)
                }
            }
            "resume" -> {
                val id = value.getString("id")
                cordova.threadPool.execute {
                    result = this.resume(id, cContext)
                }
            }
            "stop" -> {
                val id = value.getString("id")
                cordova.threadPool.execute {
                    result = this.stop(id, cContext)
                }
            }
            "setOnChangedStatus" -> {
                val id = value.getString("id")
                cordova.threadPool.execute {
                    result = this.setOnChangedStatus(id, cContext)
                }
            }
            "removeOnChangedStatus" -> {
                val id = value.getString("id")
                cordova.threadPool.execute {
                    result = this.removeOnChangedStatus(id, cContext)
                }
            }
            "setOnProgress" -> {
                val id = value.getString("id")
                cordova.threadPool.execute {
                    result = this.setOnProgress(id, cContext)
                }
            }
            "removeOnProgress" -> {
                val id = value.getString("id")
                cordova.threadPool.execute {
                    result = this.removeOnProgress(id, cContext)
                }
            }
            "setOnComplete" -> {
                val id = value.getString("id")
                cordova.threadPool.execute {
                    result = this.setOnComplete(id, cContext)
                }
            }
            "removeOnComplete" -> {
                val id = value.getString("id")
                cordova.threadPool.execute {
                    result = this.removeOnComplete(id, cContext)
                }
            }
            "setOnFailed" -> {
                val id = value.getString("id")
                cordova.threadPool.execute {
                    result = this.setOnFailed(id, cContext)
                }
            }
            "removeOnFailed" -> {
                val id = value.getString("id")
                cordova.threadPool.execute {
                    result = this.removeOnFailed(id, cContext)
                }
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
        result.keepCallback = true
        callbackContext.sendPluginResult(result)

        //MEMO: 実行順番は問わない
        val requests = tasks.map { it.value.request } as MutableList<Request?>
        fetch.enqueue(requests.filterNotNull())
                .addListener(AdvanceFetchListener())

        return true
    }

    private fun pause(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        task.request?.id?.let { fetch.pause(it) }

        return true
    }

    private fun resume(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        result.keepCallback = true
        callbackContext.sendPluginResult(result)

        task.request?.id?.let { fetch.resume(it) }

        return true
    }

    private fun stop(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        task.request?.id?.let { fetch.remove(it) }

        tasks.remove(task.id)
        editTasks(tasks)

        return true
    }

    private fun setOnChangedStatus(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        onChangedStatusCallbacks[id]?.also { ctxs ->
            ctxs.add(callbackContext)
            onChangedStatusCallbacks[id] = ctxs
        }?:run {
            onChangedStatusCallbacks[id] = mutableListOf(callbackContext)
        }

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun removeOnChangedStatus(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        onChangedStatusCallbacks[id]?.let { ctxs ->
            ctxs.forEachIndexed { k, _ ->
                if (ctxs[k] == callbackContext) { ctxs.removeAt(k) }
            }
            onChangedStatusCallbacks[id] = ctxs
        }

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun setOnProgress(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        onProgressCallbacks[id]?.also { ctxs ->
            ctxs.add(callbackContext)
            onProgressCallbacks[id] = ctxs
        }?:run {
            onProgressCallbacks[id] = mutableListOf(callbackContext)
        }

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun removeOnProgress(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        onProgressCallbacks[id]?.let { ctxs ->
            ctxs.forEachIndexed { k, _ ->
                if (ctxs[k] == callbackContext) { ctxs.removeAt(k) }
            }
            onProgressCallbacks[id] = ctxs
        }

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun setOnComplete(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        onCompleteCallbacks[id]?.also { ctxs ->
            ctxs.add(callbackContext)
            onCompleteCallbacks[id] = ctxs
        }?:run {
            onCompleteCallbacks[id] = mutableListOf(callbackContext)
        }

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun removeOnComplete(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        onCompleteCallbacks[id]?.let { ctxs ->
            ctxs.forEachIndexed { k, _ ->
                if (ctxs[k] == callbackContext) { ctxs.removeAt(k) }
            }
            onCompleteCallbacks[id] = ctxs
        }

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun setOnFailed(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        onFailedCallbacks[id]?.also { ctxs ->
            ctxs.add(callbackContext)
            onFailedCallbacks[id] = ctxs
        }?:run {
            onFailedCallbacks[id] = mutableListOf(callbackContext)
        }

        val output = Gson().toJson(task)
        val result = PluginResult(PluginResult.Status.OK, output)
        callbackContext.sendPluginResult(result)

        return true
    }

    private fun removeOnFailed(id: String, callbackContext: CallbackContext): Boolean {
        val tasks = getTasks()
        val task = tasks[id]
        task ?: return false

        onFailedCallbacks[id]?.let { ctxs ->
            ctxs.forEachIndexed { k, _ ->
                if (ctxs[k] == callbackContext) { ctxs.removeAt(k) }
            }
            onFailedCallbacks[id] = ctxs
        }

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

    private fun getFilePath(url: String) : String {
        val url = Uri.parse(url)
        val fileName = url.lastPathSegment
        val dir = getSavedDir()
        return ("$dir/DownloadList/$fileName")

    }

    private fun getSavedDir() : String {
        return cordova.activity.applicationContext.filesDir.toString()
    }

    // MARK: - LifeCycle

    override fun onResume(multitasking: Boolean) {
        super.onResume(multitasking)
        fetch.addListener(AdvanceFetchListener())
    }

    override fun onPause(multitasking: Boolean) {
        super.onPause(multitasking)
        fetch.removeListener(AdvanceFetchListener())
    }

    override fun onDestroy() {
        super.onDestroy()
        fetch.close()
        prefsTasks.edit().clear().apply()
    }

    // MARK: - FetchListener

    private inner class AdvanceFetchListener : FetchListener {
        override fun onAdded(download: Download) {
            Log.d(TAG, "Added Download: ${download.url}")
        }

        override fun onCancelled(download: Download) {
            Log.d(TAG, "Cancelled Download: ${download.url}")

            var ctxs : MutableList<CallbackContext>? = null
            getTasks().forEach { entity ->
                if (entity.value.request?.id == download.request.id) {
                    onChangedStatusCallbacks[entity.value.id]?.let { ctxs = it }
                }
            }

            val r = PluginResult(PluginResult.Status.OK, download.status.toString())
            r.keepCallback = true
            ctxs?.forEach { it.sendPluginResult(r) }
        }

        override fun onCompleted(download: Download) {
            Log.d(TAG, "Completed Download: ${download.url}")

            var ctxs : MutableList<CallbackContext>? = null
            getTasks().forEach { entity ->
                if (entity.value.request?.id == download.request.id) {
                    onCompleteCallbacks[entity.value.id]?.let { ctxs = it }
                }
            }

            val r = PluginResult(PluginResult.Status.OK, download.url)
            r.keepCallback = true
            ctxs?.forEach { it.sendPluginResult(r) }
        }

        override fun onDeleted(download: Download) {
            Log.d(TAG, "Deleted Download: ${download.url}")

            var ctxs : MutableList<CallbackContext>? = null
            getTasks().forEach { entity ->
                if (entity.value.request?.id == download.request.id) {
                    onChangedStatusCallbacks[entity.value.id]?.let { ctxs = it }
                }
            }

            val r = PluginResult(PluginResult.Status.OK, download.status.toString())
            r.keepCallback = true
            ctxs?.forEach { it.sendPluginResult(r) }

        }

        override fun onDownloadBlockUpdated(download: Download, downloadBlock: DownloadBlock, totalBlocks: Int) {
            Log.d(TAG, "DownloadBlockUpdated Download: ${download.url}")
        }

        override fun onError(download: Download, error: Error, throwable: Throwable?) {
            Log.d(TAG, "Error Download: ${download.url}")

            var ctxs : MutableList<CallbackContext>? = null
            getTasks().forEach { entity ->
                if (entity.value.request?.id == download.request.id) {
                    onFailedCallbacks[entity.value.id]?.let { ctxs = it }
                }
            }

            val r = PluginResult(PluginResult.Status.OK, download.error.toString())
            r.keepCallback = true
            ctxs?.forEach { it.sendPluginResult(r) }
        }

        override fun onPaused(download: Download) {
            Log.d(TAG, "Paused Download: ${download.url}")

            var ctxs : MutableList<CallbackContext>? = null
            getTasks().forEach { entity ->
                if (entity.value.request?.id == download.request.id) {
                    onChangedStatusCallbacks[entity.value.id]?.let { ctxs = it }
                }
            }

            val r = PluginResult(PluginResult.Status.OK, download.status.toString())
            r.keepCallback = true
            ctxs?.forEach { it.sendPluginResult(r) }
        }

        override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
            Log.d(TAG, "Progress Download: ${download.progress}")

            var ctxs : MutableList<CallbackContext>? = null
            getTasks().forEach { entity ->
                if (entity.value.request?.id == download.request.id) {
                    onProgressCallbacks[entity.value.id]?.let { ctxs = it }
                }
            }

            val r = PluginResult(PluginResult.Status.OK, download.progress)
            r.keepCallback = true
            ctxs?.forEach { it.sendPluginResult(r) }
        }

        override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
            Log.d(TAG, "Queued Download: ${download.url}")

            var ctxs : MutableList<CallbackContext>? = null
            getTasks().forEach { entity ->
                if (entity.value.request?.id == download.request.id) {
                    onChangedStatusCallbacks[entity.value.id]?.let { ctxs = it }
                }
            }

            val r = PluginResult(PluginResult.Status.OK, download.status.toString())
            r.keepCallback = true
            ctxs?.forEach { it.sendPluginResult(r) }
        }

        override fun onRemoved(download: Download) {
            Log.d(TAG, "Removed Download: ${download.url}")

            var ctxs : MutableList<CallbackContext>? = null
            getTasks().forEach { entity ->
                if (entity.value.request?.id == download.request.id) {
                    onChangedStatusCallbacks[entity.value.id]?.let { ctxs = it }
                }
            }

            val r = PluginResult(PluginResult.Status.OK, download.status.toString())
            r.keepCallback = true
            ctxs?.forEach { it.sendPluginResult(r) }
        }

        override fun onResumed(download: Download) {
            Log.d(TAG, "Resumed Download: ${download.url}")

            var ctxs : MutableList<CallbackContext>? = null
            getTasks().forEach { entity ->
                if (entity.value.request?.id == download.request.id) {
                    onChangedStatusCallbacks[entity.value.id]?.let { ctxs = it }
                }
            }

            val r = PluginResult(PluginResult.Status.OK, download.status.toString())
            r.keepCallback = true
            ctxs?.forEach { it.sendPluginResult(r) }
        }

        override fun onStarted(download: Download, downloadBlocks: List<DownloadBlock>, totalBlocks: Int) {
            Log.d(TAG, "Started Download: ${download.url}")

            var ctxs : MutableList<CallbackContext>? = null
            getTasks().forEach { entity ->
                if (entity.value.request?.id == download.request.id) {
                    onChangedStatusCallbacks[entity.value.id]?.let { ctxs = it }
                }
            }

            val r = PluginResult(PluginResult.Status.OK, download.status.toString())
            r.keepCallback = true
            ctxs?.forEach { it.sendPluginResult(r) }
        }

        override fun onWaitingNetwork(download: Download) {
            Log.d(TAG, "WaitingNetwork Download: ${download.url}")

            var ctxs : MutableList<CallbackContext>? = null
            getTasks().forEach { entity ->
                if (entity.value.request?.id == download.request.id) {
                    onChangedStatusCallbacks[entity.value.id]?.let { ctxs = it }
                }
            }

            val r = PluginResult(PluginResult.Status.OK, download.status.toString())
            r.keepCallback = true
            ctxs?.forEach { it.sendPluginResult(r) }
        }
    }
}