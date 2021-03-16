package com.video.trimmer.utils

import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer

fun SimpleExoPlayer.isPlaying() = this.getPlaybackState() === Player.STATE_READY && this.getPlayWhenReady()