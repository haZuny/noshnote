package com.hazuny.noshnote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.hazuny.noshnote.ui.NoshNoteApp
import com.hazuny.noshnote.ui.theme.NoshNoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val factory = (application as NoshNoteApplication).viewModelFactory
        val viewModel = ViewModelProvider(this, factory)[MealViewModel::class.java]
        setContent {
            NoshNoteTheme {
                NoshNoteApp(viewModel)
            }
        }
    }
}
