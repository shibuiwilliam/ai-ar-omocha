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
            Constants.OBJECT_DETECT_SINGLE_MODE,
            Constants.OBJECT_DETECT_CLASSIFICATION,
            Constants.OBJECT_DETECT_MULTIPLE,
            Constants.OBJECT_DETECT_AWAIT_MILLISECOND)

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
            Constants.MOBILENETV2_IMAGE_CLASSIFIER.MODEL_NAME,
            Constants.MOBILENETV2_IMAGE_CLASSIFIER.IMAGENET_LABEL_PATH,
            Constants.MOBILENETV2_IMAGE_CLASSIFIER.NUM_OF_BYTES_PER_CHANNEL,
            Constants.MOBILENETV2_IMAGE_CLASSIFIER.DIM_BATCH_SIZE,
            Constants.MOBILENETV2_IMAGE_CLASSIFIER.DIM_PIXEL_SIZE,
            Constants.MOBILENETV2_IMAGE_CLASSIFIER.DIM_IMG_SIZE,
            Constants.MOBILENETV2_IMAGE_CLASSIFIER.QUANTIZED,
            Constants.MOBILENETV2_IMAGE_CLASSIFIER.RESULTS_TO_SHOW,
            Constants.MOBILENETV2_IMAGE_CLASSIFIER.AWAIT_MILLISECOND)
        initializeImageClassifier(context,
            Constants.MNASNET_IMAGE_CLASSIFIER.MODEL_NAME,
            Constants.MNASNET_IMAGE_CLASSIFIER.IMAGENET_LABEL_PATH,
            Constants.MNASNET_IMAGE_CLASSIFIER.NUM_OF_BYTES_PER_CHANNEL,
            Constants.MNASNET_IMAGE_CLASSIFIER.DIM_BATCH_SIZE,
            Constants.MNASNET_IMAGE_CLASSIFIER.DIM_PIXEL_SIZE,
            Constants.MNASNET_IMAGE_CLASSIFIER.DIM_IMG_SIZE,
            Constants.MNASNET_IMAGE_CLASSIFIER.QUANTIZED,
            Constants.MNASNET_IMAGE_CLASSIFIER.RESULTS_TO_SHOW,
            Constants.MNASNET_IMAGE_CLASSIFIER.AWAIT_MILLISECOND)
    }

    fun initializeImageClassifier(context: Context,
                                  remoteModelName: String ,
                                  labelPath: String,
                                  numOfBytesPerChannel: Int,
                                  dimBatchSize: Int,
                                  dimPixelSize: Int,
                                  dimImgSize: Int,
                                  quantized: Boolean,
                                  resultsToShow: Int,
                                  awaitMilliSecond: Long){
        if (!imageClassifierMap.containsKey(remoteModelName)){
            Log.i(TAG, "Loading ${remoteModelName}")
            imageClassifierMap[remoteModelName] = ImageClassifier(
                context,
                remoteModelName,
                labelPath,
                numOfBytesPerChannel,
                dimBatchSize,
                dimPixelSize,
                dimImgSize,
                quantized,
                resultsToShow,
                awaitMilliSecond
            )
        }
        else{
            Log.i(TAG, "Using ${remoteModelName}")
        }
    }

    fun initializeObjectDetector(mode:Int,
                                 enableClassification:Boolean,
                                 multipleDetection: Boolean,
                                 awaitMilliSeconds: Long){
        if (objectDetector == null){
            Log.i(TAG, "Loading object detector")
            objectDetector = ObjectDetector(
                mode,
                enableClassification,
                multipleDetection,
                awaitMilliSeconds)
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
