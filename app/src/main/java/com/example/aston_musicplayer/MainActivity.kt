package com.example.aston_musicplayer

import android.annotation.SuppressLint
import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.widget.ImageButton
import com.example.aston_musicplayer.services.PlayerService

class MainActivity : AppCompatActivity() {

    private var playerService: PlayerService.PlayerServiceInterface? = null
    private lateinit var playerStateReceiver: BroadcastReceiver
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as PlayerService.PlayerBinder
            playerService = binder

            playerService?.init()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playerService = null
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnPlay = findViewById<ImageButton>(R.id.btn_play)
        val btnPrev = findViewById<ImageButton>(R.id.btn_prev)
        val btnNext = findViewById<ImageButton>(R.id.btn_next)

        val serviceIntent = Intent(this@MainActivity, PlayerService::class.java)
        startService(serviceIntent)

        val intent = Intent(this, PlayerService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        btnPlay.setImageResource(R.drawable.ic_baseline_play_arrow_24)

        playerStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "playerStateUpdate") {
                    val isPlaying = intent.getBooleanExtra("isPlaying", false)
                    if (!isPlaying) {
                        btnPlay.setImageResource(R.drawable.ic_baseline_pause_24)
                    } else {
                        btnPlay.setImageResource(R.drawable.ic_baseline_play_arrow_24)
                    }
                }
            }
        }

        val filter = IntentFilter()
        filter.addAction("playerStateUpdate")
        registerReceiver(playerStateReceiver, filter)

        btnPlay.setOnClickListener {
            val isPlaying = playerService?.play()
            if (isPlaying != true) {
                btnPlay.setImageResource(R.drawable.ic_baseline_play_arrow_24)
            } else {
                btnPlay.setImageResource(R.drawable.ic_baseline_pause_24)
            }

            val serviceIntent = Intent(this@MainActivity, PlayerService::class.java)
            serviceIntent.putExtra("isPlaying", isPlaying)
            startService(serviceIntent)
        }
        btnNext.setOnClickListener {
            btnPlay.setImageResource(R.drawable.ic_baseline_pause_24)
            playerService?.playNext()
        }

        btnPrev.setOnClickListener {
            btnPlay.setImageResource(R.drawable.ic_baseline_pause_24)
            playerService?.playPrevious()
        }
    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        unregisterReceiver(playerStateReceiver)
        super.onDestroy()
    }
}