package com.alex.testapp.util

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Item decoration for adding vertical spacing between RecyclerView items
 * @param spacing The space to add between items (in pixels)
 */
class VerticalSpacingItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        // Skip adding space above the first item
        val position = parent.getChildAdapterPosition(view)
        if (position > 0) {
            outRect.top = spacing
        }
    }
}