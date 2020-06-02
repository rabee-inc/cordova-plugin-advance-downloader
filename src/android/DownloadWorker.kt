package jp.rabee

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

class DownloadWorker(cxt: Context, params: WorkerParameters) : Worker(cxt, params) {
    override fun doWork(): Result {
        //FIXME: index.jsからの値が設定されていないため仮データ
        val name = "dummy.pdf"
        val path = "tmp"

        var count = 0
        try {
            val url = URL( inputData.getString("url"))
            val connection: HttpURLConnection? = url.openConnection() as HttpURLConnection?
            connection?.requestMethod = "GET"
            connection?.readTimeout = 20000
            connection?.connectTimeout = 20000
            connection?.setRequestProperty("Accept-Encoding", "identity")
            connection?.useCaches = false
            connection?.connect()

            if (connection != null && connection.responseCode == 200) {
                val lengthOfFile: Int? = connection.contentLength
                val input: BufferedInputStream? = BufferedInputStream(url.openStream());
                val outputFile = File(applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), path + "/" + name)
                val output = FileOutputStream(outputFile)
                val outputStream = BufferedOutputStream(output)

                var total: Long = 0
                val data = ByteArray(1024)
                while ({ count = input?.read(data)!!;count }() != -1) {
                    total += count.toLong()
                    val progress = (total * 100 / lengthOfFile!!)
                    Log.d("Main Activity", "Progress: " + progress)
                    outputStream.write(data, 0, count)
                }

                outputStream.flush()
                outputStream.close()
                input?.close()
                connection.disconnect()
            }

        } catch (e: Exception) {
            print("Error: " + e.message)

            return Result.failure()
        }
        return Result.success()
    }
}