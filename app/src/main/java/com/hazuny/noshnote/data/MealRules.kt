package com.hazuny.noshnote.data

import java.time.LocalTime
import java.time.LocalDate

data class CalendarGoalProgress(
    val hasEntries: Boolean,
    val caloriesKcal: Double,
    val proteinG: Double,
    val goalCount: Int,
    val achievedGoalCount: Int,
)

/** Compose와 분리된 공통 규칙으로 경계 조건을 로컬 테스트에서 직접 확인한다. */
object MealRules {
    fun inferMealTag(time: String): String {
        val parsed = runCatching { LocalTime.parse(time) }.getOrNull() ?: return "간식"
        return when (parsed.hour) {
            in 5..10 -> "아침"
            in 11..15 -> "점심"
            in 16..21 -> "저녁"
            else -> "간식"
        }
    }

    fun tagAfterTimeChange(currentTag: String, wasManuallySet: Boolean, time: String): String =
        if (wasManuallySet) currentTag else inferMealTag(time)

    fun currentRecordStreak(recordDates: Set<LocalDate>, today: LocalDate): Int {
        var cursor = if (today in recordDates) today else today.minusDays(1)
        var days = 0
        while (cursor in recordDates) {
            days += 1
            cursor = cursor.minusDays(1)
        }
        return days
    }

    /** Every date is evaluated against the currently active goals, including past dates. */
    fun calendarGoalProgress(
        entries: List<MealEntryEntity>,
        currentGoalCalories: Double?,
        currentGoalProtein: Double?,
    ): CalendarGoalProgress {
        val calories = entries.sumOf { it.caloriesKcalSnapshot }
        val protein = entries.sumOf { it.proteinGSnapshot }
        val caloriesGoal = currentGoalCalories?.takeIf { it > 0 }
        val proteinGoal = currentGoalProtein?.takeIf { it > 0 }
        val achievedGoalCount =
            (if (caloriesGoal != null && calories <= caloriesGoal) 1 else 0) +
                (if (proteinGoal != null && protein >= proteinGoal) 1 else 0)

        return CalendarGoalProgress(
            hasEntries = entries.isNotEmpty(),
            caloriesKcal = calories,
            proteinG = protein,
            goalCount = (if (caloriesGoal != null) 1 else 0) + (if (proteinGoal != null) 1 else 0),
            achievedGoalCount = achievedGoalCount,
        )
    }
}
