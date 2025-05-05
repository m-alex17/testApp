package com.alex.testapp.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alex.testapp.R
import com.alex.testapp.data.Video
import com.alex.testapp.data.VideoItem
import com.alex.testapp.domain.manager.UserManager

class VideoAdapter(
    private val userManager: UserManager,
    private val onLikeClicked: (Video, Boolean) -> Unit,
    private val onVideoClicked: (Video) -> Unit
) : ListAdapter<VideoItem, VideoAdapter.VideoViewHolder>(VideoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun onViewRecycled(holder: VideoViewHolder) {
        super.onViewRecycled(holder)
        holder.release()
    }

    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val playerView: PlayerView = itemView.findViewById(R.id.playerView)
        private val playButton: ImageView = itemView.findViewById(R.id.playButton)
        private val durationText: TextView = itemView.findViewById(R.id.durationText)
        private val titleText: TextView = itemView.findViewById(R.id.titleText)
        private val heartIcon: ImageView = itemView.findViewById(R.id.heartIcon)
        private val usernameText: TextView = itemView.findViewById(R.id.usernameText)

        private var player: ExoPlayer? = null
        private var currentVideo: Video? = null
        private var isLiked = false


        init {
            playButton.setOnClickListener {
                currentVideo?.let {
                    onVideoClicked(it)
                }
            }
            playerView.setOnClickListener {
                currentVideo?.let {
                    onVideoClicked(it)
                }
            }
            heartIcon.setOnClickListener {
                currentVideo?.let {
                    isLiked = !isLiked
                    updateLikeIcon()
                    onLikeClicked(it, isLiked)
                }
            }
        }

        fun bind(videoItem: VideoItem) {
            currentVideo = videoItem.video
            isLiked = videoItem.isLiked

            titleText.text = videoItem.video.title
            durationText.text = formatDuration(videoItem.video.duration)

            usernameText.text = userManager.currentUser.value?.name

            updateLikeIcon()
            initializePlayer(videoItem.video.url)
        }

        private fun updateLikeIcon() {
            heartIcon.setImageResource(
                if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_empty
            )
        }

        private fun formatDuration(seconds: Double): String {
            var minutes = seconds.toInt() / 60
            var remainingSeconds = Math.ceil(seconds % 60).toInt()

            if (remainingSeconds == 60) {
                minutes += 1
                remainingSeconds = 0
            }

            return String.format("%02d:%02d", minutes, remainingSeconds)
        }

        private fun initializePlayer(videoUrl: String) {
            if (player != null) return

            releasePlayer()
            player = ExoPlayer.Builder(itemView.context).build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = false
                setMediaItem(MediaItem.fromUri(videoUrl))
                prepare()
            }
            playerView.player = player
            playerView.useController = false
        }

        private fun togglePlayback() {
            player?.let {
                if (it.isPlaying) {
                    it.pause()
                    playButton.visibility = View.VISIBLE
                } else {
                    it.play()
                    playButton.visibility = View.GONE
                }
            }
        }

        fun release() {
            releasePlayer()
        }

        private fun releasePlayer() {
            player?.let {
                it.release()
                player = null
            }
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

