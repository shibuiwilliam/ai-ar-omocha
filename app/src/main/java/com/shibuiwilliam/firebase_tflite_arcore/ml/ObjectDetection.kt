package com.shibuiwilliam.firebase_tflite_arcore.ml


import android.annotation.SuppressLint
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.objects.FirebaseVisionObject
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions
import com.shibuiwilliam.firebase_tflite_arcore.common.Constants
import java.lang.Exception
import java.util.concurrent.TimeUnit


class ObjectDetector(private val detectorName: String,
                     val mode: Int,
                     val enableClassification:Boolean,
                     val multipleDetection: Boolean,
                     val awaitMilliSecond: Long) {
    private val TAG = "ObjectDetectorProcessor"

    private var options: FirebaseVisionObjectDetectorOptions? = null
    private var detector: FirebaseVisionObjectDetector? = null
    var initialized = false

    init {
        configureOptions()
        initializeObjectDetector()
        initialized = true
    }

    private fun configureOptions(){
        options = when(multipleDetection) {
            true -> when(enableClassification) {
                true -> FirebaseVisionObjectDetectorOptions.Builder()
                    .setDetectorMode(mode)
                    .enableMultipleObjects()
                    .enableClassification()
                    .build()
                false -> FirebaseVisionObjectDetectorOptions.Builder()
                    .setDetectorMode(mode)
                    .enableMultipleObjects()
                    .build()
            }
            false -> when(enableClassification) {
                true -> FirebaseVisionObjectDetectorOptions.Builder()
                    .setDetectorMode(mode)
                    .enableClassification()
                    .build()
                false -> FirebaseVisionObjectDetectorOptions.Builder()
                    .setDetectorMode(mode)
                    .build()
            }
        }
    }

    private fun initializeObjectDetector(){
        detector = when (options){
            null -> FirebaseVision.getInstance().getOnDeviceObjectDetector()
            else -> FirebaseVision.getInstance().getOnDeviceObjectDetector(options!!)
        }
    }

    internal fun getName(): String = detectorName

    @SuppressLint("UnsafeExperimentalUsageError")
    fun processImage(image: FirebaseVisionImage): Task<List<FirebaseVisionObject>> {
        if (!initialized || detector == null){
            initializeObjectDetector()
        }
        return detector!!.processImage(image)
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    fun processImageAwait(image: FirebaseVisionImage,
                          awaitMilliSeconds: Long=this.awaitMilliSecond): List<FirebaseVisionObject>? {
        if (!initialized || detector == null){
            initializeObjectDetector()
        }
        try{
            return Tasks.await(processImage(image),
                awaitMilliSeconds,
                TimeUnit.MILLISECONDS)
        }
        catch (ex: Exception){
            Log.e(TAG, "${ex.printStackTrace()}")
            return null
        }
    }

    fun close(){
        initialized = false
        detector!!.close()
        Log.d(TAG, "Closed Firebase object detector.")
    }
}

