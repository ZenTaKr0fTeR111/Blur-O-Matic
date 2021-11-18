package com.example.background.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.background.KEY_IMAGE_URI
import java.lang.IllegalArgumentException

private const val TAG = "BlurWorker"
class BlurWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val appContext = applicationContext
        val blurResourceUri = inputData.getString(KEY_IMAGE_URI)
        makeStatusNotification("Blurring image", appContext)

        return try {
            if (TextUtils.isEmpty(blurResourceUri)) {
                throw IllegalArgumentException("Invalid input uri")
            }
            val picture = BitmapFactory.decodeStream(
                appContext.contentResolver.openInputStream(Uri.parse(blurResourceUri)))

            val blurredPic = blurBitmap(picture, appContext)
            val tempFile = writeBitmapToFile(appContext, blurredPic)
            val outputData = workDataOf(KEY_IMAGE_URI to tempFile.toString())

            makeStatusNotification("Blurring completed", appContext)
            Result.success(outputData)
        } catch (t: Throwable) {
            Log.e(TAG, "Error while blurring\n${t.message}")
            Result.failure()
        }
    }
}