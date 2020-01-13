package com.shibuiwilliam.firebase_tflite_arcore.common


import android.graphics.*
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.View
import androidx.camera.core.ImageProxy
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder


object ImageUtils {
    private val TAG = "ImageUtils"

    fun convertBitmapToBuffer(bitmap: Bitmap,
                              numOfBytesPerChannel: Int,
                              dimBatchSize: Int,
                              dimPixelSize: Int,
                              quantized: Boolean,
                              image_mean: Float = 127.5f,
                              image_std: Float = 127.5f): ByteBuffer{
        val intValues = IntArray(bitmap.width * bitmap.height)

        val imgData = ByteBuffer.allocateDirect(
            numOfBytesPerChannel *
                    dimBatchSize *
                    bitmap.width *
                    bitmap.height *
                    dimPixelSize)
        imgData!!.order(ByteOrder.nativeOrder())
        imgData.rewind()

        bitmap.getPixels(
            intValues,
            0,
            bitmap.width,
            0,
            0,
            bitmap.width,
            bitmap.height
        )

        var pixel = 0
        for (i in 0 until bitmap.width) {
            for (j in 0 until bitmap.height) {
                val value = intValues[pixel++]
                addPixelValue(imgData, value, quantized, image_mean, image_std)
            }
        }
        return imgData
    }


    fun convertBitmapToResizedinBuffer(bitmap: Bitmap,
                                       numOfBytesPerChannel: Int,
                                       dimBatchSize: Int,
                                       dimPixelSize: Int,
                                       dimImgSize: Int,
                                       quantized: Boolean,
                                       image_mean: Float = 127.5f,
                                       image_std: Float = 127.5f): ByteBuffer{
        val intValues = IntArray(dimImgSize * dimImgSize)

        val imgData = ByteBuffer.allocateDirect(
            numOfBytesPerChannel *
                    dimBatchSize *
                    dimImgSize *
                    dimImgSize *
                    dimPixelSize)
        imgData!!.order(ByteOrder.nativeOrder())
        imgData.rewind()

        val resizedBitmap = Bitmap.createScaledBitmap(
            bitmap,
            dimImgSize,
            dimImgSize,
            true)

        resizedBitmap.getPixels(
            intValues,
            0,
            resizedBitmap.width,
            0,
            0,
            resizedBitmap.width,
            resizedBitmap.height
        )

        var pixel = 0
        for (i in 0 until dimImgSize) {
            for (j in 0 until dimImgSize) {
                val value = intValues[pixel++]
                addPixelValue(imgData, value, quantized, image_mean, image_std)
            }
        }
        bitmap.recycle()
        resizedBitmap.recycle()
        return imgData
    }

    fun addPixelValue(inBuffer: ByteBuffer,
                      pixelValue: Int,
                      quantized: Boolean,
                      image_mean: Float = 127.5f,
                      image_std: Float = 127.5f) {
        if (quantized) {
            inBuffer.put((pixelValue shr 16 and 0xFF).toByte())
            inBuffer.put((pixelValue shr 8 and 0xFF).toByte())
            inBuffer.put((pixelValue and 0xFF).toByte())
        } else {
            inBuffer.putFloat(((pixelValue shr 16 and 0xFF) - image_mean) / image_std)
            inBuffer.putFloat(((pixelValue shr 8 and 0xFF) - image_mean) / image_std)
            inBuffer.putFloat(((pixelValue and 0xFF) - image_mean) / image_std)
        }
    }

    fun imageToBitmap(image: ImageProxy): Bitmap {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, Constants.IMAGE_FORMAT_NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    fun imageToBitmap(image: FirebaseVisionImage): Bitmap{
        return image.bitmap
    }

    fun cropImageFromPoints(bitmap: Bitmap, points: Array<Point?>): Bitmap{
        Log.i(TAG, "Original Bitmap: ${bitmap.width}, ${bitmap.height}")
        if (points[0] == null){
            return bitmap
        }
        var croppedImage: Bitmap? = null
        val leftTopRightBottom = Utils.extractFromPoints(points)
        leftTopRightBottom["left"] = 480 * leftTopRightBottom["left"]!! / 1080
        leftTopRightBottom["right"] = 480 * leftTopRightBottom["right"]!! / 1080
        leftTopRightBottom["top"] = 640 * leftTopRightBottom["top"]!! / 1782
        leftTopRightBottom["bottom"] = 640 * leftTopRightBottom["bottom"]!! / 1782

        val height = leftTopRightBottom["bottom"]!!-leftTopRightBottom["top"]!!
        val width = leftTopRightBottom["right"]!!-leftTopRightBottom["left"]!!
        Log.i(TAG, "Cropping: ${leftTopRightBottom["left"]!!}, " +
                "${leftTopRightBottom["right"]!!}, " +
                "${leftTopRightBottom["top"]!!}, " +
                "${leftTopRightBottom["bottom"]!!}, " +
                "height: ${height}, " +
                "width: ${width}")
        if (height > 0
            && width > 0){
            croppedImage = Bitmap.createBitmap(bitmap,
                leftTopRightBottom["top"]!!,
                leftTopRightBottom["left"]!!,
                width,
                height,
                null,
                true
            )
        }
        else {
            return bitmap
        }
        return croppedImage
    }

    fun saveImgsFromBmp(bmp: Bitmap,
                        outputFile: File): Boolean {
        try {
            val byteArrOutputStream = ByteArrayOutputStream()
            val fileOutputStream = FileOutputStream(outputFile)
            bmp!!.compress(Bitmap.CompressFormat.JPEG,
                100,
                byteArrOutputStream)
            fileOutputStream.write(byteArrOutputStream.toByteArray())
            fileOutputStream.close()
            return true
        }
        catch (e:Exception){
            e.printStackTrace()
            return false
        }
    }

    fun rotateBitmap(source: Bitmap, angle: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source,
            0,
            0,
            source.width,
            source.height,
            matrix,
            true
        )
    }

    fun scaleBitmapWithRotation(bitmap: Bitmap,
                                view: View): Bitmap {
        val matrix = Matrix()
        val centerX = view.width / 2f
        val centerY = view.height / 2f

        val rotationDegrees = when (view.display.rotation) {
            Surface.ROTATION_0 -> 270
            Surface.ROTATION_90 -> 0
            Surface.ROTATION_180 -> 90
            Surface.ROTATION_270 -> 180
            else -> 0
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap,
            view.height,
            view.width,
            true)
        val rotatedBitmap = Bitmap.createBitmap(scaledBitmap,
            0,
            0,
            view.height,
            view.width,
            matrix,
            true)
        scaledBitmap.recycle()

        return rotatedBitmap
    }


}