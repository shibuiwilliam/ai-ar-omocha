package com.shibuiwilliam.firebase_tflite_arcore.common

import android.app.Application
import android.content.Context
import android.util.Log
import com.shibuiwilliam.firebase_tflite_arcore.ml.ImageClassifier
import com.shibuiwilliam.firebase_tflite_arcore.ml.ImageLabeler
import com.shibuiwilliam.firebase_tflite_arcore.ml.ImageSegmentation
import com.shibuiwilliam.firebase_tflite_arcore.ml.ObjectDetector

class Globals: Application(){
    val TAG = "Globals"

    var imageClassifierMap: MutableMap<String, ImageClassifier> = mutableMapOf()
    var objectDetector: ObjectDetector? = null
    var imageLabeler: ImageLabeler? = null
    var imageSegmentation: ImageSegmentation? = null
    var initialized = false

    override fun onCreate() {
        super.onCreate()
    }

    fun initialize(context: Context){
        initializeAllImageClassifier(context)

        initializeObjectDetector(
            Constants.OBJECT_DETECT.SINGLE_STREAM_OBJECT_DETECTOR)

        initializeImageLabeler(
            Constants.IMAGE_LABELER_MODE,
            Constants.IMAGE_LABELER_CONFIDENCE_THRESHOLD,
            Constants.IMAGE_LABELER_AWAIT_MILLISECOND)

        initializeImageSegmentation(
            context,
            Constants.IMAGE_SEGMENTATION_MODEL_NAME,
            Constants.IMAGE_SEGMENTATION_NUM_OF_BYTES_PER_CHANNEL,
            Constants.IMAGE_SEGMENTATION_DIM_BATCH_SIZE,
            Constants.IMAGE_SEGMENTATION_DIM_PIXEL_SIZE,
            Constants.IMAGE_SEGMENTATION_DIM_SIZE,
            Constants.IMAGE_SEGMENTATION_NUM_CLASSES,
            Constants.IMAGE_SEGMENTATION_AWAIT_MILLISECOND,
            Constants.IMAGE_SEGMENTATION_ALPHA,
            Constants.IMAGE_SEGMENTATION_COLOR,
            Constants.IMAGE_SEGMENTATION_PORTERDUFF
        )
        initialized = true
    }

    fun initializeAllImageClassifier(context: Context){
        initializeImageClassifier(context,
            Constants.IMAGE_CLASSIFIER.MOBILENETV2_IMAGE_CLASSIFIER)
        initializeImageClassifier(context,
            Constants.IMAGE_CLASSIFIER.MNASNET_IMAGE_CLASSIFIER)
    }

    fun initializeImageClassifier(context: Context,
                                  imageClassifier: Constants.IMAGE_CLASSIFIER){
        if (!imageClassifierMap.containsKey(imageClassifier.modelName)){
            Log.i(TAG, "Loading ${imageClassifier.modelName}")
            imageClassifierMap[imageClassifier.modelName] = ImageClassifier(
                context,
                imageClassifier.modelName,
                imageClassifier.imagenetLabelPath,
                imageClassifier.numOfBytesPerChannel,
                imageClassifier.dimBatchSize,
                imageClassifier.dimPixelSize,
                imageClassifier.dimImgSize,
                imageClassifier.quantized,
                imageClassifier.resultsToShow,
                imageClassifier.awaitMillisecond,
                imageClassifier.imageMean,
                imageClassifier.imageStd
            )
        }
        else{
            Log.i(TAG, "Using ${imageClassifier.modelName}")
        }
    }

    fun initializeObjectDetector(objectDetect: Constants.OBJECT_DETECT){
        if (objectDetector == null){
            Log.i(TAG, "Loading object detector")
            objectDetector = ObjectDetector(
                objectDetect.objectDetectName,
                objectDetect.objectDetectMode,
                objectDetect.objectDetectClassify,
                objectDetect.objectDetectMultiple,
                objectDetect.awaitMillisecond)
        }
        else{
            Log.i(TAG, "Using object detector")
        }
    }

    fun initializeImageLabeler(mode: String,
                               confidenceThreshold:Float,
                               awaitMilliSeconds: Long){
        if (imageLabeler == null){
            Log.i(TAG, "Loading image labeler")
            imageLabeler = ImageLabeler(
                mode,
                confidenceThreshold,
                awaitMilliSeconds)
        }
        else{
            Log.i(TAG, "Using image labeler")
        }
    }

    fun initializeImageSegmentation(context: Context,
                                    remoteModelName: String ,
                                    numOfBytesPerChannel: Int,
                                    dimBatchSize: Int,
                                    dimPixelSize: Int,
                                    dimImgSize: Int,
                                    numClasses: Int,
                                    awaitMilliSecond: Long,
                                    alpha: Int,
                                    color: String,
                                    porterDuff: String){
        if (imageSegmentation == null){
            Log.i(TAG, "Loading image segmentation")
            imageSegmentation = ImageSegmentation(
                context,
                remoteModelName,
                numOfBytesPerChannel,
                dimBatchSize,
                dimPixelSize,
                dimImgSize,
                numClasses,
                awaitMilliSecond,
                alpha,
                color,
                porterDuff)
        }
        else{
            Log.i(TAG, "Using image segmentation")
        }
    }

    fun closeAll(){
        imageSegmentation!!.close()
        objectDetector!!.close()
        imageLabeler!!.close()
        for ((_,v) in imageClassifierMap){
            v.close()
        }
    }
    override fun onTerminate() {
        super.onTerminate()
        closeAll()
    }

}
