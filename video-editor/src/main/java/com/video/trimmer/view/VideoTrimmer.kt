package com.video.trimmer.view

import android.content.Context
import android.graphics.Typeface
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.os.ParcelFileDescriptor
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.SeekBar
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.video.trimmer.R
import com.video.trimmer.interfaces.OnProgressVideoListener
import com.video.trimmer.interfaces.OnRangeSeekBarListener
import com.video.trimmer.interfaces.OnTrimVideoListener
import com.video.trimmer.interfaces.OnVideoListener
import com.video.trimmer.utils.*
import kotlinx.android.synthetic.main.view_trimmer.view.*
import java.io.File
import java.io.FileNotFoundException
import java.lang.ref.WeakReference
import java.util.*

class VideoTrimmer @JvmOverloads constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var player: SimpleExoPlayer
    private var firstTimeLoad = true

    private lateinit var mSrc: Uri
    private var mFinalPath: String? = null

    private var mMaxDuration: Int = -1
    private var mMinDuration: Int = -1
    private var mListeners: ArrayList<OnProgressVideoListener> = ArrayList()

    private var mOnTrimVideoListener: OnTrimVideoListener? = null
    private var mOnVideoListener: OnVideoListener? = null

    private var mDuration = 0f
    private var mTimeVideo = 0f
    private var mStartPosition = 0f

    private var mEndPosition = 0f
    private var mResetSeekBar = true
    private val mMessageHandler = MessageHandler(this)

    private var mMaxSize = -1

    private lateinit var destinationFile: File
    private var destinationPath: String
        get() {
            if (mFinalPath == null) {
                val folder = Environment.getExternalStorageDirectory()
                mFinalPath = folder.path + File.separator
            }
            return mFinalPath ?: ""
        }
        set(finalPath) {
            mFinalPath = finalPath
        }

    init {
        init(context)
    }

    private fun init(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.view_trimmer, this, true)
        initializePlayer()
        setUpListeners()
        setUpMargins()
    }

    private fun setUpListeners() {
        mListeners = ArrayList()
        mListeners.add(object : OnProgressVideoListener {
            override fun updateProgress(time: Float, max: Float, scale: Float) {
                updateVideoProgress(time)
            }
        })


        player.addListener(object : Player.EventListener {

            override fun onPlayerError(error: ExoPlaybackException) {
                super.onPlayerError(error)
                mOnTrimVideoListener?.onError("Something went wrong reason : $error")
            }

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                super.onPlayerStateChanged(playWhenReady, playbackState)

                if (playbackState == Player.STATE_READY && firstTimeLoad) {
                    onVideoPrepared()
                    firstTimeLoad = false
                    playVideo()
                } else if (playbackState == Player.STATE_ENDED) {
                    onVideoCompleted()
                }

            }

        })

        icon_video_play.setOnClickListener {
            if (player.isPlaying())
                pauseVideo()
            else playVideo()
        }

        video_loader.videoSurfaceView?.setOnClickListener {
            if (player.isPlaying())
                pauseVideo()
        }


        handlerTop.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                onPlayerIndicatorSeekChanged(progress, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                onPlayerIndicatorSeekStart()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                onPlayerIndicatorSeekStop(seekBar)
            }
        })

        timeLineBar.addOnRangeSeekBarListener(object : OnRangeSeekBarListener {
            override fun onCreate(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
            }

            override fun onSeek(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
                handlerTop.visibility = View.GONE
                onSeekThumbs(index, value)
            }

            override fun onSeekStart(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
            }

            override fun onSeekStop(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
                onStopSeekThumbs()
            }
        })
    }

    private fun initializePlayer() {
        val trackSelector: TrackSelector =
                DefaultTrackSelector(AdaptiveTrackSelection.Factory())

        player = ExoPlayerFactory.newSimpleInstance(context, trackSelector)
        video_loader.player = player
    }

    private fun onPlayerIndicatorSeekChanged(progress: Int, fromUser: Boolean) {
        val duration = (mDuration * progress / 1000L)
        if (fromUser) {
            if (duration < mStartPosition) setProgressBarPosition(mStartPosition)
            else if (duration > mEndPosition) setProgressBarPosition(mEndPosition)
        }
    }

    private fun onPlayerIndicatorSeekStart() {
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        player.playWhenReady = false
        icon_video_play.visibility = View.VISIBLE
        notifyProgressUpdate(false)
    }

    private fun onPlayerIndicatorSeekStop(seekBar: SeekBar) {
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        player.playWhenReady = false
        icon_video_play.visibility = View.VISIBLE

        val duration = (mDuration * seekBar.progress / 1000L).toInt()
        player.seekTo(duration.toLong())
        notifyProgressUpdate(false)
    }

    private fun setProgressBarPosition(position: Float) {
        if (mDuration > 0) handlerTop.progress = (1000L * position / mDuration).toInt()
    }

    private fun setUpMargins() {
        val marge = timeLineBar.thumbs[0].widthBitmap
        val lp = timeLineView.layoutParams as LayoutParams
        lp.setMargins(marge, 0, marge, 0)
        timeLineView.layoutParams = lp
    }

    fun onSaveClicked() {
        icon_video_play.visibility = View.VISIBLE
        BackgroundExecutor.execute(object : BackgroundExecutor.Task("", 0L, "") {
            override fun execute() {
                try {
                    player.playWhenReady = false
                    val mediaMetadataRetriever = MediaMetadataRetriever()
                    mediaMetadataRetriever.setDataSource(context, mSrc)
                    val metaDataKeyDuration = java.lang.Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))

                    if (mTimeVideo < MIN_TIME_FRAME) {
                        if (metaDataKeyDuration - mEndPosition > MIN_TIME_FRAME - mTimeVideo) mEndPosition += MIN_TIME_FRAME - mTimeVideo
                        else if (mStartPosition > MIN_TIME_FRAME - mTimeVideo) mStartPosition -= MIN_TIME_FRAME - mTimeVideo
                    }

                    val outputFileUri = Uri.fromFile(destinationFile)
                    val outPutPath = RealPathUtil.realPathFromUriApi19(context, outputFileUri)
                            ?: destinationFile.absolutePath
                    mOnTrimVideoListener?.onInfo("SOURCE ${safUriToFFmpegPath(mSrc)}")
                    mOnTrimVideoListener?.onInfo("DESTINATION $outPutPath")
                    val extractor = MediaExtractor()
                    var frameRate = 24
                    try {
                        extractor.setDataSource(context, mSrc, null)
                        val numTracks = extractor.trackCount
                        for (i in 0..numTracks - 1) {
                            val format = extractor.getTrackFormat(i)
                            val mime = format.getString(MediaFormat.KEY_MIME)
                            if (mime!!.startsWith("video/")) {
                                if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                                    frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        extractor.release()
                    }

                    val duration = java.lang.Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))
                    mOnTrimVideoListener?.onInfo("FRAME RATE $frameRate")
                    mOnTrimVideoListener?.onInfo("FRAME COUNT ${(duration / 1000 * frameRate)}")

                    //copy file to memory
                    val inputCopy = File.createTempFile("temp-video-input", ".mp4", context.cacheDir)
                    context.contentResolver.openInputStream(mSrc)?.apply {
                        //save to cache
                        inputCopy.copyInputStreamToFile(this)
                    }
                    //trim

                    VideoOptions().trimVideoFromMemory(
                            TrimVideoUtils.stringForTime(mStartPosition),
                            TrimVideoUtils.stringForTime(mEndPosition),
                            inputCopy.path,
//                safUriToFFmpegPath(mSrc), //todo used for android 11 with pipe protocol
                            outPutPath,
                            destinationFile,
                            mOnTrimVideoListener,
                            mMaxSize)
                } catch (e: Throwable) {
                    Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e)
                }
            }

        })


        //remove original copy from cache
    }

    private fun safUriToFFmpegPath(uri: Uri): String {
        return try {
            val parcelFileDescriptor: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, "r")
            java.lang.String.format(Locale.getDefault(), "pipe:%d", parcelFileDescriptor?.fd)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            "null"
        }
    }

    private fun pauseVideo() {
        icon_video_play.visibility = View.VISIBLE
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        player.playWhenReady = false
    }

    private fun playVideo() {
        icon_video_play.visibility = View.GONE
        if (mResetSeekBar) {
            mResetSeekBar = false
            player.seekTo(mStartPosition.toLong())
        }
        mMessageHandler.sendEmptyMessage(SHOW_PROGRESS)
        player.playWhenReady = true
    }

    fun onCancelClicked() {
        //video_loader.stopPlayback()
        //mOnTrimVideoListener?.cancelAction()
    }

    private fun onVideoPrepared() {
        //val videoWidth = mp.videoWidth
        //val videoHeight = mp.videoHeight
        //val videoProportion = videoWidth.toFloat() / videoHeight.toFloat()
//        val screenWidth = layout_surface_view.width
//        val screenHeight = layout_surface_view.height
//        val screenProportion = screenWidth.toFloat() / screenHeight.toFloat()
//        val lp = video_loader.layoutParams
//
//        if (videoProportion > screenProportion) {
//            lp.width = screenWidth
//            lp.height = (screenWidth.toFloat() / videoProportion).toInt()
//        } else {
//            lp.width = (videoProportion * screenHeight.toFloat()).toInt()
//            lp.height = screenHeight
//        }
//        video_loader.layoutParams = lp

        mDuration = player.duration.toFloat()
        setSeekBarPosition()
        setTimeFrames()
    }

    private fun setSeekBarPosition() {
        when {
            mDuration >= mMaxDuration && mMaxDuration != -1 -> {
                mStartPosition = mDuration / 2 - mMaxDuration / 2
                mEndPosition = mDuration / 2 + mMaxDuration / 2
                timeLineBar.setThumbValue(0, (mStartPosition * 100 / mDuration))
                timeLineBar.setThumbValue(1, (mEndPosition * 100 / mDuration))
            }
            mDuration <= mMinDuration && mMinDuration != -1 -> {
                mStartPosition = mDuration / 2 - mMinDuration / 2
                mEndPosition = mDuration / 2 + mMinDuration / 2
                timeLineBar.setThumbValue(0, (mStartPosition * 100 / mDuration))
                timeLineBar.setThumbValue(1, (mEndPosition * 100 / mDuration))
            }
            else -> {
                mStartPosition = 0f
                mEndPosition = mDuration
            }
        }
        player.seekTo(mStartPosition.toLong())
        mTimeVideo = mDuration
        timeLineBar.initMaxWidth()
    }

    private fun setTimeFrames() {
        val seconds = context.getString(R.string.short_seconds)
        textTimeSelection.text = String.format("%s %s - %s %s", TrimVideoUtils.stringForPreviewTime(mStartPosition), seconds, TrimVideoUtils.stringForPreviewTime(mEndPosition), seconds)
    }

    private fun onSeekThumbs(index: Int, value: Float) {
        when (index) {
            Thumb.LEFT -> {
                mStartPosition = (mDuration * value / 100L)
                if (mMaxDuration != -1 && mEndPosition - mStartPosition > mMaxDuration) {
                    val offset = mEndPosition - mStartPosition - mMaxDuration
                    mEndPosition -= offset
                }
                player.seekTo(mStartPosition.toLong())
            }
            Thumb.RIGHT -> {
                mEndPosition = (mDuration * value / 100L)
                if (mMaxDuration != -1 && mEndPosition - mStartPosition > mMaxDuration) {
                    val offset = mEndPosition - mStartPosition - mMaxDuration
                    mStartPosition += offset
                }
            }
        }
        setTimeFrames()
        mTimeVideo = mEndPosition - mStartPosition
    }

    private fun onStopSeekThumbs() {
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        player.playWhenReady = false
        icon_video_play.visibility = View.VISIBLE
    }

    private fun onVideoCompleted() {
        player.seekTo(mStartPosition.toLong())
    }

    private fun notifyProgressUpdate(all: Boolean) {
        if (mDuration == 0f) return
        val position = player.currentPosition
        if (all) {
            for (item in mListeners) {
                item.updateProgress(position.toFloat(), mDuration, (position * 100 / mDuration))
            }
        } else {
            mListeners[0].updateProgress(position.toFloat(), mDuration, (position * 100 / mDuration))
        }
    }

    private fun updateVideoProgress(time: Float) {
        if (video_loader == null) return
        if (time <= mStartPosition && time <= mEndPosition) handlerTop.visibility = View.GONE
        else handlerTop.visibility = View.VISIBLE
        if (time >= mEndPosition) {
            mMessageHandler.removeMessages(SHOW_PROGRESS)
            player.playWhenReady = false
            icon_video_play.visibility = View.VISIBLE
            mResetSeekBar = true
            return
        }
        setProgressBarPosition(time)
    }

    fun setVideoInformationVisibility(visible: Boolean): VideoTrimmer {
        timeFrame.visibility = if (visible) View.VISIBLE else View.GONE
        return this
    }

    fun setOnTrimVideoListener(onTrimVideoListener: OnTrimVideoListener): VideoTrimmer {
        mOnTrimVideoListener = onTrimVideoListener
        return this
    }

    fun setOnVideoListener(onVideoListener: OnVideoListener): VideoTrimmer {
        mOnVideoListener = onVideoListener
        return this
    }

    fun destroy() {
        BackgroundExecutor.cancelAll("", true)
        UiThreadExecutor.cancelAll("")
    }

    fun setMaxDuration(maxDuration: Int): VideoTrimmer {
        mMaxDuration = maxDuration * 1000
        return this
    }

    fun setMaxSize(maxSize: Int): VideoTrimmer {
        mMaxSize = maxSize * 1000
        return this
    }

    fun setMinDuration(minDuration: Int): VideoTrimmer {
        mMinDuration = minDuration * 1000
        return this
    }

    fun setDestinationPath(path: String): VideoTrimmer {
        destinationPath = path
        return this
    }

    fun setDestinationFile(file: File): VideoTrimmer {
        destinationFile = file
        return this
    }

    fun setVideoURI(videoURI: Uri): VideoTrimmer {
        mSrc = videoURI
        val dataSourceFactory = DefaultDataSourceFactory(context,
                Util.getUserAgent(context, context.applicationInfo.name))

        val mediaSource = ExtractorMediaSource
                .Factory(dataSourceFactory)
                .createMediaSource(mSrc)

        player.prepare(mediaSource)
        video_loader.requestFocus()
        timeLineView.setVideo(mSrc)
        return this
    }

    fun setTextTimeSelectionTypeface(tf: Typeface?): VideoTrimmer {
        if (tf != null) textTimeSelection.typeface = tf
        return this
    }

    fun releasePlayer() {
        player.stop()
        player.release()
    }


    private class MessageHandler internal constructor(view: VideoTrimmer) : Handler() {
        private val mView: WeakReference<VideoTrimmer> = WeakReference(view)
        override fun handleMessage(msg: Message) {
            val view = mView.get()
            if (view == null || view.video_loader == null) return
            view.notifyProgressUpdate(true)
            if (view.video_loader.player.playbackState == Player.STATE_READY
                    && view.video_loader.player.playWhenReady)
                sendEmptyMessageDelayed(0, 10)
        }
    }

    companion object {
        private const val MIN_TIME_FRAME = 1000
        private const val SHOW_PROGRESS = 2
    }
}
