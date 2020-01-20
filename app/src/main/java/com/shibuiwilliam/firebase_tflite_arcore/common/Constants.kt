package com.shibuiwilliam.firebase_tflite_arcore.common

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.util.SparseIntArray
import android.view.Surface
import androidx.camera.core.CameraX
import com.google.ar.core.Config
import com.google.firebase.ml.vision.objects.FirebaseVisionObject
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions

object Constants {

    /**
     * Spinners
     */
    const val MLTARGET_FULL_SCREEN = "Full Screen"
    const val MLTARGET_OBJECT_DETECTION = "Object Detection"
    const val MLTARGET_Rectangle = "Rectangle"
    const val MLCLASSIFIER_QUANT_IMAGENET = "Quant ImageNet"
    const val MLCLASSIFIER_FLOAT_IMAGENET = "Float ImageNet"
    const val MLCLASSIFIER_IMAGE_LABELER = "Image Labeler"
    val MLTARGET_ARRAY = arrayOf(MLTARGET_FULL_SCREEN,
        MLTARGET_OBJECT_DETECTION,
        MLTARGET_Rectangle)
    val MLCLASSIFIER_ARRAY = arrayOf(MLCLASSIFIER_QUANT_IMAGENET,
        MLCLASSIFIER_FLOAT_IMAGENET,
        MLCLASSIFIER_IMAGE_LABELER)

    /**
     * camera configurations
     */
    const val BACK_CAMERA2 = CameraCharacteristics.LENS_FACING_BACK
    const val FRONT_CAMERA2 = CameraCharacteristics.LENS_FACING_FRONT
    val BACK_CAMERAX = CameraX.LensFacing.BACK
    val FRONT_CAMERAX = CameraX.LensFacing.FRONT
    val FACING_CAMERAX = BACK_CAMERAX

    val ORIENTATIONS = SparseIntArray()
    init{
        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)
    }

    /**
     * Image Classifier Constants
     */
    enum class IMAGE_CLASSIFIER(
        val modelName: String,
        val imagenetLabelPath: String,
        val resultsToShow: Int,
        val dimBatchSize: Int,
        val dimPixelSize: Int,
        val numOfBytesPerChannel: Int,
        val dimImgSize: Int,
        val quantized: Boolean,
        val awaitMillisecond: Long,
        val imageMean: Float,
        val imageStd: Float){
        MOBILENETV2_IMAGE_CLASSIFIER(
            "mobilenet_quant_v2_1_0_299_tflite",
            "imagenet_labels.txt",
            5,
            1,
            3,
            1,
            299,
            true,
            200L,
            127.5f,
            127.5f),
        MNASNET_IMAGE_CLASSIFIER(
            "mnasnet_1_3_224_tflite",
            "imagenet_labels.txt",
            3,
            1,
            3,
            4,
            224,
            false,
            300L,
            127.5f,
            127.5f),
        CLASSIFIER_ACTIVE_MODEL(
            MOBILENETV2_IMAGE_CLASSIFIER.modelName,
            MOBILENETV2_IMAGE_CLASSIFIER.imagenetLabelPath,
            MOBILENETV2_IMAGE_CLASSIFIER.resultsToShow,
            MOBILENETV2_IMAGE_CLASSIFIER.dimBatchSize,
            MOBILENETV2_IMAGE_CLASSIFIER.dimPixelSize,
            MOBILENETV2_IMAGE_CLASSIFIER.numOfBytesPerChannel,
            MOBILENETV2_IMAGE_CLASSIFIER.dimImgSize,
            MOBILENETV2_IMAGE_CLASSIFIER.quantized,
            MOBILENETV2_IMAGE_CLASSIFIER.awaitMillisecond,
            MOBILENETV2_IMAGE_CLASSIFIER.imageMean,
            MOBILENETV2_IMAGE_CLASSIFIER.imageStd)
    }

    const val IMAGE_FORMAT_YUV_420_888 = ImageFormat.YUV_420_888
    const val IMAGE_FORMAT_JPEG = ImageFormat.JPEG
    const val IMAGE_FORMAT_NV21 = ImageFormat.NV21
    const val IMAGE_FORMAT = IMAGE_FORMAT_NV21

    /**
     * object detections
     */
    enum class OBJECT_DETECT(
        val objectDetectName: String,
        val objectDetectMode: Int,
        val objectDetectMultiple: Boolean,
        val objectDetectClassify: Boolean,
        val awaitMillisecond: Long){
        SINGLE_STREAM_OBJECT_DETECTOR(
            "SINGLE_STREAM_OBJECT_DETECTOR",
            FirebaseVisionObjectDetectorOptions.STREAM_MODE,
            false,
            false,
            200L
        ),
        SINGLE_SINGLE_OBJECT_DETECTOR(
            "SINGLE_SINGLE_OBJECT_DETECTOR",
            FirebaseVisionObjectDetectorOptions.SINGLE_IMAGE_MODE,
            false,
            false,
            200L
        ),
        MULTIPLE_STREAM_OBJECT_DETECTOR(
            "MULTIPLE_STREAM_OBJECT_DETECTOR",
            FirebaseVisionObjectDetectorOptions.STREAM_MODE,
            true,
            false,
            200L
        ),
        MULTIPLE_SINGLE_OBJECT_DETECTOR(
            "MULTIPLE_SINGLE_OBJECT_DETECTOR",
            FirebaseVisionObjectDetectorOptions.SINGLE_IMAGE_MODE,
            true,
            false,
            200L
        )
    }

    val OBJECT_DETECTION_DEFAULT_CATEGORIES: Map<Int, String> = mapOf(
        FirebaseVisionObject.CATEGORY_UNKNOWN to "Unknown",
        FirebaseVisionObject.CATEGORY_HOME_GOOD to "Home Goods",
        FirebaseVisionObject.CATEGORY_FASHION_GOOD to "Fashion Goods",
        FirebaseVisionObject.CATEGORY_FOOD to "Food",
        FirebaseVisionObject.CATEGORY_PLACE to "Place",
        FirebaseVisionObject.CATEGORY_PLANT to "Plant"
    )

    /**
     * image labelers
     */
    const val IMAGE_LABELER_MODE = "ondevice"
    const val IMAGE_LABELER_CLOUD_MODE = "cloud"
    const val IMAGE_LABELER_ONDEVICE_MODE = "ondevice"
    const val IMAGE_LABELER_CONFIDENCE_THRESHOLD = 0.7f
    const val IMAGE_LABELER_AWAIT_MILLISECOND = 200L


    /**
     * arcore configurations
     */
    const val MIN_OPENGL_VERSION = 3.0

    const val FOCUS_ON_ARCORE = "arcore"
    const val FOCUS_ON_CAPTURE = "capture"
    val ARCORE_FOCUS_ON = arrayOf(FOCUS_ON_CAPTURE, FOCUS_ON_ARCORE)

    val PLANE_FINDING_MODE = Config.PlaneFindingMode.HORIZONTAL
    val UPDATE_MODE = Config.UpdateMode.LATEST_CAMERA_IMAGE
    const val PLANERENDERER_ENABLED = true
    const val PLANERENDERER_VISIBLE = true
    const val PLANERENDERER_SHADOW_RECEIVER = true


    /**
     * image segmentation with deeplab v3
     * https://www.tensorflow.org/lite/models/segmentation/overview
     */
    const val IMAGE_SEGMENTATION_MODEL_NAME = "deeplabv3_257_mv_gpu_tflite"
    const val IMAGE_SEGMENTATION_DIM_SIZE = 257
    const val IMAGE_SEGMENTATION_NUM_OF_BYTES_PER_CHANNEL = 4
    const val IMAGE_SEGMENTATION_DIM_BATCH_SIZE = 1
    const val IMAGE_SEGMENTATION_DIM_PIXEL_SIZE = 3
    const val IMAGE_SEGMENTATION_AWAIT_MILLISECOND = 500L
    const val IMAGE_SEGMENTATION_NUM_CLASSES = 21

    const val IMAGE_SEGMENTATION_ALPHA_0 = 0
    const val IMAGE_SEGMENTATION_ALPHA_63 = 63
    const val IMAGE_SEGMENTATION_ALPHA_127 = 127
    const val IMAGE_SEGMENTATION_ALPHA_255 = 255
    const val IMAGE_SEGMENTATION_ALPHA = IMAGE_SEGMENTATION_ALPHA_0
    val IMAGE_SEGMENTATION_ALPHA_ARRAY = arrayOf(IMAGE_SEGMENTATION_ALPHA_0,
        IMAGE_SEGMENTATION_ALPHA_63,
        IMAGE_SEGMENTATION_ALPHA_127,
        IMAGE_SEGMENTATION_ALPHA_255)

    const val IMAGE_SEGMENTATION_COLOR_FRONT_WHITE = "FRONT_WHITE"
    const val IMAGE_SEGMENTATION_COLOR_BACK_WHITE = "BACK_WHITE"
    const val IMAGE_SEGMENTATION_COLOR_TRANSPARENT = "TRANSPARENT"
    const val IMAGE_SEGMENTATION_COLOR_INTRANSPARENT = "INTRANSPARENT"
    const val IMAGE_SEGMENTATION_COLOR = IMAGE_SEGMENTATION_COLOR_FRONT_WHITE
    val IMAGE_SEGMENTATION_COLOR_ARRAY = arrayOf(
        IMAGE_SEGMENTATION_COLOR_FRONT_WHITE,
        IMAGE_SEGMENTATION_COLOR_BACK_WHITE,
        IMAGE_SEGMENTATION_COLOR_TRANSPARENT,
        IMAGE_SEGMENTATION_COLOR_INTRANSPARENT)

    const val IMAGE_SEGMENTATION_PORTERDUFF_OVERLAY = "OVERLAY"
    const val IMAGE_SEGMENTATION_PORTERDUFF_DST = "DST"
    const val IMAGE_SEGMENTATION_PORTERDUFF_DSTIN = "DSTIN"
    const val IMAGE_SEGMENTATION_PORTERDUFF_DSTOVER = "DSTOVER"
    const val IMAGE_SEGMENTATION_PORTERDUFF = IMAGE_SEGMENTATION_PORTERDUFF_OVERLAY
    val IMAGE_SEGMENTATION_PORTERDUFF_ARRAY = arrayOf(
        IMAGE_SEGMENTATION_PORTERDUFF_OVERLAY,
        IMAGE_SEGMENTATION_PORTERDUFF_DST,
        IMAGE_SEGMENTATION_PORTERDUFF_DSTIN,
        IMAGE_SEGMENTATION_PORTERDUFF_DSTOVER)
}
