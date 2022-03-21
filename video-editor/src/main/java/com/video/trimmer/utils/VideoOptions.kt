package com.video.trimmer.utils

import android.util.Log
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.arthenica.mobileffmpeg.FFprobe
import com.google.gson.Gson
import com.video.trimmer.interfaces.OnTrimVideoListener
import com.video.trimmer.interfaces.OnVideoListener
import com.video.trimmer.model.Stream
import com.video.trimmer.model.VideoProperties
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import java.io.File


class VideoOptions {


    fun trimVideoFromMemory(
        startPosition: String,
        endPosition: String,
        inputPath: String,
        tempPath: String,
        outputPath: String?,
        file: File?,
        listener: OnTrimVideoListener?,
        maxSize: Int,
        fps: Int
    ) {
        try {
            var resultCode: Int = if (fps > 118) {
                FFmpeg.execute("-y -i $inputPath -filter_complex \"[0:v]setpts=3.3*PTS[v];[0:a]atempo=0.55,atempo=0.6,asetrate=44100*1.25,aformat=sample_rates=44100[a]\" -map \"[v]\" -map \"[a]\" -r 30 $tempPath")
                FFmpeg.execute("-y -noaccurate_seek -ss $startPosition -to $endPosition -i $tempPath -c copy $outputPath -avoid_negative_ts make_zero")
            } else {
                FFmpeg.execute("-y -noaccurate_seek -ss $startPosition -to $endPosition -i $inputPath -c copy $outputPath -avoid_negative_ts make_zero")
            }

            deleteFiles(inputPath, tempPath)

            file?.let {
                if (maxSize != -1 && it.length() / 1024 > maxSize) {
                    resultCode = 1
                }
            }

            when (resultCode) {
                TrimmerStatusCode.SUCCESS.value -> {
                    file?.let { listener?.getResult(it) }
                }
                TrimmerStatusCode.FAILED.value -> {
                    listener?.onError("Command execution cancelled by user.")
                    outputPath?.let { deleteFiles(it) }
                }
                TrimmerStatusCode.LIMIT_REACHED.value -> {
                    listener?.onError("File size is greater then ${maxSize / 1000} MB")
                    outputPath?.let { deleteFiles(it) }
                }
                else -> {
                    listener?.onError(String.format("Command execution failed with rc=%d and the output below.", resultCode))
                    Config.printLastCommandOutput(Log.INFO)
                    outputPath?.let { deleteFiles(it) }
                }
            }
        } catch (e: Exception) {
            listener?.onError(e.localizedMessage ?: "generic error")
            outputPath?.let { deleteFiles(inputPath, it) }
        }
    }

    fun getFrameRateFromMediaStreams(streams: List<Stream>): Int{
        for (stream in streams){
            if(stream.codec_type=="video"){
                return stream.avg_frame_rate.substringBefore("/").toInt() / stream.avg_frame_rate.substringAfter("/")
                    .toInt()
            }
        }
        return 30
    }

    fun encodeSlowMotionVideo(onVideoListener: OnVideoListener?, inputCopy: File, temp_file: File): Observable<Pair<String, Int>> {
        onVideoListener?.onFFmpegStarted()
        var slowMotion = false
        return Observable.fromCallable { inputCopy }
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                val mediaInformation = FFprobe.getMediaInformation(inputCopy.path)
                val mediaInformationModel = Gson().fromJson(mediaInformation.allProperties.toString(), VideoProperties::class.java)
                val mediaStreams = mediaInformationModel.streams
                getFrameRateFromMediaStreams(mediaStreams)
            }.map {
                val fps = it
                if (fps > 118) {
                    slowMotion=true
                    try {
                        val ratioVideo = ((fps / 30) * 0.83).toFloat()
                        val ratioAudio = ((1 / ratioVideo) * 2.0).toFloat()
                        Pair(
                            FFmpeg.execute("-y -i ${inputCopy.path} -filter_complex \"[0:v]setpts=$ratioVideo*PTS[v];[0:a]atempo=0.55,atempo=$ratioAudio[a]\" -preset ultrafast -preset ultrafast -crf 28 -map \"[v]\" -map \"[a]\" -r 30 ${temp_file.path}"),
                            fps
                        )
                    } catch (e: java.lang.Exception) {
                        Pair(-1, fps)
                    }
                } else {
                    Pair(-1, fps)
                }
            }.map {
                if (it.first == Config.RETURN_CODE_SUCCESS) {
                    Pair(temp_file.path, it.second)
                } else {
                    Pair(inputCopy.path, it.second)
                }
            }.doOnComplete {
                if(slowMotion)
                    deleteFiles(inputCopy.path)
                else
                    deleteFiles(temp_file.path)

            }

    }

    fun deleteFiles(vararg paths: String) {
        for (path in paths) {
            val input = File(path)
            if (input.exists())
                input.delete()
        }
    }
}