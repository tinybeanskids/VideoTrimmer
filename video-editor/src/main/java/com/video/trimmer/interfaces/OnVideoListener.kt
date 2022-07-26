package com.video.trimmer.interfaces

/**
 * This method is deprecated, please use [OnVideoLoadListener] instead.
 */
@Deprecated("Use OnVideoLoad listener")
interface OnVideoListener {
    fun onFFmpegFinished(path: String)
    fun onFFmpegStarted()
    fun onFFmpegError(throwable: Throwable)
}