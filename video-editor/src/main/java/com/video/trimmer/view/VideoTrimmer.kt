package com.video.trimmer.view

import android.annotation.SuppressLint
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
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import androidx.media3.datasource.DefaultDataSourceFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.TrackSelector
import com.video.trimmer.R
import com.video.trimmer.interfaces.OnProgressVideoListener
import com.video.trimmer.interfaces.OnRangeSeekBarListener
import com.video.trimmer.interfaces.OnTrimVideoListener
import com.video.trimmer.interfaces.OnVideoListener
import com.video.trimmer.utils.RealPathUtil
import com.video.trimmer.utils.TrimVideoUtils
import com.video.trimmer.utils.VideoOptions
import com.video.trimmer.utils.copyInputStreamToFile
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.view_trimmer.view.*
import java.io.File
import java.io.FileNotFoundException
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.concurrent.TimeUnit


@SuppressLint("UnsafeOptInUsageError")
class VideoTrimmer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = 0
) :
    FrameLayout(context, attrs, defStyleAttr) {

    private var player: ExoPlayer? = null
    private var firstTimeLoad = true

    private var videoSource: Uri? = null
    private var slowVideoSource: Uri? = null
    private var finalPath: String? = null

    private var maxDuration: Long = -1
    private var minDuration: Long = -1
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

    private val compositeDisposable = CompositeDisposable()

    var fps = 29
    var paths: MutableList<String>? = null

    private var destinationFile: File? = null
    private var destinationPath: String
        get() {
            if (finalPath == null) {
                val folder = Environment.getExternalStorageDirectory()
                finalPath = folder.path + File.separator
            }
            return finalPath ?: ""
        }
        set(finalVideoPath) {
            finalPath = finalVideoPath
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


        player?.addListener(object : Player.Listener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                super.onPlayWhenReadyChanged(playWhenReady, playbackState)

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
            if (player?.isPlaying == true)
                pauseVideo()
            else playVideo()
        }

        video_loader.videoSurfaceView?.setOnClickListener {
            if (player?.isPlaying == true)
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
            override fun onSeek(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
                handlerTop.visibility = View.GONE
                onSeekThumbs(index, value)
            }

            override fun onCreate(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
                onSeekThumbs(index, value)
            }

            override fun onSeekStart(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
                onSeekThumbs(index, value)
            }

            override fun onSeekStop(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
                onStopSeekThumbs()
            }
        })
    }

    private fun initializePlayer() {
        val trackSelector: TrackSelector = DefaultTrackSelector(context, AdaptiveTrackSelection.Factory())
        player = ExoPlayer.Builder(context).setLooper(context.mainLooper).setTrackSelector(trackSelector).build()
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
        player?.playWhenReady = false
        icon_video_play.visibility = View.VISIBLE
        notifyProgressUpdate(false)
    }

    private fun onPlayerIndicatorSeekStop(seekBar: SeekBar) {
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        player?.playWhenReady = false
        icon_video_play.visibility = View.VISIBLE

        val duration = (mDuration * seekBar.progress / 1000L).toInt()
        player?.seekTo(duration.toLong())
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
        player?.playWhenReady = false
        Observable.fromCallable {
            val mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(context, slowVideoSource)
            val metaDataKeyDuration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()

            if (mTimeVideo < MIN_TIME_FRAME) {
                if (metaDataKeyDuration != null) {
                    if (metaDataKeyDuration - mEndPosition > MIN_TIME_FRAME - mTimeVideo) mEndPosition += MIN_TIME_FRAME - mTimeVideo
                    else if (mStartPosition > MIN_TIME_FRAME - mTimeVideo) mStartPosition -= MIN_TIME_FRAME - mTimeVideo
                }
            }

            val outputFileUri = Uri.fromFile(destinationFile)
            val outPutPath = RealPathUtil.realPathFromUriApi19(context, outputFileUri) ?: destinationFile?.absolutePath
            slowVideoSource?.let { mOnTrimVideoListener?.onInfo("SOURCE ${safUriToFFmpegPath(it)}") }
            mOnTrimVideoListener?.onInfo("DESTINATION $outPutPath")
            val extractor = MediaExtractor()
            var frameRate = 24
            try {
                slowVideoSource?.let { extractor.setDataSource(context, it, null) }
                val numTracks = extractor.trackCount
                for (i in 0..numTracks - 1) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME)
                    if (mime?.startsWith("video/") == true) {
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

            val duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
            mOnTrimVideoListener?.onInfo("FRAME RATE $frameRate")
            duration?.let { mOnTrimVideoListener?.onInfo("FRAME COUNT ${(it / 1000 * frameRate)}") }

            val inputCopy = File.createTempFile("temp-video-input", ".mp4", context.cacheDir)
            val tempCopy = File.createTempFile("temp-video-recode", ".mp4", context.cacheDir)

            videoSource?.let { context.contentResolver.openInputStream(it)?.apply { inputCopy.copyInputStreamToFile(this) } }

            VideoOptions().trimVideoFromMemory(
                TrimVideoUtils.stringForTime(mStartPosition),
                TrimVideoUtils.stringForTime(mEndPosition),
                inputCopy.path,
                tempCopy.path,
                outPutPath,
                destinationFile,
                mOnTrimVideoListener,
                mMaxSize,
                fps
            )
        }
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.computation())
            .doOnError { mOnVideoListener?.onFFmpegError(it) }
            .subscribe()
            .addTo(compositeDisposable)
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
        player?.playWhenReady = false
    }

    private fun playVideo() {
        icon_video_play.visibility = View.GONE
        if (mResetSeekBar) {
            mResetSeekBar = false
            player?.seekTo(mStartPosition.toLong())
        }
        mMessageHandler.sendEmptyMessage(SHOW_PROGRESS)
        player?.playWhenReady = true
    }

    fun onCancelClicked() {
        player?.stop()
        mOnTrimVideoListener?.cancelAction()
    }

    private fun onVideoPrepared() {
        player?.let {
            mDuration = it.duration.toFloat()
        }
        setSeekBarPosition()
        setTimeFrames()
    }

    private fun setSeekBarPosition() {
        when {
            mDuration >= maxDuration && maxDuration != -1L -> {
                mStartPosition = mDuration / 2f - maxDuration / 2f
                mEndPosition = mDuration / 2f + maxDuration / 2f
                timeLineBar.setThumbValue(0, (mStartPosition * 100 / mDuration))
                timeLineBar.setThumbValue(1, (mEndPosition * 100 / mDuration))
            }
            mDuration <= minDuration && minDuration != -1L -> {
                mStartPosition = mDuration / 2f - minDuration / 2f
                mEndPosition = mDuration / 2f + minDuration / 2f
                timeLineBar.setThumbValue(0, (mStartPosition * 100 / mDuration))
                timeLineBar.setThumbValue(1, (mEndPosition * 100 / mDuration))
            }
            else -> {
                mStartPosition = 0f
                mEndPosition = mDuration
            }
        }
        player?.seekTo(mStartPosition.toLong())
        mTimeVideo = mDuration
        timeLineBar.initMaxWidth()
    }

    private fun setTimeFrames() {
        val seconds = context.getString(R.string.short_seconds)
        textTimeSelection.text = String.format(
            "%s %s - %s %s",
            TrimVideoUtils.stringForPreviewTime(mStartPosition),
            seconds,
            TrimVideoUtils.stringForPreviewTime(mEndPosition),
            seconds
        )
    }

    private fun onSeekThumbs(index: Int, value: Float) {
        when (index) {
            Thumb.LEFT -> {
                mStartPosition = (mDuration * value / 100L)
                if (maxDuration != -1L && mEndPosition - mStartPosition > maxDuration) {
                    val offset = mEndPosition - mStartPosition - maxDuration
                    mEndPosition -= offset
                }
                player?.seekTo(mStartPosition.toLong())
            }
            Thumb.RIGHT -> {
                mEndPosition = (mDuration * value / 100L)
                if (maxDuration != -1L && mEndPosition - mStartPosition > maxDuration) {
                    val offset = mEndPosition - mStartPosition - maxDuration
                    mStartPosition += offset
                }
            }
        }
        setTimeFrames()
        mTimeVideo = mEndPosition - mStartPosition
    }

    private fun onStopSeekThumbs() {
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        player?.playWhenReady = false
        icon_video_play.visibility = View.VISIBLE
    }

    private fun onVideoCompleted() {
        player?.seekTo(mStartPosition.toLong())
    }

    private fun notifyProgressUpdate(all: Boolean) {
        if (mDuration == 0f) return
        val position = player?.currentPosition
        if (all) {
            for (item in mListeners) {
                position?.let { item.updateProgress(it.toFloat(), mDuration, (it * 100 / mDuration)) }
            }
        } else {
            if (mListeners.size > 0) {
                position?.let {
                    mListeners[0].updateProgress(
                        it.toFloat(),
                        mDuration,
                        (it * 100 / mDuration)
                    )
                }
            }
        }
    }

    private fun updateVideoProgress(time: Float) {
        if (video_loader == null) return
        if (time <= mStartPosition && time <= mEndPosition) handlerTop.visibility = View.GONE
        else handlerTop.visibility = View.VISIBLE
        if (time >= mEndPosition) {
            mMessageHandler.removeMessages(SHOW_PROGRESS)
            player?.playWhenReady = false
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
        compositeDisposable.clear()
    }

    fun setMaxDuration(duration: Long, timeUnit: TimeUnit): VideoTrimmer {
        maxDuration = TimeUnit.MILLISECONDS.convert(duration, timeUnit)
        return this
    }

    fun setDestinationFile(file: File): VideoTrimmer {
        destinationFile = file
        return this
    }

    fun encodeSlowMotion(videoURI: Uri) {
        videoSource = videoURI
        val temporalFrameDropVideoFile = File.createTempFile("framerate_drop", ".mp4", context.cacheDir)
        val inputVideoFile = File.createTempFile("temp-video-input", ".mp4", context.cacheDir)
        try {
            videoSource?.let {
                context.contentResolver.openInputStream(it)?.apply {
                    inputVideoFile.copyInputStreamToFile(this)
                }
            }
        } catch (e: java.lang.Exception) {
            Toast.makeText(context, e.message.toString(), Toast.LENGTH_LONG).show()
            VideoOptions().deleteFiles(temporalFrameDropVideoFile.path, inputVideoFile.path)
            mOnVideoListener?.onFFmpegError(e)
            return
        }
        videoSource?.let { timeLineView.setVideo(it) }
        VideoOptions().encodeSlowMotionVideo(mOnVideoListener, inputVideoFile, temporalFrameDropVideoFile)
            .doOnError {
                mOnVideoListener?.onFFmpegError(it)
            }.subscribe {
                mOnVideoListener?.onFFmpegFinished(it.first)
                slowVideoSource = Uri.parse(it.first)
                fps = it.second
            }.addTo(compositeDisposable)
    }

    fun setVideoPath(path: String): VideoTrimmer {
        slowVideoSource = Uri.parse(path)

        val dataSourceFactory = DefaultDataSourceFactory(context, Util.getUserAgent(context, context.applicationInfo.name))

        slowVideoSource?.let {
            val mediaItem = MediaItem.fromUri(it)
            val mediaSource = ProgressiveMediaSource
                .Factory(dataSourceFactory)
                .createMediaSource(mediaItem)

            player?.setMediaSource(mediaSource)
            player?.prepare()
            video_loader.requestFocus()
            timeLineView.setVideo(it)
        }
        return this
    }

    fun setTextTimeSelectionTypeface(tf: Typeface?): VideoTrimmer {
        if (tf != null) textTimeSelection.typeface = tf
        return this
    }

    fun releasePlayer() {
        player?.stop()
        player?.release()
    }

    private class MessageHandler(view: VideoTrimmer) : Handler() {
        private val mView: WeakReference<VideoTrimmer> = WeakReference(view)
        override fun handleMessage(msg: Message) {
            val view = mView.get()
            if (view == null || view.video_loader == null) return
            view.notifyProgressUpdate(true)
            if (view.video_loader.player != null && view.video_loader.player?.playbackState == Player.STATE_READY
                && view.video_loader.player?.playWhenReady == true
            )
                sendEmptyMessageDelayed(0, 10)
        }
    }

    companion object {
        private const val MIN_TIME_FRAME = 1000
        private const val SHOW_PROGRESS = 2
    }

}
