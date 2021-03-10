package com.video.trimmer.utils

enum class TrimmerStatusCode (val value: Int) {
    SUCCESS(0), FAILED(255), LIMIT_REACHED(1)
}