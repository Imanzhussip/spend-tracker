package app.spendtracker

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToLong

data class RateSnapshot(val perUsd: Map<String, Double>, val asOf: Long, val nextUpdate: Long)

/**
 * Keeps the latest exchange rates on the phone. The request is a plain download that contains
 * none of the user's data, and it runs at most once per daily update. It runs when the app
 * opens and when a purchase needs converting, so the rate service could see the time of
 * the day's first such purchase, but never what it was.
 */
object Rates {
    private const val SOURCE = "https://open.er-api.com/v6/latest/USD"
    private const val PREFS = "settings"
    private const val KEY = "exchange_rates"

    private val refreshing = Mutex()
    private var state: MutableStateFlow<RateSnapshot?>? = null

    fun flow(context: Context): StateFlow<RateSnapshot?> = stateFor(context)

    private fun stateFor(context: Context): MutableStateFlow<RateSnapshot?> = synchronized(this) {
        state ?: MutableStateFlow(load(context)).also { state = it }
    }

    suspend fun refreshIfDue(context: Context, timeoutMillis: Int = 10_000): Unit = withContext(Dispatchers.IO) {
        refreshing.withLock {
            val rates = stateFor(context)
            val current = rates.value
            if (current != null && System.currentTimeMillis() < current.nextUpdate) return@withLock

            // A failure keeps the rates already saved. The next attempt tries again.
            val body = runCatching { download(timeoutMillis) }.getOrNull() ?: return@withLock
            val snapshot = parseOrNull(body) ?: return@withLock
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, body).apply()
            rates.value = snapshot
        }
    }

    private fun load(context: Context): RateSnapshot? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)?.let(::parseOrNull)

    private fun download(timeoutMillis: Int): String? {
        val connection = URL(SOURCE).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = timeoutMillis
            connection.readTimeout = timeoutMillis
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseOrNull(body: String): RateSnapshot? = runCatching {
        val json = JSONObject(body)
        if (json.getString("result") != "success") return null
        val rates = json.getJSONObject("rates")
        val perUsd = rates.keys().asSequence()
            .associateWith { rates.getDouble(it) }
            .filterValues { it > 0 }
        if (perUsd.isEmpty()) return null
        RateSnapshot(
            perUsd = perUsd,
            asOf = json.getLong("time_last_update_unix") * 1000,
            nextUpdate = json.getLong("time_next_update_unix") * 1000,
        )
    }.getOrNull()
}

/** Turns saved purchases into what the user asked to see. */
class MoneyView(val display: Currency, private val perUsd: Map<String, Double>, val asOf: Long?) {

    /** Null when there's no rate yet, for example if the phone has never been online. */
    fun inDisplay(cents: Long, code: String): Long? {
        if (code == display.code) return cents
        val from = perUsd[code] ?: return null
        val to = perUsd[display.code] ?: return null
        return (cents / from * to).roundToLong()
    }

    private fun hasSavedAmount(expense: Expense) =
        expense.convertedCents != null && expense.convertedCurrency == display.code

    /** The amount saved on the purchase day, or the latest rate's if it was saved in another currency. */
    fun inDisplay(expense: Expense): Long? =
        if (hasSavedAmount(expense)) expense.convertedCents else inDisplay(expense.cents, expense.currency)

    /** The amount as a price tag, or null if it can't be converted yet. */
    fun price(expense: Expense): PriceParts? = inDisplay(expense)?.let(display::priceParts)

    /** The converted amount, or the original one if it can't be converted yet. */
    fun show(cents: Long, code: String): String =
        inDisplay(cents, code)?.let(display::format) ?: formatAmount(cents, code)

    fun show(expense: Expense): String =
        inDisplay(expense)?.let(display::format) ?: formatAmount(expense.cents, expense.currency)

    /** The original amount, when the amount shown is a conversion of it. */
    fun original(expense: Expense): String? =
        if (expense.currency != display.code && inDisplay(expense) != null) {
            formatAmount(expense.cents, expense.currency)
        } else {
            null
        }

    fun total(expenses: List<Expense>): Long = expenses.sumOf { inDisplay(it) ?: 0L }

    fun unconvertedCount(expenses: List<Expense>): Int = expenses.count { inDisplay(it) == null }

    /** Whether any amount comes from the latest rate instead of its purchase day's. */
    fun usesLatestRate(expenses: List<Expense>): Boolean =
        expenses.any { it.currency != display.code && !hasSavedAmount(it) }

    /**
     * Where the money went: today's [options] in their own order, then "Others" for every other
     * answer, including options since renamed, then "Not answered yet", so the groups always add
     * up to the total. A fixed order keeps each group in the same place and colour every month.
     * Each purchase keeps its own answer in the list of purchases.
     */
    fun spendingGroups(expenses: List<Expense>, options: List<String>): List<SpendingGroup> {
        fun optionFor(expense: Expense) = options.firstOrNull { it.equals(expense.category, ignoreCase = true) }

        val answered = expenses.filter { it.category != null }
        val byOption = answered.filter { optionFor(it) != null }.groupBy { optionFor(it).orEmpty() }
        val chosen = options.mapNotNull { option ->
            byOption[option]?.let { SpendingGroup(option, total(it), SpendingGroup.Kind.OPTION) }
        }
        val others = answered.filter { optionFor(it) == null }
        val unanswered = expenses.filter { it.category == null }

        return buildList {
            addAll(chosen)
            if (others.isNotEmpty()) add(SpendingGroup("Others", total(others), SpendingGroup.Kind.OTHERS))
            if (unanswered.isNotEmpty()) add(SpendingGroup("Not answered yet", total(unanswered), SpendingGroup.Kind.UNANSWERED))
        }
    }

    /**
     * What's inside "Others": one group per answer, largest first, ignoring capitals. An answer
     * that used to be one of the options, listed in [oldOptions], is marked as one.
     */
    fun othersBreakdown(expenses: List<Expense>, options: List<String>, oldOptions: Set<String>): List<OthersGroup> =
        expenses
            .filter { expense -> expense.category != null && options.none { it.equals(expense.category, ignoreCase = true) } }
            .groupBy { it.category.orEmpty().lowercase() }
            .map { (_, list) ->
                val name = list.first().category.orEmpty()
                OthersGroup(name, total(list), list.size, oldOption = oldOptions.any { it.equals(name, ignoreCase = true) })
            }
            .sortedByDescending { it.cents }
}

data class SpendingGroup(val name: String, val cents: Long, val kind: Kind) {
    enum class Kind { OPTION, OTHERS, UNANSWERED }
}

data class OthersGroup(val name: String, val cents: Long, val count: Int, val oldOption: Boolean)

fun moneyView(context: Context): MoneyView {
    val rates = Rates.flow(context).value
    return MoneyView(Currencies.display(context), rates?.perUsd.orEmpty(), rates?.asOf)
}
