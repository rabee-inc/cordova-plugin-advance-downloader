package jp.rabee

import android.net.Uri
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
        @SerializedName("url") val url: Uri,
//        val headers: MutableMap<String, String>,
//        val size: Int,
        @SerializedName("path") val filePath: String,
//        val fileName: String,
        @SerializedName("progress") val progress: Double = 0.0,
        @SerializedName("status") var status: AdvanceDownloadStatus = AdvanceDownloadStatus.WAITING
)
