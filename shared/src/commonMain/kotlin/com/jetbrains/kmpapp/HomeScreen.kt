package com.motoscope

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import motoscope.shared.generated.resources.Res
import motoscope.shared.generated.resources.manrope_wght
import org.jetbrains.compose.resources.Font
import com.motoscope.viewer.MotorcycleViewer

private val Ink = Color(0xFF111111)
private val MutedInk = Color(0xFF687084)
private const val GoldenViewportWidthDp = 411.42856f

@Composable
private fun motoScopeFont(): FontFamily = FontFamily(
    Font(Res.font.manrope_wght, FontWeight.Normal),
    Font(Res.font.manrope_wght, FontWeight.Medium),
    Font(Res.font.manrope_wght, FontWeight.SemiBold),
    Font(Res.font.manrope_wght, FontWeight.Bold),
    Font(Res.font.manrope_wght, FontWeight.ExtraBold),
)

@Composable
internal fun MotoScopeHomeScreen() {
    val font = motoScopeFont()
    val scrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        val density = LocalDensity.current
        val viewportScale = (maxWidth.value / GoldenViewportWidthDp).coerceIn(0.72f, 1.12f)
        val compactFallback = maxHeight < 760.dp || maxWidth < 300.dp || density.fontScale > 1.15f
        val scrollModifier = if (compactFallback) Modifier.verticalScroll(scrollState) else Modifier

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(scrollModifier)
                .safeDrawingPadding()
                .padding(horizontal = (18.dp * viewportScale).coerceIn(16.dp, 24.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(170.dp * viewportScale))
            MotorcycleShowcaseStage(viewportScale = viewportScale)

            if (!compactFallback) {
                Spacer(Modifier.weight(1f))
            } else {
                Spacer(Modifier.height(36.dp))
            }
        }

        FloatingBottomNavigation(
            font = font,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 11.dp, end = 11.dp, top = 12.dp, bottom = 12.5.dp),
        )
    }
}

@Composable
private fun MotorcycleShowcaseStage(viewportScale: Float) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp * viewportScale),
        contentAlignment = Alignment.TopCenter,
    ) {
        val density = LocalDensity.current
        val canvasWidth = (maxWidth * 1.28f).coerceAtMost(480.dp)
        MotorcycleViewer(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = 6.dp * viewportScale, y = 81.dp * viewportScale)
                .requiredWidth(canvasWidth),
            gestureModifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 61.dp * viewportScale)
                .requiredWidth(maxWidth)
                .height(260.dp * viewportScale),
            rotationReferenceWidthPx = with(density) { canvasWidth.toPx() },
        )
    }
}

private data class NavigationItem(
    val label: String,
    val icon: MaterialSymbol,
    val active: Boolean,
)

private val navigationItems = listOf(
    NavigationItem("Home", MaterialSymbol.HomeFilled, active = true),
    NavigationItem("Stats", MaterialSymbol.BarChart, active = false),
    NavigationItem("Garage", MaterialSymbol.TwoWheeler, active = false),
    NavigationItem("More", MaterialSymbol.Article, active = false),
)

@Composable
private fun FloatingBottomNavigation(
    font: FontFamily,
    modifier: Modifier = Modifier,
) {
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
                NavigationItemView(
                    item = item,
                    font = font,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NavigationItemView(
    item: NavigationItem,
    font: FontFamily,
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
        MaterialSymbolIcon(
            symbol = item.icon,
            modifier = Modifier.size(22.dp),
            tint = tint,
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = item.label,
            color = tint,
            fontFamily = font,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = if (item.active) FontWeight.SemiBold else FontWeight.Medium,
        )
        if (item.active) {
            Spacer(Modifier.height(1.dp))
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 2.dp)
                    .background(Ink, RoundedCornerShape(2.dp)),
            )
        } else {
            Spacer(Modifier.height(3.dp))
        }
    }
}

private enum class MaterialSymbol {
    HomeFilled,
    BarChart,
    TwoWheeler,
    Article,
}

private fun MaterialSymbol.pathData(): String = when (this) {
    MaterialSymbol.HomeFilled ->
        "M160-200v-360q0-19 8.5-36t23.5-28l240-180q21-16 48-16t48 16l240 180q15 11 23.5 28t8.5 36v360q0 33-23.5 56.5T720-120H600q-17 0-28.5-11.5T560-160v-200q0-17-11.5-28.5T520-400h-80q-17 0-28.5 11.5T400-360v200q0 17-11.5 28.5T360-120H240q-33 0-56.5-23.5T160-200Z"
    MaterialSymbol.BarChart ->
        "M680-160q-17 0-28.5-11.5T640-200v-200q0-17 11.5-28.5T680-440h80q17 0 28.5 11.5T800-400v200q0 17-11.5 28.5T760-160h-80Zm-240 0q-17 0-28.5-11.5T400-200v-560q0-17 11.5-28.5T440-800h80q17 0 28.5 11.5T560-760v560q0 17-11.5 28.5T520-160h-80Zm-240 0q-17 0-28.5-11.5T160-200v-360q0-17 11.5-28.5T200-600h80q17 0 28.5 11.5T320-560v360q0 17-11.5 28.5T280-160h-80Z"
    MaterialSymbol.TwoWheeler ->
        "M160-200q-66 0-113-47T0-360q0-57 36.5-101t93.5-55l-28-24H30q-13 0-21.5-8.5T0-570q0-13 8.5-21.5T30-600h150l100 60 146-55q7-2 14-3.5t15-1.5h111l-62-80h-64q-17 0-28.5-11.5T400-720q0-17 11.5-28.5T440-760h63q19 0 35.5 8.5T566-729l60 77 105-53q10-5 19.5 1t9.5 17v67q0 8-6 14t-14 6h-72l70 92q15-6 30.5-9t31.5-3q66 0 113 47t47 113q0 66-47 113t-113 47q-66 0-113-47t-47-113q0-27 9.5-52.5T676-460l-20-24-124 186q-5 8-14 13t-19 5h-84q-8 0-14.5-2.5T389-290l-69-60q-5 63-51 106.5T160-200Zm0-80q33 0 56.5-23.5T240-360q0-33-23.5-56.5T160-440q-33 0-56.5 23.5T80-360q0 33 23.5 56.5T160-280Zm294-240-144 54 144-54h130-130Zm346 240q33 0 56.5-23.5T880-360q0-33-23.5-56.5T800-440q-33 0-56.5 23.5T720-360q0 33 23.5 56.5T800-280Zm-322-80 106-160H454l-144 54 120 106h48Z"
    MaterialSymbol.Article ->
        "M200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h560q33 0 56.5 23.5T840-760v560q0 33-23.5 56.5T760-120H200Zm0-80h560v-560H200v560Zm0-560v560-560Zm120 480h200q17 0 28.5-11.5T560-320q0-17-11.5-28.5T520-360H320q-17 0-28.5 11.5T280-320q0 17 11.5 28.5T320-280Zm0-160h320q17 0 28.5-11.5T680-480q0-17-11.5-28.5T640-520H320q-17 0-28.5 11.5T280-480q0 17 11.5 28.5t28.5 11.5Zm0-160h320q17 0 28.5-11.5T680-640q0-17-11.5-28.5T640-680H320q-17 0-28.5 11.5T280-640q0 17 11.5 28.5t28.5 11.5Z"
}

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
