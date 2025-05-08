package com.alex.testapp.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import com.alex.testapp.data.remot.RetrofitInstance
import com.alex.testapp.data.repository.AdvertiseRepository
import com.alex.testapp.databinding.FragmentAdvertiseBinding
import com.alex.testapp.ui.viewmodel.AdvertiseViewModel
import com.alex.testapp.ui.viewmodel.AdvertiseViewModelFactory
import com.alex.testapp.util.AdvertiseTimeWatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AdvertiseFragment : Fragment() {
    private var _binding: FragmentAdvertiseBinding? = null
    private val binding get() = _binding!!

    private lateinit var advertiseViewModel: AdvertiseViewModel

    private var player: ExoPlayer? = null
    private lateinit var timeWatcher: AdvertiseTimeWatcher

    private var uiUpdateJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val advertiseFactory =
            AdvertiseViewModelFactory(AdvertiseRepository(RetrofitInstance.advertiseApiService))
        advertiseViewModel = ViewModelProvider(
            requireActivity(),
            advertiseFactory
        ).get(AdvertiseViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentAdvertiseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (canNavigateBack()) {
                    findNavController().navigateUp()
                } else {
                    // todo move to resource string
                    Toast.makeText(
                        requireContext(),
                        "لطفا تا پایان آگهی صبر کنید",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            callback
        )

        advertiseViewModel.advertises.observe(viewLifecycleOwner) { adList ->
            if (adList.size > 1) {
                Log.e("Advertise", "" + adList.size)
                val secondAd = adList[0]
                playAdVideo(secondAd.url)
                monitorAdWatchTime()
            }
        }


        binding.tvSkipAdText.setOnClickListener({
            player.let {
                player!!.pause()
            }
            findNavController().navigateUp()
        })


    }

    private fun playAdVideo(videoUrl: String) {
        player = ExoPlayer.Builder(requireContext()).build().also { exoPlayer ->
            binding.playerView.player = exoPlayer
            val mediaItem = MediaItem.fromUri(videoUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
        startSeekBarUpdater(player!!)

    }

    private fun monitorAdWatchTime() {
        timeWatcher = AdvertiseTimeWatcher(
            targetSeconds = 12,
            timeProvider = { (player?.currentPosition?.div(1000))?.toInt() ?: 0 },
            onTimeReached = {
                binding.skipAdText.visibility = View.VISIBLE
                startCountdownToSkip()
            }
        )
        timeWatcher.start(viewLifecycleOwner.lifecycleScope)
    }

    private fun startCountdownToSkip() {
        timeWatcher.stop()
        lifecycleScope.launch {
            for (i in 3 downTo 1) {
                binding.skipAdText.text = "رد آگهی: $i"
                delay(1000)
            }
            binding.skipAdText.visibility = View.GONE
            binding.tvSkipAdText.visibility = View.VISIBLE
        }
    }


    private fun startSeekBarUpdater(player: ExoPlayer) {
        uiUpdateJob?.cancel()
        uiUpdateJob = lifecycleScope.launch {
            while (isActive) {
                val current = withContext(Dispatchers.Main) { player.currentPosition }
                val total = withContext(Dispatchers.Main) { player.duration }

                if (total > 0) {
                    withContext(Dispatchers.Main) {
                        binding.tvCurrentTime.text = formatTime(current)
                        binding.tvTotalTime.text = formatTime(total)
                        binding.adSeekBar.max = total.toInt()
                        binding.adSeekBar.progress = current.toInt()
                    }
                }

                delay(500L)
            }
        }
    }

    private fun formatTime(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    private fun canNavigateBack(): Boolean {
        return binding.tvSkipAdText.isVisible
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player?.release()
        player = null
        uiUpdateJob?.cancel()
    }
}