package com.shibuiwilliam.firebase_tflite_arcore.common

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View

class SegmentationView: View {
    private val TAG = "SegmentationView"

    var paint: Paint? = null
    var canvas: Canvas? = null

    var displayBitmap: Bitmap? = null

    private val lock = Any()

    constructor(context: Context) : super(context) {
        initialize()
    }

    constructor(context: Context,
                attrs: AttributeSet?) : super(context, attrs) {
        initialize()
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int) : super(context, attrs, defStyle) {
        initialize()
    }


    private fun initialize() {
        paint = Paint()
        canvas = Canvas()
        makeVisible()
        Log.i(TAG, "initialized Segmentation View.")
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        synchronized(lock){
            Log.i(TAG, "display size: ${displayBitmap!!.width}, ${displayBitmap!!.height}")
            canvas.drawBitmap(displayBitmap!!, 0.0f, 0.0f, paint)
        }
    }


    fun set(bitmap: Bitmap){
        synchronized(lock){
            displayBitmap = bitmap
        }
        postInvalidate()
    }

    fun makeVisible(){
        this.visibility = VISIBLE
        this.isFocusable = true
    }

    fun makeInvisible(){
        this.visibility = INVISIBLE
        this.isFocusable = false
    }

}