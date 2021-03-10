package com.video.trimmer.utils

import android.util.Log
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.video.trimmer.interfaces.OnTrimVideoListener
import java.io.File


class VideoOptions {

    fun trimVideoFromMemory(startPosition: String, endPosition: String, inputPath: String, outputPath: String, file: File, listener: OnTrimVideoListener?, maxSize: Int) {
        // compress: arrayOf("-i", inputPath, "-vf", "scale=$width:$height", outputPath) //iw:ih
        // crop: arrayOf("-i", inputPath, "-filter:v", "crop=$width:$height:$x:$y", "-threads", "5", "-preset", "ultrafast", "-strict", "-2", "-c:a", "copy", outputPath)

        try {
            var rc = FFmpeg.execute("-y -noaccurate_seek -ss $startPosition -i \"$inputPath\" -to $endPosition -c copy $outputPath -avoid_negative_ts make_zero")
            deleteFiles(inputPath)


            if (maxSize != -1 && file.length() / 1024 > maxSize) {
                rc = 1
            }

            when (rc) {
                TrimmerStatusCode.SUCCESS.value -> {
                    listener?.getResult(file)
                }
                TrimmerStatusCode.FAILED.value -> {
                    listener?.onError("Command execution cancelled by user.")
                    deleteFiles(outputPath)
                }
                TrimmerStatusCode.LIMIT_REACHED.value -> {
                    listener?.onError("File size is greater then ${maxSize/1000} MB")
                    deleteFiles(outputPath)
                }
                else -> {
                    listener?.onError(String.format("Command execution failed with rc=%d and the output below.", rc))
                    Config.printLastCommandOutput(Log.INFO)
                    deleteFiles(outputPath)
                }
            }
        } catch (e: Exception) {
            listener?.onError(e.localizedMessage ?: "generic error")
            deleteFiles(inputPath, outputPath)
        }
    }

    private fun deleteFiles(vararg paths: String) {
        for (path in paths) {
            val input = File(path)
            if (input.exists())
                input.delete()
        }
    }
}