package com.video.trimmer.interfaces

interface OnVideoLoadListener {
    fun onVideoLoadStarted()
    fun onVideoLoadFinished(path: String)
    fun onVideoLoadError(throwable: Throwable)
}