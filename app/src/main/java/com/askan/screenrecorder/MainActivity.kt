package com.askan.screenrecorder

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.media3.session.MediaController
class MainActivity : AppCompatActivity() {
    private val REQUEST_CODE = 1000
    private val REQUEST_PERMISSION = 1001
    private lateinit var mediaProjectionManager: MediaProjectionManager

    private lateinit var toggleBtn: FloatingActionButton
    private lateinit var videoView: VideoView

    var isChecked = false
    private var videoUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        toggleBtn = findViewById(R.id.toggleBtn)
        videoView = findViewById(R.id.videoView)

        toggleBtn.setOnClickListener {
            if (checkPermissions()) {
                toggleScreenShare()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            + ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO),
                REQUEST_PERMISSION
            )
            return false
        }
        return true
    }

    private fun toggleScreenShare() {
        if (!isChecked) {
            startScreenRecording()
        } else {
            stopScreenRecording()
        }
    }

    private fun startScreenRecording() {
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE)
    }

    private fun stopScreenRecording() {
        val serviceIntent = Intent(this, ScreenRecorderService::class.java)
        serviceIntent.action = ScreenRecorderService.ACTION_STOP_RECORDING
        startService(serviceIntent)

        // Update UI
        val sp = getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
        videoView.visibility = View.VISIBLE
        val uri = Uri.parse(sp.getString("video_uri", null))
        videoView.setVideoURI(Uri.parse(uri.toString()))
        val mediaController = android.widget.MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)
        videoView.requestFocus()
        //videoView.start()
        isChecked = false
        toggleBtn.setImageResource(R.drawable.ic_video)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != REQUEST_CODE) {
            Toast.makeText(this, "Unknown Request code", Toast.LENGTH_SHORT).show()
            return
        }

        if (resultCode != Activity.RESULT_OK) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            return
        }

        val serviceIntent = Intent(this, ScreenRecorderService::class.java)
        serviceIntent.action = ScreenRecorderService.ACTION_START_RECORDING
        serviceIntent.putExtra(ScreenRecorderService.EXTRA_RESULT_CODE, resultCode)
        serviceIntent.putExtra(ScreenRecorderService.EXTRA_RESULT_DATA, data)
        startService(serviceIntent)


        // Update UI
        isChecked = true
        toggleBtn.setImageResource(R.drawable.id_stop)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION -> {
                if (grantResults.size > 0 && grantResults[0] + grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    toggleScreenShare()
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}