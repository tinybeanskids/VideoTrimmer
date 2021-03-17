package com.video.trimmer.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import com.video.trimmer.R
import com.video.trimmer.utils.BackgroundExecutor

class TimeLineView @JvmOverloads constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    private var mVideoUri: Uri? = null
    private var mHeightView: Int = 0
    private var mBitmapList: MutableList<Bitmap> = mutableListOf()
    private var framesWidthTotal = 0f

    init {
        init()
    }

    private fun init() {
        mHeightView = context.resources.getDimensionPixelOffset(R.dimen.frames_video_height)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minW = paddingLeft + paddingRight + suggestedMinimumWidth
        val w = resolveSizeAndState(minW, widthMeasureSpec, 1)
        val minH = paddingBottom + paddingTop + mHeightView
        val h = resolveSizeAndState(minH, heightMeasureSpec, 1)
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        if (w != oldW) getBitmap(w)
    }

    private fun getBitmap(viewWidth: Int) {
        BackgroundExecutor.execute(object : BackgroundExecutor.Task("", 0L, "") {
            override fun execute() {
                try {
                    val threshold = 11
                    val mediaMetadataRetriever = MediaMetadataRetriever()

                    mediaMetadataRetriever.setDataSource(context, mVideoUri)
                    val videoLengthInMs = (Integer.parseInt(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) * 1000).toLong()

                    val frameHeight = mHeightView
                    //val initialBitmap = mediaMetadataRetriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

                    val frameWidth = width / 6
//                    var numThumbs = ceil((viewWidth.toFloat() / frameWidth)).toInt()
                    var numThumbs = 6
//                    if (numThumbs < threshold) numThumbs = threshold
                    //val cropWidth = viewWidth / threshold
                    var cropWidth = viewWidth / numThumbs
                    val interval = videoLengthInMs / numThumbs
                    for (i in 0 until numThumbs) {
                        var bitmap = mediaMetadataRetriever.getFrameAtTime(i * interval, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        if (bitmap != null) {
                            try {
                                bitmap = Bitmap.createScaledBitmap(bitmap, frameWidth, frameHeight, false)
                                bitmap = Bitmap.createBitmap(bitmap, 0, 0, cropWidth, bitmap.height)
                                mBitmapList.add(bitmap)
                                invalidate()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    mediaMetadataRetriever.release()
                } catch (e: Throwable) {
                    Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e)
                }
            }
        })
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!mBitmapList.isNullOrEmpty())
            for (i in mBitmapList) {
                canvas.drawBitmap(i, framesWidthTotal, 0f, null)
                framesWidthTotal += i.width
            }
        framesWidthTotal = 0f
    }

    fun setVideo(data: Uri) {
        mVideoUri = data
    }
}
