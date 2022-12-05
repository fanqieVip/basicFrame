package com.frame.basic.base.ktx

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

/**
 * 自动优化图片加载性能
 */
fun RecyclerView.optimizeImage() {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            when (newState) {
                RecyclerView.SCROLL_STATE_IDLE, RecyclerView.SCROLL_STATE_DRAGGING -> {
                    Glide.with(recyclerView.context).apply {
                        if (isPaused) {
                            resumeRequests()
                        }
                    }
                }
                else -> {
                    Glide.with(recyclerView.context).apply {
                        if (!isPaused) {
                            pauseRequests()
                        }
                    }
                }
            }
        }
    })
}
