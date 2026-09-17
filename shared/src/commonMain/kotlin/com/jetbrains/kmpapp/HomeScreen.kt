package com.motoscope

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motoscope.viewer.MotorcycleViewer

private val Ink = Color(0xFF111111)
private val MutedInk = Color(0xFF687084)
private val CardSurface = Color(0xFFFCFCFC)
private val FeatureSurface = Color(0xFFF5F5F5)

private data class SpecCardData(
    val label: String,
    val value: String,
    val unit: String,
    val spokenValue: String,
    val icon: VisualIcon,
)

private data class FeatureCardData(
    val title: String,
    val subtitle: String,
    val icon: VisualIcon,
)

private val specificationCards = listOf(
    SpecCardData("Displacement", "125", "CC", "125 cubic centimeters", VisualIcon.Engine),
    SpecCardData("Power", "8.2", "KW", "8.2 kilowatts", VisualIcon.Power),
    SpecCardData("Torque", "10.8", "NM", "10.8 newton meters", VisualIcon.Torque),
    SpecCardData("Weight", "111", "KG", "111 kilograms", VisualIcon.Weight),
)

private val featureCards = listOf(
    FeatureCardData("Maintenance", "Keep your ride in top condition", VisualIcon.Tools),
    FeatureCardData("Parts", "Find compatible parts easily", VisualIcon.Parts),
    FeatureCardData("Specifications", "Explore detailed technical info", VisualIcon.Clipboard),
    FeatureCardData("Compare", "See how it stacks up with others", VisualIcon.Compare),
)

@Composable
internal fun MotoScopeHomeScreen() {
    val scrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        val fontScale = LocalDensity.current.fontScale
        val pagePadding = if (maxWidth < 360.dp) 16.dp else 20.dp
        val specColumns = if (shouldUseFourSpecColumns(maxWidth, pagePadding, fontScale)) 4 else 2
        val featureColumns = if (shouldUseTwoFeatureColumns(maxWidth, pagePadding, fontScale)) 2 else 1

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .safeDrawingPadding()
                .padding(start = pagePadding, end = pagePadding, bottom = 132.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "MotoScope",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    color = Ink,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(22.dp))
                HeroSection()
                Spacer(Modifier.height(24.dp))
                SpecificationsSection(columns = specColumns)
                Spacer(Modifier.height(28.dp))
                FeaturePreviewSection(columns = featureColumns)
                Spacer(Modifier.height(20.dp))
            }
        }

        FloatingBottomNavigation(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = pagePadding, vertical = 12.dp),
        )
    }
}

private fun shouldUseFourSpecColumns(maxWidth: Dp, pagePadding: Dp, fontScale: Float): Boolean {
    val usableWidth = maxWidth - (pagePadding * 2)
    val minimumCardWidth = 76.dp
    val gaps = 8.dp * 3
    return fontScale <= 1.2f && (usableWidth - gaps) / 4 >= minimumCardWidth
}

private fun shouldUseTwoFeatureColumns(maxWidth: Dp, pagePadding: Dp, fontScale: Float): Boolean {
    val usableWidth = maxWidth - (pagePadding * 2)
    val minimumCardWidth = 148.dp
    val gap = 12.dp
    return fontScale <= 1.25f && (usableWidth - gap) / 2 >= minimumCardWidth
}

@Composable
private fun HeroSection() {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val fontScale = LocalDensity.current.fontScale
        val clickFontSize = (maxWidth.value * 0.19f / fontScale).coerceIn(52f, 94f).sp

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "H O N D A",
                color = Ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 7.sp,
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "CLICK125",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clearAndSetSemantics {},
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    color = Ink,
                    style = TextStyle(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF252525), Color(0xFFE2E3E6)),
                        ),
                        fontSize = clickFontSize,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-2).sp,
                        textAlign = TextAlign.Center,
                    ),
                )
                MotorcycleViewer(
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            RotationHint()
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Kiro",
                color = Ink,
                fontFamily = FontFamily.Cursive,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
            )
        }
    }
}

@Composable
private fun RotationHint() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clearAndSetSemantics {
                contentDescription = "Swipe horizontally to rotate the motorcycle"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        ChevronIcon(modifier = Modifier.size(28.dp), pointingLeft = true)
        Spacer(Modifier.width(22.dp))
        Text(
            text = "SWIPE TO ROTATE",
            color = Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.8.sp,
        )
        Spacer(Modifier.width(22.dp))
        ChevronIcon(modifier = Modifier.size(28.dp), pointingLeft = false)
    }
}

@Composable
private fun SpecificationsSection(columns: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeading(title = "Specifications")
        Spacer(Modifier.height(8.dp))
        if (columns == 4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                specificationCards.forEach { card ->
                    SpecificationCard(card = card, modifier = Modifier.weight(1f))
                }
            }
        } else {
            specificationCards.chunked(2).forEachIndexed { index, rowCards ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowCards.forEach { card ->
                        SpecificationCard(card = card, modifier = Modifier.weight(1f))
                    }
                }
                if (index < specificationCards.lastIndex / 2) Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = Ink,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.weight(1f))
        ChevronIcon(modifier = Modifier.size(24.dp), pointingLeft = false)
    }
}

@Composable
private fun SpecificationCard(card: SpecCardData, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .heightIn(min = 148.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${card.label}, ${card.spokenValue}"
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            VisualIcon(kind = card.icon, modifier = Modifier.size(38.dp), tint = Ink)
            Text(
                text = card.label.uppercase(),
                color = MutedInk,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                textAlign = TextAlign.Center,
            )
            Text(
                text = card.value,
                color = Ink,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = card.unit,
                color = Ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun FeaturePreviewSection(columns: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (columns == 2) {
            featureCards.chunked(2).forEachIndexed { index, rowCards ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowCards.forEach { card ->
                        FeaturePreviewCard(card = card, modifier = Modifier.weight(1f))
                    }
                }
                if (index == 0) Spacer(Modifier.height(12.dp))
            }
        } else {
            featureCards.forEachIndexed { index, card ->
                FeaturePreviewCard(card = card, modifier = Modifier.fillMaxWidth())
                if (index < featureCards.lastIndex) Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun FeaturePreviewCard(card: FeatureCardData, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .heightIn(min = 156.dp)
            .clip(shape)
            .background(FeatureSurface)
            .semantics(mergeDescendants = true) {
                contentDescription = "${card.title}. ${card.subtitle}"
            },
    ) {
        FeatureArtwork(
            kind = card.icon,
            modifier = Modifier
                .fillMaxWidth()
                .height(156.dp)
                .align(Alignment.TopStart),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, top = 16.dp, end = 38.dp, bottom = 58.dp),
        ) {
            Text(
                text = card.title,
                color = Ink,
                fontSize = if (card.title.length > 12) 18.sp else 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = card.subtitle,
                color = MutedInk,
                fontSize = 15.sp,
                lineHeight = 19.sp,
            )
        }
        ChevronIcon(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 18.dp, end = 18.dp)
                .size(22.dp),
            pointingLeft = false,
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 14.dp)
                .size(46.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            VisualIcon(kind = card.icon, modifier = Modifier.size(25.dp), tint = Ink)
        }
    }
}

private enum class VisualIcon {
    Engine,
    Power,
    Torque,
    Weight,
    Tools,
    Parts,
    Clipboard,
    Compare,
    Home,
    Stats,
    Garage,
    More,
}

@Composable
private fun VisualIcon(kind: VisualIcon, modifier: Modifier = Modifier, tint: Color) {
    Canvas(modifier = modifier.clearAndSetSemantics {}) {
        val w = size.width
        val h = size.height
        val strokeWidth = (size.minDimension * 0.075f).coerceAtLeast(1.5f)
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (kind) {
            VisualIcon.Engine -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.23f, h * 0.28f),
                    size = Size(w * 0.54f, h * 0.46f),
                    cornerRadius = CornerRadius(w * 0.09f, h * 0.09f),
                    style = stroke,
                )
                drawLine(tint, Offset(w * 0.35f, h * 0.19f), Offset(w * 0.65f, h * 0.19f), strokeWidth)
                drawLine(tint, Offset(w * 0.42f, h * 0.19f), Offset(w * 0.42f, h * 0.28f), strokeWidth)
                drawLine(tint, Offset(w * 0.58f, h * 0.19f), Offset(w * 0.58f, h * 0.28f), strokeWidth)
                drawLine(tint, Offset(w * 0.23f, h * 0.42f), Offset(w * 0.12f, h * 0.42f), strokeWidth)
                drawLine(tint, Offset(w * 0.77f, h * 0.42f), Offset(w * 0.88f, h * 0.42f), strokeWidth)
                drawCircle(
                    color = tint,
                    radius = w * 0.08f,
                    center = Offset(w * 0.5f, h * 0.52f),
                    style = stroke,
                )
            }
            VisualIcon.Power -> {
                drawArc(tint, 200f, 140f, false, style = stroke)
                drawLine(tint, Offset(w * 0.5f, h * 0.56f), Offset(w * 0.68f, h * 0.32f), strokeWidth)
                drawCircle(
                    color = tint,
                    radius = w * 0.045f,
                    center = Offset(w * 0.5f, h * 0.56f),
                )
                drawLine(tint, Offset(w * 0.2f, h * 0.63f), Offset(w * 0.2f, h * 0.7f), strokeWidth)
                drawLine(tint, Offset(w * 0.8f, h * 0.63f), Offset(w * 0.8f, h * 0.7f), strokeWidth)
            }
            VisualIcon.Torque -> {
                drawCircle(
                    color = tint,
                    radius = w * 0.28f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = stroke,
                )
                drawCircle(
                    color = tint,
                    radius = w * 0.38f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = stroke,
                )
                drawLine(tint, Offset(w * 0.2f, h * 0.22f), Offset(w * 0.32f, h * 0.34f), strokeWidth)
                drawLine(tint, Offset(w * 0.8f, h * 0.22f), Offset(w * 0.68f, h * 0.34f), strokeWidth)
                drawLine(tint, Offset(w * 0.2f, h * 0.78f), Offset(w * 0.32f, h * 0.66f), strokeWidth)
                drawLine(tint, Offset(w * 0.8f, h * 0.78f), Offset(w * 0.68f, h * 0.66f), strokeWidth)
            }
            VisualIcon.Weight -> {
                val bag = Path().apply {
                    moveTo(w * 0.3f, h * 0.35f)
                    lineTo(w * 0.7f, h * 0.35f)
                    lineTo(w * 0.82f, h * 0.78f)
                    lineTo(w * 0.18f, h * 0.78f)
                    close()
                }
                drawPath(bag, tint)
                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.38f, h * 0.15f),
                    size = Size(w * 0.24f, h * 0.33f),
                    style = stroke,
                )
            }
            VisualIcon.Tools -> {
                drawLine(tint, Offset(w * 0.24f, h * 0.24f), Offset(w * 0.76f, h * 0.76f), strokeWidth * 1.3f)
                drawLine(tint, Offset(w * 0.76f, h * 0.24f), Offset(w * 0.24f, h * 0.76f), strokeWidth * 1.3f)
                drawCircle(
                    color = tint,
                    radius = w * 0.1f,
                    center = Offset(w * 0.23f, h * 0.23f),
                    style = stroke,
                )
                drawCircle(
                    color = tint,
                    radius = w * 0.1f,
                    center = Offset(w * 0.77f, h * 0.77f),
                    style = stroke,
                )
            }
            VisualIcon.Parts -> {
                val cube = Path().apply {
                    moveTo(w * 0.5f, h * 0.14f)
                    lineTo(w * 0.82f, h * 0.31f)
                    lineTo(w * 0.5f, h * 0.48f)
                    lineTo(w * 0.18f, h * 0.31f)
                    close()
                    moveTo(w * 0.18f, h * 0.31f)
                    lineTo(w * 0.18f, h * 0.69f)
                    lineTo(w * 0.5f, h * 0.86f)
                    lineTo(w * 0.5f, h * 0.48f)
                    moveTo(w * 0.82f, h * 0.31f)
                    lineTo(w * 0.82f, h * 0.69f)
                    lineTo(w * 0.5f, h * 0.86f)
                }
                drawPath(cube, tint, style = stroke)
            }
            VisualIcon.Clipboard -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.23f, h * 0.2f),
                    size = Size(w * 0.54f, h * 0.68f),
                    cornerRadius = CornerRadius(w * 0.06f, h * 0.06f),
                    style = stroke,
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.38f, h * 0.12f),
                    size = Size(w * 0.24f, h * 0.18f),
                    cornerRadius = CornerRadius(w * 0.05f, h * 0.05f),
                    style = stroke,
                )
                drawLine(tint, Offset(w * 0.35f, h * 0.48f), Offset(w * 0.65f, h * 0.48f), strokeWidth)
                drawLine(tint, Offset(w * 0.35f, h * 0.63f), Offset(w * 0.65f, h * 0.63f), strokeWidth)
            }
            VisualIcon.Compare -> {
                drawRoundRect(tint, Offset(w * 0.2f, h * 0.57f), Size(w * 0.14f, h * 0.25f), CornerRadius(2f, 2f))
                drawRoundRect(tint, Offset(w * 0.43f, h * 0.38f), Size(w * 0.14f, h * 0.44f), CornerRadius(2f, 2f))
                drawRoundRect(tint, Offset(w * 0.66f, h * 0.2f), Size(w * 0.14f, h * 0.62f), CornerRadius(2f, 2f))
            }
            VisualIcon.Home -> {
                val home = Path().apply {
                    moveTo(w * 0.16f, h * 0.48f)
                    lineTo(w * 0.5f, h * 0.17f)
                    lineTo(w * 0.84f, h * 0.48f)
                    lineTo(w * 0.77f, h * 0.48f)
                    lineTo(w * 0.77f, h * 0.82f)
                    lineTo(w * 0.23f, h * 0.82f)
                    lineTo(w * 0.23f, h * 0.48f)
                    close()
                }
                drawPath(home, tint)
            }
            VisualIcon.Stats -> {
                drawRoundRect(tint, Offset(w * 0.18f, h * 0.5f), Size(w * 0.16f, h * 0.32f), CornerRadius(2f, 2f))
                drawRoundRect(tint, Offset(w * 0.42f, h * 0.27f), Size(w * 0.16f, h * 0.55f), CornerRadius(2f, 2f))
                drawRoundRect(tint, Offset(w * 0.66f, h * 0.13f), Size(w * 0.16f, h * 0.69f), CornerRadius(2f, 2f))
            }
            VisualIcon.Garage -> {
                drawCircle(
                    color = tint,
                    radius = w * 0.13f,
                    center = Offset(w * 0.3f, h * 0.7f),
                    style = stroke,
                )
                drawCircle(
                    color = tint,
                    radius = w * 0.13f,
                    center = Offset(w * 0.7f, h * 0.7f),
                    style = stroke,
                )
                drawLine(tint, Offset(w * 0.3f, h * 0.7f), Offset(w * 0.43f, h * 0.46f), strokeWidth)
                drawLine(tint, Offset(w * 0.43f, h * 0.46f), Offset(w * 0.64f, h * 0.46f), strokeWidth)
                drawLine(tint, Offset(w * 0.64f, h * 0.46f), Offset(w * 0.7f, h * 0.7f), strokeWidth)
                drawLine(tint, Offset(w * 0.43f, h * 0.46f), Offset(w * 0.34f, h * 0.38f), strokeWidth)
                drawLine(tint, Offset(w * 0.58f, h * 0.46f), Offset(w * 0.72f, h * 0.34f), strokeWidth)
            }
            VisualIcon.More -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.24f, h * 0.16f),
                    size = Size(w * 0.52f, h * 0.68f),
                    cornerRadius = CornerRadius(w * 0.05f, h * 0.05f),
                    style = stroke,
                )
                drawLine(tint, Offset(w * 0.36f, h * 0.38f), Offset(w * 0.64f, h * 0.38f), strokeWidth)
                drawLine(tint, Offset(w * 0.36f, h * 0.54f), Offset(w * 0.64f, h * 0.54f), strokeWidth)
                drawLine(tint, Offset(w * 0.36f, h * 0.7f), Offset(w * 0.56f, h * 0.7f), strokeWidth)
            }
        }
    }
}

@Composable
private fun ChevronIcon(modifier: Modifier, pointingLeft: Boolean) {
    Canvas(modifier = modifier.clearAndSetSemantics {}) {
        val path = Path()
        if (pointingLeft) {
            path.moveTo(size.width * 0.68f, size.height * 0.16f)
            path.lineTo(size.width * 0.35f, size.height * 0.5f)
            path.lineTo(size.width * 0.68f, size.height * 0.84f)
        } else {
            path.moveTo(size.width * 0.32f, size.height * 0.16f)
            path.lineTo(size.width * 0.65f, size.height * 0.5f)
            path.lineTo(size.width * 0.32f, size.height * 0.84f)
        }
        drawPath(
            path = path,
            color = Ink,
            style = Stroke(width = size.minDimension * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Composable
private fun FeatureArtwork(kind: VisualIcon, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.alpha(0.22f).clearAndSetSemantics {}) {
        val w = size.width
        val h = size.height
        val gray = Color(0xFF6E737A)
        val dark = Color(0xFF222222)
        val red = Color(0xFFB32B2B)
        val stroke = Stroke(width = (size.minDimension * 0.025f).coerceAtLeast(1.5f), cap = StrokeCap.Round)

        when (kind) {
            VisualIcon.Tools -> {
                drawOval(gray, Offset(w * 0.45f, h * 0.46f), Size(w * 0.56f, h * 0.32f))
                drawRoundRect(dark, Offset(w * 0.36f, h * 0.38f), Size(w * 0.28f, h * 0.34f), CornerRadius(18f, 18f), style = stroke)
                drawCircle(
                    color = dark,
                    radius = h * 0.1f,
                    center = Offset(w * 0.55f, h * 0.55f),
                    style = stroke,
                )
                drawLine(red, Offset(w * 0.2f, h * 0.83f), Offset(w * 0.83f, h * 0.58f), stroke.width * 2f)
            }
            VisualIcon.Parts -> {
                drawCircle(
                    color = gray,
                    radius = h * 0.2f,
                    center = Offset(w * 0.58f, h * 0.66f),
                    style = stroke,
                )
                drawCircle(
                    color = gray,
                    radius = h * 0.08f,
                    center = Offset(w * 0.58f, h * 0.66f),
                    style = stroke,
                )
                drawArc(
                    color = dark,
                    startAngle = 190f,
                    sweepAngle = 250f,
                    useCenter = false,
                    topLeft = Offset(w * 0.56f, h * 0.04f),
                    size = Size(w * 0.38f, h * 0.62f),
                    style = stroke,
                )
                drawLine(gray, Offset(w * 0.72f, h * 0.62f), Offset(w * 0.96f, h * 0.87f), stroke.width * 2f)
                drawRoundRect(Color(0xFF9A9DA2), Offset(w * 0.77f, h * 0.64f), Size(w * 0.15f, h * 0.21f), CornerRadius(4f, 4f))
            }
            VisualIcon.Clipboard -> {
                val body = Path().apply {
                    moveTo(w * 0.1f, h * 0.78f)
                    lineTo(w * 0.48f, h * 0.42f)
                    lineTo(w * 0.92f, h * 0.56f)
                    lineTo(w * 0.55f, h * 0.92f)
                    close()
                }
                drawPath(body, dark)
                drawLine(red, Offset(w * 0.3f, h * 0.72f), Offset(w * 0.7f, h * 0.68f), stroke.width * 2f)
                drawLine(gray, Offset(w * 0.12f, h * 0.84f), Offset(w * 0.6f, h * 0.96f), stroke.width)
            }
            VisualIcon.Compare -> {
                drawOval(Color(0xFFADB0B5), Offset(w * 0.43f, h * 0.46f), Size(w * 0.25f, h * 0.32f))
                drawOval(Color(0xFF9DA1A7), Offset(w * 0.62f, h * 0.38f), Size(w * 0.29f, h * 0.4f))
                drawOval(Color(0xFF858B92), Offset(w * 0.76f, h * 0.3f), Size(w * 0.3f, h * 0.47f))
                drawLine(gray, Offset(w * 0.38f, h * 0.84f), Offset(w * 0.98f, h * 0.84f), stroke.width)
            }
            else -> Unit
        }
    }
}

private data class NavigationItem(val label: String, val icon: VisualIcon, val active: Boolean)

private val navigationItems = listOf(
    NavigationItem("Home", VisualIcon.Home, active = true),
    NavigationItem("Stats", VisualIcon.Stats, active = false),
    NavigationItem("Garage", VisualIcon.Garage, active = false),
    NavigationItem("More", VisualIcon.More, active = false),
)

@Composable
private fun FloatingBottomNavigation(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .heightIn(min = 82.dp),
        shape = RoundedCornerShape(44.dp),
        color = Color.White,
        shadowElevation = 10.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navigationItems.forEach { item ->
                NavigationItemView(item = item, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NavigationItemView(item: NavigationItem, modifier: Modifier = Modifier) {
    val tint = if (item.active) Ink else MutedInk.copy(alpha = 0.72f)
    Column(
        modifier = modifier
            .heightIn(min = 68.dp)
            .clearAndSetSemantics {
                contentDescription = item.label
                if (item.active) selected = true else disabled()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        VisualIcon(kind = item.icon, modifier = Modifier.size(30.dp), tint = tint)
        Spacer(Modifier.height(3.dp))
        Text(
            text = item.label,
            color = tint,
            fontSize = 12.sp,
            fontWeight = if (item.active) FontWeight.Medium else FontWeight.Normal,
        )
        if (item.active) {
            Spacer(Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .size(width = 42.dp, height = 3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Ink),
            )
        } else {
            Spacer(Modifier.height(6.dp))
        }
    }
}
