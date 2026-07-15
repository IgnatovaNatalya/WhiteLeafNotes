package ru.whiteleaf.notes.common.utils

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun formatDate(timestamp: Long): String {
    val date = Instant.fromEpochMilliseconds(timestamp)
        .toLocalDateTime(TimeZone.currentSystemDefault())

    return "${date.day} ${DateHelper.getMonthName(date.month)} ${date.year}"
}

@OptIn(ExperimentalTime::class)
fun formatDateRecent(timestamp: Long): String {
    val date = Instant.fromEpochMilliseconds(timestamp)
        .toLocalDateTime(TimeZone.currentSystemDefault())

    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    return when {
        // Сегодня
        date.year == now.year && date.dayOfYear == now.dayOfYear -> "Cегодня"

        // Вчера
        date.year == now.year && date.dayOfYear == now.dayOfYear - 1 -> " Вчера"

        // Текущий год - "день месяц"
        date.year == now.year -> "${date.day} ${DateHelper.getMonthName(date.month)}"

        // Прошлые годы - "dd.mm.yyyy"
        else -> {
            val day = date.day.toString().padStart(2, '0')
            val month = date.month.number.toString().padStart(2, '0')
            "$day.$month.${date.year}"
        }
    }
}

@OptIn(ExperimentalTime::class)
fun formatDateNoteList(timestamp: Long): String {
    val date = Instant.fromEpochMilliseconds(timestamp)
        .toLocalDateTime(TimeZone.currentSystemDefault())

    val day = date.day.toString().padStart(2, '0')
    val month = date.month.number.toString().padStart(2, '0')
    return "$day.$month.${date.year}"
}