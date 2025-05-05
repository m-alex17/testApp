package com.alex.testapp.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alex.testapp.R
import com.alex.testapp.data.Video
import com.alex.testapp.data.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


interface PlaybackListener {
    fun setCurrentPlaying(holder: SingleVideoAdapter.VideoViewHolder)
}

interface VideoPlayListener {
    fun onVideoStarted(currentUserId: Int, videoId: Int)
    fun onVideoStopped(currentUserId: Int, videoId: Int)
}

class SingleVideoAdapter(
    private val videoPlayListener: VideoPlayListener,
    private val playbackListener: PlaybackListener,
    private val currentUser: String,
    private val onLikeClicked: (Video, Boolean) -> Unit,
    private val onVideoClicked: (Video) -> Unit,
    private val navController: NavController,
    private val onNextClicked: () -> Unit,
    private val onPrevClicked: () -> Unit,
) : ListAdapter<VideoItem, SingleVideoAdapter.VideoViewHolder>(VideoDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_play, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, position, currentList.size)
    }

    override fun onViewRecycled(holder: VideoViewHolder) {
        super.onViewRecycled(holder)
        holder.release()
    }


    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private lateinit var playerView: PlayerView
        private lateinit var playButton: ImageView
        private lateinit var titleText: TextView
        private lateinit var heartIcon: ImageView
        private lateinit var usernameText: TextView
        private lateinit var startTime: TextView
        private lateinit var seekBar: SeekBar
        private lateinit var endTime: TextView
        private lateinit var nextButton: ImageView
        private lateinit var prevButton: ImageView
        private lateinit var backButton: ImageView
        private lateinit var playerContainer: FrameLayout

        private var progressJob: Job? = null
        private fun formatTime(ms: Long): String {
            val totalSeconds = ms / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }


        private fun initViews() {
            playerView = itemView.findViewById(R.id.playerView)
            playButton = itemView.findViewById(R.id.playButton)
            titleText = itemView.findViewById(R.id.titleText)
            heartIcon = itemView.findViewById(R.id.heartIcon)
            usernameText = itemView.findViewById(R.id.usernameText)
            startTime = itemView.findViewById(R.id.startTime)
            seekBar = itemView.findViewById(R.id.seekBar)
            endTime = itemView.findViewById(R.id.endTime)
            nextButton = itemView.findViewById(R.id.nextButton)
            prevButton = itemView.findViewById(R.id.prevButton)
            backButton = itemView.findViewById(R.id.backButton)
            playerContainer = itemView.findViewById(R.id.playerContainer)

        }

        private var player: ExoPlayer? = null
        private var currentVideo: Video? = null
        private var isLiked = false

        private var currentVideoUrl: String? = null

        init {
            initViews()
            playButton.setOnClickListener {
                currentVideo?.let {
                    onVideoClicked(it)
                }
                togglePlayback()
            }

            heartIcon.setOnClickListener {
                currentVideo?.let {
                    isLiked = !isLiked
                    updateLikeIcon()
                    onLikeClicked(it, isLiked)
                }
            }

            backButton.setOnClickListener {
                navController.popBackStack()
            }
            nextButton.setOnClickListener {
                onNextClicked()
                player?.let {
                    if (it.isPlaying) {
                        it.pause()
                        playButton.setImageResource(R.drawable.ic_play_button)
                    }
                }
            }

            prevButton.setOnClickListener {
                onPrevClicked()
                player?.let {
                    if (it.isPlaying) {
                        it.pause()
                        playButton.setImageResource(R.drawable.ic_play_button)
                    }
                }
            }


        }

        fun pausePlayback() {
            player?.pause()
            playButton.setImageResource(R.drawable.ic_play_button)
            playButton.visibility = View.VISIBLE
        }

        fun bind(videoItem: VideoItem, position: Int, itemCount: Int) {
            currentVideo = videoItem.video
            isLiked = videoItem.isLiked


            titleText.text = videoItem.video.title
            currentUser.let {
                usernameText.text = currentUser
            }

            prevButton.visibility = if (position == 0) View.GONE else View.VISIBLE
            nextButton.visibility = if (position == itemCount - 1) View.GONE else View.VISIBLE

            updateLikeIcon()
            if (currentVideoUrl != videoItem.video.url) {
                releasePlayer()
                currentVideoUrl = videoItem.video.url
                initializePlayer(videoItem.video.url, videoItem.currentUserId, videoItem.video.id)
            }

            playButton.setImageResource(
                if (player?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play_button
            )
        }

        private fun updateLikeIcon() {
            heartIcon.setImageResource(
                if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_empty
            )
        }


        private fun initializePlayer(videoUrl: String, currentUserId: Int, videoId: Int) {
            player = ExoPlayer.Builder(itemView.context).build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = false
                setMediaItem(MediaItem.fromUri(videoUrl))

                addListener(object : Player.Listener {

                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_BUFFERING -> {
                            }

                            Player.STATE_READY -> {
                                if (isPlaying) {
                                    startProgressUpdates()
                                }
                            }

                            Player.STATE_ENDED -> {
                                playButton.post {
                                    playButton.setImageResource(R.drawable.ic_play_button)
                                    playButton.visibility = View.VISIBLE
                                }
                                progressJob?.cancel()
                            }
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying) {
                            Log.e("Playing", "playing video")
                            startProgressUpdates()
                            videoPlayListener.onVideoStarted(currentUserId, videoId)
                        } else {
                            Log.e("PlayingNot", "NOT playing")
                            progressJob?.cancel()
                            videoPlayListener.onVideoStopped(currentUserId, videoId)
                        }
                    }
                })

                prepare()
            }
            playerView.player = player
            playerView.useController = false
        }

        private fun startProgressUpdates() {
            progressJob?.cancel()
            progressJob = CoroutineScope(Dispatchers.Main).launch {
                while (isActive && player?.isPlaying == true) {
                    val position = player?.currentPosition ?: 0
                    val duration = player?.duration ?: 0

                    if (duration > 0) {
                        seekBar.max = duration.toInt()
                        seekBar.progress = position.toInt()
                        startTime.text = formatTime(position)
                        endTime.text = formatTime(duration)
                    }
                    delay(500)
                }
            }
        }


        private fun togglePlayback() {
            player?.let {
                if (it.playbackState == Player.STATE_ENDED) {
                    if (it.currentPosition >= it.duration) {
                        it.seekTo(0)
                    }
                    it.play()
                    playButton.setImageResource(R.drawable.ic_pause)
                } else
                if (it.isPlaying) {
                    it.pause()
                    playButton.setImageResource(R.drawable.ic_play_button)
                } else {
                    it.play()
                    playButton.setImageResource(R.drawable.ic_pause)
                    playbackListener.setCurrentPlaying(this)
                }
            }
        }

        fun release() {
            progressJob?.cancel()
            player?.let {
                player?.release()
                player = null
            }
        }

        private fun releasePlayer() {
            progressJob?.cancel()
            player?.let {
                player?.release()
                player = null
            }
        }
    }

    class VideoDiffCallback : DiffUtil.ItemCallback<VideoItem>() {
        override fun areItemsTheSame(oldItem: VideoItem, newItem: VideoItem): Boolean {
            return oldItem.video.id == newItem.video.id
        }

        override fun areContentsTheSame(oldItem: VideoItem, newItem: VideoItem): Boolean {
            return oldItem == newItem
        }
    }
}
