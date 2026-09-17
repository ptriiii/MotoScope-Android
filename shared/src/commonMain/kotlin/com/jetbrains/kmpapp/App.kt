package com.motoscope

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motoscope.viewer.MotorcycleViewer

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
            val containerSize = remember { mutableStateOf(IntSize.Zero) }
            val isLandscape = containerSize.value.width > containerSize.value.height

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { containerSize.value = it }
                    .background(Color.White)
                    .safeDrawingPadding()
                    .padding(horizontal = 12.dp, vertical = 16.dp),
            ) {
                Text(
                    text = "MotoScope",
                    modifier = Modifier.align(Alignment.TopCenter),
                    color = Color.Black,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                MotorcycleViewer(
                    isLandscape = isLandscape,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}
