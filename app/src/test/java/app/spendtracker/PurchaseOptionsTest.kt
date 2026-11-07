package app.spendtracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseOptionsTest {
    private val known = listOf("Food", "Кофе", "Transport")

    @Test
    fun `typed answers merge with a known name when only capitals or spaces differ`() {
        assertEquals("Food", PurchaseOptions.matchKnown("food", known))
        assertEquals("Food", PurchaseOptions.matchKnown("  FOOD  ", known))
        assertEquals("Кофе", PurchaseOptions.matchKnown("кофе", known))
    }

    @Test
    fun `a new typed answer is kept, with stray spaces removed`() {
        assertEquals("Gift", PurchaseOptions.matchKnown("Gift", known))
        assertEquals("Gift for mum", PurchaseOptions.matchKnown("  Gift   for  mum ", known))
    }

    @Test
    fun `different names are not merged`() {
        assertEquals("coffee", PurchaseOptions.matchKnown("coffee", known))
        assertEquals("Foods", PurchaseOptions.matchKnown("Foods", known))
    }

    @Test
    fun `a renamed option is remembered`() {
        val before = listOf("Food", "Transport", "Shopping")
        val after = listOf("Meals", "Transport", "Shopping")

        assertEquals(setOf("Food"), PurchaseOptions.retiredAfter(before, after, emptySet()))
    }

    @Test
    fun `a second rename adds to the names remembered`() {
        val before = listOf("Meals", "Transport", "Shopping")
        val after = listOf("Meals", "Taxi", "Shopping")

        assertEquals(setOf("Food", "Transport"), PurchaseOptions.retiredAfter(before, after, setOf("Food")))
    }

    @Test
    fun `an option brought back is no longer remembered as old`() {
        val before = listOf("Meals", "Transport", "Shopping")
        val after = listOf("food", "Transport", "Shopping")

        assertEquals(setOf("Meals"), PurchaseOptions.retiredAfter(before, after, setOf("Food")))
    }

    @Test
    fun `the longest name the app uses fits the limit`() {
        assertTrue("Not answered yet".length <= PurchaseOptions.MAX_LENGTH)
    }

    @Test
    fun `options must be filled in and different`() {
        assertTrue(PurchaseOptions.isValid(listOf("Food", "Transport", "Shopping")))
        assertFalse(PurchaseOptions.isValid(listOf("Food", "", "Shopping")))
        assertFalse(PurchaseOptions.isValid(listOf("Food", "   ", "Shopping")))
        assertFalse(PurchaseOptions.isValid(listOf("Food", "food ", "Shopping")))
        assertFalse(PurchaseOptions.isValid(listOf("Кофе", "кофе", "Shopping")))
    }
}
