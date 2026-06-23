package com.video.avd.utils

import androidx.media3.ui.DefaultTimeBar
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.video.avd.R

class CustomTimeBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : DefaultTimeBar(context, attrs, defStyleAttr) {

    private val bookmarkPositions = ArrayList<Long>()
    private var bookmarkMarker: Drawable? = null
    private var dotDrawable: Drawable? = null
    private val markerBounds = ArrayList<Rect>()
    private var onMarkerClickListener: ((Long) -> Unit)? = null

    private var durationMs: Long = 5000

    // Margins for edge markers and center markers
    private val maxMargin = 20f // Maximum margin near the edges
    private val minMargin = 5f // Minimum margin near the center

    // Touch padding to increase clickable area
    private val touchPadding = 20

    init {
        init()
        // Enable hardware layer for better GPU performance and to prevent rendering ANRs
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    private fun init() {
        bookmarkMarker = ContextCompat.getDrawable(context, R.drawable.ic_video_bookmark_new)
        dotDrawable = context.getDrawable(R.drawable.ic_video_bookmark_line)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Early return if no bookmarks to avoid unnecessary work
        if (bookmarkPositions.isEmpty()) {
            markerBounds.clear()
            return
        }
        
        // Cache frequently accessed values to avoid repeated calculations
        val drawableAreaStart = paddingLeft.toFloat()
        val drawableAreaEnd = (width - paddingRight).toFloat()
        val progressBarHeight = height / 2f
        
        // Clear marker bounds efficiently
        markerBounds.clear()
        markerBounds.ensureCapacity(bookmarkPositions.size)

        // Draw bookmark markers and dots in a single pass to reduce iterations
        bookmarkMarker?.let { marker ->
            dotDrawable?.let { dot ->
                val markerHeight = marker.intrinsicHeight.toFloat()
                val markerWidthHalf = marker.intrinsicWidth / 2
                val dotHeight = dot.intrinsicHeight.toFloat()
                val dotWidthHalf = dot.intrinsicWidth / 2
                val dotY = progressBarHeight - (dotHeight / 2)
                val markerY = progressBarHeight - markerHeight - 25

                for (position in bookmarkPositions) {
                    val x = getPositionToPixels(position)
                    val adjustedX = applyDynamicMargin(x, position)

                    // Only draw if within visible bounds
                    if (adjustedX >= drawableAreaStart && adjustedX <= drawableAreaEnd) {
                        // Draw dot marker
                        val dotLeft = (adjustedX - dotWidthHalf).toInt()
                        val dotTop = dotY.toInt()
                        dot.setBounds(
                            dotLeft,
                            dotTop,
                            (adjustedX + dotWidthHalf).toInt(),
                            (dotTop + dot.intrinsicHeight).toInt()
                        )
                        dot.draw(canvas)

                        // Draw bookmark marker
                        val markerLeft = (adjustedX - markerWidthHalf).toInt()
                        val markerTop = markerY.toInt()
                        marker.setBounds(
                            markerLeft,
                            markerTop,
                            (adjustedX + markerWidthHalf).toInt(),
                            (markerTop + marker.intrinsicHeight).toInt()
                        )
                        marker.draw(canvas)

                        // Store touch bounds for interaction
                        markerBounds.add(Rect(
                            markerLeft - touchPadding,
                            0,
                            (adjustedX + markerWidthHalf).toInt() + touchPadding,
                            height
                        ))
                    }
                }
            } ?: run {
                // Fallback if dotDrawable is null - only draw markers
                val markerHeight = marker.intrinsicHeight.toFloat()
                val markerWidthHalf = marker.intrinsicWidth / 2
                val markerY = progressBarHeight - markerHeight - 25

                for (position in bookmarkPositions) {
                    val x = getPositionToPixels(position)
                    val adjustedX = applyDynamicMargin(x, position)

                    if (adjustedX >= drawableAreaStart && adjustedX <= drawableAreaEnd) {
                        val markerLeft = (adjustedX - markerWidthHalf).toInt()
                        val markerTop = markerY.toInt()
                        marker.setBounds(
                            markerLeft,
                            markerTop,
                            (adjustedX + markerWidthHalf).toInt(),
                            (markerTop + marker.intrinsicHeight).toInt()
                        )
                        marker.draw(canvas)

                        markerBounds.add(Rect(
                            markerLeft - touchPadding,
                            0,
                            (adjustedX + markerWidthHalf).toInt() + touchPadding,
                            height
                        ))
                    }
                }
            }
        }
    }

    // Apply dynamic margins based on how far the marker is from the edges
    private fun applyDynamicMargin(x: Float, positionMs: Long): Float {
        val relativePosition = positionMs.toFloat() / durationMs.toFloat()

        // Interpolate margin size based on how far from the start or end the marker is
        val margin = if (relativePosition < 0.5f) {
            // Closer to the start
            val distanceFromStart = relativePosition / 0.5f
            minMargin + (maxMargin - minMargin) * (1 - distanceFromStart)
        } else {
            // Closer to the end
            val distanceFromEnd = (1 - relativePosition) / 0.5f
            minMargin + (maxMargin - minMargin) * (1 - distanceFromEnd)
        }

        // Apply margin to the marker position
        return if (relativePosition < 0.5f) {
            // Shift marker right by the calculated margin
            x + margin
        } else {
            // Shift marker left by the calculated margin
            x - margin
        }
    }

    fun setOnMarkerClickListener(listener: (Long) -> Unit) {
        onMarkerClickListener = listener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Check if the touch is on a marker
                val touchedMarker = findTouchedMarker(event.x, event.y)
                if (touchedMarker != null) {
                    // If a marker is touched, we'll handle it
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                // Check if the touch ended on a marker
                val touchedMarker = findTouchedMarker(event.x, event.y)
                if (touchedMarker != null) {
                    onMarkerClickListener?.invoke(touchedMarker)
                    return true
                }
            }
        }
        // If we didn't handle the event, pass it to the superclass
        return super.onTouchEvent(event)
    }

    private fun findTouchedMarker(x: Float, y: Float): Long? {
        for ((index, bounds) in markerBounds.withIndex()) {
            if (bounds.contains(x.toInt(), y.toInt())) {
                return bookmarkPositions[index]
            }
        }
        return null
    }

    fun setBookmarkPositions(positions: List<Long>, mediaDurationMs: Long) {
        bookmarkPositions.clear()
        bookmarkPositions.addAll(positions)
        durationMs = mediaDurationMs // Set the media duration
        // Use postInvalidateOnAnimation for smoother updates that sync with VSYNC
        postInvalidateOnAnimation()
    }

    // Correctly calculate the pixel position based on the duration and the width of the time bar, considering padding
    private fun getPositionToPixels(positionMs: Long): Float {
        // Calculate the drawable area, subtracting the padding on both sides
        val availableWidth = width - paddingLeft - paddingRight
        if (durationMs <= 0 || availableWidth <= 0) return 0f

        // Ensure correct scaling based on available width and duration
        val scale = (positionMs.toFloat() / durationMs.toFloat())
        val xPosition = paddingLeft + scale * availableWidth

        return xPosition // Start from paddingLeft to account for padding
    }

    fun removeMarker(positionMs: Long) {
        val removed = bookmarkPositions.remove(positionMs)
        // Use postInvalidateOnAnimation for smoother updates that sync with VSYNC
        postInvalidateOnAnimation()
    }
}