package com.go.outlooking.vpn.view

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GridSpacingItemDecoration(context: Context,
                                spanCount: Int,
                                spacing: Int,
                                includeEdge: Boolean) : RecyclerView.ItemDecoration() {


    private var spanCount = 0
    private var spacing = 0
    private var includeEdge = false

    init {
        this.spanCount = spanCount
        this.spacing = dip2px(spacing.toFloat(), context)
        this.includeEdge = includeEdge
    }


    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val column = position % spanCount
        if (includeEdge) {
            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount
            if (position < spanCount) {
                outRect.top = spacing
            }
            outRect.bottom = spacing
        } else {
            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount
            if (position < spanCount) {
                outRect.top = spacing
            }
            outRect.bottom = spacing
        }
    }



    companion object {

        fun dip2px(dpValue: Float, context: Context): Int {
            val scale: Float = context.resources.displayMetrics.density
            return (dpValue * scale + 0.5f).toInt()
        }
    }


}