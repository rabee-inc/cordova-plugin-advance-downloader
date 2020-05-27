package jp.rabee

import org.apache.cordova.*
import org.json.JSONException
import android.util.Log
import org.json.*


class AdvanceDownloader : CordovaPlugin() {
    lateinit var context: CallbackContext

    // 別の callback context を用意する
    lateinit var onProgressCallbackContext: CallbackContext

    // アプリ起動時に呼ばれる
    override public fun initialize(cordova: CordovaInterface,  webView: CordovaWebView) {
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

        val result = PluginResult(PluginResult.Status.OK, output)
        // callback を何回も呼び出したい場合は以下を既述する(ダウンロードの進捗状況を返したい時など)
        //  result.keepCallback = true
        callbackContext.sendPluginResult(result)

        return true;
//      時間のかかる処理とか非同期処理は cordova threadPool を使って下しあ
//        cordova.threadPool.run {
//            // TODO: 時間のかかる処理
//        }
    }

    companion object {
        protected val TAG = "AdvanceDownloader"
    }
}