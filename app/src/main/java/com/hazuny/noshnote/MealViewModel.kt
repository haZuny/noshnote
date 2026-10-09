package com.hazuny.noshnote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazuny.noshnote.data.DailyGoalEntity
import com.hazuny.noshnote.data.FoodTemplateEntity
import com.hazuny.noshnote.data.MealEntryEntity
import com.hazuny.noshnote.data.MealRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MealViewModel(private val repository: MealRepository) : ViewModel() {
    val entries: StateFlow<List<MealEntryEntity>> = repository.entries.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )
    val foodTemplates: StateFlow<List<FoodTemplateEntity>> = repository.foodTemplates.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )
    val goal: StateFlow<DailyGoalEntity?> = repository.goal.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    fun saveEntry(
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
        viewModelScope.launch {
            repository.saveEntry(
                existing = existing,
                dateKey = dateKey,
                time = time,
                mealTag = mealTag,
                tagWasManuallySet = tagWasManuallySet,
                foodName = foodName,
                quantity = quantity,
                unit = unit,
                calories = calories,
                protein = protein,
            )
        }
    }

    fun saveTemplate(template: FoodTemplateEntity) {
        viewModelScope.launch { repository.saveTemplate(template) }
    }

    fun saveGoal(calories: Double, protein: Double) {
        viewModelScope.launch {
            repository.saveGoal(calories, protein, LocalDate.now().toString())
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch { repository.deleteEntry(id) }
    }

    fun deleteTemplate(id: Long) {
        viewModelScope.launch { repository.deleteTemplate(id) }
    }
}
