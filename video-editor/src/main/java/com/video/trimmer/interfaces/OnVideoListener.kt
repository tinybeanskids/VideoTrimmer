package com.video.trimmer.interfaces

interface OnVideoListener {
    fun onFFmpegFinished(path: String)
    fun onFFmpegStarted()
    fun onFFmpegError(throwable: Throwable)
}