package com.motoscope.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun MotorcycleViewer(modifier: Modifier = Modifier) {
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
            .pointerInput(frames) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    state.onHorizontalDrag(dragAmount, size.width.toFloat())
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(frames[state.currentIndex]),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Fit,
        )
    }
}
