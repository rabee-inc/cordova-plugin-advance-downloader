package jp.rabee

import android.net.Uri
import org.apache.cordova.*
import org.json.JSONException
import org.json.*

class AdvanceDownloader : CordovaPlugin() {
    lateinit var context: CallbackContext

    // 別の callback context を用意する
    lateinit var onProgressCallbackContext: CallbackContext

    val onChangedStatusCallbackIDs = mutableMapOf<String, Array<String>>()
    val onProgressCallbackIDs = mutableMapOf<String, Array<String>>()
    val onCompleteCallbackIDs = mutableMapOf<String, Array<String>>()
    val onFailedCallbackIDs = mutableMapOf<String, Array<String>>()

    // アプリ起動時に呼ばれる
    override public fun initialize(cordova: CordovaInterface,  webView: CordovaWebView) {
        println("hi! This is AdvanceDownloader. Now intitilaizing ...")
    }

    // js 側で関数が実行されるとこの関数がまず発火する
    @Throws(JSONException::class)
    override fun execute(action: String, data: JSONArray, callbackContext: CallbackContext): Boolean {
        var result = true
        when(action) {
            "add" -> {
                val value = data.getJSONObject(0)
                result = this.add(value, context)
            }
            else -> {
                // TODO error
            }
        }

        return result
    }

    // ダウンロードの追加
    private fun add(inputValue: JSONObject, callbackContext: CallbackContext): Boolean {

        val input = inputValue.get("url")
        val output = "Kotlin says:" + input

//        val value = data.getString(0)
//        val urlStr = data.getString(1)
//        val url = Uri.parse(urlStr)
//        val headers = data.getJSONObject(2)
//        val size = data.getInt(3)
//        val filePath = data.getString(4)
//        val fileName = data.getString(4)

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