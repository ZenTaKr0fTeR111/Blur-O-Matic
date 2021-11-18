/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.background

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.background.databinding.ActivityBlurBinding
import com.example.background.model.BlurViewModel
import com.example.background.model.BlurViewModelFactory
import com.example.background.workers.writeBitmapToFile
import java.io.IOException

class BlurActivity : AppCompatActivity() {
    private val viewModel: BlurViewModel by viewModels {
        BlurViewModelFactory(
            application
        )
    }
    private lateinit var binding: ActivityBlurBinding
    private lateinit var setImage: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlurBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setImage = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val bundle = result.data!!.extras
                var currentBitmap: Bitmap
                if (bundle == null) {
                    try {
                        if (Build.VERSION.SDK_INT < 28) {
                            currentBitmap = MediaStore.Images.Media.getBitmap(
                                contentResolver,
                                result.data!!.data
                               )
                        } else {
                            val source = ImageDecoder.createSource(
                                contentResolver, result.data!!.data!!)
                            currentBitmap = ImageDecoder.decodeBitmap(source)
                        }
                        binding.imageView.setImageBitmap(currentBitmap)
                        viewModel.imageUri = writeBitmapToFile(this, currentBitmap)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                } else {
                    currentBitmap = bundle["data"] as Bitmap
                    binding.imageView.setImageBitmap(currentBitmap)
                    viewModel.imageUri = writeBitmapToFile(this, currentBitmap)
                }
            }
        }

        binding.goButton.setOnClickListener { viewModel.applyBlur(blurLevel) }
        binding.seeFileButton.setOnClickListener {
            viewModel.outputUri?.let { currentUri ->
                val actionView = Intent(Intent.ACTION_VIEW, currentUri)
                actionView.resolveActivity(packageManager)?.run {
                    startActivity(actionView)
                }
            }
        }
        binding.cancelButton.setOnClickListener { viewModel.cancelWork() }
        binding.imageView.setOnClickListener { chooseImage() }

        viewModel.outputWorkInfo.observe(this) { listOfWorkInfo ->
            if (listOfWorkInfo.isNullOrEmpty()) {return@observe}
            val workInfo = listOfWorkInfo[0]
            if (workInfo.state.isFinished) {
                showWorkFinished()
                val outputUri = workInfo.outputData.getString(KEY_IMAGE_URI)
                if (!outputUri.isNullOrEmpty()) {
                    viewModel.setOutputUri(outputUri)
                    binding.seeFileButton.visibility = View.VISIBLE
                }
            } else {
                showWorkInProgress()
            }
        }
    }

    /**
     * Shows and hides views for when the Activity is processing an image
     */
    private fun showWorkInProgress() {
        with(binding) {
            progressBar.visibility = View.VISIBLE
            cancelButton.visibility = View.VISIBLE
            goButton.visibility = View.GONE
            seeFileButton.visibility = View.GONE
        }
    }

    /**
     * Shows and hides views for when the Activity is done processing an image
     */
    private fun showWorkFinished() {
        with(binding) {
            progressBar.visibility = View.GONE
            cancelButton.visibility = View.GONE
            goButton.visibility = View.VISIBLE
        }
    }

    private val blurLevel: Int
        get() =
            when (binding.radioBlurGroup.checkedRadioButtonId) {
                R.id.radio_blur_lv_1 -> 1
                R.id.radio_blur_lv_2 -> 2
                R.id.radio_blur_lv_3 -> 3
                else -> 1
            }

    private fun chooseImage() {
        val options = arrayOf(getString(R.string.option_take_photo),
            getString(R.string.option_choose_from_gallery),
            getString(R.string.cancel))
        val builder = AlertDialog.Builder(this)
            .setTitle(getString(R.string.add_photo))
            .setItems(options) { dialog, item ->
                if (options[item] == options[0]) {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    setImage.launch(intent)
                } else if (options[item] == options[1]) {
                    val intent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    setImage.launch(intent)
                } else {
                    dialog.dismiss()
                }
            }
        builder.create().show()
    }
}
