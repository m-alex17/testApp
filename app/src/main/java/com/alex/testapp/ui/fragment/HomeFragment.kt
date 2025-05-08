package com.alex.testapp.ui.fragment

import UsersBottomSheetFragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alex.testapp.R
import com.alex.testapp.data.User
import com.alex.testapp.data.VideoItem
import com.alex.testapp.data.local.AppDatabase
import com.alex.testapp.data.local.UserPreferences
import com.alex.testapp.data.remot.RetrofitInstance
import com.alex.testapp.data.repository.UserRepository
import com.alex.testapp.data.repository.UserVideoLikeRepository
import com.alex.testapp.data.repository.VideoRepository
import com.alex.testapp.databinding.FragmentHomeBinding
import com.alex.testapp.domain.manager.UserManager
import com.alex.testapp.domain.usecase.GetLikedVideosUseCase
import com.alex.testapp.domain.usecase.GetVideosUseCase
import com.alex.testapp.domain.usecase.SwitchUserUseCase
import com.alex.testapp.domain.usecase.ToggleVideoLikeUseCase
import com.alex.testapp.ui.adapter.SingleVideoAdapter
import com.alex.testapp.ui.adapter.VideoAdapter
import com.alex.testapp.ui.viewmodel.UserSelectionViewModel
import com.alex.testapp.ui.viewmodel.UserSelectionViewModelFactory
import com.alex.testapp.ui.viewmodel.VideoViewModel
import com.alex.testapp.ui.viewmodel.VideoViewModelFactory
import com.alex.testapp.util.NetworkUtils
import com.alex.testapp.util.UIHelper
import com.alex.testapp.util.VideoSource
import com.alex.testapp.util.VideoWatchTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var userManager: UserManager
    private lateinit var switchUserUseCase: SwitchUserUseCase
    private lateinit var viewModel: UserSelectionViewModel
    private lateinit var videoViewModel: VideoViewModel
    var currentPos = 0
    private var scrollStateJob: Job? = null
    private lateinit var videoAdapter: VideoAdapter
    private lateinit var currentUser: User
    private var clickedIndex: Int = 0

    private var videoItems = emptyList<VideoItem>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userDao = AppDatabase.getDatabase(requireContext()).userDao()
        val videoDao = AppDatabase.getDatabase(requireContext()).videoDao()
        val userVideoLikedDao = AppDatabase.getDatabase(requireContext()).userVideoLikeDao()

        val userRepository = UserRepository(userDao)
        val userPreferences = UserPreferences(requireContext())

        userManager = UserManager.getInstance(Pair(userRepository, userPreferences))
        switchUserUseCase = SwitchUserUseCase(userManager, userPreferences)

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

    }


    private val recyclerViewScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
            val totalItemCount = layoutManager.itemCount
            val firstVisibleItem = layoutManager.findFirstCompletelyVisibleItemPosition()

            scrollStateJob?.cancel()

            scrollStateJob = lifecycleScope.launch {
                delay(200) // debounce for 200ms

                if (lastVisibleItem >= totalItemCount - 2 && !videoViewModel.isEndReached()) {
                    safeCall {
                        videoViewModel.loadNextVideos()
                    }
                }
            }

            if (firstVisibleItem != RecyclerView.NO_POSITION && firstVisibleItem != currentPos) {
                currentPos = firstVisibleItem
                pauseOtherVideosExcept(currentPos)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        UIHelper.applyTransparentStatusBar(requireActivity(), binding.root)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                userManager.currentUser.collect { user ->
                    user?.let {
                        currentUser = user
                        binding.tvUsername.text = it.name
                        videoAdapter.notifyDataSetChanged()
                    }
                }
            }
        }

        binding.userProfileButton.setOnClickListener {
            binding.userProfileButton.setBackgroundResource(R.drawable.ic_light_rounded_button)
            lifecycleScope.launch {
                val users = userManager.getUsers()
                showUsersBottomSheet(users)
            }
        }

        binding.likesButton.setOnClickListener {
            navigateLikedFragment()
        }

        viewModel.bottomSheetDismissed.observe(viewLifecycleOwner) { dismissed ->
            if (dismissed) {
                binding.userProfileButton.setBackgroundResource(R.drawable.ic_dark_rounded_button)
            }
        }

        viewModel.selectedUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                handleSelectedUser(it)
            }
        }

        videoAdapter = VideoAdapter(userManager, onLikeClicked = { video, liked ->
            viewLifecycleOwner.lifecycleScope.launch {
                videoViewModel.toggleLike(video)
            }
        }, onVideoClicked = { video ->
            val currentList = videoAdapter.currentList
            clickedIndex = currentList.indexOfFirst { it.video.id == video.id }
            currentUser.let { user ->
                navigateVideoPlayerFragment(user.name, clickedIndex)
            }
        })

        binding.contentRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = videoAdapter
            addOnScrollListener(recyclerViewScrollListener)
        }



        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    videoViewModel.videoList,
                    videoViewModel.likedVideoIds,
                ) { videos, likedIds ->
                    videos.map { video ->
                        VideoItem(
                            video = video,
                            isLiked = likedIds.contains(video.id),
                            currentUserId = userManager.currentUserId.value
                        )
                    }
                }.distinctUntilChanged()
                    .collect { items ->
                    videoItems = items
                    videoAdapter.submitList(videoItems)
                }
            }
        }


        safeCall {
            if (videoViewModel.videoList.value.isEmpty()) {
                videoViewModel.loadNextVideos()
                videoViewModel.loadLikedVideos()
            }
        }
    }

    private fun handleSelectedUser(user: User) {
        binding.tvUsername.text = user.name
        viewModel.onUserSelected(user.id)
        videoViewModel.loadLikedVideos()
        VideoWatchTracker.loadWatchCount(userManager.currentUserId.value)
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

    private fun showUsersBottomSheet(users: List<User>) {
        val bottomSheet = UsersBottomSheetFragment.newInstance(users)
        bottomSheet.show(parentFragmentManager, UsersBottomSheetFragment.TAG)
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

    private fun navigateLikedFragment() {
        if (isAdded && getView() != null) {
            val action = HomeFragmentDirections.actionHomeFragmentToLikedFragment()
            findNavController().navigate(action)
        }
    }

    private fun navigateVideoPlayerFragment(username: String, selectedIndex: Int) {
        if (isAdded && getView() != null) {
            val action = HomeFragmentDirections.actionHomeFragmentToVideoPlayerFragment(
                username,
                selectedIndex,
                VideoSource.Home.name
            )
            findNavController().navigate(action)
        }
    }

    override fun onResume() {
        super.onResume()
        videoViewModel.loadLikedVideos()
        VideoWatchTracker.loadWatchCount(userManager.currentUserId.value)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        for (i in 0 until binding.contentRecyclerView.childCount) {
            val holder = binding.contentRecyclerView.findViewHolderForAdapterPosition(i)
            if (holder is VideoAdapter.VideoViewHolder) {
                holder.release()
            }
        }
        binding.contentRecyclerView.adapter = null
        _binding = null
    }
}