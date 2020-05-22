package cordova.plugin

import org.apache.cordova.*
import org.json.JSONArray
import org.json.JSONException
import android.util.Log

class AdvanceDownloader : CordovaPlugin() {
    lateinit var context: CallbackContext

    // アプリ起動時に呼ばれる
    public fun initialize(cordova: CordovaInterface,  webView: CordovaWebView) {
        println("hi! This is AdvanceDownloader. Now intitilaizing ...")
    }

    // js 側で関数が実行されるとこの関数がまず発火する
    @Throws(JSONException::class)
    override fun execute(action: String, data: JSONArray, callbackContext: CallbackContext): Boolean {
        context = callbackContext
        var result = true
        when(action) {
            "add" -> {
                val value = data.getString(0)
                result = this.add(value, context)
            }
            else -> {
                // TODO error
            }
        }

        return result

    }

    // ダウンロードの追加
    private fun add(inputValue: String, callbackContext: CallbackContext): Boolean {
        val input = inputValue
        val output = "Kotlin says \"$input\""
        callbackContext.success(output)
        return true;
    }

    companion object {
        protected val TAG = "AdvanceDownloader"
    }
}