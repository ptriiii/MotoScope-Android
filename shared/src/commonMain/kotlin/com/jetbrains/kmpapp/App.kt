package com.motoscope

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motoscope.viewer.MotorcycleViewer

private val Ink = Color(0xFF0B0B0B)
private val Muted = Color(0xFF737373)
private val Card = Color(0xFFF9F9F9)
private val NavMuted = Color(0xFF9AA2AE)

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
            val containerSize = remember { mutableStateOf(IntSize.Zero) }
            val isLandscape = containerSize.value.width > containerSize.value.height

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { containerSize.value = it }
                    .background(Color.White)
                    .safeDrawingPadding()
                    .padding(horizontal = 18.dp),
            ) {
                TopBar()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    Text(
                        text = "H O N D A",
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 18.dp),
                        color = Color(0xFF242424),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 4.sp,
                    )

                    Text(
                        text = "CLICK",
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 48.dp),
                        color = Color(0xFF333333),
                        fontSize = if (isLandscape) 82.sp else 100.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 94.sp,
                    )

                    Text(
                        text = "125",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = if (isLandscape) 104.dp else 132.dp, end = 10.dp),
                        color = Color(0xFF4E588A),
                        fontSize = if (isLandscape) 66.sp else 84.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 80.sp,
                    )

                    MotorcycleViewer(
                        isLandscape = isLandscape,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .offset(y = if (isLandscape) 18.dp else 36.dp),
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Kiro",
                            color = Ink,
                            fontFamily = FontFamily.Cursive,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 42.sp,
                        )
                        Text(
                            text = "—   H O N D A   C L I C K   1 2 5   —",
                            color = Muted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.4.sp,
                        )
                    }
                }

                SpecsRow()
                SwipeHint()
                BottomNav()
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TopBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
    ) {
        MenuIcon(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(34.dp),
        )
        Text(
            text = "MotoScope",
            modifier = Modifier.align(Alignment.Center),
            color = Ink,
            fontSize = 29.sp,
            fontWeight = FontWeight.Bold,
        )
        SearchIcon(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(34.dp),
        )
    }
}

@Composable
private fun SpecsRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SpecCard("⚙", "ENGINE", "125", "CC", Modifier.weight(1f))
        SpecCard("◴", "POWER", "8.2", "KW", Modifier.weight(1f))
        SpecCard("◎", "TORQUE", "10.8", "NM", Modifier.weight(1f))
        SpecCard("KG", "WEIGHT", "111", "KG", Modifier.weight(1f))
    }
}

@Composable
private fun SpecCard(
    icon: String,
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(104.dp),
        color = Card,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = icon,
                color = Ink,
                fontSize = if (icon == "KG") 13.sp else 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                color = Color(0xFF414141),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp,
            )
            Text(
                text = value,
                color = Ink,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
            )
            Text(
                text = unit,
                color = Muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
            )
        }
    }
}

@Composable
private fun SwipeHint() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(text = "‹", color = Color(0xFF9A9A9A), fontSize = 34.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.width(30.dp))
        Text(
            text = "S W I P E   T O   E X P L O R E",
            color = Color(0xFF8A8A8A),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.7.sp,
        )
        Spacer(Modifier.width(30.dp))
        Text(text = "›", color = Color(0xFF9A9A9A), fontSize = 34.sp, fontWeight = FontWeight.Light)
    }
}

@Composable
private fun BottomNav() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        color = Color(0xFFFCFCFC),
        shape = RoundedCornerShape(36.dp),
        tonalElevation = 2.dp,
        shadowElevation = 5.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(symbol = "⌂", label = "Home", selected = true, modifier = Modifier.weight(1f))
            NavItem(symbol = "▥", label = "Stats", selected = false, modifier = Modifier.weight(1f))
            NavItem(symbol = "♞", label = "Garage", selected = false, modifier = Modifier.weight(1f))
            NavItem(symbol = "▤", label = "More", selected = false, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun NavItem(
    symbol: String,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = symbol,
            color = if (selected) Ink else NavMuted,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 26.sp,
        )
        Text(
            text = label,
            color = if (selected) Ink else NavMuted,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun MenuIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 2.3.dp.toPx()
        val left = size.width * 0.14f
        val right = size.width * 0.86f
        val ys = listOf(size.height * 0.28f, size.height * 0.50f, size.height * 0.72f)
        ys.forEach { y ->
            drawLine(
                color = Ink,
                start = androidx.compose.ui.geometry.Offset(left, y),
                end = androidx.compose.ui.geometry.Offset(right, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun SearchIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 2.3.dp.toPx()
        val radius = size.minDimension * 0.27f
        val center = androidx.compose.ui.geometry.Offset(size.width * 0.43f, size.height * 0.42f)
        drawCircle(
            color = Ink,
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
        )
        drawLine(
            color = Ink,
            start = androidx.compose.ui.geometry.Offset(
                center.x + radius * 0.70f,
                center.y + radius * 0.70f,
            ),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.82f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
