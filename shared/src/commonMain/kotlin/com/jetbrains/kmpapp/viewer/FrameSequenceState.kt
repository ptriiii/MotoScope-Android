package com.motoscope.viewer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/** Small, platform-independent state machine for circular image-sequence scrubbing. */
class FrameSequenceState(
    val frameCount: Int,
    initialIndex: Int = 0,
) {
    private var fractionalFrameDelta = 0f

    var currentIndex: Int by mutableIntStateOf(
        if (frameCount > 0) floorMod(initialIndex, frameCount) else 0,
    )
        private set

    val isValid: Boolean get() = frameCount > 0

    fun increment() = advanceBy(1)

    fun decrement() = advanceBy(-1)

    fun advanceBy(delta: Int) {
        if (!isValid) return
        currentIndex = floorMod(currentIndex + delta, frameCount)
    }

    /** Applies a horizontal drag while retaining fractional movement between events. */
    fun onHorizontalDrag(horizontalDeltaPx: Float, viewerWidthPx: Float) {
        if (!isValid || frameCount == 1 || viewerWidthPx <= 0f) return
        fractionalFrameDelta += -(horizontalDeltaPx / viewerWidthPx) * frameCount
        val wholeFrameDelta = fractionalFrameDelta.toInt()
        if (wholeFrameDelta != 0) {
            fractionalFrameDelta -= wholeFrameDelta
            advanceBy(wholeFrameDelta)
        }
    }

    private fun floorMod(value: Int, modulus: Int): Int {
        val remainder = value % modulus
        return if (remainder < 0) remainder + modulus else remainder
    }
}
