package jp.rabee

data class RequestResult(
        val downloadId: Long,
        val remoteUri: String,
        val localUri: String,
        val mediaType: String,
        val totalSize: Int,
        val title: String
)
