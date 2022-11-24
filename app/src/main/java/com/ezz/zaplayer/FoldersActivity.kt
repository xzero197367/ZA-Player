package com.ezz.zaplayer

import android.annotation.SuppressLint
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.ezz.zaplayer.databinding.ActivityFoldersBinding
import java.io.File
import java.lang.Exception

class FoldersActivity : AppCompatActivity() {

    companion object{
        lateinit var currentFolderVideos: ArrayList<Video>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityFoldersBinding.inflate(layoutInflater)
        // set theme
        setTheme(MainActivity.themeList[MainActivity.themeIndex])
        setContentView(binding.root)

        // get folder position
        val position = intent.getIntExtra("position", 0)
        // change action bar title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = MainActivity.folderList[position].folderName


        currentFolderVideos = getAllVideos(MainActivity.folderList[position].id)

        binding.videoRVFA.setHasFixedSize(true)
        binding.videoRVFA.setItemViewCacheSize(10)
        binding.videoRVFA.layoutManager = LinearLayoutManager(this)
        binding.videoRVFA.adapter = VideoAdapter(this, currentFolderVideos, isFolder = true)

        binding.totalVideosFA.text = "Total Videos: ${currentFolderVideos.size}"
    }

    private fun getFolderVideos(position:Int):ArrayList<Video>{
        val videos = ArrayList<Video>()
        for (video in MainActivity.videoList){
            if(video.folderId.equals(MainActivity.folderList[position].id)){
                videos.add(video)
            }
        }
        return videos
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return true
    }


    // InlinedApi
    @SuppressLint("InlinedApi","Recycle", "Range")
    private fun getAllVideos(folderId:String): ArrayList<Video>{
        val tempList = ArrayList<Video>()
        val selection = MediaStore.Video.Media.BUCKET_ID + " like? "

        // projection query to get videos from media storage
        val projection = arrayOf(
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_ID,
        )

        // cursor
        val cursor = this.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            arrayOf(folderId),
            MediaStore.Video.Media.DATE_ADDED + " DESC"
        )

        if(cursor != null){
            if(cursor.moveToNext()){
                do{
                    val titleC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE))
                    val idC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID))
                    val folderC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME))
                    val sizeC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.SIZE))
                    val pathC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA))
                    val durationC = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                    val folderIdC = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID))


                    try {
                        val file = File(pathC)
                        val artUri = Uri.fromFile(file)
                        val video = Video(
                            title=titleC, id = idC, folderName = folderC, folderId = folderIdC, duration = durationC, size = sizeC, path = pathC, artUri = artUri
                        )
                        if(file.exists()) tempList.add(video)

                    }catch (e: Exception){}
                }while (cursor.moveToNext())
            }
        }

        return tempList
    }
}