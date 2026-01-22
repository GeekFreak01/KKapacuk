package com.example.scheduler

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month

enum class ScheduleVariant {
    SEP_NOV_APR_JUN,
    DEC_MAR
}

enum class DayType {
    WEEKDAY,
    SATURDAY
}

data class Lesson(
    val start: LocalTime,
    val breakStart: LocalTime,
    val breakEnd: LocalTime,
    val end: LocalTime
) {
    fun durationMinutes(): Long = Duration.between(start, end).toMinutes()

    fun breakOffsetMinutes(): Long = Duration.between(start, breakStart).toMinutes()

    fun breakDurationMinutes(): Long = Duration.between(breakStart, breakEnd).toMinutes()

    fun progress(now: LocalTime): Float {
        if (now <= start) return 0f
        if (now >= end) return 1f
        val total = Duration.between(start, end).toMillis().toFloat()
        val passed = Duration.between(start, now).toMillis().toFloat()
        return (passed / total).coerceIn(0f, 1f)
    }

    fun passedMinutes(now: LocalTime): Long {
        val bounded = now.coerceAtMost(end)
        return Duration.between(start, bounded).toMinutes().coerceAtLeast(0)
    }

    fun remainingMinutes(now: LocalTime): Long {
        val bounded = now.coerceAtLeast(start)
        return Duration.between(bounded, end).toMinutes().coerceAtLeast(0)
    }
}

data class Schedule(val lessons: List<Lesson>)

data class ScheduleSnapshot(
    val variant: ScheduleVariant,
    val dayType: DayType,
    val lessons: List<Lesson>,
    val activeIndex: Int?
)

private const val BREAK_MINUTES = 5

private val weekdayBetweenPairs = listOf(10, 20, 20, 10, 5)
private val saturdayBetweenPairs = List(5) { 10 }

private fun buildPairs(
    start: LocalTime,
    firstPartMinutes: Int,
    secondPartMinutes: Int,
    betweenPairsMinutes: List<Int>
): List<Lesson> {
    val lessons = mutableListOf<Lesson>()
    var currentStart = start
    val pairCount = betweenPairsMinutes.size + 1
    repeat(pairCount) { index ->
        val breakStart = currentStart.plusMinutes(firstPartMinutes.toLong())
        val breakEnd = breakStart.plusMinutes(BREAK_MINUTES.toLong())
        val end = breakEnd.plusMinutes(secondPartMinutes.toLong())
        lessons.add(Lesson(currentStart, breakStart, breakEnd, end))
        if (index < betweenPairsMinutes.size) {
            currentStart = end.plusMinutes(betweenPairsMinutes[index].toLong())
        }
    }
    return lessons
}

private val weekdayLong = buildPairs(
    start = LocalTime.of(9, 0),
    firstPartMinutes = 45,
    secondPartMinutes = 45,
    betweenPairsMinutes = weekdayBetweenPairs
)

private val weekdayWinter = buildPairs(
    start = LocalTime.of(9, 0),
    firstPartMinutes = 40,
    secondPartMinutes = 40,
    betweenPairsMinutes = weekdayBetweenPairs
)

private val saturdayLong = buildPairs(
    start = LocalTime.of(9, 0),
    firstPartMinutes = 45,
    secondPartMinutes = 45,
    betweenPairsMinutes = saturdayBetweenPairs
)

private val saturdayWinter = buildPairs(
    start = LocalTime.of(9, 0),
    firstPartMinutes = 40,
    secondPartMinutes = 40,
    betweenPairsMinutes = saturdayBetweenPairs
)

val schedules: Map<ScheduleVariant, Map<DayType, List<Lesson>>> = mapOf(
    ScheduleVariant.SEP_NOV_APR_JUN to mapOf(
        DayType.WEEKDAY to weekdayLong,
        DayType.SATURDAY to saturdayLong
    ),
    ScheduleVariant.DEC_MAR to mapOf(
        DayType.WEEKDAY to weekdayWinter,
        DayType.SATURDAY to saturdayWinter
    )
)

fun variantForMonth(month: Month): ScheduleVariant = when (month) {
    Month.DECEMBER, Month.JANUARY, Month.FEBRUARY, Month.MARCH -> ScheduleVariant.DEC_MAR
    else -> ScheduleVariant.SEP_NOV_APR_JUN
}

fun dayTypeFor(date: LocalDate): DayType =
    if (date.dayOfWeek == DayOfWeek.SATURDAY) DayType.SATURDAY else DayType.WEEKDAY

fun currentLessonIndex(lessons: List<Lesson>, now: LocalTime): Int? {
    return lessons.indexOfFirst { now >= it.start && now < it.end }.takeIf { it >= 0 }
}

fun scheduleSnapshot(date: LocalDate, now: LocalTime): ScheduleSnapshot {
    val variant = variantForMonth(date.month)
    val dayType = dayTypeFor(date)
    val lessons = schedules[variant]?.get(dayType).orEmpty()
    val activeIndex = currentLessonIndex(lessons, now)
    return ScheduleSnapshot(variant, dayType, lessons, activeIndex)
}
