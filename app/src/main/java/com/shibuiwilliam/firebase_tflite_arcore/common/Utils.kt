package com.shibuiwilliam.firebase_tflite_arcore.common

import android.content.Context
import android.graphics.Point
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import kotlin.math.log

object Utils {
    private val TAG = "Utils"

    @Throws(IOException::class)
    fun loadLabelList(context: Context,
                      labelPath: String): List<String> {
        val labelList = ArrayList<String>()
        Log.i(TAG, "Loading ${labelPath}")
        try {
            BufferedReader(InputStreamReader(context.assets.open(labelPath))).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    labelList.add(line)
                    line = reader.readLine()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read label list.", e)
        }
        return labelList
    }

    fun makeToast(context: Context,
                  msg: String,
                  length: Int = Toast.LENGTH_SHORT,
                  gravity: Int = Gravity.TOP){
        val ts = Toast.makeText(
            context,
            msg,
            length
        )
        ts.setGravity(gravity, 0,0)
        ts.show()
    }

    fun logAndToast(context: Context,
                    tag: String,
                    msg: String,
                    logLevel: String = "i",
                    length: Int = Toast.LENGTH_SHORT,
                    gravity: Int = Gravity.TOP){
        makeToast(context, msg, length, gravity)
        when (logLevel){
            "i" -> Log.i(tag, msg)
            "d" -> Log.d(tag, msg)
            "e" -> Log.e(tag, msg)
            "w" -> Log.w(tag, msg)
            "v" -> Log.v(tag, msg)
            else -> Log.i(tag, msg)
        }
    }

    fun extractFromPoints(points: Array<Point?>): MutableMap<String, Int>{
        val leftTopRightBottom: MutableMap<String, Int> = mutableMapOf()
        leftTopRightBottom["left"] = points[0]!!.x
        leftTopRightBottom["top"] = points[0]!!.y
        leftTopRightBottom["right"] = points[0]!!.x
        leftTopRightBottom["bottom"] = points[0]!!.y

        for (i in 1 until points.size) {
            leftTopRightBottom["left"] = minOf(points[i]!!.x, leftTopRightBottom["left"]!!)
//                if (leftTopRightBottom["left"]!! > points[i]!!.x) points[i]!!.x
//                else leftTopRightBottom["left"]!!
            leftTopRightBottom["top"] = minOf(points[i]!!.y, leftTopRightBottom["top"]!!)
//                if (leftTopRightBottom["top"]!! > points[i]!!.y) points[i]!!.y
//                else leftTopRightBottom["top"]!!
            leftTopRightBottom["right"] = maxOf(points[i]!!.x, leftTopRightBottom["right"]!!)
//                if (leftTopRightBottom["right"]!! < points[i]!!.x) points[i]!!.x
//                else leftTopRightBottom["right"]!!
            leftTopRightBottom["bottom"] = maxOf(points[i]!!.y, leftTopRightBottom["bottom"]!!)
//                if (leftTopRightBottom["bottom"]!! < points[i]!!.y) points[i]!!.y
//                else leftTopRightBottom["bottom"]!!
        }
        return leftTopRightBottom
    }

}