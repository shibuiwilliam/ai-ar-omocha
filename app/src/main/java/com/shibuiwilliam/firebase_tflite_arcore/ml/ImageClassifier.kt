package com.shibuiwilliam.firebase_tflite_arcore.ml


import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.ml.common.FirebaseMLException
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.custom.*
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.shibuiwilliam.firebase_tflite_arcore.common.ImageUtils
import com.shibuiwilliam.firebase_tflite_arcore.common.Utils
import java.lang.Exception
import java.nio.ByteBuffer
import java.util.AbstractMap
import java.util.PriorityQueue
import java.util.concurrent.TimeUnit
import kotlin.Comparator
import kotlin.collections.ArrayList
import kotlin.experimental.and
import kotlin.math.E
import kotlin.math.pow

/**
 * A `FirebaseModelInterpreter` based image classifier.
 */
class ImageClassifier
/**
 * Initializes an `CustomImageClassifier`.
 */
@Throws(FirebaseMLException::class)
internal constructor(private val context: Context,
                     private val remoteModelName: String ,
                     private val labelPath: String,
                     private val numOfBytesPerChannel: Int,
                     private val dimBatchSize: Int,
                     private val dimPixelSize: Int,
                     private val dimImgSize: Int,
                     private val quantized: Boolean,
                     private val resultsToShow: Int,
                     private val awaitMilliSeconds: Long) {
    private val TAG = "ImageClassifier"


    private var interpreter: FirebaseModelInterpreter? = null
    var initialized: Boolean = false
    private lateinit var dataOptions: FirebaseModelInputOutputOptions

    private lateinit var labelList: List<String>
    private val sortedLabels = PriorityQueue(
        resultsToShow,
        Comparator<AbstractMap.SimpleEntry<String, Float>> { o1, o2 ->
            o1.value.compareTo(o2.value) })

    /**
     * Gets the top-K labels, to be shown in UI as the results.
     */
    private val topKLabels: List<String>
        @Synchronized get() {
            val result = ArrayList<String>()
            val size = sortedLabels.size
            for (i in 0 until size) {
                val label = sortedLabels.poll()
                result.add("${label.key}:\t${adjustStringLength(label.value.toString(),7,"0")}")
            }
            return result.reversed()
        }

    private fun adjustStringLength(str: String,
                                   max: Int,
                                   fill: String = "0"): String =
        if (str.length >= max) str.substring(0, max) else str + fill.repeat(max - str.length)

    internal fun getRemoteModeName(): String = remoteModelName

    init {
        initialize()
    }

    private fun initialize(){
        initializeLabelList()
        initializeModel()
        initializeIO()
        initialized = true
    }

    private fun initializeLabelList(){
        labelList = Utils.loadLabelList(context.applicationContext, labelPath)
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
                    "Model download failed for image classifier, please check your connection.",
                    "e",
                    Toast.LENGTH_SHORT,
                    Gravity.TOP)
                ex.printStackTrace()
            }
        Log.d(TAG, "Created a Custom Image Classifier.")
    }

    private fun initializeIO(){
        val inputDims = intArrayOf(dimBatchSize,
            dimImgSize,
            dimImgSize,
            dimPixelSize)
        val outputDims = intArrayOf(1,
            labelList.size)

        val dataType = if (quantized)
            FirebaseModelDataType.BYTE
        else
            FirebaseModelDataType.FLOAT32

        dataOptions = FirebaseModelInputOutputOptions
            .Builder()
            .setInputFormat(0, dataType, inputDims)
            .setOutputFormat(0, dataType, outputDims)
            .build()
        Log.d(TAG, "Configured input & output data for the custom image classifier.")
    }

    @Synchronized
    private fun convertBitmapToResizedinBuffer(bitmap: Bitmap): ByteBuffer {
        return ImageUtils.convertBitmapToResizedinBuffer(
            bitmap,
            numOfBytesPerChannel,
            dimBatchSize,
            dimPixelSize,
            dimImgSize,
            quantized,
            127.5f,
            127.5f)
    }

    private fun generateClassificationInputs(image: Bitmap):FirebaseModelInputs{
        return FirebaseModelInputs
            .Builder()
            .add(convertBitmapToResizedinBuffer(image))
            .build()
    }

    @Throws(FirebaseMLException::class)
    internal fun classify(image: Bitmap): Task<FirebaseModelOutputs> {
        if (!initialized || interpreter==null) {
            initialize()
        }
        return interpreter!!.run(generateClassificationInputs(image), dataOptions)
    }

    @Throws(FirebaseMLException::class)
    internal fun classifyAwait(image: Bitmap,
                               awaitMilliSeconds: Long=this.awaitMilliSeconds): FirebaseModelOutputs? {
        if (!initialized || interpreter==null) {
            initialize()
        }
        try{
            return Tasks.await(classify(image),
                awaitMilliSeconds,
                TimeUnit.MILLISECONDS)
        }
        catch (ex: Exception){
            Log.e(TAG, "${ex.printStackTrace()}")
            return null
        }
    }

    @Throws(FirebaseMLException::class)
    internal fun classify(image : ImageProxy): Task<FirebaseModelOutputs>{
        return classify(ImageUtils.imageToBitmap(image))
    }

    @Throws(FirebaseMLException::class)
    internal fun classifyAwait(image: ImageProxy,
                               awaitMilliSeconds: Long=this.awaitMilliSeconds): FirebaseModelOutputs? {
        if (!initialized || interpreter==null) {
            initialize()
        }
        try {
            return Tasks.await(
                classify(image),
                awaitMilliSeconds,
                TimeUnit.MILLISECONDS)
        }
        catch (ex: Exception){
            Log.e(TAG, "${ex.printStackTrace()}")
            return null
        }

    }

    @Throws(FirebaseMLException::class)
    internal fun classify(image : FirebaseVisionImage): Task<FirebaseModelOutputs>{
        return classify(ImageUtils.imageToBitmap(image))
    }

    @Throws(FirebaseMLException::class)
    internal fun classifyAwait(image: FirebaseVisionImage,
                               awaitMilliSeconds: Long=this.awaitMilliSeconds): FirebaseModelOutputs? {
        if (!initialized || interpreter==null) {
            initialize()
        }
        try{
            return Tasks.await(classify(image),
                awaitMilliSeconds,
                TimeUnit.MILLISECONDS)
        }
        catch (ex: Exception){
            Log.e(TAG, "${ex.printStackTrace()}")
            return null
        }
    }


    @Synchronized
    internal fun getTopLabels(labelProbArray: Array<ByteArray>): List<String> {
        for (i in labelList.indices) {
            sortedLabels.add(
                AbstractMap.SimpleEntry(
                    labelList[i],
                    (labelProbArray[0][i] and 0xff.toByte()) / 255.0f
                )
            )
            if (sortedLabels.size > resultsToShow) {
                sortedLabels.poll()
            }
        }
        return topKLabels
    }

    @Synchronized
    internal fun getTopLabels(labelProbArray: Array<FloatArray>): List<String> {
        val softmaxArray = softmax(labelProbArray[0])
        for (i in labelList.indices) {
            sortedLabels.add(
                AbstractMap.SimpleEntry(labelList[i], softmaxArray[i])
            )
            if (sortedLabels.size > resultsToShow) {
                sortedLabels.poll()
            }
        }
        return topKLabels
    }

    private fun softmax(array: FloatArray): FloatArray{
        val softmaxArray = FloatArray(array.size){0.0f}
        val eList = array.map{E.pow(it.toDouble()).toFloat()}
        val eSum = eList.sum()
        for (i in 0 until eList.size){
            softmaxArray[i] = eList[i] / eSum
        }
        return softmaxArray
    }

    internal fun extractResults(firebaseModelOutputs: FirebaseModelOutputs): List<String>{
        return when (quantized){
            true -> getTopLabels(firebaseModelOutputs.getOutput<Array<ByteArray>>(0))
            false -> getTopLabels(firebaseModelOutputs.getOutput<Array<FloatArray>>(0))
        }
    }

    fun close() {
        initialized = false
        interpreter?.close()
        Log.d(TAG, "Closed Firebase custom classifier interpreter.")
    }

}
