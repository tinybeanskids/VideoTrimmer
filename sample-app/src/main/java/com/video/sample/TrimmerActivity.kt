package com.video.sample

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.video.trimmer.interfaces.OnTrimVideoListener
import com.video.trimmer.interfaces.OnVideoListener
import kotlinx.android.synthetic.main.activity_trimmer.*
import java.io.File
import java.util.concurrent.TimeUnit


class TrimmerActivity : AppCompatActivity(), OnTrimVideoListener, OnVideoListener {

    private val progressDialog: VideoProgressIndeterminateDialog by lazy {
        VideoProgressIndeterminateDialog(
            this,
            "Cropping video. Please wait..."
        )
    }
    private val loadingDialog: VideoProgressIndeterminateDialog by lazy {
        VideoProgressIndeterminateDialog(
            this,
            "Loading slow motion video. Please wait..."
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trimmer)

        setupPermissions {
            val extraIntent = intent
            val path =
                if (extraIntent != null) extraIntent.getStringExtra(MainActivity.EXTRA_VIDEO_PATH)
                    ?: "" else ""
            videoTrimmer.setTextTimeSelectionTypeface(FontsHelper[this, FontsConstants.SEMI_BOLD])
                .setOnTrimVideoListener(this)
                .setOnVideoListener(this)
                .setVideoInformationVisibility(true)
                .setDestinationFile(getDestinationFile())
                .setMaxDuration(3,TimeUnit.MINUTES)
                .encodeSlowMotion(Uri.parse(path))
        }

        back.setOnClickListener {
            videoTrimmer.onCancelClicked()
            this.finish()
        }

        save.setOnClickListener {
            progressDialog.show()
            videoTrimmer.onSaveClicked()
        }
    }

    override fun onDestroy() {
        videoTrimmer.releasePlayer()
        super.onDestroy()
    }

    private fun getDestinationFile(): File {
        return File.createTempFile("temp-video", ".mp4", this.cacheDir)
    }

    override fun getResult(file: File) {
        RunOnUiThread(this).safely {
            Toast.makeText(this, "Done", Toast.LENGTH_SHORT).show()
            val uri = Uri.fromFile(file)
            Log.i("VIDEO TRIMMER", "Video saved at ${uri.path}")

            progressDialog.dismiss()
            val mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(this, uri)
            val duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
            val width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toLong()
            val height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toLong()
            val values = ContentValues()
            values.put(MediaStore.Video.Media.DATA, uri.path)
            values.put(MediaStore.Video.VideoColumns.DURATION, duration)
            values.put(MediaStore.Video.VideoColumns.WIDTH, width)
            values.put(MediaStore.Video.VideoColumns.HEIGHT, height)
            try {
                val id = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)?.let { ContentUris.parseId(it) }
            } catch (e: Exception) {

            }
        }
    }

    override fun cancelAction() {
        RunOnUiThread(this).safely {
            videoTrimmer.destroy()
            finish()
        }
    }

    override fun onError(message: String) {

    }

    override fun onFFmpegError(throwable: Throwable) {
        progressDialog.dismiss()
    }

    override fun onInfo(info: String) {

    }

    override fun onFFmpegFinished(path: String) {
        loadingDialog.dismiss()
        videoTrimmer.visibility = View.VISIBLE
        videoTrimmer.setVideoPath(path)
    }

    override fun onFFmpegStarted() {
        loadingDialog.show()
    }

    var doThis: (() -> Unit)? = null
    private fun setupPermissions(doSomething: () -> Unit) {
        val writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        doThis = doSomething
        if (writePermission != PackageManager.PERMISSION_GRANTED && readPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                101
            )
        } else doThis?.let { it() }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            101 -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    PermissionsDialog(
                        this@TrimmerActivity,
                        this.getString(R.string.photos_access_approve)
                    ).show()
                } else doThis?.let { it() }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
