package app.spendtracker

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

class ChartShare(val cents: Long, val color: Color, val edge: Color?)

/** Paints one share along its stretch of track, outlined first when it's pale on a pale ground. */
private fun DrawScope.paintShare(path: Path, share: ChartShare, thickness: Float) {
    share.edge?.let { drawPath(path, it, style = Stroke(thickness + 2.dp.toPx())) }
    drawPath(path, share.color, style = Stroke(thickness))
}

/** The small square beside a name in the legend, in the same colour as its share. */
@Composable
fun ChartSwatch(color: Color, edge: Color?) {
    Canvas(modifier = Modifier.size(12.dp)) {
        val corner = CornerRadius(2.dp.toPx())
        drawRoundRect(color, cornerRadius = corner)
        edge?.let { drawRoundRect(it, cornerRadius = corner, style = Stroke(1.dp.toPx())) }
    }
}

private val LineThickness = 16.dp
private val RingThickness = 20.dp
private val ShareGap = 3.dp

// How far a finger travels for a full change from the line to a ring or column, as a share of the width.
private const val FullSwipe = 0.4f

// A flick faster than this, in changes per second, moves one step even if the swipe was short.
private const val FlickSpeed = 1.2f

// The column's height, as a share of the line's length.
private const val ColumnLength = 0.75f

// The radius of the bend the line takes from the top of the track down the left side.
private val ColumnBend = 24.dp

// How far the text moves right to clear the column.
private val BesideColumn = RingThickness + 16.dp

/**
 * Spending shares as a train on a track, with three resting places. Swiping right runs the line
 * onto a circular rail, nose first, until it closes into a ring. Swiping left runs it round a bend
 * and down the left side into a column. [legend] slides aside to make room for either. Progress
 * goes from -1 (column) through 0 (line) to 1 (ring). It follows the finger, and on release it
 * rolls on to the nearest resting place.
 */
@Composable
fun SpendingChart(
    shares: List<ChartShare>,
    ringLength: Float,
    modifier: Modifier = Modifier,
    legend: @Composable () -> Unit,
) {
    if (shares.isEmpty()) {
        Column(modifier = modifier.fillMaxWidth()) { legend() }
        return
    }

    // Only the resting place is saved. Saving the moving value could keep a chart stuck partway
    // if it scrolled away or the screen changed mid-roll.
    var resting by rememberSaveable { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(resting.toFloat()) }
    var settling by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    // A swipe moves at most one step from where it started, so it can't skip past the line.
    var swipeFrom by remember { mutableIntStateOf(0) }
    val haptics = LocalHapticFeedback.current

    // Glides to the resting place, carrying on at the speed the finger left it. The spring is
    // critically damped, so it settles without bouncing past the ring or the column.
    fun rollTo(target: Int, speed: Float = 0f) {
        resting = target
        settling?.cancel()
        settling = scope.launch {
            animate(
                initialValue = progress,
                targetValue = target.toFloat(),
                initialVelocity = speed,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
            ) { value, _ -> progress = value.coerceIn(-1f, 1f) }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val fullWidth = with(density) { maxWidth.toPx() }
        val lineHeight = with(density) { LineThickness.toPx() }
        val ringHeight = with(density) { (maxWidth * (ringLength / (2 * PI).toFloat()) * 2 + RingThickness).toPx() }
        val besideRing = ringHeight + with(density) { 16.dp.toPx() }
        val columnHeight = fullWidth * ColumnLength
        val besideColumn = with(density) { BesideColumn.toPx() }
        val belowLine = with(density) { LegendBelowLine.toPx() }

        // The moving values are only read while laying out and drawing, never while composing,
        // so each frame redraws the chart without rebuilding the list beside it.
        fun height(): Float =
            if (progress >= 0f) {
                mix(lineHeight, ringHeight, smooth(progress, 0f, 0.5f))
            } else {
                mix(lineHeight, columnHeight, smooth(-progress, 0f, 0.5f))
            }

        // Where the text sits: its left edge, its top, and the room it leaves on the right.
        fun legendPlace(): Triple<Float, Float, Float> {
            val toRing = progress.coerceAtLeast(0f)
            val toColumn = (-progress).coerceAtLeast(0f)
            // The text clears the ring's or the column's side early, and only moves up once the
            // line's tail has passed.
            return Triple(
                besideColumn * smooth(toColumn, 0f, 0.3f),
                mix(belowLine, 0f, maxOf(smooth(toRing, 0.7f, 1f), smooth(toColumn, 0.8f, 1f))),
                besideRing * smooth(toRing, 0f, 0.4f),
            )
        }

        val chart = @Composable {
            Box(modifier = Modifier.fillMaxWidth()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .layout { measurable, constraints ->
                            val height = height().roundToInt()
                            val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
                            layout(placeable.width, height) { placeable.place(0, 0) }
                        },
                ) {
                    if (progress >= 0f) {
                        drawTrain(shares, ringLength, progress)
                    } else {
                        drawColumn(shares, -progress)
                    }
                }
                Column(
                    modifier = Modifier.layout { measurable, constraints ->
                        val (left, top, right) = legendPlace()
                        val x = left.roundToInt()
                        val width = (constraints.maxWidth - x - right.roundToInt()).coerceAtLeast(0)
                        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width, minHeight = 0))
                        val y = top.roundToInt()
                        layout(x + width, y + placeable.height) { placeable.place(x, y) }
                    },
                ) {
                    legend()
                }
            }
        }

        // The chart's area keeps one height whatever shape it's in, tall enough for any of them, so
        // nothing below it moves while it's swiped. The legend is measured at each shape's width
        // to find that height.
        SubcomposeLayout(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    customActions = listOf(-1 to "Show as a column", 0 to "Show as a line", 1 to "Show as a ring")
                        .filter { (target, _) -> target != resting }
                        .map { (target, label) -> CustomAccessibilityAction(label) { rollTo(target); true } }
                }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val lowest = maxOf(-1, swipeFrom - 1).toFloat()
                        val highest = minOf(1, swipeFrom + 1).toFloat()
                        val landing = progress.roundToInt()
                        progress = (progress + delta / (fullWidth * FullSwipe)).coerceIn(lowest, highest)
                        // A small tick when letting go would now land on a different shape.
                        if (progress.roundToInt() != landing) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    },
                    onDragStarted = {
                        settling?.cancel()
                        swipeFrom = resting
                    },
                    onDragStopped = { velocity ->
                        val speed = velocity / (fullWidth * FullSwipe)
                        val target = when {
                            speed > FlickSpeed -> swipeFrom + 1
                            speed < -FlickSpeed -> swipeFrom - 1
                            else -> progress.roundToInt()
                        }
                        rollTo(target.coerceIn(maxOf(-1, swipeFrom - 1), minOf(1, swipeFrom + 1)), speed)
                    },
                ),
        ) { constraints ->
            val width = constraints.maxWidth
            fun legendHeight(slot: String, room: Float): Int {
                val fixed = Constraints.fixedWidth(room.roundToInt().coerceIn(0, width))
                return subcompose(slot) { Column(modifier = Modifier.clearAndSetSemantics {}) { legend() } }
                    .maxOfOrNull { it.measure(fixed).height } ?: 0
            }
            val tallestLegend = maxOf(
                legendHeight("beside ring", width - besideRing),
                legendHeight("beside column", width - besideColumn),
                legendHeight("below line", width.toFloat()),
            )
            // While the shape changes, the text can be at its narrowest before it has moved up.
            val reserved = maxOf(ringHeight, columnHeight, belowLine + tallestLegend).roundToInt()
            val shown = subcompose("chart", chart).map { it.measure(constraints.copy(minHeight = 0)) }
            layout(width, maxOf(reserved, shown.maxOfOrNull { it.height } ?: 0)) {
                shown.forEach { it.place(0, 0) }
            }
        }
    }
}

private val LegendBelowLine: Dp = LineThickness + 8.dp

private fun smooth(value: Float, from: Float, to: Float): Float {
    val x = ((value - from) / (to - from)).coerceIn(0f, 1f)
    return x * x * (3 - 2 * x)
}

private fun mix(from: Float, to: Float, amount: Float) = from + (to - from) * amount

/**
 * The rail is a straight run into a circle, and the train keeps its place on it: a share of it
 * equal to [progress] has gone round the circle, and the rest is still on the straight. The rail
 * itself slides left as this happens, so the finished ring ends inside the right margin.
 */
private fun DrawScope.drawTrain(shares: List<ChartShare>, ringLengthShare: Float, progress: Float) {
    val width = size.width
    val ringStroke = RingThickness.toPx()
    val ringLength = width * ringLengthShare
    val radius = ringLength / (2 * PI).toFloat()

    val thickness = mix(LineThickness.toPx(), ringStroke, progress)
    val trainLength = mix(width, ringLength, progress)
    val straightLength = width * (1 - progress)
    val railJoinX = mix(width, width - radius - ringStroke / 2, progress)
    val railY = mix(LineThickness.toPx() / 2, ringStroke / 2, progress)
    val tailX = railJoinX - straightLength
    val circle = Rect(center = Offset(railJoinX, railY + radius), radius = radius)
    fun piece(from: Float, to: Float) = Path().apply {
        if (from < straightLength) {
            moveTo(tailX + from, railY)
            lineTo(tailX + min(to, straightLength), railY)
        }
        if (to > straightLength) {
            val start = maxOf(from, straightLength)
            arcTo(
                rect = circle,
                startAngleDegrees = -90f + degrees((start - straightLength) / radius),
                sweepAngleDegrees = degrees((to - start) / radius),
                forceMoveTo = from >= straightLength,
            )
        }
    }

    val gap = ShareGap.toPx()
    // A very small share still gets a visible sliver, so nothing in the list goes undrawn.
    val total = shares.sumOf { it.cents }.toFloat()
    val weights = shares.map { it.cents.toFloat().coerceAtLeast(total * 0.02f) }
    val perWeight = (trainLength - gap * shares.size) / weights.sum()

    var position = gap / 2
    shares.forEachIndexed { index, share ->
        val length = weights[index] * perWeight
        paintShare(piece(position, position + length), share, thickness)
        position += length + gap
    }
}

/**
 * The column's rail runs along the top, bends down through a quarter circle, and runs down the
 * left side. The train's left end leads: a share of it equal to [progress] has gone round the
 * bend. The rail slides right and up as this happens, so the finished column sits on the left
 * edge with its top at the top. The largest share leads, so it ends at the bottom.
 */
private fun DrawScope.drawColumn(shares: List<ChartShare>, progress: Float) {
    val width = size.width
    val bend = ColumnBend.toPx()
    val bendLength = bend * PI.toFloat() / 2
    val thickness = mix(LineThickness.toPx(), RingThickness.toPx(), progress)
    val columnLength = width * ColumnLength
    val trainLength = mix(width, columnLength, progress)

    // Distance along the rail is measured from where the top run meets the bend: negative along
    // the top, then round the bend, then down the side.
    val sideX = mix(-bend, thickness / 2, progress)
    val topY = mix(LineThickness.toPx() / 2, -bend, progress)
    val bendCentre = Offset(sideX + bend, topY + bend)
    val leadAt = progress * (bendLength + trainLength)

    fun point(distance: Float): Offset = when {
        distance <= 0f -> Offset(sideX + bend - distance, topY)
        distance >= bendLength -> Offset(sideX, topY + bend + (distance - bendLength))
        else -> {
            val angle = -PI.toFloat() / 2 - distance / bend
            Offset(bendCentre.x + bend * cos(angle), bendCentre.y + bend * sin(angle))
        }
    }

    fun piece(from: Float, to: Float) = Path().apply {
        moveTo(point(from).x, point(from).y)
        if (from < 0f) lineTo(point(min(to, 0f)).x, point(min(to, 0f)).y)
        if (to > 0f && from < bendLength) {
            val start = maxOf(from, 0f)
            val end = min(to, bendLength)
            arcTo(
                rect = Rect(center = bendCentre, radius = bend),
                startAngleDegrees = -90f - degrees(start / bend),
                sweepAngleDegrees = -degrees((end - start) / bend),
                forceMoveTo = false,
            )
        }
        if (to > bendLength) lineTo(point(to).x, point(to).y)
    }

    val gap = ShareGap.toPx()
    val total = shares.sumOf { it.cents }.toFloat()
    val weights = shares.map { it.cents.toFloat().coerceAtLeast(total * 0.02f) }
    val perWeight = (trainLength - gap * shares.size) / weights.sum()

    var fromLead = gap / 2
    shares.forEachIndexed { index, share ->
        val length = weights[index] * perWeight
        paintShare(piece(leadAt - fromLead - length, leadAt - fromLead), share, thickness)
        fromLead += length + gap
    }
}

private fun degrees(radians: Float) = radians * 180f / PI.toFloat()
