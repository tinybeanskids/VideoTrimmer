package com.video.trimmer.utils

import java.time.Duration
import java.util.*
import kotlin.math.roundToInt

object TrimVideoUtils {

    fun stringForTime(timeMs: Float): String {
        val totalSeconds = (timeMs / 1000).toInt()
        val milliseconds = (timeMs % 1000f).roundToInt()
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600


        val mFormatter = Formatter()
        return if (hours > 0) {
            mFormatter.format("%d:%02d:%02d.%02d", hours, minutes, seconds, milliseconds).toString()
        } else {
            mFormatter.format("%02d:%02d.%02d", minutes, seconds, milliseconds).toString()
        }
    }

    fun stringForPreviewTime(timeMs: Float): String {
        val totalSeconds = (timeMs / 1000).toInt()
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600

        val mFormatter = Formatter()
        return if (hours > 0) {
            mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
        } else {
            mFormatter.format("%02d:%02d", minutes, seconds).toString()
        }
    }
}
