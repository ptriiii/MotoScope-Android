package com.motoscope.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun MotorcycleViewer(
    modifier: Modifier = Modifier,
) {
    ImageSequenceViewer(
        frames = click125StandardFrames,
        contentDescription = "Honda Click125 Standard motorcycle",
        modifier = modifier,
    )
}

@Composable
fun ImageSequenceViewer(
    frames: List<DrawableResource>,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val state = remember(frames) { FrameSequenceState(frames.size) }
    if (!state.isValid) {
        Box(modifier = modifier.fillMaxWidth().aspectRatio(1.536f))
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(maxHeight = 520.dp)
            .aspectRatio(2048f / 1332f)
            .semantics(mergeDescendants = true) {
                this.contentDescription = "$contentDescription, angle ${state.currentIndex + 1} of ${frames.size}"
                customActions = listOf(
                    CustomAccessibilityAction("Rotate motorcycle left") {
                        state.decrement()
                        true
                    },
                    CustomAccessibilityAction("Rotate motorcycle right") {
                        state.increment()
                        true
                    },
                )
            }
            .pointerInput(frames) {
                val touchSlop = viewConfiguration.touchSlop

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var lastPosition = down.position
                    var direction: GestureDirection? = null

                    while (direction == null) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break

                        val totalDeltaX = change.position.x - down.position.x
                        val totalDeltaY = change.position.y - down.position.y
                        lastPosition = change.position

                        if (kotlin.math.abs(totalDeltaX) > touchSlop ||
                            kotlin.math.abs(totalDeltaY) > touchSlop
                        ) {
                            direction = classifyGestureDirection(totalDeltaX, totalDeltaY)
                            if (direction == GestureDirection.Vertical) {
                                // The parent verticalScroll has the gesture. Nothing has been
                                // consumed by this child before direction was established.
                                return@awaitEachGesture
                            }

                            change.consume()
                            state.onHorizontalDrag(totalDeltaX, size.width.toFloat())
                        }
                    }

                    if (direction == GestureDirection.Horizontal) {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break

                            val horizontalDelta = change.position.x - lastPosition.x
                            lastPosition = change.position
                            if (horizontalDelta != 0f) {
                                change.consume()
                                state.onHorizontalDrag(horizontalDelta, size.width.toFloat())
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(frames[state.currentIndex]),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
            contentScale = ContentScale.Fit,
        )
    }
}
