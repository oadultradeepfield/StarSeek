package com.oadultradeepfield.starseek.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val dateFormat by lazy { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

fun formatRelativeDate(timestamp: Long): String {
  val date = Date(timestamp)
  val calendar = Calendar.getInstance()
  val today = calendar.clone() as Calendar

  calendar.time = date

  return when {
    isSameDay(calendar, today) -> "Today"
    isYesterday(calendar, today) -> "Yesterday"
    else -> dateFormat.format(date)
  }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
  return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
      cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(cal1: Calendar, cal2: Calendar): Boolean {
  val yesterday = cal2.clone() as Calendar
  yesterday.add(Calendar.DAY_OF_YEAR, -1)
  return isSameDay(cal1, yesterday)
}
