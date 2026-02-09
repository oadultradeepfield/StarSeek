package com.oadultradeepfield.starseek.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DateUtilsTest {
  @Test
  fun `formatRelativeDate returns Today for current timestamp`() {
    val now = System.currentTimeMillis()
    assertEquals("Today", formatRelativeDate(now))
  }

  @Test
  fun `formatRelativeDate returns Today for earlier today`() {
    val earlierToday =
        Calendar.getInstance()
            .apply {
              set(Calendar.HOUR_OF_DAY, 0)
              set(Calendar.MINUTE, 1)
              set(Calendar.SECOND, 0)
              set(Calendar.MILLISECOND, 0)
            }
            .timeInMillis

    assertEquals("Today", formatRelativeDate(earlierToday))
  }

  @Test
  fun `formatRelativeDate returns Yesterday for yesterday`() {
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
    assertEquals("Yesterday", formatRelativeDate(yesterday))
  }

  @Test
  fun `formatRelativeDate returns formatted date for older dates`() {
    val sevenDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
    val result = formatRelativeDate(sevenDaysAgo)

    assertTrue(
        "Expected format 'MMM d, yyyy' but got: $result",
        result.matches(Regex("[A-Z][a-z]{2} \\d{1,2}, \\d{4}")),
    )
  }

  @Test
  fun `formatRelativeDate includes year for past year dates`() {
    val dec2025 =
        Calendar.getInstance()
            .apply {
              set(Calendar.YEAR, 2025)
              set(Calendar.MONTH, Calendar.DECEMBER)
              set(Calendar.DAY_OF_MONTH, 31)
            }
            .timeInMillis
    val result = formatRelativeDate(dec2025)

    assertTrue("Expected result to contain '2025' but got: $result", result.contains("2025"))
  }
}
