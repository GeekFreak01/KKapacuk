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

data class Lesson(val start: LocalTime, val end: LocalTime) {
    fun durationMinutes(): Long = Duration.between(start, end).toMinutes()

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

private val weekdayLong = listOf(
    Lesson(LocalTime.of(9, 0), LocalTime.of(9, 45)),
    Lesson(LocalTime.of(9, 55), LocalTime.of(10, 40)),
    Lesson(LocalTime.of(10, 50), LocalTime.of(11, 35)),
    Lesson(LocalTime.of(12, 15), LocalTime.of(13, 0)),
    Lesson(LocalTime.of(13, 10), LocalTime.of(13, 55)),
    Lesson(LocalTime.of(14, 5), LocalTime.of(14, 50)),
    Lesson(LocalTime.of(15, 0), LocalTime.of(15, 45))
)

private val weekdayWinter = listOf(
    Lesson(LocalTime.of(9, 0), LocalTime.of(9, 40)),
    Lesson(LocalTime.of(9, 50), LocalTime.of(10, 30)),
    Lesson(LocalTime.of(10, 40), LocalTime.of(11, 20)),
    Lesson(LocalTime.of(12, 0), LocalTime.of(12, 40)),
    Lesson(LocalTime.of(12, 50), LocalTime.of(13, 30)),
    Lesson(LocalTime.of(13, 40), LocalTime.of(14, 20)),
    Lesson(LocalTime.of(14, 30), LocalTime.of(15, 10))
)

private val saturdayPairs = listOf(
    Lesson(LocalTime.of(9, 0), LocalTime.of(10, 10)),
    Lesson(LocalTime.of(10, 20), LocalTime.of(11, 30)),
    Lesson(LocalTime.of(11, 40), LocalTime.of(12, 50)),
    Lesson(LocalTime.of(13, 30), LocalTime.of(14, 40)),
    Lesson(LocalTime.of(14, 50), LocalTime.of(16, 0))
)

val schedules: Map<ScheduleVariant, Map<DayType, List<Lesson>>> = mapOf(
    ScheduleVariant.SEP_NOV_APR_JUN to mapOf(
        DayType.WEEKDAY to weekdayLong,
        DayType.SATURDAY to saturdayPairs
    ),
    ScheduleVariant.DEC_MAR to mapOf(
        DayType.WEEKDAY to weekdayWinter,
        DayType.SATURDAY to saturdayPairs
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
