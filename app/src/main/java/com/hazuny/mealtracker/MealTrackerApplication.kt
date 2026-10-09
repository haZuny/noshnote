package com.hazuny.mealtracker

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hazuny.mealtracker.data.MealDatabase
import com.hazuny.mealtracker.data.MealRepository

class MealTrackerApplication : Application() {
    lateinit var viewModelFactory: MealViewModelFactory
        private set

    override fun onCreate() {
        super.onCreate()
        val database = MealDatabase.create(this)
        viewModelFactory = MealViewModelFactory(MealRepository(database.mealDao()))
    }
}

class MealViewModelFactory(
    private val repository: MealRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MealViewModel::class.java)) {
            return MealViewModel(repository) as T
        }
        error("Unknown ViewModel class: ${modelClass.name}")
    }
}
