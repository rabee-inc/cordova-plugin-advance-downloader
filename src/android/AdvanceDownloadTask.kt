package jp.rabee

import com.google.gson.annotations.SerializedName
import zlc.season.rxdownload4.manager.TaskManager

data class AdvanceDownloadTask(
        @SerializedName("id") val id: String,
        @SerializedName("url") val url: String,
        @SerializedName("size") val size: Int,
        @SerializedName("path") val filePath: String,
        @SerializedName("name") val fileName: String,
        @SerializedName("headers") val headers: MutableMap<String, String>
)
