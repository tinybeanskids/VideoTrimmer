package com.video.trimmer.utils

import android.content.Context
import android.util.DisplayMetrics
import com.video.trimmer.view.Thumb

fun Thumb.convertDpToPixel(dp: Float, context: Context): Float {
    return dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}