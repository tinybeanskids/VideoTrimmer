package com.video.trimmer.utils

import android.annotation.SuppressLint
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

@SuppressLint("UnsafeOptInUsageError")
fun ExoPlayer.isPlaying() = this.playbackState == Player.STATE_READY && this.playWhenReady