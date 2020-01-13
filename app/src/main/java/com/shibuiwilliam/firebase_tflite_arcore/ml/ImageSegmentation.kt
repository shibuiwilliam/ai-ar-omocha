package com.shibuiwilliam.firebase_tflite_arcore.ml

import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.Gravity
import android.widget.Spinner
import android.widget.Toast
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.ml.common.FirebaseMLException
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.custom.*
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.shibuiwilliam.firebase_tflite_arcore.common.Constants
import com.shibuiwilliam.firebase_tflite_arcore.common.ImageUtils
import com.shibuiwilliam.firebase_tflite_arcore.common.Utils
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * A `FirebaseModelInterpreter` based image segmentation.
 */
class ImageSegmentation
/**
 * Initializes an `CustomImageClassifier`.
 */
@Throws(FirebaseMLException::class)
internal constructor(private val context: Context,
                     private val remoteModelName: String,
                     private val numOfBytesPerChannel: Int,
                     private val dimBatchSize: Int,
                     private val dimPixelSize: Int,
                     private val dimImgSize: Int,
                     private val numClasses: Int,
                     var awaitMilliSeconds: Long,
                     private var alpha: Int,
                     private var color: String,
                     private var porterDuff: String) {
    private val TAG = "ImageSegmentation"

    private var interpreter: FirebaseModelInterpreter? = null
    var initialized: Boolean = false
    private lateinit var dataOptions: FirebaseModelInputOutputOptions

    private val lSegmentColors = Array<Int>(numClasses){0}
    private val mSegmentColors = Array<Int>(numClasses){0}
    private val nSegmentColors = Array<Int>(numClasses){0}
    private val wSegmentColors = Array<Int>(numClasses){0}
    private var activeColor = Array<Int>(numClasses){0}

    private var porterDuffMode: PorterDuff.Mode = PorterDuff.Mode.DST

    private val RANDOM: Random = Random(System.currentTimeMillis())

    init {
        initialize()
    }

    private fun initialize(){
        initializeModel()
        initializeIO()
        configureColors()
        setAlpha()
        setColor()
        setPorterDuff()
        initialized = true
    }

    private fun initializeModel(){
        val remoteModel = FirebaseCustomRemoteModel.Builder(remoteModelName).build()
        val firebaseModelManager = FirebaseModelManager.getInstance()

        firebaseModelManager
            .isModelDownloaded(remoteModel)
            .continueWithTask { task ->
                val conditions =
                    if (task.result != null && task.result == true) {
                        FirebaseModelDownloadConditions
                            .Builder()
                            .requireWifi()
                            .build()
                    }
                    else {
                        FirebaseModelDownloadConditions
                            .Builder()
                            .build()
                    }
                Utils.logAndToast(context,
                    TAG,
                    "Downloading ${remoteModelName} file.",
                    "i",
                    Toast.LENGTH_SHORT,
                    Gravity.TOP)

                firebaseModelManager.download(remoteModel, conditions)
            }
            .addOnSuccessListener {
                try {
                    val interpreterOptions =
                        FirebaseModelInterpreterOptions
                            .Builder(
                                FirebaseCustomRemoteModel
                                    .Builder(remoteModelName)
                                    .build()
                            ).build()
                    Utils.logAndToast(context,
                        TAG,
                        "Successfully downloaded ${remoteModelName} file.",
                        "i",
                        Toast.LENGTH_SHORT,
                        Gravity.TOP)

                    interpreter = FirebaseModelInterpreter.getInstance(interpreterOptions)
                } catch (ex: FirebaseMLException) {
                    Utils.logAndToast(context,
                        TAG,
                        "Failed to build FirebaseModelInterpreter.",
                        "e",
                        Toast.LENGTH_SHORT,
                        Gravity.TOP)
                }
            }
            .addOnFailureListener { ex ->
                Utils.logAndToast(
                    context,
                    TAG,
                    "Model download failed for image segmentation, please check your connection.",
                    "e",
                    Toast.LENGTH_SHORT,
                    Gravity.TOP)
                ex.printStackTrace()
            }
        Log.d(TAG, "Created a Custom image segmentation.")
    }

    private fun initializeIO(){
        val inputDims = intArrayOf(dimBatchSize,
            dimImgSize,
            dimImgSize,
            dimPixelSize)
        val outputDims = intArrayOf(1,
            dimImgSize,
            dimImgSize,
            numClasses)

        val dataType = FirebaseModelDataType.FLOAT32

        dataOptions = FirebaseModelInputOutputOptions
            .Builder()
            .setInputFormat(0, dataType, inputDims)
            .setOutputFormat(0, dataType, outputDims)
            .build()

        Log.d(TAG, "Configured input & output data for the custom image segmentation.")
    }


    internal fun setAlpha(alpha: Int = this.alpha){
        this.alpha = alpha
        configureColors()
        configureActiveColor(this.color)
    }

    internal fun setColor(color: String = this.color){
        this.color = color
        configureColors()
        configureActiveColor(this.color)
    }

    internal fun setPorterDuff(porterDuff: String = this.porterDuff){
        this.porterDuff = porterDuff
        configurePorterDuffMode(this.porterDuff)
    }
    internal fun configureColors(){
        for (i in 0 until numClasses){
            lSegmentColors[i] = if(i==0) Color.WHITE else Color.argb(
                alpha,
                RANDOM.nextInt(256),
                RANDOM.nextInt(256),
                RANDOM.nextInt(256)
            )
        }

        for (i in 0 until numClasses){
            mSegmentColors[i] = if(i==0) Color.TRANSPARENT else Color.argb(
                alpha,
                RANDOM.nextInt(256),
                RANDOM.nextInt(256),
                RANDOM.nextInt(256)
            )
        }

        for (i in 0 until numClasses){
            nSegmentColors[i] = if(i==0) Color.argb(
                alpha,
                RANDOM.nextInt(256),
                RANDOM.nextInt(256),
                RANDOM.nextInt(256)
            ) else Color.TRANSPARENT
        }

        for (i in 0 until numClasses){
            wSegmentColors[i] = if(i==0) Color.TRANSPARENT else Color.WHITE
        }
    }

    internal fun configureActiveColor(color: String = this.color){
        this.activeColor = when(color){
            Constants.IMAGE_SEGMENTATION_COLOR_FRONT_WHITE -> wSegmentColors
            Constants.IMAGE_SEGMENTATION_COLOR_BACK_WHITE -> lSegmentColors
            Constants.IMAGE_SEGMENTATION_COLOR_TRANSPARENT -> mSegmentColors
            Constants.IMAGE_SEGMENTATION_COLOR_INTRANSPARENT -> nSegmentColors
            else -> wSegmentColors
        }
    }

    internal fun configurePorterDuffMode(porterDuff: String = this.porterDuff){
        this.porterDuffMode = when(porterDuff){
            Constants.IMAGE_SEGMENTATION_PORTERDUFF_DST -> PorterDuff.Mode.DST
            Constants.IMAGE_SEGMENTATION_PORTERDUFF_OVERLAY -> PorterDuff.Mode.OVERLAY
            Constants.IMAGE_SEGMENTATION_PORTERDUFF_DSTOVER -> PorterDuff.Mode.DST_OVER
            Constants.IMAGE_SEGMENTATION_PORTERDUFF_DSTIN -> PorterDuff.Mode.DST_IN
            else -> PorterDuff.Mode.OVERLAY
        }
    }

    private fun generateSegmentationInputs(image: Bitmap): FirebaseModelInputs {
        return FirebaseModelInputs
            .Builder()
            .add(convertBitmapToBuffer(image))
            .build()
    }

    @Throws(FirebaseMLException::class)
    internal fun segment(image: Bitmap): Task<FirebaseModelOutputs> {
        if (!initialized || interpreter==null) {
            initialize()
        }
        return interpreter!!.run(generateSegmentationInputs(image), dataOptions)
    }

    @Throws(FirebaseMLException::class)
    internal fun segmentAwait(image: Bitmap,
                              awaitMilliSeconds: Long=this.awaitMilliSeconds): FirebaseModelOutputs? {
        if (!initialized || interpreter==null) {
            initialize()
        }
        try{
            return Tasks.await(segment(image),
                awaitMilliSeconds,
                TimeUnit.MILLISECONDS)
        }
        catch (ex: Exception){
            Log.e(TAG, "${ex.printStackTrace()}")
            return null
        }
    }

    @Throws(FirebaseMLException::class)
    internal fun segment(image : ImageProxy): Task<FirebaseModelOutputs> {
        return segment(Bitmap.createScaledBitmap(
            ImageUtils.imageToBitmap(
                image),
            dimImgSize,
            dimImgSize,
            true))
    }

    @Throws(FirebaseMLException::class)
    internal fun segmentAwait(image: ImageProxy,
                              awaitMilliSeconds: Long=this.awaitMilliSeconds): FirebaseModelOutputs? {
        if (!initialized || interpreter==null) {
            initialize()
        }
        try {
            return Tasks.await(
                segment(image),
                awaitMilliSeconds,
                TimeUnit.MILLISECONDS)
        }
        catch (ex: Exception){
            Log.e(TAG, "${ex.printStackTrace()}")
            return null
        }

    }

    @Throws(FirebaseMLException::class)
    internal fun segment(image : FirebaseVisionImage): Task<FirebaseModelOutputs> {
        return segment(Bitmap.createScaledBitmap(
            ImageUtils.imageToBitmap(
                image),
            dimImgSize,
            dimImgSize,
            true))
    }

    @Throws(FirebaseMLException::class)
    internal fun segmentAwait(image: FirebaseVisionImage,
                               awaitMilliSeconds: Long=this.awaitMilliSeconds): FirebaseModelOutputs? {
        if (!initialized || interpreter==null) {
            initialize()
        }
        try{
            return Tasks.await(segment(image),
                awaitMilliSeconds,
                TimeUnit.MILLISECONDS)
        }
        catch (ex: Exception){
            Log.e(TAG, "${ex.printStackTrace()}")
            return null
        }
    }

    @Synchronized
    private fun convertBitmapToBuffer(bitmap: Bitmap): ByteBuffer {
        return ImageUtils.convertBitmapToBuffer(
            bitmap,
            numOfBytesPerChannel,
            dimBatchSize,
            dimPixelSize,
            false,
            127.5f,
            127.5f)
    }

    internal fun extractSegmentation(firebaseModelOutputs: FirebaseModelOutputs):
            Array<Array<Array<FloatArray>>>{
        return firebaseModelOutputs
            .getOutput<Array<Array<Array<FloatArray>>>>(0)
    }

    internal fun postProcess(results: Array<Array<Array<FloatArray>>>): Bitmap{
        val output = Bitmap.createBitmap(dimImgSize,
            dimImgSize,
            Bitmap.Config.ARGB_8888)

        for (y in 0 until dimImgSize){
            for (x in 0 until dimImgSize){
                output.setPixel(x,
                    y,
                    activeColor[results[0][y][x].indexOf(results[0][y][x].max()!!)])
            }
        }

        return output
    }

    internal fun maskWithSegmentation(originalBitmap: Bitmap,
                                      maskingBitmap: Bitmap): Bitmap{
        val w = originalBitmap.width
        val h = originalBitmap.height
        val scaledMask =
            if (w!=maskingBitmap.width || h!=maskingBitmap.height)
                Bitmap.createScaledBitmap(maskingBitmap,
                    w,
                    h,
                    true)
            else maskingBitmap
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.xfermode = PorterDuffXfermode(porterDuffMode)
        canvas.drawBitmap(originalBitmap,0.0f,0.0f,null)
        canvas.drawBitmap(scaledMask, 0.0f, 0.0f, paint)
        paint.xfermode = null
        paint.style = Paint.Style.STROKE

        scaledMask.recycle()
        return output
    }

    fun close() {
        initialized = false
        interpreter?.close()
        Log.d(TAG, "Closed Firebase custom classifier interpreter.")
    }

}
