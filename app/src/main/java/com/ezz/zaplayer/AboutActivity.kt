package com.ezz.zaplayer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ezz.zaplayer.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(MainActivity.themeList[MainActivity.themeIndex])
        val binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = getString(R.string.about)

        binding.aboutText.text = "Developed By: Ezz Abdelmoez \nEmail: ezzabdelmoez@gmail.com \nPhone: +201014080630"

    }
}