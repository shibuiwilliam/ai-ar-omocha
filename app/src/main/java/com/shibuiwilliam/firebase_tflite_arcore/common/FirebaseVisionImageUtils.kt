package com.shibuiwilliam.firebase_tflite_arcore.common

import android.app.Activity
import android.content.Context
import android.content.Context.CAMERA_SERVICE
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.Image
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.SparseIntArray
import androidx.annotation.RequiresApi
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.objects.FirebaseVisionObject
import java.io.IOException
import java.nio.ByteBuffer

object FirebaseVisionImageUtils {

    private val TAG = "FirebaseVisionImage"

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    fun imageFromBitmap(bitmap: Bitmap): FirebaseVisionImage {
        return FirebaseVisionImage.fromBitmap(bitmap)
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    fun imageFromMediaImage(mediaImage: Image,
                            rotation: Int):FirebaseVisionImage {
        return FirebaseVisionImage.fromMediaImage(mediaImage, rotation)
    }

    fun imageFromBuffer(buffer: ByteBuffer,
                        width: Int,
                        height: Int,
                        rotation: Int):FirebaseVisionImage {
        val metadata = FirebaseVisionImageMetadata.Builder()
            .setWidth(width)
            .setHeight(height)
            .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
            .setRotation(rotation)
            .build()
        return FirebaseVisionImage.fromByteBuffer(buffer, metadata)
    }

    fun imageFromArray(byteArray: ByteArray,
                       width: Int,
                       height: Int,
                       rotation: Int):FirebaseVisionImage {
        val metadata = FirebaseVisionImageMetadata.Builder()
            .setWidth(width)
            .setHeight(height)
            .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
            .setRotation(rotation)
            .build()
        return FirebaseVisionImage.fromByteArray(byteArray, metadata)
    }

    fun imageFromPath(context: Context,
                      uri: Uri): FirebaseVisionImage? {
        try {
            return FirebaseVisionImage.fromFilePath(context, uri)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    fun getFirebaseRotation(rotationDegrees: Int): Int{
        return when (rotationDegrees) {
            0 -> FirebaseVisionImageMetadata.ROTATION_0
            90 -> FirebaseVisionImageMetadata.ROTATION_90
            180 -> FirebaseVisionImageMetadata.ROTATION_180
            270 -> FirebaseVisionImageMetadata.ROTATION_270
            else -> {
                FirebaseVisionImageMetadata.ROTATION_0
                Log.e(TAG, "Bad rotation value: ${rotationDegrees}")
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Throws(CameraAccessException::class)
    fun getRotationCompensation(cameraId: String,
                                activity: Activity,
                                context: Context,
                                orientation: SparseIntArray=Constants.ORIENTATIONS): Int {
        val deviceRotation = activity.windowManager.defaultDisplay.rotation
        var rotationCompensation = orientation.get(deviceRotation)

        val cameraManager = context.getSystemService(CAMERA_SERVICE) as CameraManager
        val sensorOrientation = cameraManager
            .getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        rotationCompensation = (rotationCompensation + sensorOrientation + 270) % 360

        return getFirebaseRotation(rotationCompensation)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Throws(CameraAccessException::class)
    fun getCompensation(activity: Activity,
                        context: Context,
                        cameraId: String,
                        orientation: SparseIntArray=Constants.ORIENTATIONS): Int {
        return getRotationCompensation(cameraId, activity, context, orientation)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Throws(CameraAccessException::class)
    fun makeBitmapFromObject(firebaseVisionObject: FirebaseVisionObject,
                             firebaseVisionImage: FirebaseVisionImage,
                             matrix: Matrix?=null,
                             filter: Boolean=true): Bitmap{
        return Bitmap.createBitmap(
            firebaseVisionImage.bitmap,
            firebaseVisionObject.boundingBox.left,
            firebaseVisionObject.boundingBox.top,
            firebaseVisionObject.boundingBox.right - firebaseVisionObject.boundingBox.left,
            firebaseVisionObject.boundingBox.bottom - firebaseVisionObject.boundingBox.top,
            matrix,
            filter
        )
    }
}