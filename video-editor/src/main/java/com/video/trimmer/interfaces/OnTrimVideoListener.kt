package com.video.trimmer.interfaces

import android.net.Uri

interface OnTrimVideoListener {
    fun getResult(uri: Uri)
    fun cancelAction()
    fun onError(message: String)
}
