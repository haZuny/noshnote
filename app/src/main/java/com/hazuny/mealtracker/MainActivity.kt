package com.hazuny.mealtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.hazuny.mealtracker.ui.MealTrackerApp
import com.hazuny.mealtracker.ui.theme.MealTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val factory = (application as MealTrackerApplication).viewModelFactory
        val viewModel = ViewModelProvider(this, factory)[MealViewModel::class.java]
        setContent {
            MealTrackerTheme {
                MealTrackerApp(viewModel)
            }
        }
    }
}
