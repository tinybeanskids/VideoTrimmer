package com.video.sample

import android.content.Context
import android.os.Handler
import android.os.Looper

class RunOnUiThread(var context: Context?) {
    fun safely(dothis: () -> Unit) {
        if (context != null) {
            if (ContextHelper.mainThread == Thread.currentThread()) {
                f(dothis)
            } else {
                ContextHelper.handler.post { f(dothis) }
            }
        }
    }

    private fun f(dothis: () -> Unit) {
        try {
            dothis.invoke()
        } catch (e: Exception) {
            context?.let {
                e.printStackTrace()
            }
        }
    }
}

private object ContextHelper {
    val handler = Handler(Looper.getMainLooper())
    val mainThread: Thread = Looper.getMainLooper().thread
}