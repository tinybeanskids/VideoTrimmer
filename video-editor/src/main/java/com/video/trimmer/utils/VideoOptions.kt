package com.video.trimmer.utils

import android.net.Uri
import android.util.Log
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS
import com.arthenica.mobileffmpeg.FFmpeg
import com.video.trimmer.interfaces.OnTrimVideoListener


class VideoOptions {
    companion object {
        const val TAG = "VideoOptions"
    }

    fun trimVideo(startPosition: String, endPosition: String, inputPath: String, outputPath: String, outputFileUri: Uri, listener: OnTrimVideoListener?) {
        // compress: arrayOf("-i", inputPath, "-vf", "scale=$width:$height", outputPath) //iw:ih
        // crop: arrayOf("-i", inputPath, "-filter:v", "crop=$width:$height:$x:$y", "-threads", "5", "-preset", "ultrafast", "-strict", "-2", "-c:a", "copy", outputPath)
        val rc = FFmpeg.execute("-y -i $inputPath -ss $startPosition -to $endPosition -c copy $outputPath")

        if (rc == RETURN_CODE_SUCCESS) {
            listener?.getResult(outputFileUri)
            Log.i(Config.TAG, "Command execution completed successfully.");
        } else if (rc == RETURN_CODE_CANCEL) {
            listener?.onError("canceled")
            Log.i(Config.TAG, "Command execution cancelled by user.");
        } else {
            listener?.onError("error")
            Log.i(Config.TAG, String.format("Command execution failed with rc=%d and the output below.", rc));
            Config.printLastCommandOutput(Log.INFO)
        }
    }
}