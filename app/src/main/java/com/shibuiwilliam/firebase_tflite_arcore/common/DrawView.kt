package com.shibuiwilliam.firebase_tflite_arcore.common

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View

import java.util.*

class DrawView : View {
    private val TAG = "DrawView"
    var points = arrayOfNulls<Point>(4)
    var groupId = -1
    private var corners = arrayOfNulls<Point>(4)

    private var balID = 0

    var paint: Paint? = null
    var canvas: Canvas? = null

    var drawId: String? = null

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
        makeInvisible()
        Log.i(TAG, "initialized Draw View.")
    }

    override fun onDraw(canvas: Canvas) {
        if (points[3] == null || corners[3] == null)
            return

        val leftTopRightBottom = Utils.extractFromPoints(points)

        paint!!.isAntiAlias = true
        paint!!.isDither = true
        paint!!.strokeJoin = Paint.Join.ROUND
        paint!!.strokeWidth = 5f

        //draw stroke
        paint!!.style = Paint.Style.STROKE
        paint!!.color = Color.parseColor("#FF00F7FF")
        paint!!.strokeWidth = 2f
        canvas.drawRect(
            leftTopRightBottom["left"]!!.toFloat(),
            leftTopRightBottom["top"]!!.toFloat(),
            leftTopRightBottom["right"]!!.toFloat(),
            leftTopRightBottom["bottom"]!!.toFloat(),
            paint!!
        )

        //fill the rectangle
        paint!!.style = Paint.Style.FILL
        paint!!.color = Color.parseColor("#3B00F7FF")
        paint!!.strokeWidth = 0f
        canvas.drawRect(
            leftTopRightBottom["left"]!!.toFloat(),
            leftTopRightBottom["top"]!!.toFloat(),
            leftTopRightBottom["right"]!!.toFloat(),
            leftTopRightBottom["bottom"]!!.toFloat(),
            paint!!
        )
    }

    // events when touching the screen
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isFocusable){
            return true
        }
        val eventaction = event.action
        val X = event.x.toInt()
        val Y = event.y.toInt()
        when (eventaction) {
            MotionEvent.ACTION_DOWN ->  {// a ball
                points = arrayOfNulls<Point>(4)
                corners = arrayOfNulls<Point>(4)
                Log.i(TAG, "Start drawing rectangle")
                points[0] = Point()
                points[0]!!.x = X
                points[0]!!.y = Y
                points[1] = Point()
                points[1]!!.x = X
                points[1]!!.y = Y + 30
                points[2] = Point()
                points[2]!!.x = X + 30
                points[2]!!.y = Y + 30
                points[3] = Point()
                points[3]!!.x = X + 30
                points[3]!!.y = Y
                balID = 2
                groupId = 1

                for ((i,pt) in points.withIndex()) {
                    corners[i] = pt
                }
            }
            MotionEvent.ACTION_MOVE ->{
                corners[balID]!!.x = X
                corners[balID]!!.y = Y

                if (groupId == 1) {
                    corners[1]!!.x = corners[0]!!.x
                    corners[1]!!.y = corners[2]!!.y
                    corners[3]!!.x = corners[2]!!.x
                    corners[3]!!.y = corners[0]!!.y
                }
                else {
                    corners[0]!!.x = corners[1]!!.x
                    corners[0]!!.y = corners[3]!!.y
                    corners[2]!!.x = corners[3]!!.x
                    corners[2]!!.y = corners[1]!!.y
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                drawId = UUID.randomUUID().toString()
                Log.i(TAG, "${drawId!!} Finished drawing rectangle with points: ")
                points.forEachIndexed{i, pt ->
                    Log.i(TAG, "${i+1}: ${pt!!.x}, ${pt!!.y}")
                }
            }
        }

        invalidate()
        return true
    }

    fun makeVisible(){
        this.visibility = VISIBLE
        this.isFocusable = true
    }

    fun makeInvisible(){
        this.visibility = INVISIBLE
        this.isFocusable = false
        this.drawId = null
        this.points[3] = null
    }

}
