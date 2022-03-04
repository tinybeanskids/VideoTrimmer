package com.video.trimmer.model

data class VideoProperties(
    val format: Format,
    val streams: List<Stream>
)