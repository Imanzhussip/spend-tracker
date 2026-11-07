package app.spendtracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object CategoryPrompt {
    private const val CHANNEL_ID = "spending_questions"
    private const val EXTRA_EXPENSE_ID = "expense_id"

    fun intent(context: Context, expenseId: Long): Intent =
        Intent(context, CategoryPromptActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(EXTRA_EXPENSE_ID, expenseId)

    fun expenseId(intent: Intent): Long = intent.getLongExtra(EXTRA_EXPENSE_ID, -1)

    /**
     * Always posts a notification, so the question can't be lost if the screen is locked.
     * With "Appear on top" allowed, it also opens the question immediately.
     */
    fun show(context: Context, expenseId: Long, amount: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Spending questions", NotificationManager.IMPORTANCE_HIGH),
        )

        val open = PendingIntent.getActivity(
            context,
            expenseId.toInt(),
            intent(context, expenseId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = android.app.Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(amount)
            .setContentText("What was it for?")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(expenseId.toInt(), notification)

        if (Settings.canDrawOverlays(context)) {
            context.startActivity(intent(context, expenseId))
        }
    }

    fun dismiss(context: Context, expenseId: Long) {
        context.getSystemService(NotificationManager::class.java).cancel(expenseId.toInt())
    }
}

/** The "What was this for?" sheet. Also used to change or remove an existing entry. */
class CategoryPromptActivity : ComponentActivity() {
    private var expenseId by mutableLongStateOf(-1L)

    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(newBase.inEnglish())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))
        // Otherwise the system tints the navigation bar, leaving a visible band below the sheet.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        expenseId = CategoryPrompt.expenseId(intent)
        setContent {
            SpendTrackerTheme {
                val expense = remember(expenseId) { Db.get(this).expense(expenseId) }
                val options = remember(expenseId) { PurchaseOptions.all(this) }
                if (expense == null) {
                    LaunchedEffect(Unit) { finish() }
                } else {
                    PromptSheet(
                        expense = expense,
                        money = remember(expenseId) { moneyView(this) },
                        options = options,
                        onCategory = { category ->
                            Db.get(this).setCategory(expense.id, category)
                            close(expense.id, "Saved as $category")
                        },
                        onTyped = { typed ->
                            val db = Db.get(this)
                            val category = PurchaseOptions.matchKnown(typed, options + db.categories())
                            db.setCategory(expense.id, category)
                            close(expense.id, "Saved as $category")
                        },
                        onNotPurchase = {
                            Db.get(this).deleteExpense(expense.id)
                            close(expense.id, "Removed")
                        },
                        onLater = { finish() },
                    )
                }
            }
        }
    }

    /** A second purchase can arrive while this is open. Its notification keeps the first one reachable. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        expenseId = CategoryPrompt.expenseId(intent)
    }

    /** The sheet disappears as soon as you answer, so a short message confirms it worked. */
    private fun close(id: Long, confirmation: String) {
        CategoryPrompt.dismiss(this, id)
        Toast.makeText(applicationContext, confirmation, Toast.LENGTH_SHORT).show()
        finish()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@androidx.compose.runtime.Composable
private fun PromptSheet(
    expense: Expense,
    money: MoneyView,
    options: List<String>,
    onCategory: (String) -> Unit,
    onTyped: (String) -> Unit,
    onNotPurchase: () -> Unit,
    onLater: () -> Unit,
) {
    var typed by rememberSaveable(expense.id) { mutableStateOf("") }
    val saveTyped = { if (typed.isNotBlank()) onTyped(typed) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = MutableInteractionSource(),
                indication = null,
                onClick = onLater,
            )
            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.ime)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = MutableInteractionSource(),
                    indication = null,
                    onClick = {},
                ),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primaryContainer) {
                    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp)) {
                        val price = money.price(expense)
                        if (price != null) {
                            PriceTag(price = price, description = money.show(expense), wholeSize = 72.sp)
                        } else {
                            // Not converted yet, so it shows the original with its own currency.
                            Text(text = money.show(expense), style = MaterialTheme.typography.titleLarge)
                        }
                        money.original(expense)?.let { original ->
                            Text(
                                text = "Charged $original",
                                modifier = Modifier.padding(top = 4.dp),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                Text(
                    text = "What was this for?",
                    modifier = Modifier.padding(bottom = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                )

                // While typing they aren't needed, and hiding them keeps the rest above the keyboard.
                if (!WindowInsets.isImeVisible) {
                    options.forEach { option ->
                        FilledTonalButton(
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            shape = TagCorners,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            onClick = { onCategory(option) },
                        ) {
                            Text(
                                text = option,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.padding(top = 8.dp).height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = typed,
                        onValueChange = { typed = it.take(PurchaseOptions.MAX_LENGTH) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Or type your own") },
                        singleLine = true,
                        shape = TagCorners,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { saveTyped() }),
                    )
                    FilledTonalButton(
                        modifier = Modifier.fillMaxHeight().heightIn(min = 56.dp),
                        shape = TagCorners,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        enabled = typed.isNotBlank(),
                        onClick = saveTyped,
                    ) {
                        Text("Save")
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(
                        modifier = Modifier.heightIn(min = 56.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        onClick = onNotPurchase,
                    ) {
                        Text("Not a purchase")
                    }
                    TextButton(
                        modifier = Modifier.heightIn(min = 56.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        onClick = onLater,
                    ) {
                        Text("Later")
                    }
                }
                }
            }
        }
    }
}
