package com.ezz.zaplayer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.ezz.zaplayer.databinding.ActivityMainBinding
import com.ezz.zaplayer.databinding.ThemeViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.lang.Exception
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle

    private var runnable: Runnable? = null

    companion object{
        val sortList = arrayOf(MediaStore.Video.Media.DATE_ADDED+" DESC", MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.TITLE, MediaStore.Video.Media.TITLE+" DESC", MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.SIZE+" DESC")
        lateinit var videoList: ArrayList<Video>
        lateinit var folderList: ArrayList<Folder>
        lateinit var searchList: ArrayList<Video>
        var search: Boolean = false
        var themeIndex : Int = 1
        var sortValue : Int = 0
        val themeList = arrayOf(R.style.coolPinkNav, R.style.coolBlueNav,
            R.style.coolPurpleNav, R.style.coolGreenNav, R.style.coolRedNav, R.style.coolBlackNav)
        var dataChanged: Boolean = false
        var adapterChanged: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // get theme
        val editor = getSharedPreferences("Themes", MODE_PRIVATE)
        themeIndex = editor.getInt("themeIndex", 0)

        setTheme(themeList[themeIndex])

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)



        // for drawer
        toggle = ActionBarDrawerToggle(this, binding.root, R.string.open, R.string.close)
        binding.root.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // ask for permission storage videos
        if(requestRuntimePermission()){
            folderList = ArrayList()
            videoList  = getAllVideos(this)
            setFragment(VideosFragment())


            if(dataChanged) {
                videoList = getAllVideos(this)
                dataChanged = false
                adapterChanged = true
            }
//            runnable = Runnable {
//                if(dataChanged) {
//                    videoList = getAllVideos()
//                    dataChanged = false
//                    adapterChanged = true
//                }
//                Handler(Looper.getMainLooper()).postDelayed(runnable!!, 200)
//            }
//            Handler(Looper.getMainLooper()).postDelayed(runnable!!, 0)
        }

        binding.bottomNav.setOnItemSelectedListener {
            when(it.itemId){
                R.id.videoView -> setFragment(VideosFragment())
                R.id.foldersView -> setFragment(FoldersFragment())
            }
            return@setOnItemSelectedListener true
        }

        // drawer nav btns clicks events
        binding.navView.setNavigationItemSelectedListener {

            when(it.itemId){
                R.id.feedbackNav -> Toast.makeText(this, "Feedback", Toast.LENGTH_SHORT).show()
                R.id.themesNav -> {
                    val customDialog = LayoutInflater.from(this).inflate(R.layout.theme_view, binding.root, false)
                    val bindingTV = ThemeViewBinding.bind(customDialog)
                    val dialog = MaterialAlertDialogBuilder(this).setView(customDialog)
                        .setTitle("Select Theme")
                        .create()

                    dialog.show()

                    when(themeIndex){
                        0 -> bindingTV.themePink.setBackgroundColor(Color.YELLOW)
                        1 -> bindingTV.themeBlue.setBackgroundColor(Color.YELLOW)
                        2 -> bindingTV.themePurple.setBackgroundColor(Color.YELLOW)
                        3 -> bindingTV.themeGreen.setBackgroundColor(Color.YELLOW)
                        4 -> bindingTV.themeRed.setBackgroundColor(Color.YELLOW)
                        5 -> bindingTV.themeBlack.setBackgroundColor(Color.YELLOW)
                    }

                    bindingTV.themePink.setOnClickListener {saveTheme(0)}
                    bindingTV.themeBlue.setOnClickListener {saveTheme(1)}
                    bindingTV.themePurple.setOnClickListener {saveTheme(2)}
                    bindingTV.themeGreen.setOnClickListener {saveTheme(3)}
                    bindingTV.themeRed.setOnClickListener {saveTheme(4)}
                    bindingTV.themeBlack.setOnClickListener {saveTheme(5)}
                }
                R.id.sortOrderNav -> {
                    val menuItems = arrayOf("Latest", "Oldest", "Name(A to Z)",
                        "Name(Z to A)", "File Size(Smallest)", "File Size(Largest)")

                    var value = sortValue
                    val dialog = MaterialAlertDialogBuilder(this)
                        .setTitle("Sort By")
                        .setPositiveButton("OK"){_, _->
                            val sortEditor = getSharedPreferences("Sorting", MODE_PRIVATE).edit()
                            sortEditor.putInt("sortValue", value)
                            sortEditor.apply()

                            // for restarting app
                            finish()
                            startActivity(intent)
                        }
                        .setSingleChoiceItems(menuItems, sortValue){_, pos ->
                            value = pos
                        }
                        .create()

                    dialog.show()

                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.BLUE)

                }
                R.id.aboutNav -> startActivity(Intent(this, AboutActivity::class.java))
                R.id.exitNav -> exitProcess(1)
            }

            return@setNavigationItemSelectedListener true
        }
    }

    private fun saveTheme(index: Int){
        val editor = getSharedPreferences("Themes", MODE_PRIVATE).edit()
        editor.putInt("themeIndex", index)
        editor.apply()

        // for restarting app
        finish()
        startActivity(intent)
    }

    private fun setFragment(fragment: Fragment){
        val transition = supportFragmentManager.beginTransaction()
        transition.replace(R.id.fragmentFL, fragment)
        transition.disallowAddToBackStack()
        transition.commit()
    }

    private fun requestRuntimePermission(): Boolean{
        if(ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 13)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == 13){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                folderList = ArrayList()
                videoList  = getAllVideos(this)
                setFragment(VideosFragment())
            }
            else
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 13)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        val gradientList = arrayOf(R.drawable.pink_gradient, R.drawable.blue_gradient, R.drawable.purple_gradient,
            R.drawable.green_gradient, R.drawable.red_gradient, R.drawable.blue_gradient)

        findViewById<LinearLayout>(R.id.gradientLayout).setBackgroundResource(gradientList[themeIndex])

        if(toggle.onOptionsItemSelected(item))
            return true
        return super.onOptionsItemSelected(item)
    }




    override fun onDestroy() {
        super.onDestroy()

        runnable = null
    }
}