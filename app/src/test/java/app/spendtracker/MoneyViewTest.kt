package app.spendtracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MoneyViewTest {
    private val perUsd = mapOf("USD" to 1.0, "HKD" to 8.0, "KZT" to 500.0, "EUR" to 0.5)
    private val inHkd = MoneyView(Currency.HKD, perUsd, asOf = 0)

    private fun expense(
        cents: Long,
        currency: String,
        convertedCents: Long? = null,
        convertedCurrency: String? = null,
    ) = Expense(0, cents, currency, convertedCents, convertedCurrency, null, 0)

    @Test
    fun `converts into the display currency`() {
        assertEquals(10000L, inHkd.inDisplay(1250, "USD"))
        assertEquals("HK$100.00", inHkd.show(1250, "USD"))
    }

    @Test
    fun `converts between two currencies that are not dollars`() {
        assertEquals(1600L, inHkd.inDisplay(100000, "KZT"))
        assertEquals(1600L, inHkd.inDisplay(100, "EUR"))
    }

    @Test
    fun `leaves the display currency untouched`() {
        val purchase = expense(1234, "HKD")
        assertEquals(1234L, inHkd.inDisplay(purchase))
        assertEquals("HK$12.34", inHkd.show(purchase))
        assertNull(inHkd.original(purchase))
    }

    @Test
    fun `a saved amount doesn't change when rates do`() {
        val purchase = expense(1250, "USD", 10000, "HKD")
        val laterRates = MoneyView(Currency.HKD, mapOf("USD" to 1.0, "HKD" to 9.0), asOf = 0)

        assertEquals("HK$100.00", laterRates.show(purchase))
        assertEquals("$12.50", laterRates.original(purchase))
        assertEquals(10000L, laterRates.total(listOf(purchase)))
        assertFalse(laterRates.usesLatestRate(listOf(purchase)))
    }

    @Test
    fun `a saved amount needs no rates`() {
        val purchase = expense(1250, "USD", 10000, "HKD")
        val noRates = MoneyView(Currency.HKD, emptyMap(), asOf = null)

        assertEquals("HK$100.00", noRates.show(purchase))
        assertEquals(0, noRates.unconvertedCount(listOf(purchase)))
    }

    @Test
    fun `changing the display currency uses the latest rate, and changing back restores the saved amount`() {
        val purchase = expense(1250, "USD", 10000, "HKD")
        val inKzt = MoneyView(Currency.KZT, perUsd, asOf = 0)
        val backInHkd = MoneyView(Currency.HKD, mapOf("USD" to 1.0, "HKD" to 9.0), asOf = 0)

        assertEquals(625000L, inKzt.inDisplay(purchase))
        assertTrue(inKzt.usesLatestRate(listOf(purchase)))
        assertEquals(10000L, backInHkd.inDisplay(purchase))
    }

    @Test
    fun `a purchase saved without a rate uses the latest one`() {
        val purchase = expense(1250, "USD")

        assertEquals(10000L, inHkd.inDisplay(purchase))
        assertTrue(inHkd.usesLatestRate(listOf(purchase)))
    }

    @Test
    fun `totals purchases made in different currencies`() {
        val expenses = listOf(expense(1250, "USD", 10000, "HKD"), expense(5000, "HKD"), expense(100000, "KZT", 1600, "HKD"))
        assertEquals(10000L + 5000L + 1600L, inHkd.total(expenses))
        assertEquals(0, inHkd.unconvertedCount(expenses))
    }

    @Test
    fun `without rates it shows the original and leaves it out of the total`() {
        val noRates = MoneyView(Currency.HKD, emptyMap(), asOf = null)
        val unconverted = expense(1250, "USD")
        val expenses = listOf(unconverted, expense(5000, "HKD"))

        assertNull(noRates.inDisplay(unconverted))
        assertEquals("$12.50", noRates.show(unconverted))
        assertNull(noRates.original(unconverted))
        assertEquals(5000L, noRates.total(expenses))
        assertEquals(1, noRates.unconvertedCount(expenses))
    }

    @Test
    fun `needs no rates when nothing has to be converted`() {
        val noRates = MoneyView(Currency.USD, emptyMap(), asOf = null)
        val expenses = listOf(expense(1250, "USD"))

        assertEquals(1250L, noRates.total(expenses))
        assertEquals(0, noRates.unconvertedCount(expenses))
        assertFalse(noRates.usesLatestRate(expenses))
    }

    private fun answered(cents: Long, category: String?) = Expense(0, cents, "HKD", null, null, category, 0)

    private val options = listOf("Food", "Transport", "Shopping")

    @Test
    fun `groups spending into the options in their own order, then Others, then unanswered`() {
        val groups = inHkd.spendingGroups(
            listOf(
                answered(100, "Food"),
                answered(900, "Transport"),
                answered(200, "Gift"),
                answered(300, "Coffee"),
                answered(50, "food"),
                answered(400, null),
            ),
            options,
        )

        assertEquals(
            listOf(
                SpendingGroup("Food", 150, SpendingGroup.Kind.OPTION),
                SpendingGroup("Transport", 900, SpendingGroup.Kind.OPTION),
                SpendingGroup("Others", 500, SpendingGroup.Kind.OTHERS),
                SpendingGroup("Not answered yet", 400, SpendingGroup.Kind.UNANSWERED),
            ),
            groups,
        )
    }

    @Test
    fun `purchases under a renamed option go into Others`() {
        val groups = inHkd.spendingGroups(
            listOf(answered(300, "Food"), answered(100, "Meals"), answered(50, "Gift")),
            options = listOf("Meals", "Transport", "Shopping"),
        )

        assertEquals(
            listOf(
                SpendingGroup("Meals", 100, SpendingGroup.Kind.OPTION),
                SpendingGroup("Others", 350, SpendingGroup.Kind.OTHERS),
            ),
            groups,
        )
    }

    @Test
    fun `what's inside Others keeps each answer, largest first, with old options marked`() {
        val inside = inHkd.othersBreakdown(
            listOf(
                answered(300, "Food"),
                answered(100, "Meals"),
                answered(50, "Gift"),
                answered(20, "coffee"),
                answered(40, "Coffee"),
                answered(10, "Gym"),
                answered(5, null),
            ),
            options = listOf("Meals", "Transport", "Shopping"),
            oldOptions = setOf("Food", "Gym"),
        )

        assertEquals(
            listOf(
                OthersGroup("Food", 300, 1, oldOption = true),
                OthersGroup("coffee", 60, 2, oldOption = false),
                OthersGroup("Gift", 50, 1, oldOption = false),
                OthersGroup("Gym", 10, 1, oldOption = true),
            ),
            inside,
        )
    }

    @Test
    fun `what's inside Others adds up to Others`() {
        val expenses = listOf(answered(300, "Food"), answered(50, "Gift"), answered(100, "Meals"), answered(7, null))
        val options = listOf("Meals", "Transport", "Shopping")
        val others = inHkd.spendingGroups(expenses, options).first { it.kind == SpendingGroup.Kind.OTHERS }

        assertEquals(others.cents, inHkd.othersBreakdown(expenses, options, setOf("Food")).sumOf { it.cents })
    }

    @Test
    fun `each group's page shows the purchases it is made of`() {
        val food = answered(100, "food")
        val gift = answered(200, "Gift")
        val open = answered(300, null)

        assertTrue(PurchaseGroup.Option("Food").matches(food, options))
        assertFalse(PurchaseGroup.Option("Food").matches(gift, options))
        assertTrue(PurchaseGroup.Answer("gift").matches(gift, options))
        assertTrue(PurchaseGroup.Unanswered.matches(open, options))
        assertFalse(PurchaseGroup.Unanswered.matches(food, options))
    }

    @Test
    fun `a group survives being saved as text`() {
        listOf(PurchaseGroup.Option("Food"), PurchaseGroup.Answer("Birthday gift"), PurchaseGroup.Unanswered)
            .forEach { assertEquals(it, PurchaseGroup.decode(it.encode())) }
    }

    @Test
    fun `leaves out groups with no purchases`() {
        assertEquals(
            listOf(SpendingGroup("Food", 100, SpendingGroup.Kind.OPTION)),
            inHkd.spendingGroups(listOf(answered(100, "Food")), options),
        )
        assertEquals(
            listOf(SpendingGroup("Not answered yet", 100, SpendingGroup.Kind.UNANSWERED)),
            inHkd.spendingGroups(listOf(answered(100, null)), options),
        )
    }

    @Test
    fun `spending groups add up to the total`() {
        val expenses = listOf(answered(100, "Food"), answered(200, "Gift"), answered(300, null))
        assertEquals(inHkd.total(expenses), inHkd.spendingGroups(expenses, options).sumOf { it.cents })
    }

    @Test
    fun `a changed amount keeps the purchase day's rate`() {
        val changed = expense(1000, "USD", 7843, "HKD").withAmount(2000)

        assertEquals(2000L, changed.cents)
        assertEquals(15686L, changed.convertedCents)
        assertEquals("HKD", changed.convertedCurrency)
    }

    @Test
    fun `a changed amount with no saved conversion stays unconverted`() {
        assertNull(expense(1000, "USD").withAmount(2000).convertedCents)
    }

    @Test
    fun `labels a date range`() {
        val first = LocalDate.of(2026, 11, 1)
        val sixth = LocalDate.of(2026, 11, 6)

        assertEquals("6 Nov 2026", DateRange(sixth, sixth).label())
        assertEquals("1 Nov – 6 Nov 2026", DateRange(first, sixth).label())
        assertEquals("20 Dec 2025 – 6 Nov 2026", DateRange(LocalDate.of(2025, 12, 20), sixth).label())
    }

    @Test
    fun `a date range survives being saved as text`() {
        val range = DateRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 6))
        assertEquals(range, DateRange.decode(range.encode()))
    }

    @Test
    fun `splits an amount into price tag parts`() {
        assertEquals(PriceParts("HK$", "1,234", "56", signAfter = false), Currency.HKD.priceParts(123456))
        assertEquals(PriceParts("₸", "5,000", "00", signAfter = true), Currency.KZT.priceParts(500000))
        assertEquals(PriceParts("$", "0", "05", signAfter = false), Currency.USD.priceParts(5))
    }

    @Test
    fun `labels currencies the app can't be set to by their code`() {
        assertEquals("EUR 1,234.50", formatAmount(123450, "EUR"))
        assertEquals("1,234.50 ₸", formatAmount(123450, "KZT"))
    }
}
