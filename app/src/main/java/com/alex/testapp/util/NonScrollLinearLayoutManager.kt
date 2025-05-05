package com.alex.testapp.util

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager

class NonScrollLinearLayoutManager(context: Context) : LinearLayoutManager(context) {
    private var isScrollEnabled = false

    fun setScrollEnabled(enabled: Boolean) {
        isScrollEnabled = enabled
    }

    override fun canScrollVertically(): Boolean {
        return isScrollEnabled && super.canScrollVertically()
    }

    override fun canScrollHorizontally(): Boolean {
        return isScrollEnabled && super.canScrollHorizontally()
    }
}