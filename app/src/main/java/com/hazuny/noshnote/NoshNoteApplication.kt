package com.hazuny.noshnote

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hazuny.noshnote.data.MealDatabase
import com.hazuny.noshnote.data.MealRepository

class NoshNoteApplication : Application() {
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
