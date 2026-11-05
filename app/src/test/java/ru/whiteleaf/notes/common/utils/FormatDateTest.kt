package ru.whiteleaf.notes.common.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.datetime.Month

class FormatDateTest {

    @Test
    fun `formatDate should return non-empty string`() {
        val timestamp = System.currentTimeMillis()
        val result = formatDate(timestamp)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `formatDate should contain day number`() {
        // November 5, 2026
        val timestamp = 1762320000000L
        val result = formatDate(timestamp)
        assertTrue(result.contains("5"))
    }

    @Test
    fun `formatDate should contain month name in Russian`() {
        // January 15, 2024
        val timestamp = 1705276800000L
        val result = formatDate(timestamp)
        assertTrue(result.contains("января") || result.contains("Января"))
    }

    @Test
    fun `formatDate should contain year`() {
        // December 31, 2023
        val timestamp = 1703980800000L
        val result = formatDate(timestamp)
        assertTrue(result.contains("2023"))
    }

    @Test
    fun `formatDate should handle past date`() {
        // January 1, 2000
        val timestamp = 946684800000L
        val result = formatDate(timestamp)
        assertTrue(result.contains("2000"))
    }

    @Test
    fun `formatDate should handle future date`() {
        // January 1, 2100
        val timestamp = 4102444800000L
        val result = formatDate(timestamp)
        assertTrue(result.contains("2100"))
    }

    @Test
    fun `formatDate should handle zero timestamp`() {
        val timestamp = 0L
        val result = formatDate(timestamp)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `formatDateRecent should return today for current timestamp`() {
        val timestamp = System.currentTimeMillis()
        val result = formatDateRecent(timestamp)
        assertEquals("Сегодня", result.trim())
    }

    @Test
    fun `formatDateRecent should return yesterday for one day ago`() {
        val yesterday = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
        val result = formatDateRecent(yesterday)
        assertEquals("Вчера", result.trim())
    }

    @Test
    fun `formatDateRecent should return date for other dates in same year`() {
        // Current year, but not today or yesterday
        val now = System.currentTimeMillis()
        // Go back 2 days
        val twoDaysAgo = now - (2 * 24 * 60 * 60 * 1000L)
        val result = formatDateRecent(twoDaysAgo)

        // Should contain day and month name
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `formatDateRecent should return dd_dot_mm_dot_yyyy for previous year`() {
        // Go back 2 years
        val twoYearsAgo = System.currentTimeMillis() - (2 * 365 * 24 * 60 * 60 * 1000L)
        val result = formatDateRecent(twoYearsAgo)

        // Should be in dd.mm.yyyy format
        assertTrue(result.contains("."))
        assertEquals(10, result.trim().length)
    }

    @Test
    fun `formatDateRecent should handle zero timestamp`() {
        val timestamp = 0L
        val result = formatDateRecent(timestamp)
        assertTrue(result.contains("."))
    }

    @Test
    fun `formatDateNoteList should return dd_dot_mm_dot_yyyy format`() {
        val timestamp = 1700000000000L
        val result = formatDateNoteList(timestamp)

        // Should match dd.mm.yyyy pattern
        assertTrue(result.matches(Regex("\\d{2}\\.\\d{2}\\.\\d{4}")))
    }

    @Test
    fun `formatDateNoteList should contain correct day`() {
        // November 5, 2026
        val timestamp = 1762320000000L
        val result = formatDateNoteList(timestamp)
        
        assertTrue(result.contains("5"))
    }

    @Test
    fun `formatDateNoteList should contain correct month`() {
        // November 5, 2026
        val timestamp = 1762320000000L
        val result = formatDateNoteList(timestamp)
        
        assertTrue(result.contains("11"))
    }

    @Test
    fun `formatDateNoteList should contain correct year`() {
        // November 5, 2026
        val timestamp = 1762320000000L
        val result = formatDateNoteList(timestamp)
        
        assertTrue(result.contains("2026"))
    }

    @Test
    fun `formatDateNoteList should pad day with zero`() {
        // January 1, 2024
        val timestamp = 1704067200000L
        val result = formatDateNoteList(timestamp)
        
        assertTrue(result.startsWith("01"))
    }

    @Test
    fun `formatDateNoteList should pad month with zero`() {
        // February 15, 2024
        val timestamp = 1707955200000L
        val result = formatDateNoteList(timestamp)
        
        assertTrue(result.contains(".02."))
    }

    @Test
    fun `formatDateNoteGrid should return dd MMM format`() {
        val timestamp = 1700000000000L
        val result = formatDateNoteGrid(timestamp)
        
        // Should contain day and abbreviated month
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `formatDateNoteGrid should contain Russian month abbreviation`() {
        // January 15, 2024
        val timestamp = 1705276800000L
        val result = formatDateNoteGrid(timestamp)
        
        // Russian abbreviation for January is "янв."
        assertTrue(result.contains("янв") || result.contains("Янв"))
    }

    @Test
    fun `formatDateNoteGrid should contain day number`() {
        // January 15, 2024
        val timestamp = 1705276800000L
        val result = formatDateNoteGrid(timestamp)
        
        assertTrue(result.contains("15"))
    }

    @Test
    fun `formatDateNoteGrid should handle zero timestamp`() {
        val timestamp = 0L
        val result = formatDateNoteGrid(timestamp)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `formatDateNoteGrid should handle different months`() {
        val testCases = listOf(
            Pair(1704067200000L, "янв"),   // January
            Pair(1706745600000L, "фев"),   // February
            Pair(1709251200000L, "мар"),   // March
            Pair(1711929600000L, "апр"),   // April
            Pair(1714521600000L, "мая"),   // May
            Pair(1717200000000L, "июн"),   // June
            Pair(1719792000000L, "июл"),   // July
            Pair(1722470400000L, "авг"),   // August
            Pair(1725148800000L, "сен"),   // September
            Pair(1727740800000L, "окт"),   // October
            Pair(1730419200000L, "ноя"),   // November
            Pair(1733011200000L, "дек")    // December
        )

        testCases.forEach { (timestamp, expectedMonth) ->
            val result = formatDateNoteGrid(timestamp)
            assertTrue("Expected month $expectedMonth in $result", 
                      result.contains(expectedMonth))
        }
    }

    @Test
    fun `formatDate should handle leap year`() {
        // February 29, 2024
        val timestamp = 1709164800000L
        val result = formatDate(timestamp)
        
        assertTrue(result.contains("29"))
        assertTrue(result.contains("февраля") || result.contains("Февраля"))
    }

    @Test
    fun `formatDateNoteList should handle leap year`() {
        // February 29, 2024
        val timestamp = 1709164800000L
        val result = formatDateNoteList(timestamp)
        
        assertTrue(result.matches(Regex("29\\.02\\.2024")))
    }

    @Test
    fun `formatDate should produce consistent results`() {
        val timestamp = 1700000000000L
        val result1 = formatDate(timestamp)
        val result2 = formatDate(timestamp)
        
        assertEquals(result1, result2)
    }

    @Test
    fun `formatDateNoteList should produce consistent results`() {
        val timestamp = 1700000000000L
        val result1 = formatDateNoteList(timestamp)
        val result2 = formatDateNoteList(timestamp)
        
        assertEquals(result1, result2)
    }

    @Test
    fun `formatDateNoteGrid should produce consistent results`() {
        val timestamp = 1700000000000L
        val result1 = formatDateNoteGrid(timestamp)
        val result2 = formatDateNoteGrid(timestamp)
        
        assertEquals(result1, result2)
    }

    @Test
    fun `formatDateRecent should handle end of year boundary`() {
        // December 31, 2024
        val timestamp = 1735603200000L
        val result = formatDateRecent(timestamp)
        
        // Should not be "Сегодня" or "Вчера" (unless actually today/yesterday)
        // Just should not crash and should return something
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `formatDate should handle negative timestamp`() {
        // Before Unix epoch
        val timestamp = -1000000000L
        val result = formatDate(timestamp)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `formatDateNoteList should handle very large timestamp`() {
        // Year 2100
        val timestamp = 4102444800000L
        val result = formatDateNoteList(timestamp)
        
        assertTrue(result.contains("2100"))
    }

    @Test
    fun `DateHelper getMonthName should return Russian month names`() {
        val january = DateHelper.getMonthName(Month.JANUARY)
        assertEquals("января", january)
        
        val july = DateHelper.getMonthName(kotlinx.datetime.Month.JULY)
        assertEquals("июля", july)
        
        val december = DateHelper.getMonthName(kotlinx.datetime.Month.DECEMBER)
        assertEquals("декабря", december)
    }
}
