package com.hazuny.noshnote.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class MealRepository(private val mealDao: MealDao) {
    val entries = mealDao.observeAllEntries()
    val foodTemplates = mealDao.observeFoodTemplates()
    val goal = mealDao.observeGoal()

    suspend fun saveEntry(
        existing: MealEntryEntity?,
        dateKey: String,
        time: String,
        mealTag: String,
        tagWasManuallySet: Boolean,
        foodName: String,
        quantity: Double,
        unit: String,
        calories: Double,
        protein: Double,
    ) {
        val eatenAt = LocalDate.parse(dateKey)
            .atTime(LocalTime.parse(time))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val entry = MealEntryEntity(
            id = existing?.id ?: 0,
            dateKey = dateKey,
            eatenAtEpochMillis = eatenAt,
            mealTag = mealTag,
            tagSource = if (tagWasManuallySet) "manual" else "auto",
            foodNameSnapshot = foodName,
            quantity = quantity,
            unitSnapshot = unit,
            caloriesPerUnitKcalSnapshot = calories / quantity,
            proteinPerUnitGSnapshot = protein / quantity,
            caloriesKcalSnapshot = calories,
            proteinGSnapshot = protein,
            createdAtEpochMillis = existing?.createdAtEpochMillis ?: System.currentTimeMillis(),
        )
        if (existing == null) mealDao.insertEntry(entry) else mealDao.updateEntry(entry)
    }

    suspend fun saveTemplate(template: FoodTemplateEntity) {
        mealDao.saveFoodTemplate(template.copy(updatedAtEpochMillis = System.currentTimeMillis()))
    }

    suspend fun saveGoal(calories: Double, protein: Double, effectiveFromDate: String) {
        mealDao.replaceCurrentGoal(
            DailyGoalEntity(
                caloriesKcal = calories,
                proteinG = protein,
                effectiveFromDate = effectiveFromDate,
            ),
        )
    }

    suspend fun deleteEntry(id: Long) = mealDao.deleteEntry(id)
    suspend fun deleteTemplate(id: Long) = mealDao.deleteFoodTemplate(id)
}
