package com.shibuiwilliam.firebase_tflite_arcore.ml


import android.annotation.SuppressLint
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import android.graphics.Bitmap
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionCloudImageLabelerOptions
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceImageLabelerOptions
import com.shibuiwilliam.firebase_tflite_arcore.common.Constants
import com.shibuiwilliam.firebase_tflite_arcore.common.FirebaseVisionImageUtils
import java.lang.Exception
import java.util.concurrent.TimeUnit

class ImageLabeler(private val mode: String,
                   private val confidenceThreshold: Float,
                   private val awaitMilliSeconds: Long) {
    private val TAG = "ImageLabeler"

    private var labeler: FirebaseVisionImageLabeler? = null
    private var labelerName: String = "ImageLabeler"
    var initialized = false

    init {
        generateName()
        initializeImageLabeler()
        initialized = true
    }

    private fun initializeImageLabeler(){
        labeler = when (mode){
            Constants.IMAGE_LABELER_ONDEVICE_MODE ->
                FirebaseVision
                    .getInstance()
                    .getOnDeviceImageLabeler(FirebaseVisionOnDeviceImageLabelerOptions
                        .Builder()
                        .setConfidenceThreshold(confidenceThreshold)
                        .build())
            Constants.IMAGE_LABELER_CLOUD_MODE ->
                FirebaseVision
                    .getInstance()
                    .getCloudImageLabeler(FirebaseVisionCloudImageLabelerOptions
                        .Builder()
                        .setConfidenceThreshold(confidenceThreshold)
                        .build())
            else -> null
        }
    }

    private fun generateName(){
        labelerName = when(mode){
            Constants.IMAGE_LABELER_ONDEVICE_MODE -> "Ondevice" + labelerName
            Constants.IMAGE_LABELER_CLOUD_MODE -> "Cloud" + labelerName
            else -> labelerName
        }
    }

    internal fun getName(): String = labelerName


    @SuppressLint("UnsafeExperimentalUsageError")
    fun processImage(image: FirebaseVisionImage): Task<List<FirebaseVisionImageLabel>> {
        if (!initialized || labeler == null){
            initializeImageLabeler()
        }
        return labeler!!.processImage(image)
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    fun processImage(image: Bitmap): Task<List<FirebaseVisionImageLabel>> {
        if (!initialized || labeler == null){
            initializeImageLabeler()
        }
        return labeler!!.processImage(
            FirebaseVisionImageUtils
                .imageFromBitmap(image))
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    fun processImageAwait(image: FirebaseVisionImage,
                          awaitMilliSeconds: Long=this.awaitMilliSeconds): List<FirebaseVisionImageLabel>? {
        if (!initialized || labeler == null){
            initializeImageLabeler()
        }
        try {
            return Tasks.await(
                processImage(image),
                awaitMilliSeconds,
                TimeUnit.MILLISECONDS
            )
        }
        catch (ex: Exception){
            Log.e(TAG, "${ex.printStackTrace()}")
            return null
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    fun processImageAwait(image: Bitmap,
                          awaitMilliSeconds: Long=this.awaitMilliSeconds): List<FirebaseVisionImageLabel>? {
        if (!initialized || labeler == null){
            initializeImageLabeler()
        }
        try {
            return Tasks.await(
                processImage(image),
                awaitMilliSeconds,
                TimeUnit.MILLISECONDS
            )
        }
        catch (ex: Exception){
            Log.e(TAG, "${ex.printStackTrace()}")
            return null
        }
    }

    fun close(){
        initialized = false
        labeler!!.close()
        Log.d(TAG, "Closed Firebase image labeler.")
    }
}

