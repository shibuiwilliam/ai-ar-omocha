package com.shibuiwilliam.firebase_tflite_arcore.common

import android.graphics.Rect

data class BoxData(
    val texts: List<String>,
    val boundingBox: Rect
)