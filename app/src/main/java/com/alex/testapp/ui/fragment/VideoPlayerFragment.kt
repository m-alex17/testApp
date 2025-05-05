package com.alex.testapp.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alex.testapp.data.VideoItem
import com.alex.testapp.data.local.AppDatabase
import com.alex.testapp.data.local.UserPreferences
import com.alex.testapp.data.remot.RetrofitInstance
import com.alex.testapp.data.repository.UserRepository
import com.alex.testapp.data.repository.UserVideoLikeRepository
import com.alex.testapp.data.repository.VideoRepository
import com.alex.testapp.databinding.FragmentVideoPlayerBinding
import com.alex.testapp.domain.manager.UserManager
import com.alex.testapp.domain.usecase.GetLikedVideosUseCase
import com.alex.testapp.domain.usecase.GetVideosUseCase
import com.alex.testapp.domain.usecase.SwitchUserUseCase
import com.alex.testapp.domain.usecase.ToggleVideoLikeUseCase
import com.alex.testapp.ui.adapter.PlaybackListener
import com.alex.testapp.ui.adapter.SingleVideoAdapter
import com.alex.testapp.ui.adapter.VideoPlayListener
import com.alex.testapp.ui.viewmodel.LikeViewModel
import com.alex.testapp.ui.viewmodel.LikedViewModelFactory
import com.alex.testapp.ui.viewmodel.UserSelectionViewModel
import com.alex.testapp.ui.viewmodel.UserSelectionViewModelFactory
import com.alex.testapp.ui.viewmodel.VideoViewModel
import com.alex.testapp.ui.viewmodel.VideoViewModelFactory
import com.alex.testapp.util.NetworkUtils
import com.alex.testapp.util.NonScrollLinearLayoutManager
import com.alex.testapp.util.VideoSource
import com.alex.testapp.util.VideoWatchTracker
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch


class VideoPlayerFragment : Fragment(), VideoPlayListener {
    private var _binding: FragmentVideoPlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var videoTracker: VideoWatchTracker


    private lateinit var userManager: UserManager

    private lateinit var viewModel: UserSelectionViewModel
    private lateinit var videoViewModel: VideoViewModel
    private lateinit var likeViewModel: LikeViewModel

    private lateinit var videoList: List<VideoItem>
    private var likedVideoList: List<VideoItem> = emptyList()

    private lateinit var videoAdapter: SingleVideoAdapter
    private var currentPlayingHolder: SingleVideoAdapter.VideoViewHolder? = null

    private lateinit var userName: String
    private lateinit var userRepository: UserRepository

    private var videoSource: VideoSource = VideoSource.Home

    var currentPos = 0
    private var currentIndex: Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userDao = AppDatabase.getDatabase(requireContext()).userDao()
        val videoDao = AppDatabase.getDatabase(requireContext()).videoDao()
        val userVideoLikedDao = AppDatabase.getDatabase(requireContext()).userVideoLikeDao()

        userRepository = UserRepository(userDao)
        val userPreferences = UserPreferences(requireContext())

        userManager = UserManager.getInstance(Pair(userRepository, userPreferences))
        val switchUserUseCase = SwitchUserUseCase(userManager, userPreferences)

        val factory = UserSelectionViewModelFactory(userManager, switchUserUseCase)
        viewModel =
            ViewModelProvider(requireActivity(), factory).get(UserSelectionViewModel::class.java)

        val videoRepository = VideoRepository(videoDao, RetrofitInstance.videoApiService)
        val videoFactory = VideoViewModelFactory(
            GetVideosUseCase(videoRepository),
            ToggleVideoLikeUseCase(
                UserVideoLikeRepository(userVideoLikedDao), userManager,
            ),
            videoRepository,
            GetLikedVideosUseCase(UserVideoLikeRepository(userVideoLikedDao), userManager)
        )
        videoViewModel =
            ViewModelProvider(requireActivity(), videoFactory).get(VideoViewModel::class.java)

        val likedFactory = LikedViewModelFactory(
            getLikedVideosUseCase = GetLikedVideosUseCase(
                UserVideoLikeRepository(userVideoLikedDao), userManager
            ), videoRepository = videoRepository,
            toggleLikeUseCase = ToggleVideoLikeUseCase(
                UserVideoLikeRepository(userVideoLikedDao),
                userManager
            )
        )
        likeViewModel =
            ViewModelProvider(requireActivity(), likedFactory).get(LikeViewModel::class.java)


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentVideoPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }


    private val recyclerViewScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
            val totalItemCount = layoutManager.itemCount
            val firstVisibleItem = layoutManager.findFirstCompletelyVisibleItemPosition()

            when (videoSource) {
                VideoSource.Home -> {
                    if (lastVisibleItem >= totalItemCount - 2 && !videoViewModel.isEndReached()) {
                        safeCall {
                            if (!videoViewModel.isEndReached()) {
                                videoViewModel.loadNextVideos()
                            }

                        }
                    }
                }

                VideoSource.Liked -> Unit
            }

            if (firstVisibleItem != RecyclerView.NO_POSITION && firstVisibleItem != currentPos) {
                currentPos = firstVisibleItem
                pauseOtherVideosExcept(currentPos)
            }
        }
    }

    inner class AppBackgroundObserver : DefaultLifecycleObserver {

        override fun onPause(owner: LifecycleOwner) {
            currentPlayingHolder?.pausePlayback()
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        arguments?.let {
            userName = it.getString("userName", "Unknown User")
            currentIndex = it.getInt("selectedIndex")
            val sourceString = arguments?.getString("videoSource") ?: "Home"
            videoSource = VideoSource.valueOf(sourceString)


        }

        videoTracker = VideoWatchTracker(
            viewLifecycleOwner.lifecycleScope,
            userRepository
        )


        lifecycleScope.launch {
            videoTracker.getWatchedCountFlow(userManager.currentUserId.value)
                .collectLatest { count ->
                    Log.e("VideoTracker", "User watched $count videos")
                    val adIndex = getAdIndexForWatchedCount(count)
                    adIndex?.let { index ->
                        Log.e("AdLogic", "Show Ad $index")
                    }
                }
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(AppBackgroundObserver())


        videoAdapter = SingleVideoAdapter(
            this,
            playbackListener = object : PlaybackListener {
                override fun setCurrentPlaying(holder: SingleVideoAdapter.VideoViewHolder) {
                    currentPlayingHolder = holder
                }
            },
            currentUser = userName,
            onLikeClicked = { video, liked ->
                // handle like
                viewLifecycleOwner.lifecycleScope.launch {
                    videoViewModel.toggleLike(video)
                }
            },
            onVideoClicked = { video ->

            },
            navController = findNavController(),
            onNextClicked = { goToNextVideo() },
            onPrevClicked = { goToPreviousVideo() },
        )

        binding.contentRecyclerView.apply {
            layoutManager =
                NonScrollLinearLayoutManager(requireContext()).apply {
                    orientation = LinearLayoutManager.VERTICAL
                }
            adapter = videoAdapter
            addOnScrollListener(recyclerViewScrollListener)
        }


        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (videoSource == VideoSource.Liked) {
                    likeViewModel.likedVideos.collect { likedVideos ->
                        val likedItems = likedVideos.map { video ->
                            VideoItem(
                                video = video,
                                isLiked = true,
                                currentUserId = userManager.currentUserId.value
                            )
                        }
                        likedVideoList = likedItems
                        videoAdapter.submitList(likedVideoList) {
                            binding.contentRecyclerView.adapter?.notifyDataSetChanged()
                            if (currentIndex in likedVideoList.indices) {
                                binding.contentRecyclerView.scrollToPosition(currentIndex)
                                binding.contentRecyclerView.adapter?.notifyItemChanged(currentIndex)
                            }
                        }
                    }
                } else {
                    combine(
                        videoViewModel.videoList,
                        videoViewModel.likedVideoIds
                    ) { videos, likedIds ->
                        videos.map { video ->
                            VideoItem(
                                video = video,
                                isLiked = likedIds.contains(video.id),
                                currentUserId = userManager.currentUserId.value
                            )
                        }
                    }.collect { videoItems ->
                        videoList = videoItems
                        videoAdapter.submitList(videoList) {
                            binding.contentRecyclerView.adapter?.notifyDataSetChanged()
                        }
                        if (currentIndex in videoItems.indices) {
                            binding.contentRecyclerView.scrollToPosition(currentIndex)
                            binding.contentRecyclerView.adapter?.notifyItemChanged(currentIndex)
                        }
                    }
                }
            }
        }

    }

    private fun goToNextVideo() {
        when (videoSource) {
            VideoSource.Home -> {
                if (currentIndex < videoList.size - 1) {
                    currentIndex++
                    binding.contentRecyclerView.scrollToPosition(currentIndex)
                }
            }

            VideoSource.Liked -> {
                if (currentIndex < likedVideoList.size - 1) {
                    currentIndex++
                    binding.contentRecyclerView.scrollToPosition(currentIndex)
                }
            }
        }
    }

    private fun goToPreviousVideo() {

        when (videoSource) {
            VideoSource.Home -> {
                if (currentIndex > 0) {
                    currentIndex--
                    binding.contentRecyclerView.scrollToPosition(currentIndex)
                }
            }

            VideoSource.Liked -> {
                if (currentIndex > 0) {
                    currentIndex--
                    binding.contentRecyclerView.scrollToPosition(currentIndex)
                }
            }
        }
    }

    fun Fragment.safeCall(
        onNoNetwork: () -> Unit = {
            Toast.makeText(requireContext(), "به اینترنت اتصال ندارید", Toast.LENGTH_SHORT).show()
        },
        block: () -> Unit
    ) {
        if (NetworkUtils.isNetworkAvailable(requireContext())) {
            block()
        } else {
            onNoNetwork()
        }
    }

    private fun pauseOtherVideosExcept(position: Int) {
        for (i in 0 until videoAdapter.itemCount) {
            if (i != position) {
                val holder =
                    binding.contentRecyclerView.findViewHolderForAdapterPosition(i) as? SingleVideoAdapter.VideoViewHolder
                holder?.pausePlayback()
            }
        }
    }


    override fun onResume() {
        super.onResume()
        videoViewModel.loadLikedVideos()
    }


    override fun onDestroyView() {
        val layoutManager = binding.contentRecyclerView.layoutManager as? LinearLayoutManager
        layoutManager?.let {
            val firstVisible = it.findFirstVisibleItemPosition()
            val lastVisible = it.findLastVisibleItemPosition()
            for (i in firstVisible..lastVisible) {
                val holder = binding.contentRecyclerView.findViewHolderForLayoutPosition(i)
                if (holder is SingleVideoAdapter.VideoViewHolder) {
                    holder.release()
                }
            }
        }

        binding.contentRecyclerView.removeOnScrollListener(recyclerViewScrollListener)
        binding.contentRecyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }

    override fun onVideoStarted(currentUserId: Int, videoId: Int) {
        Log.e("VideoTracker", "onVideoStarted - User ID: $currentUserId, Video ID: $videoId")
        videoTracker.onVideoStarted(currentUserId, videoId)
    }

    override fun onVideoStopped(currentUserId: Int, videoId: Int) {
        Log.e("VideoTracker", "onVideoStopped - User ID: $currentUserId, Video ID: $videoId")
        videoTracker.onVideoStopped(currentUserId, videoId)

    }

    // Returns the ad index to show or null if none
    private fun getAdIndexForWatchedCount(watchedCount: Int): Int? {
        if (watchedCount == 0) return null

        // Ads after every 4 videos
        if (watchedCount % 4 != 0) return null

        val group = watchedCount / 4

        return when {
            group in 1..3 -> 0        // First ad
            group == 4 -> 1           // Second ad
            group == 5 -> 2           // Third ad
            group >= 6 -> (group - 6) % 3 // Rotate ads after 20 videos
            else -> null
        }
    }

}