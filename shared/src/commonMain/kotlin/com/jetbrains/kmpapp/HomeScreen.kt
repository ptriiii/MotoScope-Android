package com.motoscope

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motoscope.viewer.MotorcycleViewer
import motoscope.shared.generated.resources.Res
import motoscope.shared.generated.resources.anton_regular
import motoscope.shared.generated.resources.kaushan_script_regular
import motoscope.shared.generated.resources.manrope_wght
import org.jetbrains.compose.resources.Font
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlin.math.ceil

private val Ink = Color(0xFF111111)
private val MutedInk = Color(0xFF687084)
private val CardSurface = Color(0xFFFCFCFC)
private val FeatureSurface = Color(0xFFF5F5F5)
private const val GoldenViewportWidthDp = 411.42856f

// Measured from the bundled Kaushan Script glyph outlines at the baseline. Keeping this
// proportion with the TextLayoutResult makes the flow region follow font scaling without using
// a fixed negative visual offset.
private const val KaushanInkBottomFromBaselineEm = 0.079f
private const val KaushanWordScaleX = 1.045f
private val SectionHeadingInkLead = 4.5.dp

private data class MotoScopeFonts(
    val manrope: FontFamily,
    val anton: FontFamily,
    val kaushanScript: FontFamily,
)

@Composable
private fun motoScopeFonts(): MotoScopeFonts = MotoScopeFonts(
    manrope = FontFamily(
        Font(Res.font.manrope_wght, FontWeight.Normal),
        Font(Res.font.manrope_wght, FontWeight.Medium),
        Font(Res.font.manrope_wght, FontWeight.SemiBold),
        Font(Res.font.manrope_wght, FontWeight.Bold),
        Font(Res.font.manrope_wght, FontWeight.ExtraBold),
    ),
    anton = FontFamily(Font(Res.font.anton_regular, FontWeight.Normal)),
    kaushanScript = FontFamily(Font(Res.font.kaushan_script_regular, FontWeight.Normal)),
)

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
    val fonts = motoScopeFonts()
    var rotationHintDismissed by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        val fontScale = LocalDensity.current.fontScale
        val viewportScale = (maxWidth.value / GoldenViewportWidthDp).coerceIn(0.72f, 1.12f)
        val pagePadding = (18.dp * viewportScale).coerceIn(16.dp, 24.dp)
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
                        .padding(top = 23.dp * viewportScale),
                    color = Ink,
                    fontFamily = fonts.manrope,
                    fontSize = 19.3.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp * viewportScale))
                HeroSection(
                    fonts = fonts,
                    viewportScale = viewportScale,
                    rotationHintVisible = !rotationHintDismissed,
                    onHorizontalIntentLocked = { rotationHintDismissed = true },
                )
                SpecificationsSection(fonts = fonts, columns = specColumns, fontScale = fontScale)
                Spacer(Modifier.height(11.5.dp * viewportScale))
                FeaturePreviewSection(fonts = fonts, columns = featureColumns, fontScale = fontScale)
                Spacer(Modifier.height(20.dp))
            }
        }

        FloatingBottomNavigation(
            fonts = fonts,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 11.dp, end = 11.dp, top = 12.dp, bottom = 12.5.dp),
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
private fun HeroSection(
    fonts: MotoScopeFonts,
    viewportScale: Float,
    rotationHintVisible: Boolean,
    onHorizontalIntentLocked: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val clickWidth = maxWidth.coerceAtLeast(280.dp)
        val canvasWidth = (maxWidth * 1.28f).coerceAtMost(480.dp).coerceAtLeast(280.dp)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "HONDA",
                color = Ink,
                fontFamily = fonts.manrope,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 10.sp,
            )
            HeroLayout(
                fonts = fonts,
                viewportScale = viewportScale,
                clickWidth = clickWidth,
                canvasWidth = canvasWidth,
                rotationHintVisible = rotationHintVisible,
                onHorizontalIntentLocked = onHorizontalIntentLocked,
            )
            NicknameRegion(fonts = fonts, viewportScale = viewportScale)
        }
    }
}

/**
 * The hero deliberately separates drawing, gesture, and flow bounds. The image plane is allowed
 * to paint through the cue slot for taller rotational silhouettes, while the cue is an overlay and
 * the nickname remains a separately measured flow region.
 */
@Composable
private fun HeroLayout(
    fonts: MotoScopeFonts,
    viewportScale: Float,
    clickWidth: Dp,
    canvasWidth: Dp,
    rotationHintVisible: Boolean,
    onHorizontalIntentLocked: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            // Preserve the calibrated flow position. Drawing may extend beyond this layout.
            .height(333.dp * viewportScale),
        contentAlignment = Alignment.TopCenter,
    ) {
        val density = LocalDensity.current
        FittedClickBackdrop(
            fonts = fonts,
            desiredWidth = clickWidth,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-36).dp * viewportScale),
        )
        MotorcycleViewer(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = 6.dp * viewportScale, y = 54.dp * viewportScale)
                .requiredWidth(canvasWidth),
            gestureModifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 34.dp * viewportScale)
                .requiredWidth(maxWidth)
                .height(260.dp * viewportScale),
            rotationReferenceWidthPx = with(density) { canvasWidth.toPx() },
            onHorizontalIntentLocked = onHorizontalIntentLocked,
        )
        if (rotationHintVisible) {
            RotationHint(
                fonts = fonts,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 326.dp * viewportScale),
            )
        }
    }
}

@Composable
private fun NicknameRegion(fonts: MotoScopeFonts, viewportScale: Float) {
    val text = "Kiro"
    val fontSize = 48.sp
    val style = TextStyle(
        color = Ink,
        fontFamily = fonts.kaushanScript,
        fontSize = fontSize,
        lineHeight = 52.sp,
        fontWeight = FontWeight.Normal,
        textGeometricTransform = TextGeometricTransform(scaleX = KaushanWordScaleX),
    )
    val textMeasurer = rememberTextMeasurer()
    val layoutResult = textMeasurer.measure(
        text = AnnotatedString(text),
        style = style,
        maxLines = 1,
        softWrap = false,
    )
    val density = LocalDensity.current
    val fontSizePx = with(density) { fontSize.toPx() }
    val lineTop = layoutResult.getLineTop(0)
    val baseline = layoutResult.getLineBaseline(0)
    val visibleBottom = baseline + (fontSizePx * KaushanInkBottomFromBaselineEm)
    val desiredInkToContentGap = with(density) { (9.dp * viewportScale).toPx() }
    val nextSectionInkLead = with(density) { SectionHeadingInkLead.toPx() }
    val regionHeight = ceil(
        visibleBottom - lineTop + desiredInkToContentGap - nextSectionInkLead,
    ).toInt().coerceAtLeast(1)

    // Flow height is derived from the measured glyph ink rather than the font line box. The text
    // child may extend beyond that line box without being clipped; the measured region protects
    // the exact visible-ink-to-content gap while keeping the hero above it unchanged.
    Layout(
        content = {
            Text(
                text = text,
                maxLines = 1,
                softWrap = false,
                style = style,
            )
        },
        modifier = Modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        // The parent intentionally fills the content width, but the nickname child must retain
        // its measured natural width so that the visible glyphs can be centered independently.
        val childWidth = layoutResult.size.width.coerceAtMost(constraints.maxWidth)
        val child = measurables.single().measure(
            Constraints.fixed(width = childWidth, height = layoutResult.size.height),
        )
        layout(constraints.maxWidth, regionHeight) {
            child.placeRelative(
                x = (constraints.maxWidth - child.width) / 2,
                // Keep the font's natural baseline position. Only the parent flow height is
                // reduced to the measured visible ink, so the hero and nickname do not move.
                y = 0,
            )
        }
    }
}

@Composable
private fun FittedClickBackdrop(
    fonts: MotoScopeFonts,
    desiredWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val style = TextStyle(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF1D1D1D), Color(0xFFE2E2E2)),
        ),
        fontFamily = fonts.anton,
        fontSize = 148.sp,
        lineHeight = 148.sp,
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.Center,
    )
    val textMeasurer = rememberTextMeasurer()
    val measured = textMeasurer.measure(
        text = AnnotatedString("CLICK125"),
        style = style,
        maxLines = 1,
        softWrap = false,
    )
    val density = LocalDensity.current
    val naturalWidth = with(density) { measured.size.width.toDp() }
    val horizontalScale = (desiredWidth / naturalWidth).coerceIn(0.5f, 1.6f)
    val fittedFontSize = (148f * horizontalScale * 1.22f).sp

    Text(
        text = "CLICK125",
        modifier = modifier
            .requiredWidth(desiredWidth)
            .clearAndSetSemantics {},
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        style = style.copy(
            fontSize = fittedFontSize,
            lineHeight = fittedFontSize,
            textGeometricTransform = TextGeometricTransform(scaleX = 1f / 1.22f),
        ),
    )
}

@Composable
private fun RotationHint(fonts: MotoScopeFonts, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(13.dp)
            .clearAndSetSemantics {
                contentDescription = "Swipe horizontally to rotate the motorcycle"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        MaterialSymbolIcon(
            symbol = MaterialSymbol.ChevronLeft,
            modifier = Modifier.size(13.dp),
            tint = MutedInk.copy(alpha = 0.8f),
        )
        Spacer(Modifier.width(24.dp))
        Text(
            text = "SWIPE TO ROTATE",
            color = MutedInk,
            fontFamily = fonts.manrope,
            fontSize = 9.5.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
        )
        Spacer(Modifier.width(24.dp))
        MaterialSymbolIcon(
            symbol = MaterialSymbol.ChevronRight,
            modifier = Modifier.size(13.dp),
            tint = MutedInk.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun SpecificationsSection(fonts: MotoScopeFonts, columns: Int, fontScale: Float) {
    val cardHeight = (94.dp * fontScale).coerceAtLeast(94.dp)
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeading(fonts = fonts, title = "Specifications")
        Spacer(Modifier.height(if (columns == 4) 5.5.dp else 10.dp))
        if (columns == 4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                specificationCards.forEach { card ->
                    SpecificationCard(
                        card = card,
                        fonts = fonts,
                        cardHeight = cardHeight,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            specificationCards.chunked(2).forEachIndexed { index, rowCards ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowCards.forEach { card ->
                        SpecificationCard(
                            card = card,
                            fonts = fonts,
                            cardHeight = cardHeight,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (index < specificationCards.lastIndex / 2) Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun SectionHeading(fonts: MotoScopeFonts, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = Ink,
            fontFamily = fonts.manrope,
            modifier = Modifier.offset(y = (-0.25).dp),
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.weight(1f))
        MaterialSymbolIcon(symbol = MaterialSymbol.ChevronRight, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SpecificationCard(
    card: SpecCardData,
    fonts: MotoScopeFonts,
    cardHeight: Dp,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .height(cardHeight)
            .semantics(mergeDescendants = true) {
                contentDescription = "${card.label}, ${card.spokenValue}"
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 5.dp, end = 5.dp, top = 10.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SpecificationIcon(card = card, fonts = fonts)
            Spacer(Modifier.height(1.dp))
            Text(
                text = card.label.uppercase(),
                color = MutedInk,
                fontFamily = fonts.manrope,
                fontSize = 8.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.4.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(0.dp))
            Text(
                text = card.value,
                color = Ink,
                fontFamily = fonts.manrope,
                fontSize = 17.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Spacer(Modifier.height(0.dp))
            Text(
                text = card.unit,
                color = Ink,
                fontFamily = fonts.manrope,
                fontSize = 8.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SpecificationIcon(card: SpecCardData, fonts: MotoScopeFonts) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        VisualIcon(
            kind = card.icon,
            modifier = Modifier.size(if (card.icon == VisualIcon.Engine || card.icon == VisualIcon.Weight) 28.dp else 26.dp),
            tint = Ink,
        )
        if (card.icon == VisualIcon.Weight) {
            Text(
                text = "KG",
                color = Color.White,
                fontFamily = fonts.manrope,
                fontSize = 6.5.sp,
                lineHeight = 7.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun FeaturePreviewSection(fonts: MotoScopeFonts, columns: Int, fontScale: Float) {
    val cardHeight = (94.5.dp * fontScale).coerceAtLeast(94.5.dp)
    Column(modifier = Modifier.fillMaxWidth()) {
        if (columns == 2) {
            featureCards.chunked(2).forEachIndexed { index, rowCards ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowCards.forEach { card ->
                        FeaturePreviewCard(
                            card = card,
                            fonts = fonts,
                            bodyMaxWidth = 100.dp,
                            compactText = true,
                            cardHeight = cardHeight,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (index == 0) Spacer(Modifier.height(9.dp))
            }
        } else {
            featureCards.forEachIndexed { index, card ->
                FeaturePreviewCard(
                    card = card,
                    fonts = fonts,
                    bodyMaxWidth = 320.dp,
                    compactText = false,
                    cardHeight = cardHeight,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (index < featureCards.lastIndex) Spacer(Modifier.height(9.dp))
            }
        }
    }
}

@Composable
private fun FeaturePreviewCard(
    card: FeatureCardData,
    fonts: MotoScopeFonts,
    bodyMaxWidth: Dp,
    compactText: Boolean,
    cardHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .height(cardHeight)
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
                .height(cardHeight)
                .align(Alignment.TopStart),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 5.dp, end = 28.dp, bottom = 40.dp),
        ) {
            Text(
                text = card.title,
                color = Ink,
                fontFamily = fonts.manrope,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = if (compactText) compactFeatureSubtitle(card) else card.subtitle,
                modifier = Modifier.widthIn(max = bodyMaxWidth),
                color = MutedInk,
                fontFamily = fonts.manrope,
                fontSize = 10.5.sp,
                lineHeight = 13.sp,
            )
        }
        MaterialSymbolIcon(
            symbol = MaterialSymbol.ChevronRight,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp)
                .size(16.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 8.dp)
                .size(29.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            VisualIcon(kind = card.icon, modifier = Modifier.size(16.dp), tint = Ink)
        }
    }
}

private fun compactFeatureSubtitle(card: FeatureCardData): String = when (card.title) {
    "Maintenance" -> "Keep your ride\nin top condition"
    "Parts" -> "Find compatible\nparts easily"
    "Specifications" -> "Explore detailed\ntechnical info"
    "Compare" -> "See how it stacks\nup with others"
    else -> card.subtitle
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

private enum class MaterialSymbol {
    ChevronLeft,
    ChevronRight,
    HomeFilled,
    BarChart,
    TwoWheeler,
    MoreHoriz,
    Build,
    Inventory2,
    Description,
    Article,
    Balance,
    Handyman,
    ViewInAr,
    CompareArrows,
    Speed,
    RotateRight,
    Weight,
}

private fun VisualIcon.materialSymbol(): MaterialSymbol = when (this) {
    VisualIcon.Engine -> error("Engine uses the local normalized engine glyph")
    VisualIcon.Power -> MaterialSymbol.Speed
    VisualIcon.Torque -> MaterialSymbol.RotateRight
    VisualIcon.Weight -> MaterialSymbol.Weight
    VisualIcon.Tools -> MaterialSymbol.Handyman
    VisualIcon.Parts -> MaterialSymbol.ViewInAr
    VisualIcon.Clipboard -> MaterialSymbol.Description
    VisualIcon.Compare -> MaterialSymbol.Balance
    VisualIcon.Home -> MaterialSymbol.HomeFilled
    VisualIcon.Stats -> MaterialSymbol.BarChart
    VisualIcon.Garage -> MaterialSymbol.TwoWheeler
    VisualIcon.More -> MaterialSymbol.Article
}

private fun MaterialSymbol.pathData(): String = when (this) {
    MaterialSymbol.ChevronLeft ->
        "m432-480 156 156q11 11 11 28t-11 28q-11 11-28 11t-28-11L348-452q-6-6-8.5-13t-2.5-15q0-8 2.5-15t8.5-13l184-184q11-11 28-11t28 11q11 11 11 28t-11 28L432-480Z"
    MaterialSymbol.ChevronRight ->
        "M504-480 348-636q-11-11-11-28t11-28q11-11 28-11t28 11l184 184q6 6 8.5 13t2.5 15q0 8-2.5 15t-8.5 13L404-268q-11 11-28 11t-28-11q-11-11-11-28t11-28l156-156Z"
    MaterialSymbol.HomeFilled ->
        "M160-200v-360q0-19 8.5-36t23.5-28l240-180q21-16 48-16t48 16l240 180q15 11 23.5 28t8.5 36v360q0 33-23.5 56.5T720-120H600q-17 0-28.5-11.5T560-160v-200q0-17-11.5-28.5T520-400h-80q-17 0-28.5 11.5T400-360v200q0 17-11.5 28.5T360-120H240q-33 0-56.5-23.5T160-200Z"
    MaterialSymbol.BarChart ->
        "M680-160q-17 0-28.5-11.5T640-200v-200q0-17 11.5-28.5T680-440h80q17 0 28.5 11.5T800-400v200q0 17-11.5 28.5T760-160h-80Zm-240 0q-17 0-28.5-11.5T400-200v-560q0-17 11.5-28.5T440-800h80q17 0 28.5 11.5T560-760v560q0 17-11.5 28.5T520-160h-80Zm-240 0q-17 0-28.5-11.5T160-200v-360q0-17 11.5-28.5T200-600h80q17 0 28.5 11.5T320-560v360q0 17-11.5 28.5T280-160h-80Z"
    MaterialSymbol.TwoWheeler ->
        "M160-200q-66 0-113-47T0-360q0-57 36.5-101t93.5-55l-28-24H30q-13 0-21.5-8.5T0-570q0-13 8.5-21.5T30-600h150l100 60 146-55q7-2 14-3.5t15-1.5h111l-62-80h-64q-17 0-28.5-11.5T400-720q0-17 11.5-28.5T440-760h63q19 0 35.5 8.5T566-729l60 77 105-53q10-5 19.5 1t9.5 17v67q0 8-6 14t-14 6h-72l70 92q15-6 30.5-9t31.5-3q66 0 113 47t47 113q0 66-47 113t-113 47q-66 0-113-47t-47-113q0-27 9.5-52.5T676-460l-20-24-124 186q-5 8-14 13t-19 5h-84q-8 0-14.5-2.5T389-290l-69-60q-5 63-51 106.5T160-200Zm0-80q33 0 56.5-23.5T240-360q0-33-23.5-56.5T160-440q-33 0-56.5 23.5T80-360q0 33 23.5 56.5T160-280Zm294-240-144 54 144-54h130-130Zm346 240q33 0 56.5-23.5T880-360q0-33-23.5-56.5T800-440q-33 0-56.5 23.5T720-360q0 33 23.5 56.5T800-280Zm-322-80 106-160H454l-144 54 120 106h48Z"
    MaterialSymbol.MoreHoriz ->
        "M240-400q-33 0-56.5-23.5T160-480q0-33 23.5-56.5T240-560q33 0 56.5 23.5T320-480q0 33-23.5 56.5T240-400Zm240 0q-33 0-56.5-23.5T400-480q0-33 23.5-56.5T480-560q33 0 56.5 23.5T560-480q0 33-23.5 56.5T480-400Zm240 0q-33 0-56.5-23.5T640-480q0-33 23.5-56.5T720-560q33 0 56.5 23.5T800-480q0 33-23.5 56.5T720-400Z"
    MaterialSymbol.Build ->
        "M360-360q-100 0-170-70t-70-170q0-20 3-40t11-38q5-10 12.5-15t16.5-7q9-2 18.5.5T199-689l105 105 72-72-105-105q-8-8-10.5-17.5T260-797q2-9 7-16.5t15-12.5q18-8 38-11t40-3q100 0 170 70t70 170q0 23-4 43.5T584-516l202 200q29 29 29 71t-29 71q-29 29-71 29t-71-30L444-376q-20 8-40.5 12t-43.5 4Zm0-80q26 0 52-8t47-25l243 243q5 5 13.5 4.5T729-231q5-5 5-13.5t-5-13.5L486-500q18-20 26-46.5t8-53.5q0-60-38.5-104.5T386-758l74 74q12 12 12 28t-12 28L332-500q-12 12-28 12t-28-12l-74-74q9 57 53.5 95.5T360-440Zm109-51Z"
    MaterialSymbol.Inventory2 ->
        "M200-80q-33 0-56.5-23.5T120-160v-451q-18-11-29-28.5T80-680v-120q0-33 23.5-56.5T160-880h640q33 0 56.5 23.5T880-800v120q0 23-11 40.5T840-611v451q0 33-23.5 56.5T760-80H200Zm0-520v440h560v-440H200Zm-40-80h640v-120H160v120Zm240 280h160q17 0 28.5-11.5T600-440q0-17-11.5-28.5T560-480H400q-17 0-28.5 11.5T360-440q0 17 11.5 28.5T400-400Zm80 20Z"
    MaterialSymbol.Description ->
        "M360-240h240q17 0 28.5-11.5T640-280q0-17-11.5-28.5T600-320H360q-17 0-28.5 11.5T320-280q0 17 11.5 28.5T360-240Zm0-160h240q17 0 28.5-11.5T640-440q0-17-11.5-28.5T600-480H360q-17 0-28.5 11.5T320-440q0 17 11.5 28.5T360-400ZM240-80q-33 0-56.5-23.5T160-160v-640q0-33 23.5-56.5T240-880h287q16 0 30.5 6t25.5 17l194 194q11 11 17 25.5t6 30.5v447q0 33-23.5 56.5T720-80H240Zm280-560v-160H240v640h480v-440H560q-17 0-28.5-11.5T520-640ZM240-800v200-200 640-640Z"
    MaterialSymbol.Article ->
        "M200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h560q33 0 56.5 23.5T840-760v560q0 33-23.5 56.5T760-120H200Zm0-80h560v-560H200v560Zm0-560v560-560Zm120 480h200q17 0 28.5-11.5T560-320q0-17-11.5-28.5T520-360H320q-17 0-28.5 11.5T280-320q0 17 11.5 28.5T320-280Zm0-160h320q17 0 28.5-11.5T680-480q0-17-11.5-28.5T640-520H320q-17 0-28.5 11.5T280-480q0 17 11.5 28.5T320-440Zm0-160h320q17 0 28.5-11.5T680-640q0-17-11.5-28.5T640-680H320q-17 0-28.5 11.5T280-640q0 17 11.5 28.5T320-600Z"
    MaterialSymbol.Balance ->
        "M120-120q-17 0-28.5-11.5T80-160q0-17 11.5-28.5T120-200h320v-447q-26-9-45-28t-28-45H240l110 258q5 11 6 22.5t-1 23.5q-9 46-49.5 71T220-320q-45 0-85.5-25T85-416q-2-12-1-23.5t6-22.5l110-258h-40q-17 0-28.5-11.5T120-760q0-17 11.5-28.5T160-800h207q12-35 43-57.5t70-22.5q39 0 70 22.5t43 57.5h207q17 0 28.5 11.5T840-760q0 17-11.5 28.5T800-720h-40l110 258q5 11 6 22.5t-1 23.5q-9 46-49.5 71T740-320q-45 0-85.5-25T605-416q-2-12-1-23.5t6-22.5l110-258H593q-9 26-28 45t-45 28v447h320q17 0 28.5 11.5T880-160q0 17-11.5 28.5T840-120H120Zm545-320h150l-75-174-75 174Zm-520 0h150l-75-174-75 174Zm335-280q17 0 28.5-11.5T520-760q0-17-11.5-28.5T480-800q-17 0-28.5 11.5T440-760q0 17 11.5 28.5T480-720Z"
    MaterialSymbol.Handyman ->
        "M754-81q-8 0-15-2.5T726-92L522-296q-6-6-8.5-13t-2.5-15q0-8 2.5-15t8.5-13l85-85q6-6 13-8.5t15-2.5q8 0 15 2.5t13 8.5l204 204q6 6 8.5 13t2.5 15q0 8-2.5 15t-8.5 13l-85 85q-6 6-13 8.5T754-81Zm0-95 29-29-147-147-29 29 147 147ZM205-80q-8 0-15.5-3T176-92l-84-84q-6-6-9-13.5T80-205q0-8 3-15t9-13l212-212h85l34-34-165-165h-57L80-765l113-113 121 121v57l165 165 116-116-43-43 56-56H495l-28-28 142-142 28 28v113l56-56 142 142q17 17 26 38.5t9 45.5q0 24-9 46t-26 39l-85-85-56 56-42-42-207 207v84L233-92q-6 6-13 9t-15 3Zm0-96 170-170v-29h-29L176-205l29 29Zm0 0-29-29 15 14 14 15Zm549 0 29-29-29 29Z"
    MaterialSymbol.ViewInAr ->
        "M440-181 240-296q-19-11-29.5-29T200-365v-230q0-22 10.5-40t29.5-29l200-115q19-11 40-11t40 11l200 115q19 11 29.5 29t10.5 40v230q0 22-10.5 40T720-296L520-181q-19 11-40 11t-40-11Zm0-92v-184l-160-93v185l160 92Zm80 0 160-92v-185l-160 93v184ZM120-640q-17 0-28.5-11.5T80-680v-120q0-33 23.5-56.5T160-880h120q17 0 28.5 11.5T320-840q0 17-11.5 28.5T280-800H160v120q0 17-11.5 28.5T120-640Zm40 560q-33 0-56.5-23.5T80-160v-120q0-17 11.5-28.5T120-320q17 0 28.5 11.5T160-280v120h120q17 0 28.5 11.5T320-120q0 17-11.5 28.5T280-80H160Zm640 0H680q-17 0-28.5-11.5T640-120q0-17 11.5-28.5T680-160h120v-120q0-17 11.5-28.5T840-320q17 0 28.5 11.5T880-280v120q0 33-23.5 56.5T800-80Zm0-600v-120H680q-17 0-28.5-11.5T640-840q0-17 11.5-28.5T680-800h120q33 0 56.5 23.5T880-720v120q0 17-11.5 28.5T840-640q-17 0-28.5-11.5T800-680ZM480-526l158-93-158-91-158 91 158 93Zm0 45Zm0-45Zm40 69Zm-80 0Z"
    MaterialSymbol.CompareArrows ->
        "M367-320H120q-17 0-28.5-11.5T80-360q0-17 11.5-28.5T120-400h247l-75-75q-11-11-11-27.5t11-28.5q12-12 28.5-12t28.5 12l143 143q6 6 8.5 13t2.5 15q0 8-2.5 15t-8.5 13L348-188q-12 12-28 11.5T292-189q-11-12-11.5-28t11.5-28l75-75Zm226-240 75 75q11 11 11 27.5T668-429q-12 12-28.5 12T611-429L468-572q-6-6-8.5-13t-2.5-15q0-8 2.5-15t8.5-13l144-144q12-12 28-11.5t28 12.5q11 12 11.5 28T668-715l-75 75h247q17 0 28.5 11.5T880-600q0 17-11.5 28.5T840-560H593Z"
    MaterialSymbol.Speed ->
        "M418-340q24 24 62 23.5t56-27.5l169-253q9-14-2.5-25.5T677-625L424-456q-27 18-28.5 55t22.5 61Zm62-460q36 0 71 6t68 19q16 6 34 22.5t10 31.5q-8 15-36 20t-45-1q-25-9-50.5-13.5T480-720q-133 0-226.5 93.5T160-400q0 42 11.5 83t32.5 77h552q23-38 33.5-79t10.5-85q0-26-4.5-51T782-504q-6-17-2-33t18-27q13-10 28.5-6t21.5 18q15 35 23 71.5t9 74.5q1 57-13 109t-41 99q-11 18-30 28t-40 10H204q-21 0-40-10t-30-28q-26-45-40-95.5T80-400q0-83 31.5-155.5t86-127Q252-737 325-768.5T480-800Zm7 313Z"
    MaterialSymbol.RotateRight ->
        "M790-478q-12 0-23.5-8.5T752-508q-5-19-12.5-37.5T722-581q-7-11-5-24.5t11-22.5q15-14 34.5-12t29.5 19q13 22 23 45t16 48q5 20-7.5 35T790-478ZM522-130q0-12 8.5-23.5T552-168q19-5 37.5-12.5T625-198q11-7 24.5-5t22.5 11q14 15 12 34.5T665-128q-23 13-46 22.5T572-89q-20 5-35-7t-15-34Zm206-118q-9-8-11-22t5-25q10-17 17.5-35.5T752-368q3-13 14-21.5t24-8.5q22 0 34 15t7 35q-7 24-16.5 47T792-255q-10 17-29.5 19T728-248ZM393-90q-119-32-195-128t-76-220q0-150 105-255t255-105h6l-35-35q-11-11-11-28t12-29q11-11 28-11t29 11l103 104q6 6 8.5 13t2.5 15q0 8-2.5 15t-8.5 13L510-626q-11 11-28 11t-28-11q-11-11-11-28t11-28l36-36h-8q-117 0-198.5 81.5T202-438q0 97 59 171.5T412-168q13 3 21.5 14t8.5 24q0 21-14.5 33T393-90Z"
    MaterialSymbol.Weight ->
        "M240-200h480l-57-400H297l-57 400Zm240-480q17 0 28.5-11.5T520-720q0-17-11.5-28.5T480-760q-17 0-28.5 11.5T440-720q0 17 11.5 28.5T480-680Zm113 0h70q30 0 52 20t27 49l57 400q5 36-18.5 63.5T720-120H240q-37 0-60.5-27.5T161-211l57-400q5-29 27-49t52-20h70q-3-10-5-19.5t-2-20.5q0-50 35-85t85-35q50 0 85 35t35 85q0 11-2 20.5t-5 19.5ZM240-200h480-480Z"
}

private fun materialSymbolVector(symbol: MaterialSymbol): ImageVector = ImageVector.Builder(
    name = "MaterialSymbolsRounded_${symbol.name}",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
).addGroup(translationY = 960f)
    .addPath(
        pathData = PathParser().parsePathString(symbol.pathData()).toNodes(),
        fill = SolidColor(Color.Black),
        pathFillType = PathFillType.NonZero,
    )
    .clearGroup()
    .build()

@Composable
private fun MaterialSymbolIcon(
    symbol: MaterialSymbol,
    modifier: Modifier = Modifier,
    tint: Color = Ink,
) {
    val vector = remember(symbol) { materialSymbolVector(symbol) }
    Image(
        painter = rememberVectorPainter(vector),
        contentDescription = null,
        modifier = modifier.clearAndSetSemantics {},
        colorFilter = ColorFilter.tint(tint),
    )
}

@Composable
private fun VisualIcon(kind: VisualIcon, modifier: Modifier = Modifier, tint: Color) {
    if (kind != VisualIcon.Engine) {
        MaterialSymbolIcon(symbol = kind.materialSymbol(), modifier = modifier, tint = tint)
        return
    }

    Canvas(modifier = modifier.clearAndSetSemantics {}) {
        val w = size.width
        val h = size.height
        val strokeWidth = (size.minDimension / 24f * 1.7f).coerceAtLeast(1.5f)
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val engine = Path().apply {
            moveTo(w * 0.16f, h * 0.28f)
            lineTo(w * 0.76f, h * 0.28f)
            lineTo(w * 0.88f, h * 0.42f)
            lineTo(w * 0.88f, h * 0.72f)
            lineTo(w * 0.7f, h * 0.88f)
            lineTo(w * 0.3f, h * 0.88f)
            lineTo(w * 0.12f, h * 0.7f)
            lineTo(w * 0.12f, h * 0.42f)
            close()
        }
        drawPath(engine, tint, style = stroke)
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.34f, h * 0.08f),
            size = Size(w * 0.32f, h * 0.2f),
            cornerRadius = CornerRadius(w * 0.09f, h * 0.09f),
            style = stroke,
        )
        drawLine(tint, Offset(w * 0.04f, h * 0.42f), Offset(w * 0.13f, h * 0.42f), strokeWidth)
        drawLine(tint, Offset(w * 0.87f, h * 0.42f), Offset(w * 0.96f, h * 0.42f), strokeWidth)
        drawLine(tint, Offset(w * 0.28f, h * 0.48f), Offset(w * 0.72f, h * 0.48f), strokeWidth)
        drawLine(tint, Offset(w * 0.28f, h * 0.63f), Offset(w * 0.72f, h * 0.63f), strokeWidth)
        drawCircle(color = tint, radius = w * 0.06f, center = Offset(w * 0.32f, h * 0.75f), style = stroke)
        drawCircle(color = tint, radius = w * 0.06f, center = Offset(w * 0.68f, h * 0.75f), style = stroke)
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
private fun FloatingBottomNavigation(fonts: MotoScopeFonts, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .height(56.75.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navigationItems.forEach { item ->
                NavigationItemView(item = item, fonts = fonts, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NavigationItemView(
    item: NavigationItem,
    fonts: MotoScopeFonts,
    modifier: Modifier = Modifier,
) {
    val tint = if (item.active) Ink else MutedInk.copy(alpha = 0.72f)
    Column(
        modifier = modifier
            .fillMaxSize()
            .clearAndSetSemantics {
                contentDescription = item.label
                if (item.active) selected = true else disabled()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        VisualIcon(kind = item.icon, modifier = Modifier.size(22.dp), tint = tint)
        Spacer(Modifier.height(1.dp))
        Text(
            text = item.label,
            color = tint,
            fontFamily = fonts.manrope,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = if (item.active) FontWeight.SemiBold else FontWeight.Medium,
        )
        if (item.active) {
            Spacer(Modifier.height(1.dp))
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 2.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Ink),
            )
        } else {
            Spacer(Modifier.height(3.dp))
        }
    }
}
