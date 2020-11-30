package com.video.trimmer.interfaces

import java.io.File

interface OnTrimVideoListener {
    fun getResult(file: File)
    fun cancelAction()
    fun onError(message: String)
}
