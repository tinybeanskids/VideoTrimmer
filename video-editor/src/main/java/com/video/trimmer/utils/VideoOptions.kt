package com.video.trimmer.utils

import android.util.Log
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS
import com.arthenica.mobileffmpeg.FFmpeg
import com.video.trimmer.interfaces.OnTrimVideoListener
import java.io.File


class VideoOptions {


    fun trimVideo(startPosition: String, endPosition: String, inputPath: String, outputPath: String, file: File, listener: OnTrimVideoListener?) {
        // compress: arrayOf("-i", inputPath, "-vf", "scale=$width:$height", outputPath) //iw:ih
        // crop: arrayOf("-i", inputPath, "-filter:v", "crop=$width:$height:$x:$y", "-threads", "5", "-preset", "ultrafast", "-strict", "-2", "-c:a", "copy", outputPath)
        try {
            val rc = FFmpeg.execute("-y -i \"$inputPath\" -ss $startPosition -to $endPosition -c copy $outputPath")

            when (rc) {
                RETURN_CODE_SUCCESS -> {
                    listener?.getResult(file)
                }
                RETURN_CODE_CANCEL -> {
                    listener?.onError("Command execution cancelled by user.")
                }
                else -> {
                    listener?.onError(String.format("Command execution failed with rc=%d and the output below.", rc))
                    Config.printLastCommandOutput(Log.INFO)
                }
            }
        } catch (e: Exception) {
            listener?.onError(e.localizedMessage ?: "generic error")
        }
    }

    fun trimVideoFromMemory(startPosition: String, endPosition: String, inputPath: String, outputPath: String, file: File, listener: OnTrimVideoListener?) {
        // compress: arrayOf("-i", inputPath, "-vf", "scale=$width:$height", outputPath) //iw:ih
        // crop: arrayOf("-i", inputPath, "-filter:v", "crop=$width:$height:$x:$y", "-threads", "5", "-preset", "ultrafast", "-strict", "-2", "-c:a", "copy", outputPath)
        try {
            Log.e("START POSITION", startPosition)
            Log.e("END POSITION", endPosition)
            Log.e("INPUT PATH", inputPath)
            Log.e("OUTPUT PATH", outputPath)
            val rc = FFmpeg.execute("-y -noaccurate_seek -ss $startPosition -i \"$inputPath\" -to $endPosition -c copy $outputPath -avoid_negative_ts make_zero")

            when (rc) {
                RETURN_CODE_SUCCESS -> {
                    listener?.getResult(file)
                }
                RETURN_CODE_CANCEL -> {
                    listener?.onError("Command execution cancelled by user.")
                }
                else -> {
                    listener?.onError(String.format("Command execution failed with rc=%d and the output below.", rc))
                    Config.printLastCommandOutput(Log.INFO)
                }
            }
        } catch (e: Exception) {
            listener?.onError(e.localizedMessage ?: "generic error")
        }
    }
}