package com.ezz.zaplayer

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.content.res.Resources.Theme
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.media.Image
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ddd.androidutils.DoubleClick
import com.ezz.zaplayer.databinding.ActivityPlayerBinding
import com.ezz.zaplayer.databinding.BoosterBinding
import com.ezz.zaplayer.databinding.MoreFeaturesBinding
import com.ezz.zaplayer.databinding.SpeedDialogBinding
import com.github.vkay94.dtpv.youtube.YouTubeOverlay
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.DefaultTimeBar
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.lang.Math.abs
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.system.exitProcess

class PlayerActivity : AppCompatActivity(), AudioManager.OnAudioFocusChangeListener, GestureDetector.OnGestureListener {

    lateinit var binding : ActivityPlayerBinding
    private var isSubtitle : Boolean = true
    private lateinit var playPauseBtn: ImageButton
    private lateinit var fullScreenBtn: ImageButton
    private lateinit var videoTitle: TextView
    private lateinit var gestureDetectorCompat: GestureDetectorCompat


    companion object{
        private var audioManager: AudioManager? = null
        private var timer : Timer? = null
        private lateinit var player : ExoPlayer
        lateinit var playerList : ArrayList<Video>
        var position : Int = -1
        private var repeat: Boolean = false
        private var isFullscreen :Boolean = false
        private var isLocked :Boolean = false
        private lateinit var trackSelector: DefaultTrackSelector
        private lateinit var loudnessEnhancer: LoudnessEnhancer
        private var speed : Float = 1.0f
        var pipStatus : Int = 0
        var nowPlayingId : String = ""
        private var brightness : Int = 0
        private var volume : Int = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        setTheme(R.style.playerActivityTheme)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // init var
        videoTitle = findViewById(R.id.videoTitle)
        playPauseBtn = findViewById(R.id.playPauseBtn)
        fullScreenBtn = findViewById(R.id.fullScreenBtn)

        // gesture detector init
        gestureDetectorCompat = GestureDetectorCompat(this, this)


        // FOR immersive mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE
        }

        // for orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // for handling video file intent
        try{
            if(intent.data?.scheme.contentEquals("content")){
                playerList = ArrayList()
                position = 0
                val cursor = contentResolver.query(intent.data!!, arrayOf(MediaStore.Video.Media.DATA), null, null, null)
                cursor?.let {
                    it.moveToFirst()
                    val path = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                    val file = File(path)
                    val video = Video(id = "", title = file.name, duration = 0L,
                        artUri = Uri.fromFile(file), path = path, size = "", folderName = "", folderId = "")
                    playerList.add(video)
                    cursor.close()
                }
                createPlayer()
                initialzeBinding()
            }else {
                initializeLayout()
                initialzeBinding()
            }
        }catch (e:Exception){Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()}



    }


    private fun initializeLayout(){

        when(intent.getStringExtra("class")){
            "AllVideos" -> {
                playerList = ArrayList()
                playerList.addAll(MainActivity.videoList)
                createPlayer()
            }
            "FolderActivity" -> {
                playerList = ArrayList()
                playerList.addAll(FoldersActivity.currentFolderVideos)
                createPlayer()
            }
            "SearchedVideos" -> {
                playerList = ArrayList()
                playerList.addAll(MainActivity.searchList)
                createPlayer()
            }
            "NowPlaying" -> {
                videoTitle.text = playerList[position].title
                videoTitle.isSelected = true
                doubleTapEnable()
                playVideo()
                playInFullscreen(isFullscreen)
                //setVisibility()
            }
        }


        if(repeat) findViewById<ImageButton>(R.id.repeatBtn).setImageResource(R.drawable.repeat_on_icon)
        else findViewById<ImageButton>(R.id.repeatBtn).setImageResource(R.drawable.repeat_icon)


    }

    private fun initialzeBinding(){

        findViewById<ImageButton>(R.id.backBtn).setOnClickListener{
            finish()
        }
        playPauseBtn.setOnClickListener {
            if(player.isPlaying) pauseVideo()
            else playVideo()
        }

        findViewById<ImageButton>(R.id.nextBtn).setOnClickListener {
            nextPrevVideo()
        }

        findViewById<ImageButton>(R.id.prevBtn).setOnClickListener {
            nextPrevVideo(false)
        }

        findViewById<ImageButton>(R.id.repeatBtn).setOnClickListener {
            if(repeat){
                repeat = false
                player.repeatMode = Player.REPEAT_MODE_OFF
                findViewById<ImageButton>(R.id.repeatBtn).setImageResource(R.drawable.repeat_icon)
            }else{
                repeat = true
                player.repeatMode = Player.REPEAT_MODE_ONE
                findViewById<ImageButton>(R.id.repeatBtn).setImageResource(R.drawable.repeat_on_icon)
            }
        }

        fullScreenBtn.setOnClickListener {
            if(isFullscreen){
                isFullscreen = false
                playInFullscreen(false)
            }else{
                isFullscreen = true
                playInFullscreen(true)
            }
        }

        findViewById<ImageButton>(R.id.changeView).setOnClickListener{
            if(requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            else
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        binding.lockBtn.setOnClickListener {
            if(!isLocked){
                // for hiding
                isLocked = true
                binding.playerView.hideController()
                binding.playerView.useController = false
                binding.lockBtn.setImageResource(R.drawable.lock_close_icon)
            }else{
                // for showing
                isLocked = false
                binding.playerView.useController = true
                binding.playerView.showController()
                binding.lockBtn.setImageResource(R.drawable.lock_open_icon)
            }
        }

        // show more feature dialog
        findViewById<ImageButton>(R.id.moreFeatureBtn).setOnClickListener {
            //pauseVideo()
            val customDialog = LayoutInflater.from(this).inflate(R.layout.more_features, binding.root, false)
            val bindingMF = MoreFeaturesBinding.bind(customDialog)
            val dialog = MaterialAlertDialogBuilder(this).setView(customDialog)
                .setOnCancelListener { playVideo() }
                .setBackground(ColorDrawable(0x803700B3.toInt()))
                .create()

            dialog.show()

            bindingMF.audioTrack.setOnClickListener {
                dialog.dismiss()

                val audioTrack = ArrayList<String>()
                for(i in 0 until player.currentTrackGroups.length){
                    if(player.currentTrackGroups.get(i).getFormat(0).selectionFlags == C.SELECTION_FLAG_DEFAULT){
                        audioTrack.add(Locale(player.currentTrackGroups.get(i).getFormat(0).language.toString()).displayName)
                    }
                }

                val tempTracks = audioTrack.toArray(arrayOfNulls<CharSequence>(audioTrack.size))

                MaterialAlertDialogBuilder(this, R.style.alertDialog)
                    .setTitle("Select Language")
                    .setOnCancelListener { playVideo() }
                    .setBackground(ColorDrawable(0x803700B3.toInt()))
                    .setItems(tempTracks){_, position ->
                        Toast.makeText(this, audioTrack[position]+" Selected", Toast.LENGTH_SHORT).show()
                        trackSelector.setParameters(trackSelector.buildUponParameters().setPreferredAudioLanguage(audioTrack[position]))
                    }
                    .create()
                    .show()
            }

            bindingMF.subtitlesBtn.setOnClickListener {
                if(isSubtitle){
                    trackSelector.parameters = DefaultTrackSelector.ParametersBuilder(this).setRendererDisabled(
                        C.TRACK_TYPE_VIDEO, true
                    ).build()
                    Toast.makeText(this, "Subtitles Off", Toast.LENGTH_SHORT).show()
                    isSubtitle = false
                }else{
                    trackSelector.parameters = DefaultTrackSelector.ParametersBuilder(this).setRendererDisabled(
                        C.TRACK_TYPE_VIDEO, false
                    ).build()
                    Toast.makeText(this, "Subtitles On", Toast.LENGTH_SHORT).show()
                    isSubtitle = true
                }
                dialog.dismiss()
            }

            bindingMF.audioBooster.setOnClickListener {
                dialog.dismiss()
                val customDialogB = LayoutInflater.from(this).inflate(R.layout.booster, binding.root, false)
                val bindingB = BoosterBinding.bind(customDialogB)
                val dialogB = MaterialAlertDialogBuilder(this).setView(customDialogB)
                    .setOnCancelListener { playVideo() }
                    .setPositiveButton("OK"){self, _ ->
                        loudnessEnhancer.setTargetGain(bindingB.verticalBar.progress *100)
                        self.dismiss()
                        playVideo()
                    }
                    .setBackground(ColorDrawable(0x803700B3.toInt()))
                    .create()
                dialogB.show()

                // show loadness details
                bindingB.verticalBar.progress = loudnessEnhancer.targetGain.toInt()/100
                bindingB.progressText.text = "Audio Boost\n\n${loudnessEnhancer.targetGain.toInt()/10} %"
                bindingB.verticalBar.setOnProgressChangeListener {
                    bindingB.progressText.text = "Audio Boost\n\n${it*10} %"
                }
            }

            // speed btn
            bindingMF.speedBtn.setOnClickListener {
                dialog.dismiss()
                playVideo()

                val customDialogS = LayoutInflater.from(this).inflate(R.layout.speed_dialog, binding.root, false)
                val bindingS = SpeedDialogBinding.bind(customDialogS)
                val dialogS = MaterialAlertDialogBuilder(this).setView(customDialogS)
                    .setCancelable(false)
                    .setPositiveButton("OK"){self, _->
                        //playVideo()
                        self.dismiss()
                    }
                    .setBackground(ColorDrawable(0x803700B3.toInt()))
                    .create()
                dialogS.show()

                bindingS.speedText.text = "${DecimalFormat("#.##").format(speed)} X"

                bindingS.minusBtn.setOnClickListener {
                    changeSpeed(false)
                    bindingS.speedText.text = "${DecimalFormat("#.##").format(speed)} X"
                }

                bindingS.plusBtn.setOnClickListener {
                    changeSpeed(true)
                    bindingS.speedText.text = "${DecimalFormat("#.##").format(speed)} X"
                }
            }

            // timer btn
            bindingMF.sleepTimer.setOnClickListener {
                dialog.dismiss()
                if(timer != null){
                    Toast.makeText(this, "Timer Already Running!\nClose App to Reset Timer", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                var sleepTime : Int = 15
                val customDialogS = LayoutInflater.from(this).inflate(R.layout.speed_dialog, binding.root, false)
                val bindingS = SpeedDialogBinding.bind(customDialogS)
                val dialogS = MaterialAlertDialogBuilder(this).setView(customDialogS)
                    .setCancelable(false)
                    .setPositiveButton("OK"){self, _->
                        // set timer
                        timer = Timer()
                        val task = object : TimerTask(){
                            override fun run() {
                                moveTaskToBack(true)
                                exitProcess(1)
                            }
                        }
                        timer!!.schedule(task, sleepTime*60*1000.toLong())
                        playVideo()
                        self.dismiss()
                    }
                    .setBackground(ColorDrawable(0x803700B3.toInt()))
                    .create()
                dialogS.show()

                bindingS.speedText.text = "$sleepTime Min"

                bindingS.minusBtn.setOnClickListener {
                    if(sleepTime > 15) sleepTime -= 15
                    bindingS.speedText.text = "$sleepTime Min"
                }

                bindingS.plusBtn.setOnClickListener {
                    if(sleepTime < 120) sleepTime += 15
                    bindingS.speedText.text = "$sleepTime Min"
                }
            }

            // pip mode
            bindingMF.pipModeBtn.setOnClickListener {
                val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appOps.checkOpNoThrow(AppOpsManager.OPSTR_PICTURE_IN_PICTURE, android.os.Process.myUid(), packageName)== AppOpsManager.MODE_ALLOWED
                } else {
                    false
                }

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    if(status){
                        this.enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                        dialog.dismiss()
                        binding.playerView.hideController()
                        playVideo()
                        pipStatus = 0
                    }else{
                        val intent = Intent("android.settings.PICTURE_IN_PICTURE_SETTINGS", Uri.parse("package:$packageName"))
                        startActivity(intent)
                    }
                }else{
                    Toast.makeText(this, "Feature Not Supported!!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    playVideo()
                }
            }
        }
    }

    private fun playVideo(){
        playPauseBtn.setImageResource(R.drawable.pause_icon)
        player.play()
    }

    private fun pauseVideo(){
        playPauseBtn.setImageResource(R.drawable.play_icon)
        player.pause()
    }

    private fun nextPrevVideo(isNext:Boolean = true){
        if(isNext) setPosition()
        else setPosition(false)
        createPlayer()
    }

    private fun setPosition(isIncrement:Boolean = true){
        if(!repeat){
            if(isIncrement){
                if(playerList.size - 1 == position)
                    position = 0
                else ++position
            }else{
                if(position == 0)
                    position = playerList.size - 1
                else --position
            }
        }
    }

    private fun playInFullscreen(enable: Boolean){
        if(enable){
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            fullScreenBtn.setImageResource(R.drawable.full_screen_exit_icon)
        }else{
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            fullScreenBtn.setImageResource(R.drawable.full_screen_icon)
        }
    }

    private fun createPlayer(){
        try{
            player.release()
        }catch (e: Exception){}
        trackSelector = DefaultTrackSelector(this)

        videoTitle.text = playerList[position].title
        videoTitle.isSelected = true
        player = ExoPlayer.Builder(this).setTrackSelector(trackSelector).build()

        doubleTapEnable()

        val mediaItem = MediaItem.fromUri(playerList[position].artUri)
        player.setMediaItem(mediaItem)

        speed = 1.0f
//        player.setPlaybackSpeed(speed)

        player.prepare()
        playVideo()


        player.addListener(object: Player.Listener{
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)

                if(playbackState == Player.STATE_ENDED) {
                    if(repeat){
                        player.release()
                        createPlayer()
                    }else{
                        nextPrevVideo()
                    }
                }
            }
        })

        playInFullscreen(isFullscreen)
        //setVisibility()
        loudnessEnhancer = LoudnessEnhancer(player.audioSessionId)
        loudnessEnhancer.enabled = true
        nowPlayingId = playerList[position].id


        binding.playerView.setControllerVisibilityListener {
            when{
                isLocked-> {
                    binding.lockBtn.visibility = View.VISIBLE
                }
                binding.playerView.isControllerVisible -> {
                    binding.lockBtn.visibility = View.VISIBLE
                }
                else-> {
                    binding.lockBtn.visibility = View.INVISIBLE
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    WindowInsetsControllerCompat(window, binding.root).let { controller ->
                        controller.hide(WindowInsetsCompat.Type.systemBars())
                        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE
                    }
                }
            }

        }
    }




    private fun changeSpeed(isIncrement: Boolean){
        if(isIncrement){
            if(speed <= 2.9f){
                speed += 0.10f
            }
        }else{
            if(speed > 0.20f){
                speed -= 0.10f
            }
        }
        player.setPlaybackSpeed(speed)
    }


    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        if(pipStatus != 0){
            finish()
            val intent = Intent(this, PlayerActivity::class.java)
            when(pipStatus){
                1 -> intent.putExtra("class","FolderActivity")
                2 -> intent.putExtra("class","SearchedVideos")
                3 -> intent.putExtra("class","AllVideos")
            }
            startActivity(intent)
        }
        if(!isInPictureInPictureMode) pauseVideo()

    }



    override fun onDestroy() {
        super.onDestroy()

        player.pause()
        audioManager?.abandonAudioFocus(this)
    }

    override fun onAudioFocusChange(focusChange: Int) {
        if(focusChange <= 0) pauseVideo()
    }

    override fun onResume() {
        super.onResume()

        if(audioManager == null) audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager!!.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)

        if(brightness != 0) setScreenBrightness(brightness)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun doubleTapEnable(){
        binding.playerView.player = player
        binding.ytOverlay.performListener(object : YouTubeOverlay.PerformListener {
            override fun onAnimationEnd() {
                binding.ytOverlay.visibility = View.GONE
            }

            override fun onAnimationStart() {
                binding.ytOverlay.visibility = View.VISIBLE
            }

        })

        binding.ytOverlay.player(player)

        binding.playerView.setOnTouchListener { _, motionEvent ->

            binding.playerView.isDoubleTapEnabled = false
            if(!isLocked){
                binding.playerView.isDoubleTapEnabled = true
                gestureDetectorCompat.onTouchEvent(motionEvent)

                if(motionEvent.action == MotionEvent.ACTION_UP){
                    binding.brightnessIcon.visibility = View.GONE
                    binding.volumeIcon.visibility = View.GONE
                }

                // FOR immersive mode
//                WindowCompat.setDecorFitsSystemWindows(window, false)
//                WindowInsetsControllerCompat(window, binding.root).let { controller ->
//                    controller.hide(WindowInsetsCompat.Type.systemBars())
//                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE
//                }
            }

            return@setOnTouchListener false
        }
    }


    // seek bar setup
    private fun seekBarFeature(){
        findViewById<DefaultTimeBar>(com.google.android.exoplayer2.ui.R.id.exo_progress).addListener(object: TimeBar.OnScrubListener{
            override fun onScrubStart(timeBar: TimeBar, position: Long) {
                pauseVideo()
            }

            override fun onScrubMove(timeBar: TimeBar, position: Long) {
                player.seekTo(position)
            }

            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                pauseVideo()
            }

        })
    }

    override fun onDown(e: MotionEvent): Boolean = false
    override fun onShowPress(e: MotionEvent) = Unit
    override fun onSingleTapUp(e: MotionEvent): Boolean = false
    override fun onLongPress(e: MotionEvent) = Unit
    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean = false

    override fun onScroll(event: MotionEvent, event1: MotionEvent, distanceX: Float, distanceY: Float): Boolean {

        val sWidth = Resources.getSystem().displayMetrics.widthPixels
        val sHeight = Resources.getSystem().displayMetrics.heightPixels

        val border = 100 * Resources.getSystem().displayMetrics.density.toInt()
        if(event.x < border || event.y < border || event.x > sWidth-border || event.y > sHeight - border)
            return false

        if(abs(distanceX) < abs(distanceY)){
            if(event.x < sWidth/2){
                // brightness
                binding.brightnessIcon.visibility = View.VISIBLE
                binding.volumeIcon.visibility = View.GONE

                val increase = distanceY > 0
                val newValue = if(increase) brightness +1 else brightness - 1
                if(newValue in 0..30) brightness = newValue
                binding.brightnessIcon.text = brightness.toString()
                setScreenBrightness(brightness)
            }else{
                // volume
                binding.brightnessIcon.visibility = View.GONE
                binding.volumeIcon.visibility = View.VISIBLE

                val maxVolume = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val increase = distanceY > 0
                val newValue = if(increase) volume + .5 else volume - .5
                if(newValue.toInt() in 0..maxVolume) volume = newValue.toInt()

                binding.volumeIcon.text = volume.toString()
                audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
            }
        }


        return true
    }

    private fun setScreenBrightness(value: Int){
        val d = 1.0f/30
        val lp = this.window.attributes
        lp.screenBrightness = d * value
        this.window.attributes = lp
    }

}