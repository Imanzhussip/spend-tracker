package app.spendtracker

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

private val SwipeLabelWidth = 104.dp

// How far a row can travel, as a share of its width. Reaching this stop is what makes it act.
private const val StopShare = 0.3f

// How much of the finger's movement the row follows.
private const val DragFollow = 0.8f

/**
 * A row that can be pulled sideways until it hits a stop. Letting go at the stop changes it when
 * pulled right and deletes it when pulled left, and the phone gives a small buzz on reaching it.
 * Letting go earlier springs it back with nothing done. Screen readers get both actions from the
 * row's actions menu.
 */
@Composable
fun SwipeActions(
    modifier: Modifier = Modifier,
    onChange: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    var rowWidth by remember { mutableIntStateOf(0) }
    var offset by remember { mutableFloatStateOf(0f) }
    var settling by remember { mutableStateOf<Job?>(null) }
    // Kept while springing back, so a small bounce past the middle doesn't flash the other action.
    var side by remember { mutableIntStateOf(0) }
    val stop = rowWidth * StopShare

    // The drag is detected on this outer box, which stays still. On the part that slides, the
    // row's own movement would be read back as finger movement and make it wobble.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(MaterialTheme.colorScheme.background)
            .onSizeChanged { rowWidth = it.width }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    val next = (offset + delta * DragFollow).coerceIn(-stop, stop)
                    if (abs(next) >= stop && abs(offset) < stop) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    offset = next
                    if (next != 0f) side = next.sign.toInt()
                },
                onDragStarted = { settling?.cancel() },
                onDragStopped = {
                    when {
                        offset >= stop -> onChange()
                        offset <= -stop -> onDelete()
                    }
                    settling = launch {
                        animate(
                            initialValue = offset,
                            targetValue = 0f,
                            animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
                        ) { value, _ -> offset = value }
                    }
                },
            ),
    ) {
        // A small gap keeps the panel distinct from the row, even a red one.
        val panelWidth = with(density) { abs(offset).toDp() } - 3.dp
        val armed = abs(offset) >= stop
        if (offset > 0f && side > 0) {
            SwipeAction(
                label = "Change",
                icon = Icons.Filled.Edit,
                color = ChangeGreen,
                contentColor = Color.White,
                armed = armed,
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.align(Alignment.CenterStart).width(panelWidth.coerceAtLeast(0.dp)),
            )
        }
        if (offset < 0f && side < 0) {
            SwipeAction(
                label = "Delete",
                icon = Icons.Filled.Delete,
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                armed = armed,
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier.align(Alignment.CenterEnd).width(panelWidth.coerceAtLeast(0.dp)),
            )
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.roundToInt(), 0) }
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction("Change") { onChange(); true },
                        CustomAccessibilityAction("Delete") { onDelete(); true },
                    )
                },
        ) {
            content()
        }
    }
}

@Composable
private fun SwipeAction(
    label: String,
    icon: ImageVector,
    color: Color,
    contentColor: Color,
    armed: Boolean,
    contentAlignment: Alignment,
    modifier: Modifier = Modifier,
) {
    // The label grows when the pull is far enough, so letting go now will act.
    val scale by animateFloatAsState(
        targetValue = if (armed) 1.25f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "swipe label",
    )
    Box(
        modifier = modifier.fillMaxHeight().background(color).clipToBounds(),
        contentAlignment = contentAlignment,
    ) {
        Column(
            modifier = Modifier
                .wrapContentWidth(unbounded = true)
                .requiredWidth(SwipeLabelWidth)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, tint = contentColor)
            Text(label, color = contentColor, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun DeleteDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Do you really want to delete them?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.heightIn(min = 48.dp),
                shape = TagCorners,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/** Edits a purchase's amount and answer. Its date, currency and saved rate stay as they were. */
@Composable
fun ChangePurchaseScreen(expense: Expense, money: MoneyView, onClose: () -> Unit) {
    val context = LocalContext.current
    val db = remember { Db.get(context) }
    val options = remember { PurchaseOptions.all(context) }
    var amount by rememberSaveable(expense.id) {
        mutableStateOf(String.format(Locale.US, "%.2f", expense.cents / 100.0))
    }
    var answer by rememberSaveable(expense.id) { mutableStateOf(expense.category.orEmpty()) }
    val cents = AmountParser.parseTyped(amount)
    val converted = cents?.let { expense.withAmount(it) }?.takeIf { money.original(it) != null }

    SubScreen("Change purchase", onClose) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Amount in ${expense.currency}") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            when {
                cents == null -> Text(
                    text = "Enter an amount above 0, like 12.50.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                converted != null -> Text(
                    text = "Shows as ${money.show(converted)}, at the same purchase day's rate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = "What was this for?",
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            options.forEach { option ->
                val isChosen = answer.trim().equals(option, ignoreCase = true)
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = TagCorners,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isChosen) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        contentColor = if (isChosen) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                    ),
                    onClick = { answer = option },
                ) {
                    Text(option, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium)
                }
            }
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it.take(PurchaseOptions.MAX_LENGTH) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Or type your own") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            Button(
                enabled = cents != null,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp),
                shape = TagCorners,
                onClick = {
                    val category = answer.takeIf { it.isNotBlank() }
                        ?.let { PurchaseOptions.matchKnown(it, options + db.categories()) }
                    cents?.let { db.updateExpense(expense.withAmount(it).copy(category = category)) }
                    if (category != null) CategoryPrompt.dismiss(context, expense.id)
                    onClose()
                },
            ) {
                Text("Save")
            }
        }
    }
}

/** Full-screen calendar for choosing which dates Home shows. The X leaves without changing them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeScreen(
    current: DateRange?,
    shownMonth: YearMonth,
    onClose: () -> Unit,
    onChoose: (DateRange) -> Unit,
) {
    val today = LocalDate.now()
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = current?.start?.utcMillis(),
        initialSelectedEndDateMillis = current?.end?.utcMillis(),
        initialDisplayedMonthMillis = (current?.start ?: shownMonth.atDay(1)).utcMillis(),
        yearRange = (today.year - 20)..today.year,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= today.utcMillis()
            override fun isSelectableYear(year: Int) = year <= today.year
        },
    )
    // Tapping one day and pressing Show means just that day.
    val chosen = state.selectedStartDateMillis?.let { start ->
        DateRange(start.utcDate(), (state.selectedEndDateMillis ?: start).utcDate())
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Choose dates") },
                colors = appBarColors(),
                navigationIcon = {
                    IconButton(modifier = Modifier.padding(start = 4.dp), onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close calendar")
                    }
                },
                actions = {
                    TextButton(
                        enabled = chosen != null,
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.45f),
                        ),
                        onClick = { chosen?.let(onChoose) },
                    ) {
                        Text("Show")
                    }
                },
            )
        },
    ) { padding ->
        DateRangePicker(
            state = state,
            modifier = Modifier.padding(padding).fillMaxSize(),
            title = null,
            headline = {
                Row(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 12.dp)) {
                    Text(
                        text = chosen?.label() ?: "Tap a start and an end date",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            },
            showModeToggle = false,
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        )
    }
}

// The calendar works in UTC midnights, whatever the phone's time zone.
private fun LocalDate.utcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.utcDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
