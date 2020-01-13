package com.shibuiwilliam.firebase_tflite_arcore.ml

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageProxy
import com.shibuiwilliam.firebase_tflite_arcore.common.BoxData
import com.shibuiwilliam.firebase_tflite_arcore.common.FirebaseVisionImageUtils
import com.shibuiwilliam.firebase_tflite_arcore.common.Globals
import com.shibuiwilliam.firebase_tflite_arcore.common.GraphicOverlay

class MLProcessor(private val globals:Globals){

    private val TAG = "MLProcessor"


    fun classify(image: ImageProxy,
                 overlay: GraphicOverlay,
                 activeModelName: String){
        val currentModelName = activeModelName
        if (!globals.imageClassifierMap.containsKey(currentModelName)
            || !globals.imageClassifierMap[currentModelName]!!.initialized){
            return
        }

        val list = mutableListOf<BoxData>()
        globals
            .imageClassifierMap[currentModelName]!!
            .classify(image)
            .addOnSuccessListener {
                Log.i(TAG, "${it}")
                val results = globals
                    .imageClassifierMap[currentModelName]!!
                    .extractResults(it)
                Log.i(TAG, "Classified: ${results}.")
                list.add(
                    BoxData(results,
                        Rect(100,10,200,20)
                    )
                )
                overlay.set(list)
            }.addOnFailureListener {ex ->
                Log.e(TAG, "Failed on Classifier: ${ex}")
            }
    }

    fun classifyAwait(image: ImageProxy,
                      overlay: GraphicOverlay,
                      activeModelName: String){
        val currentModelName = activeModelName
        if (!globals.imageClassifierMap.containsKey(currentModelName)
            || !globals.imageClassifierMap[currentModelName]!!.initialized){
            return
        }

        val classified = globals
            .imageClassifierMap[currentModelName]!!
            .classifyAwait(image)
        if(classified == null){
            return
        }

        val list = mutableListOf<BoxData>()
        val results = globals
            .imageClassifierMap[currentModelName]!!
            .extractResults(classified!!)
        list.add(BoxData(results, Rect(100,10,200,20)))
        Log.i(TAG, "Classified: ${results}.")
        overlay.set(list)
    }


    fun classifyAwait(image: Bitmap,
                      overlay: GraphicOverlay,
                      activeModelName: String){
        val currentModelName = activeModelName
        if (!globals.imageClassifierMap.containsKey(currentModelName)
            || !globals.imageClassifierMap[currentModelName]!!.initialized){
            return
        }

        val classified = globals
            .imageClassifierMap[currentModelName]!!
            .classifyAwait(image)
        if(classified == null){
            return
        }

        val list = mutableListOf<BoxData>()
        val results = globals
            .imageClassifierMap[currentModelName]!!
            .extractResults(classified!!)
        list.add(BoxData(results, Rect(100,10,200,20)))
        Log.i(TAG, "Classified: ${results}.")
        overlay.set(list)
    }


    fun classifyFromDetection(image: ImageProxy,
                              rotationDegrees: Int,
                              overlay: GraphicOverlay,
                              activeModelName: String){
        if (!globals!!.imageClassifierMap.containsKey(activeModelName)
            || !globals!!.imageClassifierMap[activeModelName]!!.initialized
            || !globals!!.objectDetector!!.initialized){
            return
        }
        val firebaseVisionImage = FirebaseVisionImageUtils
            .imageFromMediaImage(image.image!!,
                FirebaseVisionImageUtils.getFirebaseRotation(rotationDegrees))
        var detectedBitmap: Bitmap? = null

        val list = mutableListOf<BoxData>()
        globals!!.objectDetector!!.processImage(firebaseVisionImage)
            .addOnSuccessListener { detectedObjects ->
                overlay.setConfiguration(
                    firebaseVisionImage.bitmap.width,
                    firebaseVisionImage.bitmap.height,
                    Color.WHITE)
                for (obj in detectedObjects) {
                    detectedBitmap = FirebaseVisionImageUtils.makeBitmapFromObject(obj,
                        firebaseVisionImage,
                        null,
                        true)
                    Log.i(TAG, "DETECTED: (${obj.boundingBox.left}, ${obj.boundingBox.top})")
                    globals!!
                        .imageClassifierMap[activeModelName]!!
                        .classify(detectedBitmap!!)
                        .addOnSuccessListener {
                            val results = globals!!
                                .imageClassifierMap[activeModelName]!!
                                .extractResults(it)
                            Log.i(TAG, "Classified: ${results}.")
                            list.add(BoxData(results, obj.boundingBox))
                        }.addOnFailureListener {ex ->
                            Log.e(TAG, "Failed on Classifier: ${ex}")
                            list.add(BoxData(List<String>(1){"Unknown object"}, obj.boundingBox))
                        }
                }
                overlay.set(list)
            }
            .addOnFailureListener { ex ->
                Log.e(TAG, "Failed to Detect Object: ${ex}")
            }
    }

    fun classifyFromDetectionAwait(image: ImageProxy,
                                   rotationDegrees: Int,
                                   overlay: GraphicOverlay,
                                   activeModelName: String,
                                   objectDetectAwaitSecond: Long){
        val currentModelName = activeModelName
        if (!globals!!.imageClassifierMap.containsKey(currentModelName)
            || !globals!!.imageClassifierMap[currentModelName]!!.initialized
            || !globals!!.objectDetector!!.initialized){
            return
        }
        val firebaseVisionImage = FirebaseVisionImageUtils
            .imageFromMediaImage(image.image!!,
                FirebaseVisionImageUtils.getFirebaseRotation(rotationDegrees))
        var detectedBitmap: Bitmap? = null

        val detectedObjects = globals!!
            .objectDetector!!
            .processImageAwait(firebaseVisionImage, objectDetectAwaitSecond)
        if (detectedObjects == null){
            return
        }

        overlay.setConfiguration(firebaseVisionImage.bitmap.width,
            firebaseVisionImage.bitmap.height,
            Color.WHITE)
        val list = mutableListOf<BoxData>()
        for (obj in detectedObjects!!) {
            detectedBitmap = FirebaseVisionImageUtils.makeBitmapFromObject(obj,
                firebaseVisionImage,
                null,
                true)

            Log.i(TAG, "DETECTED: (${obj.boundingBox.left}, ${obj.boundingBox.top})")
            Log.i(TAG, "DETECTED ID: ${obj.trackingId}")
            val classified = globals!!
                .imageClassifierMap[currentModelName]!!
                .classifyAwait(detectedBitmap)
            if(classified == null){
                continue
            }

            val results = globals!!
                .imageClassifierMap[currentModelName]!!
                .extractResults(classified!!)
            Log.i(TAG, "Classified: ${results}.")
            list.add(BoxData(results, obj.boundingBox))
        }
        overlay.set(list)
    }


    fun labelImage(image: ImageProxy,
                   rotationDegrees: Int,
                   overlay: GraphicOverlay){
        val firebaseVisionImage = FirebaseVisionImageUtils
            .imageFromMediaImage(image.image!!,
                FirebaseVisionImageUtils.getFirebaseRotation(rotationDegrees))

        globals!!.imageLabeler!!.processImage(firebaseVisionImage)
            .addOnSuccessListener { labels ->
                val list = mutableListOf<BoxData>()
                val texts = mutableListOf<String>()
                for (label in labels) {
                    texts.add("${label.text}: ${label.confidence}")
                    list.add(BoxData(texts, Rect(100,10,200,20)))
                }
                Log.i(TAG, "Labeled: ${texts}")
                overlay.set(list)
            }
            .addOnFailureListener { ex ->
                Log.e(TAG, "Failed to Label Image: ${ex}")
            }
    }

    fun labelImageAwait(image: ImageProxy,
                        rotationDegrees: Int,
                        overlay: GraphicOverlay,
                        imageLabelerAwaidSecond: Long){
        val firebaseVisionImage = FirebaseVisionImageUtils
            .imageFromMediaImage(image.image!!,
                FirebaseVisionImageUtils.getFirebaseRotation(rotationDegrees))

        val labels = globals!!
            .imageLabeler!!
            .processImageAwait(firebaseVisionImage,
                imageLabelerAwaidSecond)
        if (labels == null){
            return
        }

        val list = mutableListOf<BoxData>()
        val texts = mutableListOf<String>()
        for (label in labels!!) {
            texts.add("${label.text}: ${label.confidence}")
            list.add(BoxData(texts, Rect(100,10,200,20)))
        }
        Log.i(TAG, "Labeled: ${texts}")
        overlay.set(list)
    }

    fun labelImageAwait(image: Bitmap,
                        rotationDegrees: Int,
                        overlay: GraphicOverlay,
                        imageLabelerAwaidSecond: Long){
        val firebaseVisionImage = FirebaseVisionImageUtils
            .imageFromBitmap(image)

        val labels = globals!!
            .imageLabeler!!
            .processImageAwait(firebaseVisionImage,
                imageLabelerAwaidSecond)
        if (labels == null){
            return
        }

        val list = mutableListOf<BoxData>()
        val texts = mutableListOf<String>()
        for (label in labels!!) {
            texts.add("${label.text}: ${label.confidence}")
            list.add(BoxData(texts, Rect(100,10,200,20)))
        }
        Log.i(TAG, "Labeled: ${texts}")
        overlay.set(list)
    }

    fun labelImageFromDetectionAwait(image: ImageProxy,
                                     rotationDegrees: Int,
                                     overlay: GraphicOverlay,
                                     objectDetectAwaitSecond: Long,
                                     imageLabelerAwaidSecond: Long){
        val firebaseVisionImage = FirebaseVisionImageUtils
            .imageFromMediaImage(image.image!!,
                FirebaseVisionImageUtils.getFirebaseRotation(rotationDegrees))
        var detectedBitmap: Bitmap? = null

        val detectedObjects = globals!!
            .objectDetector!!
            .processImageAwait(firebaseVisionImage, objectDetectAwaitSecond)
        if (detectedObjects == null){
            return
        }

        overlay.setConfiguration(firebaseVisionImage.bitmap.width,
            firebaseVisionImage.bitmap.height,
            Color.WHITE)

        val list = mutableListOf<BoxData>()
        for (obj in detectedObjects!!) {
            detectedBitmap = FirebaseVisionImageUtils
                .makeBitmapFromObject(obj,
                    firebaseVisionImage,
                    null,
                    true)

            Log.i(TAG, "DETECTED: (${obj.boundingBox.left}, ${obj.boundingBox.top})")
            val labels = globals!!
                .imageLabeler!!
                .processImageAwait(detectedBitmap,
                    imageLabelerAwaidSecond)
            if (labels == null){
                continue
            }

            val texts = mutableListOf<String>()
            for (label in labels!!) {
                texts.add("${label.text}: ${label.confidence}")
            }
            Log.i(TAG, "Labeled: ${texts}.")
            list.add(BoxData(texts, obj.boundingBox))
        }
        overlay.set(list)
    }

    fun labelImageFromDetection(image: ImageProxy,
                                rotationDegrees: Int,
                                overlay: GraphicOverlay){
        if (!globals!!.imageLabeler!!.initialized
            || !globals!!.objectDetector!!.initialized){
            return
        }
        val firebaseVisionImage = FirebaseVisionImageUtils
            .imageFromMediaImage(image.image!!,
                FirebaseVisionImageUtils.getFirebaseRotation(rotationDegrees))
        var detectedBitmap: Bitmap? = null

        val list = mutableListOf<BoxData>()
        globals!!.objectDetector!!.processImage(firebaseVisionImage)
            .addOnSuccessListener { detectedObjects ->
                overlay.setConfiguration(firebaseVisionImage.bitmap.width,
                    firebaseVisionImage.bitmap.height,
                    Color.WHITE)
                for (obj in detectedObjects) {
                    detectedBitmap = FirebaseVisionImageUtils.makeBitmapFromObject(obj,
                        firebaseVisionImage,
                        null,
                        true)
                    Log.i(TAG, "DETECTED: (${obj.boundingBox.left}, ${obj.boundingBox.top})")
                    globals!!
                        .imageLabeler!!
                        .processImage(detectedBitmap!!)
                        .addOnSuccessListener {
                            val texts = mutableListOf<String>()
                            for (label in it!!) {
                                texts.add("${label.text}: ${label.confidence}")
                            }
                            Log.i(TAG, "Labeled: ${texts}.")
                            list.add(BoxData(texts, obj.boundingBox))
                        }.addOnFailureListener {ex ->
                            Log.e(TAG, "Failed on Labeling: ${ex}")
                        }
                }
                overlay.set(list)
            }
            .addOnFailureListener { ex ->
                Log.e(TAG, "Failed to Detect Object: ${ex}")
            }
    }

}