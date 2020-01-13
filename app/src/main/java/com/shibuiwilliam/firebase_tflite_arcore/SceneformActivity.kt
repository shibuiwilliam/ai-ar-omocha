package com.shibuiwilliam.firebase_tflite_arcore

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.widget.*
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.math.Vector3
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.shibuiwilliam.firebase_tflite_arcore.ar.SceneformArFragment
import com.shibuiwilliam.firebase_tflite_arcore.common.*
import kotlin.concurrent.thread


class SceneformActivity : AppCompatActivity() {
    private val TAG = "SceneformActivity"

    private var globals: Globals? = null

    private lateinit var arFragment: ArFragment

    private var selectedObject: Int = R.layout.saturn
    private var isShadowCast = true
    private var isShadowReceive = true

    private var callbackThread = HandlerThread("callback-worker")
    private lateinit var callbackHandler: Handler

    private lateinit var drawView: DrawView
    private lateinit var drawable_image_view: ImageView
    private var targetBitmap: Bitmap? = null
    private var segmentedBitmap: Bitmap? = null

    private lateinit var arcoreFocusOnSpinner: Spinner
    private var arcoreFocusOn = Constants.FOCUS_ON_CAPTURE

    private var initialized = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkIsSupportedDeviceOrFinish(this)) {
            Utils.logAndToast(this, TAG,"ARcore ready to use", "i")
        }
        else {
            return
        }

        initializeGlobals()
        initialize()
        globals!!.imageSegmentation!!.awaitMilliSeconds = 1000L
        globals!!.imageSegmentation!!.setAlpha(Constants.IMAGE_SEGMENTATION_ALPHA_255)
        globals!!.imageSegmentation!!.setColor(Constants.IMAGE_SEGMENTATION_COLOR_FRONT_WHITE)
        globals!!.imageSegmentation!!.setPorterDuff(Constants.IMAGE_SEGMENTATION_PORTERDUFF_DSTIN)


        setContentView(R.layout.activity_sceneform)
        arFragment = supportFragmentManager
            .findFragmentById(R.id.sceneform_fragment) as SceneformArFragment
        drawView = findViewById(R.id.sceneform_drawview)
        drawView.makeVisible()

        configureSpinner()


//        arFragment.setOnTapArPlaneListener { hitResult: HitResult,
//                                             plane: Plane?,
//                                             motionEvent: MotionEvent? ->
//            placeObject(arFragment!!,
//                hitResult.getTrackable().createAnchor(
//                    hitResult.hitPose.compose(
//                        Pose.makeTranslation(0.0f, 0.0f, 0.0f))))
//        }

        arFragment.arSceneView.scene.addOnUpdateListener(this::onUpdateFrame)
    }

    private fun initializeGlobals(){
        if (globals == null){
            globals = application as Globals
            globals!!.initialize(this)
        }
    }

    private fun initialize(){
        if (!initialized) {
            val saturn = ImageView(this)
            saturn.setImageResource(R.drawable.saturn)
            saturn.setOnClickListener {
                selectedObject = R.layout.saturn
            }
            saturn.adjustViewBounds = true

            callbackThread.start()
            callbackHandler = Handler(callbackThread.looper)
            initialized = true
        }
    }

    private fun configureSpinner(){
        arcoreFocusOnSpinner = findViewById(R.id.arcore_focus_on)
        val arcoreFocusOnAdapter = ArrayAdapter(
            applicationContext,
            android.R.layout.simple_spinner_item,
            Constants.ARCORE_FOCUS_ON
        )
        arcoreFocusOnAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        arcoreFocusOnSpinner.adapter = arcoreFocusOnAdapter
        arcoreFocusOnSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?,
                                        view: View?,
                                        position: Int,
                                        id: Long) {
                val spinnerParent = parent as Spinner
                arcoreFocusOn = spinnerParent.selectedItem as String
                when(arcoreFocusOn){
                    Constants.FOCUS_ON_CAPTURE ->{
                        drawView.makeVisible()
                    }
                    Constants.FOCUS_ON_ARCORE -> {
                        drawView.makeInvisible()
                    }
                    else -> {
                        drawView.makeVisible()
                    }
                }
                Log.i(TAG, "Selected arcore focus on ${arcoreFocusOn}")
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                arcoreFocusOn = Constants.FOCUS_ON_CAPTURE
            }
        }
    }

    private fun onUpdateFrame(frameTime: FrameTime?){
        val frame = arFragment.arSceneView.arFrame
        if (frame==null){
            return
        }
        if (drawView.drawId != null){
            val points = drawView.points
            if (points[0] != null){
                val leftTopRightBottom = Utils.extractFromPoints(points)

                Log.i(TAG, "POINTS: ${leftTopRightBottom}")
                copyPixelFromView(arFragment.getArSceneView()){
                    Log.i(TAG, "Inside copy pixel")
                    targetBitmap = Bitmap.createBitmap(it,
                        leftTopRightBottom["left"]!!,
                        leftTopRightBottom["top"]!!,
                        leftTopRightBottom["right"]!!-leftTopRightBottom["left"]!!,
                        leftTopRightBottom["bottom"]!!-leftTopRightBottom["top"]!!,
                        null,
                        true)
                }

                Log.i(TAG, "View renderable from image view")
                drawView.drawId = null
                drawView.makeInvisible()
                drawView.makeVisible()
            }
        }
        if (targetBitmap != null){
            thread {
                createSegmentedBitmap(targetBitmap)
            }
        }
        if (segmentedBitmap != null){
            placeRenderableFromDrawable(arFragment, segmentedBitmap!!)
            segmentedBitmap = null
        }
    }

    private fun copyPixelFromView(view: SurfaceView, callback: (Bitmap)->Unit){
        var bitmap = Bitmap.createBitmap(view!!.width,
            view.height,
            Bitmap.Config.ARGB_8888)
        PixelCopy.request(view, bitmap, { copyResult ->
            if (copyResult == PixelCopy.SUCCESS) {
                Log.i(TAG, "Copying ArFragment view.")
                callback(bitmap)
                Log.i(TAG, "Copied ArFragment view.")

            } else {
                Log.e(TAG, "Failed to copy ArFragment view.")
            }
        }, callbackHandler)
    }

    private fun createSegmentedBitmap(bitmap: Bitmap?){
        if (bitmap == null){
            Log.i(TAG, "segmentation: bitmap is null")
            return
        }

        val scaledInputBitmap = Bitmap.createScaledBitmap(bitmap!!,
            Constants.IMAGE_SEGMENTATION_DIM_SIZE,
            Constants.IMAGE_SEGMENTATION_DIM_SIZE,
            true)
        targetBitmap = null

        val segmented = globals!!
            .imageSegmentation!!
            .segmentAwait(scaledInputBitmap)
        if(segmented == null){
            Log.i(TAG, "segmentation: segmentation is null")
            return
        }

        val results = globals!!
            .imageSegmentation!!
            .extractSegmentation(segmented)

        val segmentationBitmap = globals!!
            .imageSegmentation!!
            .postProcess(results)

        val output = globals!!
            .imageSegmentation!!
            .maskWithSegmentation(bitmap,
                segmentationBitmap)

        setSegmentedBitmap(output)

        scaledInputBitmap.recycle()
        segmentationBitmap.recycle()

        Log.i(TAG, "segment: ${segmentedBitmap!!.width}, ${segmentedBitmap!!.height}")
    }

    private fun setSegmentedBitmap(bitmap: Bitmap){
        segmentedBitmap = bitmap
    }

    private fun placeRenderableFromDrawable(arFragment: ArFragment, bitmap: Bitmap) {
        drawable_image_view = ImageView(this)
        drawable_image_view.setImageBitmap(bitmap)
        drawable_image_view.alpha = 1.0f
        Log.i(TAG, "Image view from bitmap")

        ViewRenderable
            .builder()
            .setView(arFragment.context, drawable_image_view)
            .build()
            .thenAccept{it ->
                Log.i(TAG,"Set drawable view renderable.")
                addDrawable(it)
            }
    }

    private fun addDrawable(viewRenderable: ViewRenderable){
        val frame = arFragment.arSceneView.arFrame!!
        val hitTest = frame.hitTest(frame.screenCenter().x,
            frame.screenCenter().y)

        val hitResult = hitTest[0]
        Log.i(TAG, "${hitResult.distance}, " +
                "${hitResult.hitPose.xAxis.asList()}, " +
                "${hitResult.hitPose.yAxis.asList()}, " +
                "${hitResult.hitPose.zAxis.asList()}")

        //Create an anchor at the plane hit
        val modelAnchor = arFragment
            .arSceneView
            .session!!
            .createAnchor(hitResult.hitPose)

        //Attach a node to this anchor with the scene as the parent
        val anchorNode = AnchorNode(modelAnchor)
        anchorNode.setParent(arFragment.arSceneView.scene)

        //create a new TranformableNode that will carry our object
        val transformableNode = TransformableNode(arFragment.transformationSystem)
        transformableNode.setParent(anchorNode)
        transformableNode.renderable = viewRenderable

        //Alter the real world position to ensure object renders on the table top. Not somewhere inside.
        transformableNode.worldPosition = Vector3(
            modelAnchor.pose.tx(),
            modelAnchor.pose.ty(),
            modelAnchor.pose.tz()
        )
    }

    private fun Frame.screenCenter(): Vector3 {
        val vw = findViewById<View>(android.R.id.content)
        return Vector3(vw.width / 2f, vw.height / 2f, 0f)
    }

    private fun placeObject(arFragment: ArFragment, anchor: Anchor) {
        ViewRenderable
            .builder()
            .setView(arFragment.context, selectedObject)
            .build()
            .thenAccept {it->
                it.setShadowCaster(isShadowCast)
                it.setShadowReceiver(isShadowReceive)
                addNodeToScene(arFragment, anchor, it)
                Log.i(TAG, "Placing AR object")
            }
            .exceptionally {
                val builder = AlertDialog.Builder(this)
                builder.setMessage(it.message).setTitle("${TAG}: Error")
                builder.create().show()
                return@exceptionally null
            }
    }

    private fun addNodeToScene(arFragment: ArFragment,
                               anchor: Anchor,
                               renderable: Renderable) {
        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(arFragment.arSceneView.scene)
        val node = TransformableNode(arFragment.transformationSystem)
        node.renderable = renderable
        node.setParent(anchorNode)
        node.select()
    }


    private fun classifySurfaceView(view: SurfaceView) {
        val bitmap = Bitmap.createBitmap(view!!.width,
            view.height,
            Bitmap.Config.ARGB_8888)

        PixelCopy.request(view, bitmap, { copyResult ->
            if (copyResult == PixelCopy.SUCCESS) {
                Log.i(TAG, "Copied ArFragment view.")
                val classified = globals!!
                    .imageClassifierMap[Constants.CLASSIFIER_MODEL_NAME]!!
                    .classifyAwait(bitmap)
                if (classified == null){
                    return@request
                }
                val results = globals!!
                    .imageClassifierMap[Constants.CLASSIFIER_MODEL_NAME]!!
                    .extractResults(classified!!)

                Utils.logAndToast(
                    this,
                    TAG,
                    "Classified: ${results}.",
                    "i")

            } else {
                Utils.logAndToast(
                    this,
                    TAG,
                    "Failed to copy ARfragment.",
                    "e")
            }
        }, callbackHandler)
    }

    private fun classifyArFragmentView(arFragment: ArFragment) {
        val view = arFragment.getArSceneView()
        classifySurfaceView(view)
    }

    private fun classifyDrawnArFragmentView(arFragment: ArFragment,
                                            left: Int,
                                            right: Int,
                                            top: Int,
                                            bottom: Int) {
        val view = arFragment.getArSceneView()

        val bitmap = Bitmap.createBitmap(view!!.width,
            view.height,
            Bitmap.Config.ARGB_8888)

        PixelCopy.request(view, bitmap, { copyResult ->
            if (copyResult == PixelCopy.SUCCESS) {
                Log.i(TAG, "Copied ArFragment view.")

                Log.i(TAG, "${right-left}, ${bottom-top}")
                val croppedBitmap = Bitmap.createBitmap(bitmap,
                    left,
                    top,
                    right-left,
                    bottom-top,
                    null,
                    true)
                Log.i(TAG, "Cropped ArFragment view.")

                val classified = globals!!
                    .imageClassifierMap[Constants.CLASSIFIER_MODEL_NAME]!!
                    .classifyAwait(croppedBitmap)
                if (classified == null){
                    return@request
                }
                val results = globals!!
                    .imageClassifierMap[Constants.CLASSIFIER_MODEL_NAME]!!
                    .extractResults(classified!!)

                Utils.logAndToast(
                    this,
                    TAG,
                    "Classified: ${results}.",
                    "i")

            } else {
                Utils.logAndToast(
                    this,
                    TAG,
                    "Failed to copy ARfragment.",
                    "e")
            }
        }, callbackHandler)
    }

    private fun detectLabelArFragmentView(arFragment: ArFragment) {
        val view = arFragment.getArSceneView()

        val bitmap = Bitmap.createBitmap(view!!.width,
            view.height,
            Bitmap.Config.ARGB_8888)

        PixelCopy.request(view, bitmap, { copyResult ->
            if (copyResult == PixelCopy.SUCCESS) {
                val firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap)
                globals!!.imageLabeler!!.processImage(firebaseVisionImage)
                    .addOnSuccessListener { labels ->
                        var texts: String = ""
                        if (labels.isNullOrEmpty()){
                            Utils.logAndToast(this,
                                TAG,
                                "No image label",
                                "i")
                        }
                        else{
                            for (label in labels) {
                                texts = texts + "${label.text}: ${label.confidence} \n"
                            }
                            Utils.logAndToast(this,
                                TAG,
                                "Results: ${texts}",
                                "i")
                        }
                    }
                    .addOnFailureListener { ex ->
                        Utils.logAndToast(this,
                            TAG,
                            "Failed to label image: ${ex}",
                            "e")
                    }
            } else {
                Utils.logAndToast(this,
                    TAG,
                    "Failed to copy ARfragment.",
                    "e")
            }
        }, callbackHandler)
    }


    private fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Utils.logAndToast(
                this,
                TAG,
                "Sceneform requires Android N or later",
                "e")
            activity.finish()
            return false
        }
        val openGlVersionString =
            (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .deviceConfigurationInfo
                .glEsVersion
        if (openGlVersionString.toDouble() < Constants.MIN_OPENGL_VERSION) {
            Utils.logAndToast(
                this,
                TAG,
                "Sceneform requires OpenGL ES 3.0 or later",
                "e")
            activity.finish()
            return false
        }
        return true
    }

    override fun onStop() {
        super.onStop()
        callbackThread.quitSafely()
    }

    override fun onDestroy() {
        super.onDestroy()
        callbackThread.quitSafely()
    }

}
