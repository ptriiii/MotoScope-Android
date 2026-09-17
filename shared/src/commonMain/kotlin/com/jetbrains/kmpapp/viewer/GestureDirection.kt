package com.motoscope.viewer

import kotlin.math.abs

internal enum class GestureDirection {
    Horizontal,
    Vertical,
}

/** Equal-axis movement yields to the parent vertical scroll. */
internal fun classifyGestureDirection(horizontal: Float, vertical: Float): GestureDirection =
    if (abs(horizontal) > abs(vertical)) {
        GestureDirection.Horizontal
    } else {
        GestureDirection.Vertical
    }
