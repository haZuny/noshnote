package com.hazuny.noshnote.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MealRulesTest {
    @Test
    fun mealTagUsesDocumentedTimeBoundaries() {
        assertEquals("간식", MealRules.inferMealTag("04:59"))
        assertEquals("아침", MealRules.inferMealTag("05:00"))
        assertEquals("아침", MealRules.inferMealTag("10:59"))
        assertEquals("점심", MealRules.inferMealTag("11:00"))
        assertEquals("저녁", MealRules.inferMealTag("16:00"))
        assertEquals("간식", MealRules.inferMealTag("22:00"))
    }

    @Test
    fun invalidMealTimeFallsBackToSnack() {
        assertEquals("간식", MealRules.inferMealTag("not-a-time"))
    }

    @Test
    fun timeChangeRecalculatesOnlyAutomaticTags() {
        assertEquals("점심", MealRules.tagAfterTimeChange("아침", false, "12:00"))
        assertEquals("아침", MealRules.tagAfterTimeChange("아침", true, "12:00"))
    }

    @Test
    fun currentStreakKeepsYesterdayChainWhenTodayHasNoRecord() {
        val today = LocalDate.parse("2026-02-03")
        val dates = setOf(today.minusDays(1), today.minusDays(2), today.minusDays(3))

        assertEquals(3, MealRules.currentRecordStreak(dates, today))
    }

    @Test
    fun currentStreakStartsAtTodayWhenRecordedAndStopsAtGap() {
        val today = LocalDate.parse("2026-02-03")
        val dates = setOf(today, today.minusDays(1), today.minusDays(3))

        assertEquals(2, MealRules.currentRecordStreak(dates, today))
    }

    @Test
    fun calendarProgressSumsSnapshotsAndIncludesGoalBoundaries() {
        val progress = MealRules.calendarGoalProgress(
            entries = listOf(entry(calories = 300.0, protein = 12.0), entry(calories = 700.0, protein = 18.0)),
            currentGoalCalories = 1_000.0,
            currentGoalProtein = 30.0,
        )

        assertTrue(progress.hasEntries)
        assertEquals(1_000.0, progress.caloriesKcal, 0.0)
        assertEquals(30.0, progress.proteinG, 0.0)
        assertEquals(2, progress.goalCount)
        assertEquals(2, progress.achievedGoalCount)
    }

    @Test
    fun calendarProgressKeepsMissingRecordsAndMissingGoalsDistinct() {
        val progress = MealRules.calendarGoalProgress(emptyList(), null, 0.0)

        assertFalse(progress.hasEntries)
        assertEquals(0, progress.goalCount)
        assertEquals(0, progress.achievedGoalCount)
        assertEquals(0.0, progress.caloriesKcal, 0.0)
        assertEquals(0.0, progress.proteinG, 0.0)
    }

    @Test
    fun calendarProgressCanShowRecordsWithoutConfiguredGoals() {
        val progress = MealRules.calendarGoalProgress(
            entries = listOf(entry(calories = 400.0, protein = 20.0)),
            currentGoalCalories = null,
            currentGoalProtein = null,
        )

        assertTrue(progress.hasEntries)
        assertEquals(0, progress.goalCount)
        assertEquals(0, progress.achievedGoalCount)
    }

    @Test
    fun calendarProgressUsesOnlyTheCurrentGoalsProvidedForEveryDate() {
        val pastDateEntries = listOf(entry(calories = 1_400.0, protein = 50.0))

        val progress = MealRules.calendarGoalProgress(
            entries = pastDateEntries,
            currentGoalCalories = 1_500.0,
            currentGoalProtein = 45.0,
        )

        assertEquals(2, progress.achievedGoalCount)
    }

    private fun entry(calories: Double, protein: Double) = MealEntryEntity(
        dateKey = "2026-01-01",
        eatenAtEpochMillis = 0L,
        mealTag = "점심",
        tagSource = "auto",
        foodNameSnapshot = "테스트 음식",
        quantity = 1.0,
        unitSnapshot = "인분",
        caloriesPerUnitKcalSnapshot = calories,
        proteinPerUnitGSnapshot = protein,
        caloriesKcalSnapshot = calories,
        proteinGSnapshot = protein,
    )
}
