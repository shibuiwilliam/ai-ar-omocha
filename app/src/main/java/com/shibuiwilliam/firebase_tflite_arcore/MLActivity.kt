package com.shibuiwilliam.firebase_tflite_arcore

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.shibuiwilliam.firebase_tflite_arcore.common.*
import com.shibuiwilliam.firebase_tflite_arcore.ml.MLProcessor

class MLActivity : AppCompatActivity() {
    private val TAG = "MLActivity"

    private val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.INTERNET)

    private var globals: Globals? = null

    private lateinit var cameraTextureView: TextureView
    private lateinit var overlay: GraphicOverlay

    private var facingCameraX = Constants.FACING_CAMERAX
    private val objectDetectAwaitSecond = Constants.OBJECT_DETECT_AWAIT_MILLISECOND
    private val imageLabelerAwaidSecond = Constants.IMAGE_LABELER_AWAIT_MILLISECOND

    private lateinit var mlTargetSpinner: Spinner
    private lateinit var mlClassifierSpinner: Spinner
    private var mlClassifier = "Quant ImageNet"
    private var mlTarget = "Full Screen"

    private lateinit var drawView: DrawView

    private lateinit var activeModelName: String

    private lateinit var mlProcessor: MLProcessor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (!allPermissionsGranted()) {
                ActivityCompat.requestPermissions(
                    this,
                    REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS
                )
                return
            }
            setContentView(R.layout.activity_ml)
            initializeGlobals()
            mlProcessor = MLProcessor(globals!!)

            cameraTextureView = findViewById(R.id.cameraTextureView)
            overlay = findViewById(R.id.overlay)
            drawView = findViewById(R.id.camera_drawview)
            drawView.isFocusable = false

            configureSpinner()

            cameraTextureView.post { startCameraX() }
            cameraTextureView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                updateTransform()
            }
        }
        catch (ex: Exception){
            Utils.logAndToast(this,
                TAG,
                "Failed to start CameraX",
                "e",
                Toast.LENGTH_SHORT,
                Gravity.TOP)
        }
    }

    private fun initializeGlobals(){
        if (globals == null){
            globals = application as Globals
            globals!!.initialize(this)
        }
    }

    private fun updateActiveModelName(){
        activeModelName = when(mlClassifier){
            "Quant ImageNet" -> Constants.MOBILENETV2_IMAGE_CLASSIFIER.MODEL_NAME
            "Float ImageNet" -> Constants.MNASNET_IMAGE_CLASSIFIER.MODEL_NAME
            "Image Labeler" -> "Image Labeler"
            else -> "Image Labeler"
        }
    }

    private fun configureSpinner(){
        mlTargetSpinner = findViewById(R.id.mlTarget)
        val mlTargetAdapter = ArrayAdapter(
            applicationContext,
            android.R.layout.simple_spinner_item,
            Constants.MLTARGET_ARRAY
        )
        mlTargetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mlTargetSpinner.adapter = mlTargetAdapter
        mlTarget = Constants.MLTARGET_FULL_SCREEN
        mlTargetSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?,
                                        view: View?,
                                        position: Int,
                                        id: Long) {
                val spinnerParent = parent as Spinner
                mlTarget = spinnerParent.selectedItem as String
                when(mlTarget){
                    Constants.MLTARGET_Rectangle ->{
                        drawView.makeVisible()
                    }
                    else -> {
                        drawView.makeInvisible()
                    }
                }
                Log.i(TAG, "Selected mlTarget ${mlTarget}")
                cameraTextureView.post { startCameraX() }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                mlTarget = Constants.MLTARGET_FULL_SCREEN
            }
        }

        mlClassifierSpinner = findViewById(R.id.mlClassifier)
        val mlClassifierAdapter = ArrayAdapter(
            applicationContext,
            android.R.layout.simple_spinner_item,
            Constants.MLCLASSIFIER_ARRAY
        )
        mlClassifierAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mlClassifierSpinner.adapter = mlClassifierAdapter
        mlClassifier = Constants.MLCLASSIFIER_QUANT_IMAGENET
        mlClassifierSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?,
                                        view: View?,
                                        position: Int,
                                        id: Long) {
                val spinnerParent = parent as Spinner
                mlClassifier = spinnerParent.selectedItem as String
                updateActiveModelName()
                Log.i(TAG, "Selected mlClassifier ${mlClassifier}")
                cameraTextureView.post { startCameraX() }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                mlClassifier = Constants.MLCLASSIFIER_QUANT_IMAGENET
            }
        }
        updateActiveModelName()
    }


    private fun startCameraX() {
        CameraX.unbindAll()
        val screenSize = Size(cameraTextureView.width, cameraTextureView.height)
        val screenAspectRatio = Rational(1, 1)
        Log.i(TAG, "Screen size: (${screenSize.width}, ${screenSize.height}).")

        val previewConfig = buildPreviewConfig(screenSize,
            screenAspectRatio)

        val preview = Preview(previewConfig)
        preview.setOnPreviewOutputUpdateListener {
            val parent = cameraTextureView.parent as ViewGroup
            parent.removeView(cameraTextureView)
            cameraTextureView.surfaceTexture = it.surfaceTexture
            parent.addView(cameraTextureView, 0)
            updateTransform()
        }

        val analyzerConfig = buildAnalyzerConfig()

        val imageAnalysis = ImageAnalysis(analyzerConfig)
        imageAnalysis.analyzer = ImageAnalysis.Analyzer {
                image: ImageProxy, rotationDegrees: Int ->
            mlImageAnalysis(image, rotationDegrees)
        }

        CameraX.bindToLifecycle(this, preview, imageAnalysis)
    }


    private fun updateTransform() {
        val matrix = Matrix()
        val centerX = cameraTextureView.width / 2f
        val centerY = cameraTextureView.height / 2f

        val rotationDegrees = when (cameraTextureView.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)
        cameraTextureView.setTransform(matrix)
    }

    private fun buildPreviewConfig(screenSize: Size,
                                   screenAspectRatio: Rational
    ): PreviewConfig {
        return PreviewConfig
            .Builder()
            .apply {
                setLensFacing(facingCameraX)
                setTargetResolution(screenSize)
                setTargetAspectRatio(screenAspectRatio)
                setTargetRotation(windowManager.defaultDisplay.rotation)
                setTargetRotation(cameraTextureView.display.rotation)
            }.build()
    }

    private fun buildAnalyzerConfig(): ImageAnalysisConfig {
        return ImageAnalysisConfig.Builder().apply {
            val analyzerThread = HandlerThread("AnalysisThread").apply {
                start()
            }
            setCallbackHandler(Handler(analyzerThread.looper))
            setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
        }.build()
    }

    private fun mlImageAnalysis(image: ImageProxy, rotationDegrees: Int) {
        when (mlTarget){
            Constants.MLTARGET_FULL_SCREEN -> {
                overlay.setConfiguration(400,
                    380,
                    Color.TRANSPARENT)
                when (activeModelName) {
                    Constants.MOBILENETV2_IMAGE_CLASSIFIER.MODEL_NAME,
                    Constants.MNASNET_IMAGE_CLASSIFIER.MODEL_NAME -> {
                        mlProcessor.classifyAwait(
                            image,
                            overlay,
                            activeModelName)
                    }
                    Constants.MLCLASSIFIER_IMAGE_LABELER -> {
                        mlProcessor.labelImageAwait(
                            image,
                            rotationDegrees,
                            overlay,
                            imageLabelerAwaidSecond)
                    }
                    else -> {
                        Log.e(TAG, "Wrong configuration for mlClassifier")
                    }
                }
            }
            Constants.MLTARGET_OBJECT_DETECTION -> {
                when (activeModelName) {
                    Constants.MOBILENETV2_IMAGE_CLASSIFIER.MODEL_NAME,
                    Constants.MNASNET_IMAGE_CLASSIFIER.MODEL_NAME -> {
                        mlProcessor.classifyFromDetectionAwait(
                            image,
                            rotationDegrees,
                            overlay,
                            activeModelName,
                            objectDetectAwaitSecond)
                    }
                    Constants.MLCLASSIFIER_IMAGE_LABELER -> {
                        mlProcessor.labelImageFromDetectionAwait(
                            image,
                            rotationDegrees,
                            overlay,
                            objectDetectAwaitSecond,
                            imageLabelerAwaidSecond)
                    }
                    else -> {
                        Log.e(TAG, "Wrong configuration for mlClassifier")
                    }
                }
            }
            Constants.MLTARGET_Rectangle -> {
                val bitmap = ImageUtils.imageToBitmap(image)
                val croppedImage: Bitmap =
                    if (drawView.drawId == null) bitmap
                    else ImageUtils.cropImageFromPoints(bitmap, drawView.points)

                when (activeModelName) {
                    Constants.MOBILENETV2_IMAGE_CLASSIFIER.MODEL_NAME,
                    Constants.MNASNET_IMAGE_CLASSIFIER.MODEL_NAME -> {
                        mlProcessor.classifyAwait(
                            croppedImage!!,
                            overlay,
                            activeModelName)
                    }
                    Constants.MLCLASSIFIER_IMAGE_LABELER -> {
                        mlProcessor.labelImageAwait(
                            croppedImage!!,
                            rotationDegrees,
                            overlay,
                            imageLabelerAwaidSecond)
                    }
                    else -> {
                        Log.e(TAG, "Wrong configuration for mlClassifier")
                    }
                }
            }
            else -> {
                Log.e(TAG, "Wrong configuration for mlTarget")
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!allPermissionsGranted()) {
                Utils.logAndToast(this,
                    TAG,
                    "Permissions not granted by the user.",
                    "e",
                    Toast.LENGTH_SHORT,
                    Gravity.TOP)
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    this, permission) != PackageManager.PERMISSION_GRANTED) {
                Utils.logAndToast(this,
                    TAG,
                    "Permissions not granted by the user.",
                    "e",
                    Toast.LENGTH_SHORT,
                    Gravity.TOP)
                return false
            }
        }
        Log.i(TAG, "Permitted to use camera")
        return true
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}