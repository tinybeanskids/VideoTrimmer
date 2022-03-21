package com.video.trimmer.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.video.trimmer.R
import com.video.trimmer.interfaces.OnRangeSeekBarListener

class RangeSeekBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var mHeightTimeLine = 0
    var thumbs: List<Thumb>? = null
    private var mListeners: MutableList<OnRangeSeekBarListener>? = null
    private var mMaxWidth = 0f
    private var mThumbWidth = 0f
    private var mThumbHeight = 0f
    private var mViewWidth = 0
    private var mPixelRangeMin = 0f
    private var mPixelRangeMax = 0f
    private var mScaleRangeMax = 0f
    private var mFirstRun = false

    private val mShadow = Paint()
    private val mLine = Paint()

    private var currentThumb = 0

    init {
        init()
    }

    private fun init() {
        thumbs = Thumb.initThumbs(context)
        thumbs?.let {
            mThumbWidth = Thumb.getWidthBitmap(it).toFloat()
            mThumbHeight = Thumb.getHeightBitmap(it).toFloat()
        }

        mScaleRangeMax = 100f
        mHeightTimeLine = context.resources.getDimensionPixelOffset(R.dimen.frames_video_height)

        isFocusable = true
        isFocusableInTouchMode = true

        mFirstRun = true

        val shadowColor = ContextCompat.getColor(context, R.color.shadow_color)
        mShadow.isAntiAlias = true
        mShadow.color = shadowColor
        mShadow.alpha = 177

        val lineColor = ContextCompat.getColor(context, R.color.line_color)
        mLine.isAntiAlias = true
        mLine.color = lineColor
        mLine.alpha = 200
    }

    fun initMaxWidth() {
        thumbs?.let { thumbsList ->
            if (thumbsList.size == 2) {
                mMaxWidth = thumbsList[1].pos - thumbsList[0].pos
                onSeekStop(this, 0, thumbsList[0].value)
                onSeekStop(this, 1, thumbsList[1].value)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val minW = paddingLeft + paddingRight + suggestedMinimumWidth
        mViewWidth = resolveSizeAndState(minW, widthMeasureSpec, 1)

        val minH = paddingBottom + paddingTop + mThumbHeight.toInt() + mHeightTimeLine
        val viewHeight = resolveSizeAndState(minH, heightMeasureSpec, 1)

        setMeasuredDimension(mViewWidth, viewHeight)

        mPixelRangeMin = 0f
        mPixelRangeMax = mViewWidth - mThumbWidth

        if (mFirstRun) {
            thumbs?.let { thumbsList ->
                for (index in thumbsList.indices) {
                    val thumb = thumbsList[index]
                    thumb.value = mScaleRangeMax * index
                    thumb.pos = mPixelRangeMax * index
                }
                getThumbValue(currentThumb)?.let { it -> onCreate(this, currentThumb, it) }
                mFirstRun = false
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawShadow(canvas)
        drawThumbs(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val mThumb: Thumb
        val mThumb2: Thumb
        val coordinate = ev.x
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                currentThumb = getClosestThumb(coordinate)
                if (currentThumb == -1) return false
                thumbs?.let {
                    mThumb = it[currentThumb]
                    mThumb.lastTouchX = coordinate
                    onSeekStart(this, currentThumb, mThumb.value)
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (currentThumb == -1) return false
                thumbs?.let {
                    mThumb = it[currentThumb]
                    onSeekStop(this, currentThumb, mThumb.value)
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                thumbs?.let {
                    mThumb = it[currentThumb]
                    mThumb2 = it[if (currentThumb == 0) 1 else 0]
                    val dx = coordinate - mThumb.lastTouchX
                    val newX = mThumb.pos + dx
                    if (currentThumb == 0) {
                        when {
                            newX + mThumb.widthBitmap >= mThumb2.pos -> mThumb.pos =
                                mThumb2.pos - mThumb.widthBitmap
                            newX <= mPixelRangeMin -> mThumb.pos = mPixelRangeMin
                            else -> {
                                checkPositionThumb(mThumb, mThumb2, dx, true)
                                mThumb.pos = mThumb.pos + dx
                                mThumb.lastTouchX = coordinate
                            }
                        }

                    } else {
                        when {
                            newX <= mThumb2.pos + mThumb2.widthBitmap -> mThumb.pos =
                                mThumb2.pos + mThumb.widthBitmap
                            newX >= mPixelRangeMax -> mThumb.pos = mPixelRangeMax
                            else -> {
                                checkPositionThumb(mThumb2, mThumb, dx, false)
                                mThumb.pos = mThumb.pos + dx
                                mThumb.lastTouchX = coordinate
                            }
                        }
                    }

                    setThumbPos(currentThumb, mThumb.pos)
                    invalidate()
                    return true
                }
            }
        }
        return false
    }

    private fun checkPositionThumb(
        mThumbLeft: Thumb,
        mThumbRight: Thumb,
        dx: Float,
        isLeftMove: Boolean
    ) {
        if (isLeftMove && dx < 0) {
            if (mThumbRight.pos + dx - mThumbLeft.pos > mMaxWidth) {
                mThumbRight.pos = mThumbLeft.pos + dx + mMaxWidth
                setThumbPos(1, mThumbRight.pos)
            }
        } else if (!isLeftMove && dx > 0) {
            if (mThumbRight.pos + dx - mThumbLeft.pos > mMaxWidth) {
                mThumbLeft.pos = mThumbRight.pos + dx - mMaxWidth
                setThumbPos(0, mThumbLeft.pos)
            }
        }
    }

    private fun pixelToScale(index: Int, pixelValue: Float): Float {
        val scale = pixelValue * 100 / mPixelRangeMax
        return if (index == 0) {
            val pxThumb = scale * mThumbWidth / 100
            scale + pxThumb * 100 / mPixelRangeMax
        } else {
            val pxThumb = (100 - scale) * mThumbWidth / 100
            scale - pxThumb * 100 / mPixelRangeMax
        }
    }

    private fun scaleToPixel(index: Int, scaleValue: Float): Float {
        val px = scaleValue * mPixelRangeMax / 100
        return if (index == 0) {
            val pxThumb = scaleValue * mThumbWidth / 100
            px - pxThumb
        } else {
            val pxThumb = (100 - scaleValue) * mThumbWidth / 100
            px + pxThumb
        }
    }

    private fun calculateThumbValue(index: Int) {
        thumbs?.let {
            if (index < it.size && it.isNotEmpty()) {
                val th = it[index]
                th.value = pixelToScale(index, th.pos)
                onSeek(this, index, th.value)
            }
        }
    }

    private fun calculateThumbPos(index: Int) {
        thumbs?.let {
            if (index < it.size && it.isNotEmpty()) {
                val th = it[index]
                th.pos = scaleToPixel(index, th.value)
            }
        }
    }

    private fun getThumbValue(index: Int): Float? {
        return thumbs?.get(index)?.value
    }

    fun setThumbValue(index: Int, value: Float) {
        thumbs?.let {
            it[index].value  = value
        }
        calculateThumbPos(index)
        invalidate()
    }

    private fun setThumbPos(index: Int, pos: Float) {
        thumbs?.let {
            it[index].pos = pos
        }
        calculateThumbValue(index)
        invalidate()
    }

    private fun getClosestThumb(coordinate: Float): Int {
        var closest = -1
        thumbs?.let {
            if (it.isNotEmpty()) {
                for (i in it.indices) {
                    val tcoordinate = it[i].pos + mThumbWidth
                    if (coordinate >= it[i].pos && coordinate <= tcoordinate) {
                        closest = it[i].index
                    }
                }
            }
        }
        return closest
    }

    private fun drawShadow(canvas: Canvas) {
        thumbs?.let { listOfThumbs->
            if (listOfThumbs.isNotEmpty()) {
                for (th in listOfThumbs) {
                    if (th.index == 0) {
                        val x = th.pos + paddingLeft
                        if (x > mPixelRangeMin) {
                            val mRect = Rect(mThumbWidth.toInt(), 0, (x + mThumbWidth).toInt(), mHeightTimeLine)
                            canvas.drawRect(mRect, mShadow)
                        }
                    } else {
                        val x = th.pos - paddingRight
                        if (x < mPixelRangeMax) {
                            val mRect = Rect(x.toInt(), 0, (mViewWidth - mThumbWidth).toInt(), mHeightTimeLine)
                            canvas.drawRect(mRect, mShadow)
                        }
                    }
                }
            }
        }
    }

    private fun drawThumbs(canvas: Canvas) {
        thumbs?.let { listOfThumbs ->
            if (listOfThumbs.isNotEmpty()) {
                for (th in listOfThumbs) {
                    if (th.index == 0) {
                        th.bitmap?.let { thumb ->
                            canvas.drawBitmap(thumb, th.pos + paddingLeft, 0f, null)
                        }
                    } else {
                        th.bitmap?.let { thumb ->
                            canvas.drawBitmap(thumb, th.pos - paddingRight, 0f, null)
                        }
                    }
                }
            }
        }
    }

    fun addOnRangeSeekBarListener(listener: OnRangeSeekBarListener) {
        if (mListeners == null) mListeners = ArrayList()
        mListeners?.add(listener)
    }

    private fun onCreate(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
        mListeners?.let { listener ->
            for (item in listener) {
                item.onCreate(rangeSeekBarView, index, value)
            }
        }
    }

    private fun onSeek(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
        mListeners?.let { listener ->
            for (item in listener) {
                item.onSeek(rangeSeekBarView, index, value)
            }
        }
    }

    private fun onSeekStart(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
        mListeners?.let { listener ->
            for (item in listener) {
                item.onSeekStart(rangeSeekBarView, index, value)
            }
        }
    }

    private fun onSeekStop(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
        mListeners?.let { listener ->
            for (item in listener) {
                item.onSeekStop(rangeSeekBarView, index, value)
            }
        }
    }
}
