package com.askan.screenrecorder

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Environment
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.SparseIntArray
import android.view.Surface
import androidx.core.app.NotificationCompat
import java.io.File

class ScreenRecorderService : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var mScreenDensity: Int = 0
    private var DISPLAY_WIDTH = 720
    private var DISPLAY_HEIGHT = 1280

    private val ORIENTATIONS = SparseIntArray()

    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        val metrics = DisplayMetrics()
        val displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager
        displayManager.getDisplay(0)?.getMetrics(metrics)
        mScreenDensity = metrics.densityDpi
        DISPLAY_WIDTH= metrics.widthPixels
        DISPLAY_HEIGHT = metrics.heightPixels

        // Start the foreground service on create
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val data = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                mediaProjection = (getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).getMediaProjection(resultCode, data!!)
                startRecording()
            }
            ACTION_STOP_RECORDING -> {
                stopRecording()
            }
        }
        return START_STICKY
    }

    private fun startRecording() {
        initRecorder()
        virtualDisplay = createVirtualDisplay()
        mediaRecorder?.start()
    }

    private fun stopRecording() {
        mediaRecorder?.stop()
        mediaRecorder?.reset()
        virtualDisplay?.release()
        mediaProjection?.stop()
        stopForeground(true)
        stopSelf()
    }

    private fun initRecorder() {
        try {
            mediaRecorder = MediaRecorder()
            mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)

            val recordingFile = "ScreenREC${System.currentTimeMillis()}.mp4"
            val newPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            val folder = File(newPath, "SultanREC/")
            if (!folder.exists()) {
                folder.mkdirs()
            }
            val file = File(folder, recordingFile)
            val videoUri = file.absolutePath
            val sp = getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
            val editor = sp.edit()
            editor.putString("video_uri", videoUri)
            editor.apply()

            mediaRecorder?.setOutputFile(videoUri)
            mediaRecorder?.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT)
            mediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mediaRecorder?.setVideoEncodingBitRate(512 * 1000)
            mediaRecorder?.setVideoFrameRate(30)

            val rotation = (getSystemService(DISPLAY_SERVICE) as DisplayManager).getDisplay(0)?.rotation ?: 0
            val orientation = ORIENTATIONS.get(rotation + 90)
            mediaRecorder?.setOrientationHint(orientation)
        //asd
            mediaRecorder?.prepare()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createVirtualDisplay(): VirtualDisplay? {
        return mediaProjection?.createVirtualDisplay(
            "ScreenRecorderService",
            DISPLAY_WIDTH,
            DISPLAY_HEIGHT,
            mScreenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder?.surface,
            null,
            null
        )
    }

    private fun startForegroundService() {
        val channelId =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                createNotificationChannel("my_service", "My Background Service")
            } else {
                // If earlier version channel ID is not used
                ""
            }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Screen Recording")
            .setContentText("Recording in progress...")
            .build()

        startForeground(1, notification)
    }

    private fun createNotificationChannel(channelId: String, channelName: String): String{
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE)
            chan.lightColor = R.color.purple_200
            chan.lockscreenVisibility = android.app.Notification.VISIBILITY_SECRET
            val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            service.createNotificationChannel(chan)
            return channelId
        } else {
            return ""
        }
    }

    companion object {
        const val ACTION_START_RECORDING = "com.askan.screenrecorder.action.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.askan.screenrecorder.action.STOP_RECORDING"
        const val EXTRA_RESULT_CODE = "com.askan.screenrecorder.extra.RESULT_CODE"
        const val EXTRA_RESULT_DATA = "com.askan.screenrecorder.extra.RESULT_DATA"
    }
}