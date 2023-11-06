package com.example.aston_musicplayer.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.aston_musicplayer.R

class PlayerService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var songsArray: List<Int>
    private var trackIndex = 0
    private val NOTIFICATION_ID = 1000
    private var playerBinder: PlayerBinder? = null
    private var isPlaying: Boolean? = null

    override fun onBind(intent: Intent): IBinder {
        playerBinder = PlayerBinder()
        return PlayerBinder()
    }

    override fun onCreate() {
        val rawResources: List<Int> = listOf(
            R.raw.mus1,
            R.raw.mus2,
            R.raw.mus3
        )
        mediaPlayer = MediaPlayer()
        songsArray = rawResources
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())

        intent?.let {
            when (intent.action) {
                "PLAY_ACTION" -> {
                    playerBinder?.play()
                    updateActivity(isPlaying)
                    startForeground(NOTIFICATION_ID, createNotification())
                }
                "PREVIOUS_ACTION" -> {
                    playerBinder?.playPrevious()
                    startForeground(NOTIFICATION_ID, createNotification())
                }
                "NEXT_ACTION" -> {
                    playerBinder?.playNext()
                    startForeground(NOTIFICATION_ID, createNotification())
                }
            }
        }

        isPlaying = intent?.getBooleanExtra("isPlaying", false)

        return START_STICKY
    }

    private fun updateActivity(isPlaying: Boolean?) {
        val intent = Intent("playerStateUpdate")
        intent.putExtra("isPlaying", isPlaying)
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        val notificationLayout = RemoteViews(packageName, R.layout.notification_layout)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val playIntent = Intent(this, PlayerService::class.java).apply {
            action = "PLAY_ACTION"
        }
        val playPendingIntent: PendingIntent =
            PendingIntent.getService(this, 0, playIntent, PendingIntent.FLAG_MUTABLE)
        notificationLayout.setOnClickPendingIntent(R.id.btn_play, playPendingIntent)

        val previousIntent = Intent(this, PlayerService::class.java).apply {
            action = "PREVIOUS_ACTION"
        }
        val previousPendingIntent: PendingIntent =
            PendingIntent.getService(this, 0, previousIntent, PendingIntent.FLAG_MUTABLE)
        notificationLayout.setOnClickPendingIntent(R.id.btn_previous, previousPendingIntent)

        val nextIntent = Intent(this, PlayerService::class.java).apply {
            action = "NEXT_ACTION"
        }
        val nextPendingIntent: PendingIntent =
            PendingIntent.getService(this, 0, nextIntent, PendingIntent.FLAG_MUTABLE)
        notificationLayout.setOnClickPendingIntent(R.id.btn_next, nextPendingIntent)

        if (isPlaying != null) {
            val playIcon = if (playerBinder?.state() == true) {
                R.drawable.ic_baseline_pause_24
            } else {
                R.drawable.ic_baseline_play_arrow_24
            }
            notificationLayout.setImageViewResource(R.id.btn_play, playIcon)
        }

        val notification = NotificationCompat.Builder(this, "default_notification_channel_id")
            .setSmallIcon(R.drawable.ic_baseline_play_arrow_24)
            .setSound(null)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(notificationLayout)
            .addAction(R.drawable.ic_baseline_play_arrow_24, "Play", playPendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "default_notification_channel_id",
                "Music",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            channel.setSound(null, null)
            notificationManager.createNotificationChannel(channel)
            notification.setChannelId("default_notification_channel_id")
            notification.addAction(R.drawable.ic_baseline_play_arrow_24, "Play", playPendingIntent)
        }

        return notification.build()
    }

    interface PlayerServiceInterface {
        fun playNext()
        fun playPrevious()
        fun state(): Boolean
        fun play(): Boolean
        fun init()
    }

    inner class PlayerBinder : Binder(), PlayerServiceInterface {

        override fun playNext() {
            if (trackIndex == songsArray.size - 1) {
                trackIndex = 0
            } else {
                trackIndex++
            }
            playTrack(songsArray[trackIndex])
        }

        override fun init() {
            initTrack(songsArray[trackIndex])
        }

        private fun initTrack(trackResId: Int) {
            mediaPlayer?.reset()
            mediaPlayer = MediaPlayer.create(this@PlayerService, trackResId)
            setOnCompletionListener()
        }

        private fun setOnCompletionListener() {
            mediaPlayer?.setOnCompletionListener {
                playNext()
            }
        }

        override fun playPrevious() {
            if (trackIndex == 0) {
                trackIndex = songsArray.size - 1
            } else {
                trackIndex--
            }
            playTrack(songsArray[trackIndex])
        }

        private fun playTrack(trackResId: Int) {
            mediaPlayer?.reset()
            mediaPlayer = MediaPlayer.create(this@PlayerService, trackResId)
            mediaPlayer?.start()
            setOnCompletionListener()
        }

        override fun play(): Boolean {
            if (state()) {
                mediaPlayer?.pause()
            } else {
                mediaPlayer?.start()
            }
            return state()
        }

        override fun state(): Boolean {
            return mediaPlayer?.isPlaying == true
        }
    }
}