package jp.rabee

import com.google.gson.annotations.SerializedName

enum class AdvanceDownloadStatus {
    WAITING,
    PROCESSING,
    PAUSED,
    CANCELED,
    COMPLETE,
    FAILED
}

data class AdvanceDownloadTask(
        @SerializedName("id") val id: String,
        @SerializedName("url") val url: String,
        @SerializedName("size") val size: Int,
        @SerializedName("path") val filePath: String,
        @SerializedName("name") val fileName: String,
        @SerializedName("headers") val headers: MutableMap<String, String>,
        @SerializedName("progress") val progress: Double = 0.0,
        @SerializedName("status") var status: AdvanceDownloadStatus = AdvanceDownloadStatus.WAITING
)
