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


class ObjectDetector(private val mode: Int,
                     private val enableClassification:Boolean,
                     private val multipleDetection: Boolean,
                     private val awaitMilliSeconds: Long) {
    private val TAG = "ObjectDetectorProcessor"

    private var options: FirebaseVisionObjectDetectorOptions? = null
    private var detector: FirebaseVisionObjectDetector? = null
    private var detectorName: String = "ObjectDetector"
    var initialized = false

    init {
        generateName()
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

    private fun generateName(){
        when (Constants.OBJECT_DETECT_MODE){
            Constants.OBJECT_DETECT_STREAM_MODE -> detectorName = "Stream" + detectorName
            Constants.OBJECT_DETECT_SINGLE_MODE -> detectorName = "Single" + detectorName
            else -> detectorName = detectorName
        }
        if (multipleDetection){
            detectorName = "Multiple" + detectorName
        }
        if (enableClassification){
            detectorName += "Classifier"
        }
    }

    internal fun getName(): String = detectorName

    private fun initializeObjectDetector(){
        detector = when (options){
            null -> FirebaseVision.getInstance().getOnDeviceObjectDetector()
            else -> FirebaseVision.getInstance().getOnDeviceObjectDetector(options!!)
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    fun processImage(image: FirebaseVisionImage): Task<List<FirebaseVisionObject>> {
        if (!initialized || detector == null){
            initializeObjectDetector()
        }
        return detector!!.processImage(image)
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    fun processImageAwait(image: FirebaseVisionImage,
                          awaitMilliSeconds: Long=this.awaitMilliSeconds): List<FirebaseVisionObject>? {
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

