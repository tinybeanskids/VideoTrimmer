package com.video.trimmer.utils

import android.content.Context
import android.util.DisplayMetrics

fun convertDpToPixel(dp: Float, context: Context): Float {
    return dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}