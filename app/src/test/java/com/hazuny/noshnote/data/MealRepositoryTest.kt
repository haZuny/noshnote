package com.hazuny.noshnote.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MealRepositoryTest {
    @Test
    fun saveEntryStoresServingSnapshotAndManualTagSource() = runBlocking {
        val dao = RecordingMealDao()
        val repository = MealRepository(dao)

        repository.saveEntry(
            existing = null,
            dateKey = "2026-02-03",
            time = "12:30",
            mealTag = "저녁",
            tagWasManuallySet = true,
            foodName = "두부",
            quantity = 2.0,
            unit = "조각",
            calories = 180.0,
            protein = 16.0,
        )

        val saved = dao.insertedEntry
        assertNotNull(saved)
        assertEquals(90.0, saved!!.caloriesPerUnitKcalSnapshot, 0.0)
        assertEquals(8.0, saved.proteinPerUnitGSnapshot, 0.0)
        assertEquals(180.0, saved.caloriesKcalSnapshot, 0.0)
        assertEquals(16.0, saved.proteinGSnapshot, 0.0)
        assertEquals("manual", saved.tagSource)
        assertEquals("저녁", saved.mealTag)
        assertEquals("2026-02-03", saved.dateKey)
    }

    @Test
    fun editingEntryPreservesOriginalCreationTime() = runBlocking {
        val original = MealEntryEntity(
            id = 4L,
            dateKey = "2026-02-03",
            eatenAtEpochMillis = 0L,
            mealTag = "점심",
            tagSource = "auto",
            foodNameSnapshot = "밥",
            quantity = 1.0,
            unitSnapshot = "공기",
            caloriesPerUnitKcalSnapshot = 300.0,
            proteinPerUnitGSnapshot = 6.0,
            caloriesKcalSnapshot = 300.0,
            proteinGSnapshot = 6.0,
            createdAtEpochMillis = 1_234L,
        )
        val dao = RecordingMealDao()

        MealRepository(dao).saveEntry(
            existing = original,
            dateKey = "2026-02-03",
            time = "13:00",
            mealTag = "점심",
            tagWasManuallySet = false,
            foodName = "현미밥",
            quantity = 1.0,
            unit = "공기",
            calories = 280.0,
            protein = 7.0,
        )

        assertEquals(4L, dao.updatedEntry?.id)
        assertEquals(1_234L, dao.updatedEntry?.createdAtEpochMillis)
        assertEquals("현미밥", dao.updatedEntry?.foodNameSnapshot)
    }

    @Test
    fun savingGoalReplacesTheSingleCurrentGoal() = runBlocking {
        val dao = RecordingMealDao()
        dao.goals += DailyGoalEntity(
            id = 8L,
            caloriesKcal = 2_000.0,
            proteinG = 100.0,
            effectiveFromDate = "2026-01-01",
        )

        MealRepository(dao).saveGoal(1_900.0, 110.0, "2026-02-03")

        assertEquals(1, dao.goals.size)
        assertEquals(1L, dao.goals.single().id)
        assertEquals(1_900.0, dao.goals.single().caloriesKcal, 0.0)
        assertEquals(110.0, dao.goals.single().proteinG, 0.0)
    }

    private class RecordingMealDao : MealDao {
        var insertedEntry: MealEntryEntity? = null
        var updatedEntry: MealEntryEntity? = null
        val goals = mutableListOf<DailyGoalEntity>()

        override fun observeAllEntries(): Flow<List<MealEntryEntity>> = emptyFlow()
        override fun observeEntriesForDate(dateKey: String): Flow<List<MealEntryEntity>> = emptyFlow()
        override suspend fun insertEntry(entry: MealEntryEntity) { insertedEntry = entry }
        override suspend fun updateEntry(entry: MealEntryEntity) { updatedEntry = entry }
        override suspend fun deleteEntry(entryId: Long) = Unit
        override fun observeFoodTemplates(): Flow<List<FoodTemplateEntity>> = emptyFlow()
        override suspend fun saveFoodTemplate(food: FoodTemplateEntity) = Unit
        override suspend fun deleteFoodTemplate(foodId: Long) = Unit
        override fun observeGoal(): Flow<DailyGoalEntity?> = emptyFlow()
        override suspend fun deleteAllGoals() { goals.clear() }
        override suspend fun saveGoal(goal: DailyGoalEntity) { goals += goal }
    }
}
