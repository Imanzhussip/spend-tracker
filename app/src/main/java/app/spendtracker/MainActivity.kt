package app.spendtracker

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class Screen { Home, Settings, Banks, BankCurrency, DisplayCurrency, PurchaseOptions, RingChoice, Alerts }

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(newBase.inEnglish())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Every screen has a yellow top bar, so the status bar icons stay dark in both themes.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        // Otherwise the system tints the navigation bar, leaving a visible band at the bottom.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        setContent {
            SpendTrackerTheme {
                var screen by rememberSaveable { mutableStateOf(Screen.Home) }
                val parent = when (screen) {
                    Screen.Home -> null
                    Screen.Settings -> Screen.Home
                    else -> Screen.Settings
                }
                Pages(
                    page = screen,
                    parent = parent,
                    onBack = { parent?.let { screen = it } },
                    depth = { if (it == Screen.Home) 0 else if (it == Screen.Settings) 1 else 2 },
                ) { shown ->
                    when (shown) {
                        Screen.Home -> HomeScreen(
                            onOpenSettings = { screen = Screen.Settings },
                            onPickBank = { screen = Screen.Banks },
                            onPickCurrency = { screen = Screen.BankCurrency },
                        )
                        Screen.Settings -> SettingsScreen(
                            onBack = { screen = Screen.Home },
                            onOpen = { screen = it },
                        )
                        Screen.Banks -> BanksScreen(onBack = { screen = Screen.Settings })
                        Screen.BankCurrency -> BankCurrencyScreen(onBack = { screen = Screen.Settings })
                        Screen.DisplayCurrency -> DisplayCurrencyScreen(onBack = { screen = Screen.Settings })
                        Screen.PurchaseOptions -> PurchaseOptionsScreen(onBack = { screen = Screen.Settings })
                        Screen.RingChoice -> RingSizeScreen(onBack = { screen = Screen.Settings })
                        Screen.Alerts -> AlertsScreen(onBack = { screen = Screen.Settings })
                    }
                }
                // Scrolling content shouldn't show through behind the navigation buttons.
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsBottomHeight(WindowInsets.navigationBars)
                            .background(MaterialTheme.colorScheme.background),
                    )
                }
            }
        }
    }
}

/** Increments each time the app returns to the foreground, so permission changes show up. */
@Composable
private fun resumeTick(): Int {
    val owner = LocalLifecycleOwner.current
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick++
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return tick
}

// ---------- Home ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(onOpenSettings: () -> Unit, onPickBank: () -> Unit, onPickCurrency: () -> Unit) {
    val context = LocalContext.current
    val db = remember { Db.get(context) }
    val changes by db.changes.collectAsState()
    val tick = resumeTick()

    var monthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = YearMonth.parse(monthText)
    var rangeText by rememberSaveable { mutableStateOf<String?>(null) }
    val range = rangeText?.let(DateRange::decode)
    val expenses = remember(month, range, changes, tick) {
        if (range != null) db.expensesIn(range) else db.expensesIn(month)
    }
    var addingExpense by remember { mutableStateOf(false) }
    var pickingDates by rememberSaveable { mutableStateOf(false) }
    var changingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showingOthers by rememberSaveable { mutableStateOf(false) }
    var viewingGroup by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<Expense?>(null) }
    val listState = rememberLazyListState()

    val rates by remember { Rates.flow(context) }.collectAsState()
    val money = remember(rates, tick) { moneyView(context) }
    LaunchedEffect(tick) { if (tick > 0) Rates.refreshIfDue(context) }

    val now = YearMonth.now()
    val newestShown = if (month.isAfter(now.minusMonths(6))) now else month
    val recentMonths = remember(newestShown, changes, money) {
        (5L downTo 0L).map { newestShown.minusMonths(it) }.map { it to money.total(db.expensesIn(it)) }
    }

    // Shown on any page, so a purchase can be deleted from a group's page too.
    deleting?.let { expense ->
        DeleteDialog(
            onConfirm = {
                db.deleteExpense(expense.id)
                CategoryPrompt.dismiss(context, expense.id)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }

    // Pages open on top of Home in this order, and Back closes the top one.
    val changing = changingId?.let { id -> remember(id, changes) { db.expense(id) } }
    val viewing = viewingGroup
    fun pageFor(dates: Boolean, change: Long?, group: String?, others: Boolean): HomePage = when {
        dates -> HomePage.Dates
        change != null -> HomePage.Change(change)
        group != null -> HomePage.Group(group)
        others -> HomePage.Others
        else -> HomePage.Main
    }
    val page = pageFor(pickingDates, changing?.id, viewing, showingOthers)
    val parent = when (page) {
        HomePage.Main -> null
        HomePage.Dates -> pageFor(false, changing?.id, viewing, showingOthers)
        is HomePage.Change -> pageFor(false, null, viewing, showingOthers)
        is HomePage.Group -> pageFor(false, null, null, showingOthers)
        HomePage.Others -> HomePage.Main
    }
    val period = range?.label() ?: month.format(MonthYear)

    Pages(
        page = page,
        parent = parent,
        onBack = {
            when (page) {
                HomePage.Dates -> pickingDates = false
                is HomePage.Change -> changingId = null
                is HomePage.Group -> viewingGroup = null
                HomePage.Others -> showingOthers = false
                HomePage.Main -> Unit
            }
        },
        depth = { it.depth },
        // The calendar is a picker with a close button, so it comes up from below like a sheet.
        rises = { it == HomePage.Dates },
    ) { shown ->
        when (shown) {
            HomePage.Dates -> DateRangeScreen(
                current = range,
                shownMonth = month,
                onClose = { pickingDates = false },
                onChoose = {
                    rangeText = it.encode()
                    pickingDates = false
                },
            )
            is HomePage.Change -> {
                // Still drawn while it slides away, so it looks the purchase up itself.
                val expense = remember(shown.id, changes) { db.expense(shown.id) }
                if (expense != null) ChangePurchaseScreen(expense = expense, money = money, onClose = { changingId = null })
            }
            is HomePage.Group -> GroupPurchasesScreen(
                group = PurchaseGroup.decode(shown.code),
                expenses = expenses,
                money = money,
                period = period,
                onBack = { viewingGroup = null },
                onChange = { changingId = it },
                onDelete = { deleting = it },
            )
            HomePage.Others -> OthersScreen(
                expenses = expenses,
                money = money,
                period = period,
                onBack = { showingOthers = false },
                onOpenAnswer = { viewingGroup = PurchaseGroup.Answer(it).encode() },
            )
            HomePage.Main -> {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    TopAppBar(
                        // The bar insets its title by 16dp. This puts it on the same 24dp edge as the price.
                        title = { Text("Spending", modifier = Modifier.padding(start = 8.dp)) },
                        colors = appBarColors(),
                        actions = {
                            IconButton(modifier = Modifier.size(56.dp), onClick = onOpenSettings) {
                                Icon(Icons.Filled.Settings, contentDescription = "Settings")
                            }
                        },
                    )
                },
                // Centred, because amounts sit on the right of each purchase and shouldn't be covered.
                floatingActionButtonPosition = FabPosition.Center,
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { addingExpense = true },
                        icon = { Icon(Icons.Filled.Add, contentDescription = "Add a purchase") },
                        text = { Text("Add") },
                    )
                },
            ) { padding ->
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding() + 96.dp,
                    ),
                ) {
                    item(key = "summary") {
                        ShelfField(
                            month = month,
                            range = range,
                            expenses = expenses,
                            recentMonths = recentMonths,
                            money = money,
                            onMonth = { monthText = it.toString() },
                            onPickDates = { pickingDates = true },
                            onClearRange = { rangeText = null },
                        )
                    }
                    item(key = "setup") {
                        SetupSteps(tick = tick, onPickBank = onPickBank, onPickCurrency = onPickCurrency)
                    }

                    if (expenses.isNotEmpty()) {
                        item(key = "categories") {
                            CategoryBreakdown(expenses, money) { group ->
                                when (group.kind) {
                                    SpendingGroup.Kind.OTHERS -> showingOthers = true
                                    SpendingGroup.Kind.OPTION -> viewingGroup = PurchaseGroup.Option(group.name).encode()
                                    SpendingGroup.Kind.UNANSWERED -> viewingGroup = PurchaseGroup.Unanswered.encode()
                                }
                            }
                        }
                    }

                    if (expenses.isEmpty()) {
                        item(key = "empty") {
                            EmptyPurchases(
                                title = when {
                                    range != null -> "No purchases on these dates"
                                    month == YearMonth.now() -> "No purchases yet"
                                    else -> "No purchases this month"
                                },
                                showHint = range == null && month == YearMonth.now(),
                            )
                        }
                    } else {
                        item(key = "list-title") {
                            Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 12.dp)) {
                                Text(text = "Purchases", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = "Swipe left to delete, right to change.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        items(expenses, key = { it.id }) { expense ->
                            SwipeActions(
                                modifier = Modifier.animateItem(),
                                onChange = { changingId = expense.id },
                                onDelete = { deleting = expense },
                            ) {
                                ExpenseRow(expense, money) {
                                    context.startActivity(CategoryPrompt.intent(context, expense.id))
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }

    if (addingExpense) {
        AddExpenseDialog(
            currencyCode = money.display.code,
            onDismiss = { addingExpense = false },
            onAdd = { cents ->
                addingExpense = false
                val id = db.addExpense(cents, money.display.code, cents, money.display.code)
                context.startActivity(CategoryPrompt.intent(context, id))
            },
        )
    }
}

/** The pages that open on top of Home, deepest last. */
private sealed interface HomePage {
    val depth: Int

    data object Main : HomePage { override val depth = 0 }
    data object Others : HomePage { override val depth = 1 }
    data class Group(val code: String) : HomePage { override val depth = 2 }
    data class Change(val id: Long) : HomePage { override val depth = 3 }
    data object Dates : HomePage { override val depth = 4 }
}

private val MonthYear = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.ENGLISH)
private val ShortMonth = DateTimeFormatter.ofPattern("LLL", Locale.ENGLISH)

/** The yellow field that owns the top of Home: the month, its total as a price, and recent months. */
@Composable
private fun ShelfField(
    month: YearMonth,
    range: DateRange?,
    expenses: List<Expense>,
    recentMonths: List<Pair<YearMonth, Long>>,
    money: MoneyView,
    onMonth: (YearMonth) -> Unit,
    onPickDates: () -> Unit,
    onClearRange: () -> Unit,
) {
    val isCurrentMonth = month == YearMonth.now()
    val unconverted = money.unconvertedCount(expenses)
    val rateNote = when {
        unconverted > 0 -> "$unconverted not counted yet. Exchange rates need an internet connection."
        money.usesLatestRate(expenses) -> money.asOf?.let {
            "Some purchases use the latest rates, from ${it.toLocalDate().format(DayMonthFormat)}"
        }
        else -> null
    }
    val total = money.total(expenses)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)) {
            // The arrows stay put whatever the month's name, so they can be tapped repeatedly. Their
            // offsets line the visible chevrons up with the 24dp edges of the field.
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (range == null) {
                    IconButton(modifier = Modifier.offset(x = (-20).dp), onClick = { onMonth(month.minusMonths(1)) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                    }
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .offset(x = if (range == null) (-20).dp else 0.dp)
                        .heightIn(min = 48.dp)
                        .clickable(onClickLabel = "Choose dates", role = Role.Button, onClick = onPickDates),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = range?.label() ?: month.format(MonthYear),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
                if (range == null) {
                    IconButton(
                        modifier = Modifier.offset(x = 20.dp),
                        enabled = !isCurrentMonth,
                        onClick = { onMonth(month.plusMonths(1)) },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                    }
                } else {
                    IconButton(modifier = Modifier.offset(x = 17.dp), onClick = onClearRange) {
                        Icon(Icons.Filled.Close, contentDescription = "Back to the month")
                    }
                }
            }
            Crossfade(targetState = total, animationSpec = tween(180), label = "month total") { amount ->
                PriceTag(
                    price = money.display.priceParts(amount),
                    description = money.display.format(amount),
                    wholeSize = 92.sp,
                )
            }
            Text(
                text = when (expenses.size) {
                    1 -> "1 purchase"
                    else -> "${expenses.size} purchases"
                },
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.titleSmall,
            )
            rateNote?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }
            if (range == null) {
                MonthBars(
                    months = recentMonths,
                    selected = month,
                    money = money,
                    onSelect = onMonth,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

/** Recent months at one fixed scale, so they can be compared at a glance. Tapping one opens it. */
@Composable
private fun MonthBars(
    months: List<Pair<YearMonth, Long>>,
    selected: YearMonth,
    money: MoneyView,
    onSelect: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    val largest = months.maxOf { it.second }.coerceAtLeast(1L).toFloat()
    // Each bar's gap is part of its tap target. The row runs 4dp past both edges to keep the bars
    // themselves on the field's margins.
    Row(
        modifier = modifier.layout { measurable, constraints ->
            val overhang = 4.dp.roundToPx()
            val width = constraints.maxWidth + overhang * 2
            val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
            layout(constraints.maxWidth, placeable.height) { placeable.place(-overhang, 0) }
        },
    ) {
        months.forEach { (month, total) ->
            val isSelected = month == selected
            val barHeight by animateDpAsState(
                targetValue = (BarMaxHeight * (total / largest)).coerceAtLeast(4.dp),
                animationSpec = tween(220),
                label = "month bar",
            )
            val barAlpha by animateFloatAsState(if (isSelected) 1f else 0.5f, tween(220), label = "month bar tone")
            val shortName = month.format(ShortMonth)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(role = Role.Button) { onSelect(month) }
                    .semantics(mergeDescendants = true) {
                        contentDescription = "$shortName ${month.year}, ${money.display.format(total)}"
                        this.selected = isSelected
                    }
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(BarMaxHeight), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeight)
                            .graphicsLayer { alpha = barAlpha }
                            .background(LocalContentColor.current, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)),
                    )
                }
                Text(
                    text = shortName,
                    style = if (isSelected) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
            }
        }
    }
}

private val BarMaxHeight = 56.dp

@Composable
private fun EmptyPurchases(title: String, showHint: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        if (showHint) {
            Text(
                text = "Pay as usual and your purchases appear here on their own. Tap Add for anything else.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CategoryBreakdown(expenses: List<Expense>, money: MoneyView, onOpen: (SpendingGroup) -> Unit) {
    val chart = chartColors()
    val needsAnswer = MaterialTheme.colorScheme.errorContainer
    val context = LocalContext.current
    val options = PurchaseOptions.all(context)
    val groups = money.spendingGroups(expenses, options)
    // A colour belongs to its option's place in the list, not to how much was spent on it.
    val groupColors = groups.map { group ->
        when (group.kind) {
            SpendingGroup.Kind.UNANSWERED -> needsAnswer
            SpendingGroup.Kind.OTHERS -> chart.others
            SpendingGroup.Kind.OPTION -> chart.options[
                options.indexOfFirst { it.equals(group.name, ignoreCase = true) }.coerceIn(0, chart.options.lastIndex),
            ]
        }
    }

    // Pale colours would disappear on the light background without an edge.
    val onLightGround = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val edge = MaterialTheme.colorScheme.outline
    fun edgeFor(color: androidx.compose.ui.graphics.Color) = edge.takeIf { onLightGround && color.luminance() > 0.5f }

    SpendingChart(
        // A group's total can be 0 while rates are missing, and a 0 share can't be drawn.
        shares = groups.indices
            .filter { groups[it].cents > 0 }
            .map { ChartShare(groups[it].cents, groupColors[it], edgeFor(groupColors[it])) },
        ringLength = RingSizes.selected(context).length,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp),
    ) {
        groups.forEachIndexed { index, group ->
            val category = group.name
            val cents = group.cents
            val color = groupColors[index]
            val isOthers = group.kind == SpendingGroup.Kind.OTHERS
            val square: @Composable () -> Unit = { ChartSwatch(color, edgeFor(color)) }
            val name: @Composable (Modifier) -> Unit = { modifier ->
                Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = category,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
            val amount: @Composable () -> Unit = {
                Text(
                    text = money.display.format(cents),
                    style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Medium,
                )
            }
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(
                        onClickLabel = if (isOthers) "Show what's in Others" else "Show these purchases",
                        role = Role.Button,
                        onClick = { onOpen(group) },
                    )
                    .padding(vertical = 8.dp),
            ) {
                if (maxWidth >= SideBySideLegendWidth) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        square()
                        name(Modifier.weight(1f))
                        amount()
                    }
                } else {
                    // Beside the ring there's room for a full name only with the amount under it.
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.padding(top = 6.dp)) { square() }
                        Column {
                            name(Modifier)
                            amount()
                        }
                    }
                }
            }
        }
    }
}

/** The top of a group's page: the dates shown, the total as a price tag, and a short note. */
@Composable
private fun PageSummary(period: String, total: Long, money: MoneyView, note: String) {
    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp)) {
        Text(text = period, style = MaterialTheme.typography.titleMedium)
        PriceTag(
            price = money.display.priceParts(total),
            description = money.display.format(total),
            wholeSize = 56.sp,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Text(
            text = note,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Every purchase in one group, for the same month or dates as Home, with the same actions. */
@Composable
private fun GroupPurchasesScreen(
    group: PurchaseGroup,
    expenses: List<Expense>,
    money: MoneyView,
    period: String,
    onBack: () -> Unit,
    onChange: (Long) -> Unit,
    onDelete: (Expense) -> Unit,
) {
    val context = LocalContext.current
    val options = PurchaseOptions.all(context)
    val purchases = expenses.filter { group.matches(it, options) }

    SubScreen(group.title, onBack) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
        ) {
            item(key = "summary") {
                PageSummary(
                    period = period,
                    total = money.total(purchases),
                    money = money,
                    note = when (purchases.size) {
                        0 -> "No purchases here for these dates."
                        1 -> "1 purchase. Swipe left to delete, right to change."
                        else -> "${purchases.size} purchases. Swipe left to delete, right to change."
                    },
                )
            }
            items(purchases, key = { it.id }) { expense ->
                SwipeActions(
                    modifier = Modifier.animateItem(),
                    onChange = { onChange(expense.id) },
                    onDelete = { onDelete(expense) },
                ) {
                    ExpenseRow(expense, money) {
                        context.startActivity(CategoryPrompt.intent(context, expense.id))
                    }
                }
            }
        }
    }
}

/** Its own page for what "Others" is made of, for the same month or dates as Home. */
@Composable
private fun OthersScreen(
    expenses: List<Expense>,
    money: MoneyView,
    period: String,
    onBack: () -> Unit,
    onOpenAnswer: (String) -> Unit,
) {
    val context = LocalContext.current
    val groups = remember(expenses) {
        money.othersBreakdown(expenses, PurchaseOptions.all(context), PurchaseOptions.oldOptions(context))
    }

    SubScreen("Others", onBack) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
        ) {
            item(key = "summary") {
                PageSummary(
                    period = period,
                    total = groups.sumOf { it.cents },
                    money = money,
                    note = "Answers other than your three options, including options you've renamed. " +
                        "Tap one to see its purchases.",
                )
            }
            if (groups.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = "Nothing in Others for these dates.",
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            items(groups, key = { it.name.lowercase() }) { group ->
                Column {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClickLabel = "Show these purchases", role = Role.Button) {
                                onOpenAnswer(group.name)
                            }
                            .heightIn(min = 72.dp)
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = group.name,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = listOfNotNull(
                                    "Old option".takeIf { group.oldOption },
                                    if (group.count == 1) "1 purchase" else "${group.count} purchases",
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = money.display.format(group.cents),
                            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Medium,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private val SideBySideLegendWidth = 240.dp

@Composable
private fun ExpenseRow(expense: Expense, money: MoneyView, onClick: () -> Unit) {
    val needsCategory = expense.category == null
    val date = expense.createdAt.toLocalDate().format(DayMonthFormat)
    val original = money.original(expense)
    val price = money.price(expense)
    Surface(
        color = if (needsCategory) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        val secondary = if (needsCategory) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
        Column {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp),
                color = if (needsCategory) {
                    MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.4f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
            )
            Row(
                modifier = Modifier.heightIn(min = 72.dp).padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.category ?: "Choose a category",
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (original == null) date else "$date · $original",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondary,
                    )
                }
                if (price != null) {
                    RowPrice(price = price, description = money.show(expense))
                } else {
                    // Not converted yet, so it shows the original with its own currency.
                    Text(text = money.show(expense), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun AddExpenseDialog(currencyCode: String, onDismiss: () -> Unit, onAdd: (Long) -> Unit) {
    var text by remember { mutableStateOf("") }
    val amountField = remember { FocusRequester() }
    val cents = AmountParser.parseTyped(text)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a purchase") },
        text = {
            // Inside the dialog, so it runs once the dialog's own window can take focus.
            LaunchedEffect(Unit) { amountField.requestFocus() }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.focusRequester(amountField),
                label = { Text("Amount in $currencyCode") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
        confirmButton = {
            TextButton(enabled = cents != null, onClick = { cents?.let(onAdd) }) { Text("Next") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ---------- Setup ----------

private data class SetupStep(val title: String, val detail: String, val action: String, val run: () -> Unit)

private fun hasNotificationAccess(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

private fun canPostNotifications(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun isBatteryUnrestricted(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

@SuppressLint("BatteryLife")
@Composable
private fun SetupSteps(tick: Int, onPickBank: () -> Unit, onPickCurrency: () -> Unit) {
    val context = LocalContext.current
    var permissionTick by remember { mutableIntStateOf(0) }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { permissionTick++ }

    val steps = remember(tick, permissionTick) {
        buildList {
            if (!hasNotificationAccess(context)) {
                add(
                    SetupStep(
                        "Read bank notifications",
                        "Find Spend Tracker in the list and turn it on.",
                        "Open",
                    ) {
                        val component = ComponentName(context, BankNotificationListener::class.java)
                        val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                            .putExtra(
                                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                                component.flattenToString(),
                            )
                        runCatching { context.startActivity(detail) }.onFailure {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    },
                )
            }
            if (BankApps.selected(context).isEmpty()) {
                add(SetupStep("Choose your bank app", "Only this app's notifications are read.", "Choose", onPickBank))
            }
            if (!Currencies.isBankChosen(context)) {
                add(SetupStep("Choose your bank's currency", "The currency your bank alerts are in.", "Choose", onPickCurrency))
            }
            if (!canPostNotifications(context)) {
                add(
                    SetupStep("Allow notifications", "Needed to ask about a purchase.", "Allow") {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
            }
            if (!Settings.canDrawOverlays(context)) {
                add(
                    SetupStep("Allow the pop-up", "Lets the question appear right after you pay.", "Open") {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}"),
                            ),
                        )
                    },
                )
            }
            if (!isBatteryUnrestricted(context)) {
                add(
                    SetupStep("Keep it running", "Stops your phone putting the app to sleep.", "Allow") {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:${context.packageName}"),
                            ),
                        )
                    },
                )
            }
        }
    }

    if (steps.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 24.dp)) {
        Text(
            text = "Finish setting up",
            modifier = Modifier.padding(bottom = 12.dp),
            style = MaterialTheme.typography.titleLarge,
        )
        steps.forEach { step ->
            HorizontalDivider()
            Row(
                modifier = Modifier.heightIn(min = 72.dp).padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(step.title, fontWeight = FontWeight.Medium)
                    Text(
                        text = step.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = step.run,
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = TagCorners,
                ) {
                    Text(step.action)
                }
            }
        }
    }
}

// ---------- Settings ----------

/** Every screen's top bar is the same price-label yellow, whatever the theme. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun appBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer,
    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SubScreen(title: String, onBack: () -> Unit, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                colors = appBarColors(),
                navigationIcon = {
                    IconButton(modifier = Modifier.size(56.dp), onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        content = content,
    )
}

@Composable
private fun SettingsScreen(onBack: () -> Unit, onOpen: (Screen) -> Unit) {
    val context = LocalContext.current
    SubScreen("Settings", onBack) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            SettingsRow(
                title = "Bank apps",
                detail = "${BankApps.selected(context).size} selected",
                onClick = { onOpen(Screen.Banks) },
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
            val bank = Currencies.bank(context)
            SettingsRow(
                title = "Bank alert currency",
                detail = "${bank.label} (${bank.code})",
                onClick = { onOpen(Screen.BankCurrency) },
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
            val display = Currencies.display(context)
            SettingsRow(
                title = "Show spending in",
                detail = "${display.label} (${display.code})\nConverted at each purchase day's rate",
                onClick = { onOpen(Screen.DisplayCurrency) },
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
            SettingsRow(
                title = "Purchase options",
                detail = PurchaseOptions.all(context).joinToString(", "),
                onClick = { onOpen(Screen.PurchaseOptions) },
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
            SettingsRow(
                title = "Ring size",
                detail = RingSizes.selected(context).label,
                onClick = { onOpen(Screen.RingChoice) },
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
            SettingsRow(
                title = "Bank alerts",
                detail = "See how recent alerts were read, or test one",
                onClick = { onOpen(Screen.Alerts) },
            )
        }
    }
}

@Composable
private fun SettingsRow(title: String, detail: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class InstalledApp(val packageName: String, val label: String)

private fun installedApps(context: Context): List<InstalledApp> {
    val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val manager = context.packageManager
    return manager.queryIntentActivities(launcher, 0)
        .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(manager).toString()) }
        .filter { it.packageName != context.packageName }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

@Composable
private fun BanksScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val apps = remember { installedApps(context) }
    // Ticks are only a draft until Confirm, so a mis-tap can't start or stop reading an app's alerts.
    val saved = remember { BankApps.selected(context) }
    var selected by remember { mutableStateOf(saved) }
    var search by rememberSaveable { mutableStateOf("") }
    val visible = apps.filter { it.label.contains(search.trim(), ignoreCase = true) }

    SubScreen("Bank apps", onBack) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // People often tick both their bank and the wallet that charges it, which counts every
            // purchase twice.
            Text(
                text = "Pick the apps that alert you when money leaves your account, like your bank's app. " +
                    "If you pay with Alipay or Google Wallet using your bank card, your bank already sends " +
                    "the alert, so don't pick them too, or each purchase will be counted twice.",
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                label = { Text("Search apps") },
                singleLine = true,
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(visible, key = { it.packageName }) { app ->
                    val checked = app.packageName in selected
                    val toggle = {
                        selected = if (checked) selected - app.packageName else selected + app.packageName
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = toggle)
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = app.label,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Checkbox(checked = checked, onCheckedChange = { toggle() })
                    }
                }
            }
            Button(
                enabled = selected != saved,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp).height(56.dp),
                shape = TagCorners,
                onClick = {
                    BankApps.setSelected(context, selected)
                    onBack()
                },
            ) {
                Text(
                    when (selected.size) {
                        0 -> "Confirm: no apps"
                        1 -> "Confirm 1 app"
                        else -> "Confirm ${selected.size} apps"
                    },
                )
            }
        }
    }
}

@Composable
private fun PurchaseOptionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var options by rememberSaveable { mutableStateOf(PurchaseOptions.all(context)) }

    SubScreen("Purchase options", onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "These are the buttons in the pop-up after a purchase. Each needs a different " +
                    "name, up to ${PurchaseOptions.MAX_LENGTH} characters. Renaming one doesn't change past " +
                    "purchases: they move to Others, still under their old name.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            options.forEachIndexed { index, option ->
                OutlinedTextField(
                    value = option,
                    onValueChange = { text ->
                        options = options.toMutableList().also { it[index] = text.take(PurchaseOptions.MAX_LENGTH) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Option ${index + 1}") },
                    singleLine = true,
                )
            }
            Button(
                enabled = PurchaseOptions.isValid(options),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = TagCorners,
                onClick = {
                    PurchaseOptions.set(context, options)
                    onBack()
                },
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun RingSizeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val current = RingSizes.selected(context)

    SubScreen("Ring size", onBack) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Text(
                text = "How big the ring is when you swipe the spending line to the right.",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RingSize.entries.forEach { size ->
                val choose = {
                    RingSizes.set(context, size)
                    onBack()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = choose)
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(size.label, fontWeight = FontWeight.Medium)
                        Text(
                            text = size.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    RadioButton(selected = size == current, onClick = choose)
                }
            }
        }
    }
}

@Composable
private fun BankCurrencyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    CurrencyScreen(
        title = "Bank alert currency",
        note = "Used when an alert doesn't say which currency it's in. Purchases you already " +
            "saved keep their currency.",
        current = Currencies.bank(context).takeIf { Currencies.isBankChosen(context) },
        onChoose = { Currencies.setBank(context, it) },
        onBack = onBack,
    )
}

@Composable
private fun DisplayCurrencyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    CurrencyScreen(
        title = "Show spending in",
        note = "Each purchase is converted at that day's exchange rate, and the amount stays fixed. " +
            "If you change this later, older purchases use the latest rate.",
        current = Currencies.display(context),
        onChoose = { Currencies.setDisplay(context, it) },
        onBack = onBack,
    ) {
        Text(
            text = "Rates By Exchange Rate API",
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.exchangerate-api.com")))
                }
                .padding(horizontal = 24.dp, vertical = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        )
    }
}

@Composable
private fun CurrencyScreen(
    title: String,
    note: String,
    current: Currency?,
    onChoose: (Currency) -> Unit,
    onBack: () -> Unit,
    footer: @Composable () -> Unit = {},
) {
    SubScreen(title, onBack) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Text(
                text = note,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Currency.entries.forEach { currency ->
                val choose = {
                    onChoose(currency)
                    onBack()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = choose)
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${currency.label} (${currency.symbol})",
                        modifier = Modifier.weight(1f),
                    )
                    RadioButton(selected = currency == current, onClick = choose)
                }
            }
            footer()
        }
    }
}

@Composable
private fun AlertsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { Db.get(context) }
    val changes by db.changes.collectAsState()
    val alerts = remember(changes) { db.recentAlerts() }
    var sample by rememberSaveable { mutableStateOf("") }
    val money = remember { moneyView(context) }
    val result = sample.takeIf { it.isNotBlank() }
        ?.let { AmountParser.parse(null, it, Currencies.bank(context)) }

    SubScreen("Bank alerts", onBack) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "test") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sample,
                        onValueChange = { sample = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Test a notification's text") },
                    )
                    if (result != null) {
                        Text(
                            text = when (result) {
                                is ParseResult.Spend -> {
                                    val shown = money.show(result.cents, result.currency)
                                    if (result.currency == money.display.code) {
                                        "Would add $shown"
                                    } else {
                                        "Would add $shown (${formatAmount(result.cents, result.currency)})"
                                    }
                                }
                                is ParseResult.Skipped -> "Would skip: ${result.reason}"
                            },
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            item(key = "title") {
                Text(
                    text = "Latest alerts, kept only on this phone",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            if (alerts.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = "Nothing yet. Alerts from your bank app will appear here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(alerts) { alert ->
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(alert.outcome, fontWeight = FontWeight.Medium)
                        Text(alert.text, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = alert.createdAt.toLocalDate().format(DayMonthFormat),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
