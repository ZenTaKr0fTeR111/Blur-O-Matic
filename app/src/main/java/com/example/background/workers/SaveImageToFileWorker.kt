package com.example.background.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.background.KEY_IMAGE_URI

private const val TAG = "SaveImageToFileWorker"
class SaveImageToFileWorker(context: Context, params: WorkerParameters) : Worker(context,params) {

    override fun doWork(): Result {
        makeStatusNotification("Saving image", applicationContext)

        val resolver = applicationContext.contentResolver
        return try {
            val resourceUri = inputData.getString(KEY_IMAGE_URI)
            val bitmap = BitmapFactory.decodeStream(
                resolver.openInputStream(Uri.parse(resourceUri)))
            val imageUri = saveImageInQ(applicationContext, bitmap).toString()

            if (imageUri.isNotEmpty()) {
                val output = workDataOf(KEY_IMAGE_URI to imageUri)
                makeStatusNotification("Image saved", applicationContext)
                Result.success(output)
            } else {
                Log.e(TAG, "Writing to MediaStore failed")
                Result.failure()
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
            Result.failure()
        }
    }
}