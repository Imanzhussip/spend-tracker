package app.spendtracker

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

private val TagYellow = Color(0xFFFFD400)
private val TagInk = Color(0xFF151412)
private val Paper = Color(0xFFF4F1E8)
private val NeedsAnswerRed = Color(0xFFD90D25)

/** Behind a purchase row, marking the Change action. White text on it is 5.0:1. */
val ChangeGreen = Color(0xFF1B7F45)

private val LightColors = lightColorScheme(
    primary = TagInk,
    onPrimary = Color.White,
    primaryContainer = TagYellow,
    onPrimaryContainer = TagInk,
    secondaryContainer = Color(0xFFE9E5D9),
    onSecondaryContainer = TagInk,
    errorContainer = NeedsAnswerRed,
    onErrorContainer = Color.White,
    background = Color(0xFFFAF8F1),
    onBackground = TagInk,
    surface = Color(0xFFFAF8F1),
    onSurface = TagInk,
    surfaceVariant = Color(0xFFE9E5D9),
    onSurfaceVariant = Color(0xFF5C584E),
    surfaceContainerHigh = Color(0xFFF1EEE3),
    outline = Color(0xFF8D897D),
    outlineVariant = Color(0xFFDAD6C9),
)

private val DarkColors = darkColorScheme(
    primary = TagYellow,
    onPrimary = TagInk,
    primaryContainer = TagYellow,
    onPrimaryContainer = TagInk,
    secondaryContainer = Color(0xFF302D28),
    onSecondaryContainer = Paper,
    errorContainer = NeedsAnswerRed,
    onErrorContainer = Color.White,
    background = TagInk,
    onBackground = Paper,
    surface = TagInk,
    onSurface = Paper,
    surfaceVariant = Color(0xFF302D28),
    onSurfaceVariant = Color(0xFFB8B4A8),
    surfaceContainerHigh = Color(0xFF26241F),
    outline = Color(0xFF8D897D),
    outlineVariant = Color(0xFF34322D),
)

/** The small corner shared by every button, field and tag in the app. */
val TagCorners = RoundedCornerShape(12.dp)

private val AppShapes = Shapes(
    extraSmall = TagCorners,
    small = TagCorners,
    medium = TagCorners,
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

@OptIn(ExperimentalTextApi::class)
private fun golos(weight: FontWeight) = Font(
    resId = R.font.golos_text,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private val Golos = FontFamily(
    golos(FontWeight.Normal),
    golos(FontWeight.Medium),
    golos(FontWeight.Bold),
    golos(FontWeight.ExtraBold),
    golos(FontWeight.Black),
)

private fun TextStyle.golos(size: TextUnit, lineHeight: TextUnit, weight: FontWeight) =
    copy(fontFamily = Golos, fontSize = size, lineHeight = lineHeight, fontWeight = weight, letterSpacing = 0.sp)

/** Nothing smaller than 14sp, so the app can be read at a glance. */
private val AppTypography = Typography().run {
    copy(
        headlineLarge = headlineLarge.golos(28.sp, 36.sp, FontWeight.ExtraBold),
        headlineMedium = headlineMedium.golos(26.sp, 34.sp, FontWeight.ExtraBold),
        headlineSmall = headlineSmall.golos(24.sp, 32.sp, FontWeight.ExtraBold),
        titleLarge = titleLarge.golos(22.sp, 28.sp, FontWeight.ExtraBold),
        titleMedium = titleMedium.golos(18.sp, 24.sp, FontWeight.Bold),
        titleSmall = titleSmall.golos(16.sp, 24.sp, FontWeight.Bold),
        bodyLarge = bodyLarge.golos(16.sp, 24.sp, FontWeight.Normal),
        bodyMedium = bodyMedium.golos(14.sp, 20.sp, FontWeight.Normal),
        bodySmall = bodySmall.golos(14.sp, 20.sp, FontWeight.Normal),
        labelLarge = labelLarge.golos(16.sp, 24.sp, FontWeight.Bold),
        labelMedium = labelMedium.golos(14.sp, 20.sp, FontWeight.Bold),
        labelSmall = labelSmall.golos(14.sp, 20.sp, FontWeight.Bold),
    )
}

@Composable
fun SpendTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

/**
 * Chart colours, all from the app's own palette, solid and spread far apart in lightness. Each of
 * the three purchase options keeps its own colour wherever it appears, so Food looks the same
 * every month. Others is a grey, and "Not answered yet" uses the red. Every pair that can sit
 * side by side, including where the ring and column wrap round, was checked to stay apart for
 * colour-blind readers too.
 */
class ChartColors(val options: List<Color>, val others: Color)

// Price-label yellow darkened toward ink, the tone the month bars already use.
private val DeepYellow = Color(0xFFA88B00)

@Composable
fun chartColors(): ChartColors =
    if (isSystemInDarkTheme()) {
        ChartColors(options = listOf(TagYellow, Paper, DeepYellow), others = Color(0xFF6E6A60))
    } else {
        ChartColors(options = listOf(TagInk, TagYellow, DeepYellow), others = Color(0xFFC9C5B9))
    }

/**
 * The app's signature: an amount set like a shelf price, with a small raised currency sign and
 * raised, underlined cents. Everything scales together, so a long amount still fits on one line.
 */
@Composable
fun PriceTag(price: PriceParts, description: String, wholeSize: TextUnit, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val base = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, lineHeight = TextUnit.Unspecified)
    val signRatio = 0.28f
    val centsRatio = 0.36f
    val gap = 4.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
    ) {
        val available = with(density) { maxWidth.toPx() }
        val scale = remember(price, wholeSize, available) {
            fun width(text: String, size: TextUnit, tracking: TextUnit = 0.sp) = measurer
                .measure(text, base.copy(fontSize = size, letterSpacing = tracking))
                .size.width
            fun totalAt(scale: Float) = width(price.symbol, wholeSize * scale * signRatio) +
                width(price.whole, wholeSize * scale, (-0.04).em) +
                width(price.cents, wholeSize * scale * centsRatio) +
                with(density) { (gap * 2).toPx() }
            // Text width isn't quite proportional to size, so check the fit at the size chosen.
            var scale = 1f
            repeat(3) {
                val total = totalAt(scale)
                if (total > available) scale *= available / total * 0.98f
            }
            scale
        }
        val whole = wholeSize * scale
        val sign = base.copy(fontSize = whole * signRatio)
        val cents = base.copy(fontSize = whole * centsRatio)
        // Golos's capitals start 0.28em below the top of the line, so these line up the tops of
        // all three sizes. Its descender takes 0.22em, which the rule mostly moves up into.
        val signDrop = with(density) { (whole * (1 - signRatio) * 0.28f).toDp() }
        val centsDrop = with(density) { (whole * (1 - centsRatio) * 0.28f).toDp() }
        val ruleLift = with(density) { (whole * centsRatio * 0.16f).toDp() }

        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            if (!price.signAfter) {
                Text(price.symbol, modifier = Modifier.padding(top = signDrop), style = sign, maxLines = 1, softWrap = false)
            }
            Text(
                text = price.whole,
                style = base.copy(fontSize = whole, letterSpacing = (-0.04).em),
                maxLines = 1,
                softWrap = false,
            )
            Column(modifier = Modifier.padding(top = centsDrop).width(IntrinsicSize.Max)) {
                Text(price.cents, style = cents, maxLines = 1, softWrap = false)
                Box(
                    modifier = Modifier
                        .offset(y = -ruleLift)
                        .fillMaxWidth()
                        .height(with(density) { (whole * 0.045f).toDp() })
                        .background(LocalContentColor.current),
                )
                if (price.signAfter) {
                    Text(price.symbol, modifier = Modifier.offset(y = -ruleLift), style = sign, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

/** The same price tag, small, for a row in a list. */
@Composable
fun RowPrice(price: PriceParts, description: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.Top,
    ) {
        val numbers = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum")
        val small = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
        if (!price.signAfter) {
            Text(price.symbol, modifier = Modifier.padding(end = 2.dp), style = small, maxLines = 1, softWrap = false)
        }
        Text(price.whole, style = numbers, maxLines = 1, softWrap = false)
        Column(modifier = Modifier.padding(start = 2.dp).width(IntrinsicSize.Max)) {
            Text(price.cents, style = small, maxLines = 1, softWrap = false)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(LocalContentColor.current),
            )
            if (price.signAfter) Text(price.symbol, style = small, maxLines = 1, softWrap = false)
        }
    }
}
