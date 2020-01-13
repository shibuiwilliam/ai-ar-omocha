package com.shibuiwilliam.firebase_tflite_arcore.common

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View


class GraphicOverlay(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val TAG = "GraphicOverlay"
    private val lock = Any()

    private val graphics = mutableListOf<BoxData>()

    private var rectPaintStyle: Paint? = null

    private val rectPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2 * resources.displayMetrics.density
    }

    private val rectTransparent = Paint().apply {
        color = Color.TRANSPARENT
        style = Paint.Style.STROKE
        strokeWidth = 2 * resources.displayMetrics.density
    }

    private val textBackgroundPaint = Paint().apply {
        color = Color.parseColor("#66000000")
        style = Paint.Style.FILL
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 20 * resources.displayMetrics.density
    }

    private val rect = RectF()
    private val offset = resources.displayMetrics.density * 8
    private val round = resources.displayMetrics.density * 4

    private var scale: Float = 1f

    private var xOffset: Float = 0f
    private var yOffset: Float = 0f

    fun setConfiguration(imageWidth: Int,
                         imageHeight: Int,
                         rectColor: Int = Color.WHITE) {
        setRectPaint(rectColor)
        val overlayRatio = width / height.toFloat()
        val imageRatio = imageWidth / imageHeight.toFloat()

        if (overlayRatio < imageRatio) {
            scale = height / imageHeight.toFloat()

            xOffset = (imageWidth * scale - width) * 0.5f
            yOffset = 0f
        } else {
            scale = width / imageWidth.toFloat()

            xOffset = 0f
            yOffset = (imageHeight * scale - height) * 0.5f
        }
    }

    private fun setRectPaint(rectColor: Int = Color.WHITE){
        rectPaintStyle = Paint().apply{
            color = rectColor
            style = Paint.Style.STROKE
            strokeWidth = 2 * resources.displayMetrics.density
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        synchronized(lock) {
            for (graphic in graphics) {
                rect.set(
                    graphic.boundingBox.left * scale,
                    graphic.boundingBox.top * scale,
                    graphic.boundingBox.right * scale,
                    graphic.boundingBox.bottom * scale
                )

                rect.offset(-xOffset, -yOffset)

                canvas.drawRect(rect, rectPaintStyle!!)

                if (graphic.texts.isNotEmpty()) {
                    canvas.drawRoundRect(
                        rect.left,
                        rect.bottom - offset,
                        rect.left + (offset*2) + (40*20),
                        rect.bottom + (textPaint.textSize * graphic.texts.size) + offset,
                        round,
                        round,
                        textBackgroundPaint
                    )
                    for ((i, text) in graphic.texts.withIndex()){
                        canvas.drawText(
                            "${i}: ${text}",
                            rect.left + offset,
                            rect.bottom + (textPaint.textSize * (i+1)),
                            textPaint
                        )
                    }
                }
            }
        }
    }

    fun set(list: List<BoxData>) {
        synchronized(lock) {
            graphics.clear()
            for (boxData in list) {
                graphics.add(boxData)
            }
        }
        postInvalidate()
    }
}
