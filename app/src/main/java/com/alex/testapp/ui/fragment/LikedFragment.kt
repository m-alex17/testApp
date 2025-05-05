package com.alex.testapp.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.alex.testapp.data.VideoItem
import com.alex.testapp.data.local.AppDatabase
import com.alex.testapp.data.local.UserPreferences
import com.alex.testapp.data.remot.RetrofitInstance
import com.alex.testapp.data.repository.UserRepository
import com.alex.testapp.data.repository.UserVideoLikeRepository
import com.alex.testapp.data.repository.VideoRepository
import com.alex.testapp.databinding.FragmentLikedBinding
import com.alex.testapp.domain.manager.UserManager
import com.alex.testapp.domain.usecase.GetLikedVideosUseCase
import com.alex.testapp.domain.usecase.ToggleVideoLikeUseCase
import com.alex.testapp.ui.adapter.VideoAdapter
import com.alex.testapp.ui.viewmodel.LikeViewModel
import com.alex.testapp.ui.viewmodel.LikedViewModelFactory
import com.alex.testapp.util.UIHelper
import com.alex.testapp.util.VideoSource
import kotlinx.coroutines.launch


class LikedFragment : Fragment() {
    private var _binding: FragmentLikedBinding? = null
    private val binding get() = _binding!!

    private lateinit var videoAdapter: VideoAdapter
    private lateinit var userManager:UserManager

    private lateinit var likeViewModel: LikeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userDao = AppDatabase.getDatabase(requireContext()).userDao()
        val videoDao = AppDatabase.getDatabase(requireContext()).videoDao()
        val userVideoLikedDao = AppDatabase.getDatabase(requireContext()).userVideoLikeDao()

        val userRepository = UserRepository(userDao)
        val userPreferences = UserPreferences(requireContext())

        userManager = UserManager.getInstance(Pair(userRepository, userPreferences))

        val videoRepository = VideoRepository(videoDao, RetrofitInstance.videoApiService)

        val likedFactory = LikedViewModelFactory(getLikedVideosUseCase= GetLikedVideosUseCase(
            UserVideoLikeRepository(userVideoLikedDao), userManager
        ), videoRepository = videoRepository,
            toggleLikeUseCase = ToggleVideoLikeUseCase(UserVideoLikeRepository(userVideoLikedDao),userManager))
        likeViewModel = ViewModelProvider(requireActivity(), likedFactory).get(LikeViewModel::class.java)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentLikedBinding.inflate(inflater, container, false)
        return binding.root    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        UIHelper.applyTransparentStatusBar(requireActivity(), binding.root)

        binding.arrowBack.setOnClickListener {
            findNavController().navigateUp()
        }


        videoAdapter = VideoAdapter(userManager, onLikeClicked = { video, liked ->
            viewLifecycleOwner.lifecycleScope.launch {
                likeViewModel.toggleLike(video)
            }
        }, onVideoClicked = { video ->
            val currentList = videoAdapter.currentList
            val user = userManager.currentUser.value
            val clickedIndex = currentList.indexOfFirst { it.video.id == video.id }
            val action = LikedFragmentDirections.actionLikedFragmentToVideoPlayerFragment(
              user?.name.toString(),
                clickedIndex,
                VideoSource.Liked.name)
            findNavController().navigate(action)
        })

        binding.contentRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = videoAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                likeViewModel.likedVideos.collect { videos ->
                    val videoItems = videos.map { video -> VideoItem(video, isLiked = true, currentUserId = userManager.currentUserId.value) }
                    videoAdapter.submitList(videoItems)
                }
            }
        }

        likeViewModel.loadLikedVideos()

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