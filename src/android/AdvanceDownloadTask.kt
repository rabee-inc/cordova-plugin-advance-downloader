package jp.rabee

import com.google.gson.annotations.SerializedName
import com.tonyodev.fetch2.Request

data class AdvanceDownloadTask(
        @SerializedName("id") val id: String,
        @SerializedName("url") val url: String,
        @SerializedName("size") val size: Int,
        @SerializedName("file_path") val filePath: String,
        @SerializedName("file_name") val fileName: String,
        @SerializedName("headers") val headers: MutableMap<String, String>,
        @SerializedName("request") var request: Request? = null,
        @SerializedName("index") var index: Long
)
