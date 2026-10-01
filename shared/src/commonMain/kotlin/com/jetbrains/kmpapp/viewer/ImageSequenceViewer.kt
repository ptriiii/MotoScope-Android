package com.motoscope.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun MotorcycleViewer(
    modifier: Modifier = Modifier,
    gestureModifier: Modifier = Modifier.fillMaxSize(),
    rotationReferenceWidthPx: Float? = null,
    onHorizontalIntentLocked: () -> Unit = {},
) {
    ImageSequenceViewer(
        frames = click125StandardFrames,
        contentDescription = "Honda Click125 Standard motorcycle",
        initialFrameIndex = 26,
        modifier = modifier,
        gestureModifier = gestureModifier,
        rotationReferenceWidthPx = rotationReferenceWidthPx,
        onHorizontalIntentLocked = onHorizontalIntentLocked,
    )
}

@Composable
fun ImageSequenceViewer(
    frames: List<DrawableResource>,
    contentDescription: String,
    initialFrameIndex: Int = 0,
    modifier: Modifier = Modifier,
    gestureModifier: Modifier = Modifier.fillMaxSize(),
    rotationReferenceWidthPx: Float? = null,
    onHorizontalIntentLocked: () -> Unit = {},
) {
    ImageSequenceViewerCore(
        frameCount = frames.size,
        contentDescription = contentDescription,
        initialFrameIndex = initialFrameIndex,
        modifier = modifier,
        gestureModifier = gestureModifier,
        rotationReferenceWidthPx = rotationReferenceWidthPx,
        onHorizontalIntentLocked = onHorizontalIntentLocked,
        stateKey = frames,
        canvasAspectRatio = 2048f / 1332f,
    ) { index ->
        Image(
            painter = painterResource(frames[index]),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().clearAndSetSemantics {},
            contentScale = ContentScale.Fit,
        )
    }
}

/** Same frame state, gestures, and semantics for platform-provided image sources. */
@Composable
fun ImageSequenceViewerContent(
    frameCount: Int,
    contentDescription: String,
    initialFrameIndex: Int = 0,
    modifier: Modifier = Modifier,
    gestureModifier: Modifier = Modifier.fillMaxSize(),
    rotationReferenceWidthPx: Float? = null,
    onHorizontalIntentLocked: () -> Unit = {},
    canvasAspectRatio: Float = 2048f / 1332f,
    frameContent: @Composable (Int) -> Unit,
) {
    ImageSequenceViewerCore(
        frameCount = frameCount,
        contentDescription = contentDescription,
        initialFrameIndex = initialFrameIndex,
        modifier = modifier,
        gestureModifier = gestureModifier,
        rotationReferenceWidthPx = rotationReferenceWidthPx,
        onHorizontalIntentLocked = onHorizontalIntentLocked,
        stateKey = frameCount,
        canvasAspectRatio = canvasAspectRatio,
        frameContent = frameContent,
    )
}

@Composable
private fun ImageSequenceViewerCore(
    frameCount: Int,
    contentDescription: String,
    initialFrameIndex: Int,
    modifier: Modifier,
    gestureModifier: Modifier,
    rotationReferenceWidthPx: Float?,
    onHorizontalIntentLocked: () -> Unit,
    stateKey: Any,
    canvasAspectRatio: Float,
    frameContent: @Composable (Int) -> Unit,
) {
    val state = remember(stateKey, initialFrameIndex) {
        FrameSequenceState(frameCount, initialFrameIndex)
    }
    val onHorizontalIntentLockedState = rememberUpdatedState(onHorizontalIntentLocked)
    if (!state.isValid) {
        Box(modifier = modifier.fillMaxWidth().aspectRatio(1.30f))
        return
    }

    Box(
        modifier = modifier.aspectRatio(canvasAspectRatio),
    ) {
        frameContent(state.currentIndex)
        Box(
            modifier = gestureModifier
                .semantics(mergeDescendants = true) {
                    this.contentDescription = "$contentDescription, angle ${state.currentIndex + 1} of $frameCount"
                    customActions = listOf(
                        CustomAccessibilityAction("Rotate motorcycle left") {
                            onHorizontalIntentLockedState.value()
                            state.decrement()
                            true
                        },
                        CustomAccessibilityAction("Rotate motorcycle right") {
                            onHorizontalIntentLockedState.value()
                            state.increment()
                            true
                        },
                    )
                }
                .pointerInput(frameCount, rotationReferenceWidthPx) {
                val touchSlop = viewConfiguration.touchSlop
                val dragWidthPx = rotationReferenceWidthPx?.takeIf { it > 0f } ?: size.width.toFloat()

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

                            onHorizontalIntentLockedState.value()
                            change.consume()
                            state.onHorizontalDrag(totalDeltaX, dragWidthPx)
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
                                state.onHorizontalDrag(horizontalDelta, dragWidthPx)
                            }
                        }
                    }
                }
            },
        )
    }
}
