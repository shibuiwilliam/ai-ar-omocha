package com.shibuiwilliam.firebase_tflite_arcore

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.view.View
import com.shibuiwilliam.firebase_tflite_arcore.common.*
import java.util.*
import android.os.Environment
import android.widget.*
import java.io.File

class SegmentationActivity : AppCompatActivity() {
    private val TAG = "SegmentationActivity"

    private val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.INTERNET)

    private var globals: Globals? = null

    private lateinit var cameraTextureView: TextureView
    private lateinit var segmentationView: SegmentationView

    private lateinit var alphaSpinner: Spinner
    private lateinit var colorSpinner: Spinner
    private lateinit var porterduffSpinner: Spinner

    private var alpha = 0
    private var color = ""
    private var porterDuff = ""

    private var facingCameraX = Constants.FACING_CAMERAX

    private var analyzerThread = HandlerThread("AnalysisThread")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
            return
        }

        setContentView(R.layout.activity_segmentation)
        initializeGlobals()
        cameraTextureView = findViewById(R.id.cameraTextureView)

        segmentationView = findViewById(R.id.segmentation_view)
        segmentationView.makeVisible()
        segmentationView.displayBitmap = BitmapFactory.decodeResource(resources, R.drawable.saturn)

        configureSpinner()

        cameraTextureView.post { startCameraX() }
        cameraTextureView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }
    }

    private fun initializeGlobals(){
        if (globals == null){
            globals = application as Globals
            globals!!.initialize(this)
        }
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
            segmentAwait(image)
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


    private fun configureSpinner(){
        alphaSpinner = findViewById(R.id.alpha_spinner)
        val alphaAdapter = ArrayAdapter(
            applicationContext,
            android.R.layout.simple_spinner_item,
            Constants.IMAGE_SEGMENTATION_ALPHA_ARRAY
        )
        alphaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        alphaSpinner.adapter = alphaAdapter
        alpha = Constants.IMAGE_SEGMENTATION_ALPHA
        alphaSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?,
                                        view: View?,
                                        position: Int,
                                        id: Long) {
                val spinnerParent = parent as Spinner
                alpha = spinnerParent.selectedItem as Int
                globals!!.imageSegmentation!!.setAlpha(alpha)
                Log.i(TAG, "Selected alpha ${alpha}")
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                alpha = Constants.IMAGE_SEGMENTATION_ALPHA
            }
        }

        colorSpinner = findViewById(R.id.color_spinner)
        val colorAdapter = ArrayAdapter(
            applicationContext,
            android.R.layout.simple_spinner_item,
            Constants.IMAGE_SEGMENTATION_COLOR_ARRAY
        )
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        colorSpinner.adapter = colorAdapter
        color = Constants.IMAGE_SEGMENTATION_COLOR
        colorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?,
                                        view: View?,
                                        position: Int,
                                        id: Long) {
                val spinnerParent = parent as Spinner
                color = spinnerParent.selectedItem as String
                globals!!.imageSegmentation!!.setColor(color)
                Log.i(TAG, "Selected color config ${color}")
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                color = Constants.IMAGE_SEGMENTATION_COLOR
            }
        }

        porterduffSpinner = findViewById(R.id.porterduff_spinner)
        val porterduffAdapter = ArrayAdapter(
            applicationContext,
            android.R.layout.simple_spinner_item,
            Constants.IMAGE_SEGMENTATION_PORTERDUFF_ARRAY
        )
        porterduffAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        porterduffSpinner.adapter = porterduffAdapter
        porterDuff = Constants.IMAGE_SEGMENTATION_PORTERDUFF
        porterduffSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?,
                                        view: View?,
                                        position: Int,
                                        id: Long) {
                val spinnerParent = parent as Spinner
                porterDuff = spinnerParent.selectedItem as String
                globals!!.imageSegmentation!!.setPorterDuff(porterDuff)
                Log.i(TAG, "Selected porterduff mode ${porterDuff}")
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                porterDuff = Constants.IMAGE_SEGMENTATION_PORTERDUFF
            }
        }
    }



    private fun buildPreviewConfig(screenSize: Size,
                                   screenAspectRatio: Rational): PreviewConfig {
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
            analyzerThread.start()
            setCallbackHandler(Handler(analyzerThread.looper))
            setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
        }.build()
    }

    fun segmentAwait(image: ImageProxy){
        if (globals!!.imageSegmentation == null){
            return
        }

        val inputBitmap = ImageUtils.imageToBitmap(image)
        Log.i(TAG, "input size: ${inputBitmap.width}, ${inputBitmap.height}")

        val scaledInputBitmap = Bitmap.createScaledBitmap(inputBitmap,
            Constants.IMAGE_SEGMENTATION_DIM_SIZE,
            Constants.IMAGE_SEGMENTATION_DIM_SIZE,
            true)

        val segmented = globals!!
            .imageSegmentation!!
            .segmentAwait(scaledInputBitmap)
        if(segmented == null){
            return
        }

        val results = globals!!
            .imageSegmentation!!
            .extractSegmentation(segmented)

        val segmentedBitmap = globals!!
            .imageSegmentation!!
            .postProcess(results)

        val output = globals!!
            .imageSegmentation!!
            .maskWithSegmentation(inputBitmap,
                segmentedBitmap)

        val imageBitmap = ImageUtils.scaleBitmapWithRotation(output,
            segmentationView)

        segmentationView.set(imageBitmap)

        output.recycle()
        segmentedBitmap.recycle()
        scaledInputBitmap.recycle()
        inputBitmap.recycle()
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
        analyzerThread.interrupt()
    }

    override fun onDestroy() {
        super.onDestroy()
        analyzerThread.quitSafely()
    }

}
