package app.spendtracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AmountParserTest {
    private fun assertSpend(
        cents: Long,
        title: String?,
        body: String?,
        bank: Currency = Currency.USD,
        currency: String = bank.code,
    ) {
        assertEquals("$title | $body", ParseResult.Spend(cents, currency), AmountParser.parse(title, body, bank))
    }

    private fun assertSkipped(title: String?, body: String?, bank: Currency = Currency.USD) {
        val result = AmountParser.parse(title, body, bank)
        assertTrue("$title | $body gave $result", result is ParseResult.Skipped)
    }

    @Test
    fun `reads an amount someone typed`() {
        assertEquals(1250L, AmountParser.parseTyped("12.50"))
        assertEquals(1250L, AmountParser.parseTyped(" 12,50 "))
        assertEquals(1200L, AmountParser.parseTyped("12"))
        assertEquals(null, AmountParser.parseTyped("0"))
        assertEquals(null, AmountParser.parseTyped("12.505"))
        assertEquals(null, AmountParser.parseTyped("abc"))
        assertEquals(null, AmountParser.parseTyped(""))
    }

    private val freedomAlert =
        "Alipayhk*coffee shop\n\n%s\n\nДоступно 100 000,00 Т\n\nСписано с SuperCard **1234"

    @Test
    fun `reads a real Freedom Bank purchase alert`() {
        assertSpend(627, "Покупка", freedomAlert.format("-6,27 $"), Currency.USD)
        assertSpend(627, "Покупка", freedomAlert.format("-6,27 $"), Currency.KZT, "USD")
    }

    @Test
    fun `reads tenge written as a lone T, in either alphabet`() {
        assertSpend(500000, "Покупка", freedomAlert.format("-5 000,00 Т"), Currency.USD, "KZT")
        assertSpend(500000, "Покупка", freedomAlert.format("-5 000,00 T"), Currency.USD, "KZT")
        assertSpend(500000, "Покупка", "Перевод на карту 5000 Т", Currency.USD, "KZT")
        assertSpend(1000, "Purchase", "5 T-shirts for $10.00", Currency.USD)
    }

    @Test
    fun `skips Russian messages that are not purchases`() {
        assertSkipped("Безопасность", "Ваш код подтверждения: 482913")
        assertSkipped("Банк", "Код для входа 4829")
        assertSkipped("Банк", "Выполнен вход в приложение 14:30")
        assertSkipped("Покупка", "Операция на 10,00 USD отклонена")
        assertSkipped("Покупка", "Недостаточно средств: 10,00 USD")
        assertSkipped("Покупка", "Не удалось списать 10,00 USD")
        assertSkipped("Возврат", freedomAlert.format("6,27 $"))
        assertSkipped("Банк", "Отмена операции 10,00 USD")
        assertSkipped("Банк", "Пополнение на 50,00 USD")
        assertSkipped("Банк", "Вам поступил перевод 50,00 USD")
        assertSkipped("Банк", "Зачисление 50 000,00 Т")
        assertSkipped("Банк", "Скидка 20% на первую покупку до 15 ноября")
        assertSkipped("Банк", "Вам начислены бонусы 100")
        assertSkipped("Банк", "Вам начислен кешбэк 0,25 USD")
    }

    @Test
    fun `ignores Russian dates, cards and references`() {
        assertSpend(1000, "Покупка", "Карта 1234 покупка 10")
        assertSpend(1000, "Покупка", "5 ноября 2026 г., 10")
        assertSpend(1000, "Покупка", "Покупка 10 USD, код авторизации 123456")
        assertSpend(1000, "Покупка", "Чек 123456, сумма 10")
        assertSpend(1000, "Покупка", "10,00 USD, лимит 500,00 USD")
    }

    @Test
    fun `a Russian word inside a longer word doesn't skip a purchase`() {
        assertSpend(1000, "Покупка", "Магазин Кодак 10,00 USD")
    }

    @Test
    fun `prefers the bank's currency when a purchase shows two`() {
        assertSpend(10000, "Purchase", "HKD 100.00 (USD 12.80)", Currency.HKD)
        assertSpend(10000, "Purchase", "HK$100.00 charged as $12.80", Currency.HKD)
        assertSpend(2500, "Purchase", "SGD 25.00 (USD 18.50)", Currency.SGD)
        assertSpend(2500, "Purchase", "US$18.50 (S$25.00)", Currency.SGD)
        assertSpend(1850, "Purchase", "S$25.00 charged as $18.50", Currency.USD)
        assertSpend(490000, "Purchase", "$10.00 (4 900 ₸)", Currency.KZT)
    }

    @Test
    fun `a plain dollar sign means the bank's currency when it is a dollar`() {
        assertSpend(10000, "Purchase", "$100.00", Currency.HKD)
        assertSpend(10000, "Purchase", "Spent 100 dollars", Currency.HKD)
        assertSpend(2500, "Purchase", "$25.00", Currency.SGD)
    }

    @Test
    fun `reads each currency's own markers`() {
        assertSpend(1250, "Purchase", "Spent HK$12.50", Currency.HKD)
        assertSpend(1250, "Purchase", "12.50 Hong Kong dollars", Currency.HKD)
        assertSpend(1250, "Purchase", "Spent S$12.50", Currency.SGD)
        assertSpend(1250, "Purchase", "12.50 Singapore dollars", Currency.SGD)
        assertSpend(500000, "Purchase", "5000 ₸ Magnum", Currency.KZT)
        assertSpend(500000, "Purchase", "5 000,00 тг", Currency.KZT)
        assertSpend(500000, "Purchase", "KZT 5,000", Currency.KZT)
    }

    @Test
    fun `an amount keeps the currency written next to it`() {
        assertSpend(1000, "Purchase", "EUR 10.00", Currency.HKD, "EUR")
        assertSpend(1000, "Purchase", "€10.00", Currency.USD, "EUR")
        assertSpend(1000, "Purchase", "USD 10.00", Currency.HKD, "USD")
        assertSpend(1000, "Purchase", "$10.00", Currency.KZT, "USD")
        assertSpend(1000, "Purchase", "CN¥10.00", Currency.USD, "CNY")
        assertSpend(10000, "Purchase", "HKD 100.00 (SGD 17.00)", Currency.KZT, "HKD")
    }

    @Test
    fun `an amount with no currency is in the bank's`() {
        assertSpend(4500, "Debit", "Amount: 45", Currency.HKD)
        assertSpend(1000, "Purchase", "Card 1234 at 14:30 on 05.11.2026: 10.00", Currency.KZT)
    }

    @Test
    fun `formats amounts in each currency`() {
        assertEquals("$1,234.56", Currency.USD.format(123456))
        assertEquals("HK$1,234.56", Currency.HKD.format(123456))
        assertEquals("S$1,234.56", Currency.SGD.format(123456))
        assertEquals("1,234.56 ₸", Currency.KZT.format(123456))
    }

    @Test
    fun `reads the amount regardless of wording`() {
        assertSpend(1250, "Freedom Bank", "You spent 12.50 US dollars")
        assertSpend(1250, "Purchase", "USD 12.50 at Starbucks")
        assertSpend(1250, null, "Payment of $12.50 was successful")
        assertSpend(1250, "Card payment", "-US$12.50")
        assertSpend(1250, "Покупка", "Вы потратили 12,50 USD")
        assertSpend(1200, "Spent", "12 dollars")
        assertSpend(1200, "Payment", "12USD at a shop")
        assertSpend(4500, "Debit", "Amount: 45")
    }

    @Test
    fun `reads thousands separators`() {
        assertSpend(123456, "Purchase", "$1,234.56")
        assertSpend(123456, "Purchase", "1.234,56 USD")
        assertSpend(123456, "Purchase", "1 234,56 USD")
        assertSpend(123400, "Purchase", "USD 1,234")
    }

    @Test
    fun `cashback notifications are never counted as spending`() {
        assertSkipped("Cashback", "$0.25")
        assertSkipped("Freedom Bank", "You earned $0.25 cashback")
        assertSkipped("Freedom Bank", "Cashback earned: 0.25 USD")
        assertSkipped("Freedom Bank", "Cash back of $0.25 from your purchase")
        assertSkipped("Freedom Bank", "You got cashback\n$0.25")
        assertSkipped("Freedom Bank", "You earned cashback of $0.25 on your $10.00 purchase")
        assertSkipped("Freedom Bank", "Вам начислен кэшбэк 0,25 USD")
    }

    @Test
    fun `takes the spending amount when cashback follows in the same notification`() {
        assertSpend(1000, "Purchase", "You spent $10.00. Cashback: $0.25")
        assertSpend(1000, "Purchase", "You spent $10.00 and earned $0.25 cashback")
    }

    @Test
    fun `ignores balances and limits`() {
        assertSpend(1000, "Purchase", "$10.00 spent, balance $500.00")
        assertSpend(1000, "Purchase", "Available balance $500.00. You spent $10.00")
        assertSpend(1000, "Purchase", "Spent $10.00 available balance $500.00")
        assertSkipped("Freedom Bank", "Your balance is now $500.00")
    }

    @Test
    fun `ignores card digits dates times and references`() {
        assertSpend(1000, "Purchase", "Card *1234 spent $10.00")
        assertSpend(1000, "Purchase", "Card ending in 1234: 10.00 USD")
        assertSpend(1000, "Purchase", "Card 1234 at 14:30 on 05.11.2026: 10.00")
        assertSpend(1000, "Purchase", "5 Nov 2026, $10.00, ref 987654321")
        assertSpend(1000, "Purchase", "Nov 5, 2026 spent $10.00")
        assertSpend(1000, "Purchase", "Galaxy S24 store $10.00")
    }

    @Test
    fun `prefers US dollars when a purchase shows two currencies`() {
        assertSpend(1280, "Purchase", "HKD 100.00 (USD 12.80)")
        assertSpend(1280, "Purchase", "HK$100.00 charged as $12.80")
    }

    @Test
    fun `skips messages that are not purchases`() {
        assertSkipped("Security", "Your OTP is 482913")
        assertSkipped("Freedom Bank", "Verification code: 482913")
        assertSkipped("Purchase", "Payment of $10.00 declined")
        assertSkipped("Freedom Bank", "Refund of $10.00")
        assertSkipped("Freedom Bank", "You received $50.00 from Sam")
        assertSkipped("Freedom Bank", "Get 20% off with our new offer")
        assertSkipped("Freedom Bank", "Your statement is ready")
        assertSkipped(null, null)
    }
}
