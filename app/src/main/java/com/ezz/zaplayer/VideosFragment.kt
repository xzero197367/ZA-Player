package com.ezz.zaplayer

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Button
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.ezz.zaplayer.databinding.FragmentVideosBinding


class VideosFragment : Fragment() {

    private lateinit var adapter: VideoAdapter
    private lateinit var binding: FragmentVideosBinding

    private var isLinear: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        requireContext().theme.applyStyle(MainActivity.themeList[MainActivity.themeIndex], true)

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_videos, container, false)
        binding = FragmentVideosBinding.bind(view)

        binding.VideoRV.setHasFixedSize(true)
        binding.VideoRV.setItemViewCacheSize(10)
        binding.VideoRV.layoutManager = LinearLayoutManager(requireContext())

        adapter = VideoAdapter(requireContext(), MainActivity.videoList)
        binding.VideoRV.adapter = adapter

        binding.totalVideos.text = "Total Videos: ${MainActivity.videoList.size.toString()}"

        // for refreshing layout
        binding.root.setOnRefreshListener {

            MainActivity.videoList = getAllVideos(requireContext())
            adapter.updateList(MainActivity.videoList)
            binding.totalVideos.text = "Total Videos: ${MainActivity.videoList.size.toString()}"

            binding.root.isRefreshing = false
        }


        binding.nowPlayingBtn.setOnClickListener {
            val intent = Intent(requireContext(), PlayerActivity::class.java)
            intent.putExtra("class", "NowPlaying")
            startActivity(intent)
        }



        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        inflater.inflate(R.menu.search_view, menu)
        val searchView = menu.findItem(R.id.searchView)?.actionView as SearchView

        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean = true

            override fun onQueryTextChange(newText: String?): Boolean {

                if(newText != null){
                    MainActivity.searchList = ArrayList()
                    for (video in MainActivity.videoList){
                        if(video.title.toLowerCase().contains(newText.toLowerCase())){
                            MainActivity.searchList.add(video)

                        }
                    }
                    MainActivity.search = true
                    adapter.updateList(MainActivity.searchList)
                }

                return true
            }

        })


        super.onCreateOptionsMenu(menu, inflater)
    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        // changegrid view
//        when(item.itemId){
//            R.id.viewStyle->{
//                if(isLinear){
//                    binding.VideoRV.layoutManager = GridLayoutManager(requireContext(), 2)
//                }else{
//                    binding.VideoRV.layoutManager = LinearLayoutManager(requireContext())
//                }
//            }
//        }
//
//        return true
//    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        if(PlayerActivity.position != -1) binding.nowPlayingBtn.visibility = View.VISIBLE
        if(MainActivity.adapterChanged) adapter.notifyDataSetChanged()
        MainActivity.adapterChanged = false
    }
}