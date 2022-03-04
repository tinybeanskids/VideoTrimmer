package com.video.trimmer.view

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.video.trimmer.R
import com.video.trimmer.utils.convertDpToPixel
import java.util.*


class Thumb private constructor() {

    var index: Int = 0
        private set
    var value: Float = 0.toFloat()
    var pos: Float = 0.toFloat()
    var bitmap: Bitmap? = null
        private set(bitmap) {
            field = bitmap
            widthBitmap = bitmap?.width ?: 0
            heightBitmap = bitmap?.height ?: 0
        }
    var widthBitmap: Int = 0
        private set
    private var heightBitmap: Int = 0

    var lastTouchX: Float = 0.toFloat()

    init {
        value = 0f
        pos = 0f
    }

    companion object {
        const val LEFT = 0
        const val RIGHT = 1

        fun initThumbs(context: Context): List<Thumb> {
            val thumbs = Vector<Thumb>()
            for (i in 0..1) {
                val th = Thumb()
                th.index = i
                if (i == 0) {
                    val resImageLeft = ContextCompat.getDrawable(context, R.drawable.thumb)
                    th.bitmap = resImageLeft?.toBitmap(
                        convertDpToPixel(10f, context).toInt(), convertDpToPixel(
                            40f,
                            context
                        ).toInt(), null
                    )
                } else {

                    val resImageRight = ContextCompat.getDrawable(context, R.drawable.thumb)
                    th.bitmap = resImageRight?.toBitmap(
                        convertDpToPixel(10f, context).toInt(), convertDpToPixel(
                            40f,
                            context
                        ).toInt(), null
                    )
                }
                thumbs.add(th)
            }
            return thumbs
        }

        fun getWidthBitmap(thumbs: List<Thumb>): Int = thumbs[0].widthBitmap

        fun getHeightBitmap(thumbs: List<Thumb>): Int = thumbs[0].heightBitmap

    }


}
