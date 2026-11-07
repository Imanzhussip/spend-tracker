package app.spendtracker

import android.content.ContentValues
import android.content.Context
import android.content.res.Configuration
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

/**
 * [cents] is in 1/100ths of [currency], the currency the purchase was actually made in.
 * [convertedCents] is that amount in [convertedCurrency] at the rate on the purchase day, and
 * never changes. Both are null if no rate was available then.
 */
data class Expense(
    val id: Long,
    val cents: Long,
    val currency: String,
    val convertedCents: Long?,
    val convertedCurrency: String?,
    val category: String?,
    val createdAt: Long,
) {
    /** The same purchase with a corrected amount, still converted at its purchase day's rate. */
    fun withAmount(newCents: Long): Expense = copy(
        cents = newCents,
        convertedCents = convertedCents?.let { (it.toDouble() * newCents / cents).roundToLong() },
    )
}

val DayMonthFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val DayMonthYearFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

/** The days Home shows when a range is chosen, both ends included. */
data class DateRange(val start: LocalDate, val end: LocalDate) {
    fun label(): String = when {
        start == end -> start.format(DayMonthYearFormat)
        start.year == end.year -> "${start.format(DayMonthFormat)} – ${end.format(DayMonthYearFormat)}"
        else -> "${start.format(DayMonthYearFormat)} – ${end.format(DayMonthYearFormat)}"
    }

    fun encode(): String = "$start|$end"

    companion object {
        fun decode(text: String): DateRange {
            val (start, end) = text.split("|")
            return DateRange(LocalDate.parse(start), LocalDate.parse(end))
        }
    }
}

data class AlertEntry(val text: String, val outcome: String, val createdAt: Long)

/** The currencies a user can choose. An alert can still be in another one, such as EUR. */
enum class Currency(val code: String, val symbol: String, val label: String) {
    KZT("KZT", "₸", "Kazakhstani tenge"),
    USD("USD", "$", "US dollar"),
    HKD("HKD", "HK$", "Hong Kong dollar"),
    SGD("SGD", "S$", "Singapore dollar");

    /** Whether a plain "$" in a bank alert can mean this currency. */
    val usesDollarSign: Boolean get() = symbol.endsWith("$")

    fun format(cents: Long): String {
        val amount = String.format(Locale.US, "%,.2f", cents / 100.0)
        return if (this == KZT) "$amount $symbol" else "$symbol$amount"
    }

    /** The same amount in pieces, for showing it as a price tag. */
    fun priceParts(cents: Long): PriceParts {
        val (whole, fraction) = String.format(Locale.US, "%,.2f", cents / 100.0).split(".")
        return PriceParts(symbol, whole, fraction, signAfter = this == KZT)
    }
}

data class PriceParts(val symbol: String, val whole: String, val cents: String, val signAfter: Boolean)

fun formatAmount(cents: Long, code: String): String =
    Currency.entries.firstOrNull { it.code == code }?.format(cents)
        ?: "$code ${String.format(Locale.US, "%,.2f", cents / 100.0)}"

/** All spending data stays in this private on-device database. Nothing in it is sent anywhere. */
class Db private constructor(private val appContext: Context) :
    SQLiteOpenHelper(appContext, "spending.db", null, 3) {

    private val _changes = MutableStateFlow(0)

    /** Increments after every write so open screens know to reload. */
    val changes: StateFlow<Int> = _changes

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE expenses (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                cents INTEGER NOT NULL,
                currency TEXT NOT NULL,
                converted_cents INTEGER,
                converted_currency TEXT,
                category TEXT,
                created_at INTEGER NOT NULL
            )""",
        )
        db.execSQL("CREATE INDEX expenses_created_at ON expenses(created_at)")
        db.execSQL(
            """CREATE TABLE alerts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                notification_key TEXT,
                text TEXT NOT NULL,
                outcome TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )""",
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Older purchases had no currency of their own, so they were all in the bank's.
            db.execSQL("ALTER TABLE expenses ADD COLUMN currency TEXT NOT NULL DEFAULT 'USD'")
            db.execSQL("UPDATE expenses SET currency = ?", arrayOf(Currencies.bank(appContext).code))
        }
        if (oldVersion < 3) {
            // Older purchases have no saved rate, so they keep using the latest one.
            db.execSQL("ALTER TABLE expenses ADD COLUMN converted_cents INTEGER")
            db.execSQL("ALTER TABLE expenses ADD COLUMN converted_currency TEXT")
        }
    }

    fun addExpense(
        cents: Long,
        currency: String,
        convertedCents: Long?,
        convertedCurrency: String,
        category: String? = null,
        createdAt: Long = System.currentTimeMillis(),
    ): Long {
        val values = ContentValues().apply {
            put("cents", cents)
            put("currency", currency)
            put("converted_cents", convertedCents)
            put("converted_currency", convertedCurrency.takeIf { convertedCents != null })
            put("category", category)
            put("created_at", createdAt)
        }
        return writableDatabase.insert("expenses", null, values).also { _changes.value++ }
    }

    fun expense(id: Long): Expense? =
        readableDatabase.rawQuery(
            "SELECT $EXPENSE_COLUMNS FROM expenses WHERE id = ?",
            arrayOf(id.toString()),
        ).use { if (it.moveToFirst()) it.toExpense() else null }

    fun categories(): List<String> =
        readableDatabase.rawQuery(
            "SELECT DISTINCT category FROM expenses WHERE category IS NOT NULL",
            null,
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }

    fun setCategory(id: Long, category: String) {
        val values = ContentValues().apply { put("category", category) }
        writableDatabase.update("expenses", values, "id = ?", arrayOf(id.toString()))
        _changes.value++
    }

    fun deleteExpense(id: Long) {
        writableDatabase.delete("expenses", "id = ?", arrayOf(id.toString()))
        _changes.value++
    }

    /** Saves a changed purchase. Its date and original currency never change. */
    fun updateExpense(expense: Expense) {
        val values = ContentValues().apply {
            put("cents", expense.cents)
            put("converted_cents", expense.convertedCents)
            put("category", expense.category)
        }
        writableDatabase.update("expenses", values, "id = ?", arrayOf(expense.id.toString()))
        _changes.value++
    }

    fun expensesIn(month: YearMonth): List<Expense> = expensesIn(DateRange(month.atDay(1), month.atEndOfMonth()))

    fun expensesIn(range: DateRange): List<Expense> {
        val zone = ZoneId.systemDefault()
        val start = range.start.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = range.end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return readableDatabase.rawQuery(
            """SELECT $EXPENSE_COLUMNS FROM expenses
               WHERE created_at >= ? AND created_at < ? ORDER BY created_at DESC""",
            arrayOf(start.toString(), end.toString()),
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.toExpense()) }
        }
    }

    /** Android re-delivers a notification when it updates, so the same alert can arrive twice. */
    fun isRepeatAlert(key: String, text: String, withinMillis: Long): Boolean =
        readableDatabase.rawQuery(
            "SELECT 1 FROM alerts WHERE notification_key = ? AND text = ? AND created_at >= ? LIMIT 1",
            arrayOf(key, text, (System.currentTimeMillis() - withinMillis).toString()),
        ).use { it.moveToFirst() }

    /** Keeps the latest bank alerts on the phone so a misread one can be checked and fixed. */
    fun logAlert(key: String, text: String, outcome: String) {
        val values = ContentValues().apply {
            put("notification_key", key)
            put("text", text)
            put("outcome", outcome)
            put("created_at", System.currentTimeMillis())
        }
        writableDatabase.insert("alerts", null, values)
        writableDatabase.execSQL(
            "DELETE FROM alerts WHERE id NOT IN (SELECT id FROM alerts ORDER BY id DESC LIMIT $MAX_ALERTS)",
        )
        _changes.value++
    }

    fun recentAlerts(): List<AlertEntry> =
        readableDatabase.rawQuery(
            "SELECT text, outcome, created_at FROM alerts ORDER BY id DESC",
            null,
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(AlertEntry(cursor.getString(0), cursor.getString(1), cursor.getLong(2)))
                }
            }
        }

    private fun android.database.Cursor.toExpense() = Expense(
        id = getLong(0),
        cents = getLong(1),
        currency = getString(2),
        convertedCents = if (isNull(3)) null else getLong(3),
        convertedCurrency = if (isNull(4)) null else getString(4),
        category = if (isNull(5)) null else getString(5),
        createdAt = getLong(6),
    )

    companion object {
        private const val MAX_ALERTS = 30
        private const val EXPENSE_COLUMNS =
            "id, cents, currency, converted_cents, converted_currency, category, created_at"

        @Volatile
        private var instance: Db? = null

        fun get(context: Context): Db =
            instance ?: synchronized(this) {
                instance ?: Db(context.applicationContext).also { instance = it }
            }
    }
}

object BankApps {
    private const val PREFS = "settings"
    private const val KEY = "bank_packages"

    fun selected(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet())
            .orEmpty()

    fun setSelected(context: Context, packages: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY, packages)
            .apply()
    }
}

/** The answers offered as buttons in the pop-up after a purchase. */
object PurchaseOptions {
    /** Long enough for "Not answered yet", the longest name the app itself uses. */
    const val MAX_LENGTH = 16

    private const val PREFS = "settings"
    private const val KEY_PREFIX = "purchase_option_"
    private const val KEY_RETIRED = "retired_purchase_options"
    private val DEFAULTS = listOf("Food", "Transport", "Shopping")

    fun all(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return DEFAULTS.mapIndexed { index, default -> prefs.getString("$KEY_PREFIX$index", default) ?: default }
    }

    /** Names that used to be options. Their past purchases are marked as such inside "Others". */
    fun oldOptions(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(KEY_RETIRED, emptySet()).orEmpty()

    fun set(context: Context, options: List<String>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val cleaned = options.map(::clean)
        val retired = retiredAfter(all(context), cleaned, prefs.getStringSet(KEY_RETIRED, emptySet()).orEmpty())
        val editor = prefs.edit().putStringSet(KEY_RETIRED, retired)
        cleaned.forEachIndexed { index, option -> editor.putString("$KEY_PREFIX$index", option) }
        editor.apply()
    }

    /** Names no longer offered, after [before] became [after]. One that's offered again leaves the list. */
    fun retiredAfter(before: List<String>, after: List<String>, retired: Set<String>): Set<String> {
        fun offered(name: String) = after.any { it.equals(name, ignoreCase = true) }
        return (retired + before).filterNot(::offered).toSet()
    }

    /** Each option needs a name, and two with the same name would just be the same button. */
    fun isValid(options: List<String>): Boolean {
        val cleaned = options.map { clean(it).lowercase() }
        return cleaned.none { it.isEmpty() } && cleaned.distinct().size == cleaned.size
    }

    /** Reuses a known name when [typed] only differs by capitals or spaces, so totals don't split. */
    fun matchKnown(typed: String, known: List<String>): String {
        val cleaned = clean(typed)
        return known.firstOrNull { it.equals(cleaned, ignoreCase = true) } ?: cleaned
    }

    private fun clean(text: String): String = text.trim().replace(Regex("""\s+"""), " ")
}

/** A set of purchases that can be opened as its own page from the spending chart. */
sealed interface PurchaseGroup {
    val title: String

    fun matches(expense: Expense, options: List<String>): Boolean

    fun encode(): String

    /** One of today's options. */
    data class Option(val name: String) : PurchaseGroup {
        override val title get() = name
        override fun matches(expense: Expense, options: List<String>) = expense.category.equals(name, ignoreCase = true)
        override fun encode() = "option:$name"
    }

    /** One answer inside "Others", such as a typed answer or a renamed option. */
    data class Answer(val name: String) : PurchaseGroup {
        override val title get() = name
        override fun matches(expense: Expense, options: List<String>) = expense.category.equals(name, ignoreCase = true)
        override fun encode() = "answer:$name"
    }

    data object Unanswered : PurchaseGroup {
        override val title = "Not answered yet"
        override fun matches(expense: Expense, options: List<String>) = expense.category == null
        override fun encode() = "unanswered"
    }

    companion object {
        fun decode(text: String): PurchaseGroup = when {
            text == "unanswered" -> Unanswered
            text.startsWith("option:") -> Option(text.removePrefix("option:"))
            else -> Answer(text.removePrefix("answer:"))
        }
    }
}

/** How big the spending ring is, as its circumference compared with the line's length. */
enum class RingSize(val label: String, val detail: String, val length: Float) {
    SMALL("Small", "Most room for names", 0.9f),
    MEDIUM("Medium", "A balance of both", 1.1f),
    LARGE("Large", "Easiest to read the ring", 1.4f),
}

object RingSizes {
    private const val PREFS = "settings"
    private const val KEY = "ring_size"

    fun selected(context: Context): RingSize {
        val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        return RingSize.entries.firstOrNull { it.name == name } ?: RingSize.MEDIUM
    }

    fun set(context: Context, size: RingSize) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, size.name).apply()
    }
}

object Currencies {
    private const val PREFS = "settings"
    private const val KEY_BANK = "currency"
    private const val KEY_DISPLAY = "display_currency"

    private fun read(context: Context, key: String): Currency? {
        val code = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key, null)
        return Currency.entries.firstOrNull { it.code == code }
    }

    private fun write(context: Context, key: String, currency: Currency) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(key, currency.code)
            .apply()
    }

    fun isBankChosen(context: Context): Boolean = read(context, KEY_BANK) != null

    /** What a bank alert's amount is in, when the alert doesn't say. */
    fun bank(context: Context): Currency = read(context, KEY_BANK) ?: Currency.USD

    fun setBank(context: Context, currency: Currency) = write(context, KEY_BANK, currency)

    /** What the user sees their spending in. Follows the bank's until they choose another. */
    fun display(context: Context): Currency = read(context, KEY_DISPLAY) ?: bank(context)

    fun setDisplay(context: Context, currency: Currency) = write(context, KEY_DISPLAY, currency)
}

/** The app is English only, so its built-in text doesn't follow the phone's language either. */
fun Context.inEnglish(): Context =
    createConfigurationContext(Configuration(resources.configuration).apply { setLocale(Locale.ENGLISH) })

fun Long.toLocalDate() = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
