package app.spendtracker

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Shows one page at a time and moves between them the way Android's own apps do. A deeper page
 * slides in from the right over the one it came from, and Back slides it off again. A page that
 * [rises] comes up from below instead, like a sheet.
 *
 * The back swipe on Android 14 and later moves the page with the finger, showing [parent] under
 * it, and letting go early puts it back. [depth] orders the pages, so the app knows which way to
 * move. [parent] is null where there's nowhere to go back to inside these pages.
 */
@Composable
fun <T : Any> Pages(
    page: T,
    parent: T?,
    onBack: () -> Unit,
    depth: (T) -> Int,
    rises: (T) -> Boolean = { false },
    content: @Composable (T) -> Unit,
) {
    val state = remember { SeekableTransitionState(page) }
    val scope = rememberCoroutineScope()
    val backTo by rememberUpdatedState(parent)
    val goBack by rememberUpdatedState(onBack)

    // Picks up from wherever a back swipe left the transition.
    LaunchedEffect(page) { state.animateTo(page) }

    PredictiveBackHandler(enabled = parent != null) { swipe ->
        val target = backTo ?: return@PredictiveBackHandler
        try {
            swipe.collect { event -> state.seekTo(event.progress, targetState = target) }
            goBack()
        } catch (cancelled: CancellationException) {
            // Let go early: the page slides back into place.
            scope.launch { state.animateTo(state.currentState) }
            throw cancelled
        }
    }

    rememberTransition(state, label = "pages").AnimatedContent(
        transitionSpec = {
            val forward = depth(targetState) > depth(initialState)
            val moving = if (forward) targetState else initialState
            when {
                rises(moving) && forward ->
                    (slideInVertically(tween(OpenMs, easing = Arrive)) { it / 6 } + fadeIn(tween(OpenMs / 2)))
                        .togetherWith(fadeOut(tween(OpenMs, easing = Arrive), targetAlpha = 0.99f))
                rises(moving) ->
                    fadeIn(tween(CloseMs), initialAlpha = 0.99f)
                        .togetherWith(slideOutVertically(tween(CloseMs, easing = Leave)) { it / 6 } + fadeOut(tween(CloseMs, easing = Leave)))
                forward ->
                    slideInHorizontally(tween(OpenMs, easing = Arrive)) { it }
                        .togetherWith(slideOutHorizontally(tween(OpenMs, easing = Arrive)) { -it / 4 } + fadeOut(tween(OpenMs), targetAlpha = 0.6f))
                else ->
                    (slideInHorizontally(tween(CloseMs, easing = Leave)) { -it / 4 } + fadeIn(tween(CloseMs), initialAlpha = 0.6f))
                        .togetherWith(slideOutHorizontally(tween(CloseMs, easing = Leave)) { it })
            }.withPageOnTop(forward)
        },
    ) { shown -> content(shown) }
}

// The page that's moving stays on top: the new one going in, the old one going out.
private fun ContentTransform.withPageOnTop(forward: Boolean) = apply { targetContentZIndex = if (forward) 1f else -1f }

private const val OpenMs = 380
private const val CloseMs = 300

// Material's emphasised deceleration: a page arrives quickly and settles gently.
private val Arrive = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

// A gentler curve for leaving, so a back swipe moves the page in step with the finger.
private val Leave = LinearOutSlowInEasing
