package app.spendtracker

import java.math.BigDecimal

sealed interface ParseResult {
    data class Spend(val cents: Long, val currency: String) : ParseResult
    data class Skipped(val reason: String) : ParseResult
}

/**
 * Finds the amount spent in a bank notification without relying on its exact wording.
 *
 * It works by ruling numbers out rather than matching a fixed sentence:
 *  1. Notifications that aren't spending at all (login codes, declines, refunds) are skipped.
 *  2. Dates, times, percentages, card digits and reference numbers are removed.
 *  3. Everything from the first cashback/reward clause onward is ignored, so a separate
 *     cashback notification produces no amount and never counts as spending.
 *  4. Anything after a balance or limit word is ignored.
 *  5. The first remaining number wins, preferring the bank's currency, then a plain "$" if
 *     the bank's currency is a dollar, then other currencies, then numbers with decimals,
 *     then whole numbers.
 *  6. The amount keeps the currency written next to it. With none, it's the bank's currency.
 */
object AmountParser {
    private val ignoreCase = setOf(RegexOption.IGNORE_CASE)

    private val notSpending = Regex(
        """\b(otp|verification code|security code|one[- ]time|passcode|password|declined|failed|unsuccessful|rejected|insufficient|refund(ed)?|revers(ed|al)|you received|money received|incoming|deposit(ed)?|credited|top[- ]?up|log[- ]?in|signed? in|promo(tion)?|offer)\b|\d\s*%\s*off""",
        ignoreCase,
    )

    /**
     * The same idea in Russian, matched by word stem to cover word endings. It uses a letter
     * check instead of \b, because \b only recognises Latin letters on some Java versions.
     */
    private val notSpendingRussian = Regex(
        """(?<!\p{L})(код(а|ом)?(?!\p{L})(?!\s+авториз)|парол|промо|вход(а|ом)?(?!\p{L})|входящ|отклон|отказ|неуспешн|не\s+удал|не\s+выполн|ошибк|недостаточно|отмен|возврат|возвращ|сторно|пополн|поступ|зачисл|получен|зарплат|акци|скидк|предложен)""",
        ignoreCase,
    )

    private val rewardWords = Regex(
        """cash[\s-]?back|к[эе]шб[эе]к|(?<!\p{L})бонус|\bbonus(es)?\b|\brewards?\b|\bpoints\b|\bmiles\b""",
        ignoreCase,
    )

    private val balanceWords = Regex(
        """\bbalance\b|\bavailable\b|\bremaining\b|\blimit\b|остаток|баланс|доступно|лимит|сальдо""",
        ignoreCase,
    )

    private const val MONTH = "jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec"
    private const val MONTH_RUSSIAN = "янв|фев|мар|апр|ма[йя]|июн|июл|авг|сен|окт|ноя|дек"

    /** Numbers that are never the amount. Removed before any number is considered. */
    private val noise = listOf(
        Regex("""\b\d{4}[-/.]\d{1,2}[-/.]\d{1,2}\b"""),
        Regex("""\b\d{1,2}[-/.]\d{1,2}[-/.]\d{2,4}\b"""),
        Regex("""\b\d{1,2}/\d{1,2}\b"""),
        Regex("""\b\d{1,2}\s+($MONTH)[a-z]*\.?(,?\s+\d{4})?\b""", ignoreCase),
        Regex("""\b($MONTH)[a-z]*\.?\s+\d{1,2}(,?\s+\d{4})?\b""", ignoreCase),
        Regex("""\b\d{1,2}:\d{2}(:\d{2})?\b"""),
        Regex("""\d+(?:[.,]\d+)?\s*%"""),
        Regex("""[*•#]+\s*\d+"""),
        Regex("""\b[xX]{1,}\d{2,}\b"""),
        Regex(
            """\b(card|account|acct|a/c)\s*(no\.?|number|ending(\s+in)?|#|:)?\s*[*•xX#]*\s*\d{3,}\b""",
            ignoreCase,
        ),
        Regex(
            """\b(ending(\s+in)?|ref(erence)?(\s*(no\.?|number))?|id|auth(orization)?(\s*code)?|transaction\s*(no\.?|number|id))\s*[:#]?\s*\w*\d\w*""",
            ignoreCase,
        ),
        Regex("""(?<!\d)\d{1,2}\s+($MONTH_RUSSIAN)\p{L}*\.?(,?\s+\d{4})?(\s*г\.?)?""", ignoreCase),
        // Four digits after "карта" are the card, unless a currency follows: "на карту 5000 Т".
        Regex(
            """(?<!\p{L})(карт\p{L}*|сч[её]т\p{L}*)\s*(№|#|:|номер)?\s*\d{4}(?!\d)(?!\s*(\p{Sc}|[ТT](?![\p{L}\d-])|тг|тенге|USD|KZT|HKD|SGD))""",
            ignoreCase,
        ),
        Regex("""(?<!\p{L})(код\s+авториз\p{L}*|чек\p{L}*|номер)\s*[:#№]?\s*\d+|№\s*\d+""", ignoreCase),
    )

    private val clauseBreak = Regex("""\n|;|!|\||\.(?=\s|$)|,(?=\s)|\band\b""", ignoreCase)

    private val number = Regex(
        """(?<![\d.,])(\d{1,3}(?:[ ,. ]\d{3})+(?:[.,]\d{1,2})?|\d+(?:[.,]\d{1,2})?)(?![\d])""",
    )

    /** How a currency is written next to a number. The letter check keeps "US$" from reading as "S$". */
    private class Marker(before: String, after: String) {
        private val beforeNumber = Regex("""(?<!\p{L})($before)\s*$""", ignoreCase)
        private val afterNumber = Regex("""^\s*($after)""", ignoreCase)

        fun matches(before: String, after: String) =
            beforeNumber.containsMatchIn(before) || afterNumber.containsMatchIn(after)
    }

    private val markers = mapOf(
        // Freedom Bank writes tenge as a lone "Т". The check keeps "5 T-shirts" from matching.
        "KZT" to Marker("""KZT|₸""", """KZT|₸|тг|тенге|[ТT](?![\p{L}\d-])"""),
        "USD" to Marker("""US\$|USD""", """USD|US\$|US\s+dollars?\b"""),
        "HKD" to Marker("""HK\$|HKD""", """HKD|HK\$|hong\s+kong\s+dollars?\b"""),
        "SGD" to Marker("""S\$|SG\$|SGD""", """SGD|SG\$|S\$|singapore\s+dollars?\b"""),
        "EUR" to Marker("""EUR|€""", """EUR|€"""),
        "GBP" to Marker("""GBP|£""", """GBP|£"""),
        "CNY" to Marker("""CNY|RMB|CN¥""", """CNY|RMB|CN¥"""),
        "RUB" to Marker("""RUB|₽""", """RUB|₽|руб"""),
        "JPY" to Marker("""JPY|¥""", """JPY|¥"""),
    )
    private val plainDollar = Marker("""\$""", """\$|dollars?\b|доллар""")

    /** An amount a person typed, like 12 or 12.50 or 12,50. Null unless it's valid and above 0. */
    fun parseTyped(text: String): Long? =
        text.trim()
            .takeIf { Regex("""\d+([.,]\d{1,2})?""").matches(it) }
            ?.let(::toCents)
            ?.takeIf { it > 0 }

    fun parse(title: String?, body: String?, bank: Currency): ParseResult {
        val text = listOfNotNull(title, body).joinToString("\n").trim()
        if (text.isEmpty()) return ParseResult.Skipped("Empty notification")
        if (notSpending.containsMatchIn(text) || notSpendingRussian.containsMatchIn(text)) {
            return ParseResult.Skipped("Not a purchase")
        }

        var cleaned = text.replace(Regex("""U\.S\.\s*""", ignoreCase), "US ")
        noise.forEach { cleaned = it.replace(cleaned, " ") }

        val clauses = cleaned.split(clauseBreak)
        val rewardAt = clauses.indexOfFirst { rewardWords.containsMatchIn(it) }
        val usable = if (rewardAt >= 0) clauses.take(rewardAt) else clauses

        val best = usable
            .asSequence()
            .map { clause -> balanceWords.find(clause)?.let { clause.substring(0, it.range.first) } ?: clause }
            .flatMap { candidatesIn(it, bank) }
            .minByOrNull { it.rank }

        return when {
            best != null -> ParseResult.Spend(best.cents, best.currency)
            rewardAt >= 0 -> ParseResult.Skipped("Cashback or rewards message")
            else -> ParseResult.Skipped("No amount found")
        }
    }

    private class Candidate(val cents: Long, val currency: String, val rank: Int)

    private fun candidatesIn(clause: String, bank: Currency): Sequence<Candidate> =
        number.findAll(clause).mapNotNull { match ->
            val before = clause.substring(0, match.range.first)
            val after = clause.substring(match.range.last + 1)

            val named = markers.entries.firstOrNull { it.value.matches(before, after) }?.key
            val dollar = named == null && plainDollar.matches(before, after)
            val marked = named != null || dollar

            // A number joined to letters ("S24", "3pm") is only valid next to a currency code.
            if (!marked) {
                if (before.lastOrNull()?.isLetter() == true) return@mapNotNull null
                if (after.firstOrNull()?.isLetter() == true) return@mapNotNull null
            }

            val raw = match.value
            val hasDecimals = Regex("""[.,]\d{1,2}$""").containsMatchIn(raw)
            val digitCount = raw.count { it.isDigit() }
            if (!marked && !hasDecimals && digitCount > 7) return@mapNotNull null

            val cents = toCents(raw) ?: return@mapNotNull null
            if (cents <= 0 || cents >= 100_000_000_00L) return@mapNotNull null

            val rank = when {
                named == bank.code -> 0
                dollar && bank.usesDollarSign -> 1
                marked -> 2
                hasDecimals -> 3
                else -> 4
            }
            // Only a bank in another dollar can make a plain "$" mean anything but US dollars.
            val code = named ?: if (dollar && !bank.usesDollarSign) "USD" else bank.code
            Candidate(cents, code, rank)
        }

    /** Handles 12, 12.5, 12,50, 1,234.56, 1.234,56 and 1 234,56. */
    internal fun toCents(raw: String): Long? {
        val compact = raw.replace(" ", "").replace(" ", "")
        val decimals = Regex("""[.,](\d{1,2})$""").find(compact)?.groupValues?.get(1)
        val whole = (if (decimals != null) compact.dropLast(decimals.length + 1) else compact)
            .replace(",", "")
            .replace(".", "")
        return runCatching {
            BigDecimal("$whole.${decimals ?: "0"}").movePointRight(2).toBigInteger().toLong()
        }.getOrNull()
    }
}
