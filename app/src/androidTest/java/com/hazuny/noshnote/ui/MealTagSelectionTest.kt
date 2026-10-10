package com.hazuny.noshnote.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hazuny.noshnote.data.FoodTemplateEntity
import com.hazuny.noshnote.data.MealEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MealTagSelectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun changingMealTagInRecordEditorSavesSelectedTag() {
        var savedDraft: RecordDraft? = null
        composeRule.setContent {
            MaterialTheme {
                RecordEditorDialog(
                    entry = MealEntryEntity(
                        id = 1L,
                        dateKey = "2026-10-10",
                        eatenAtEpochMillis = 0L,
                        mealTag = "아침",
                        tagSource = "auto",
                        foodNameSnapshot = "밥",
                        quantity = 1.0,
                        unitSnapshot = "공기",
                        caloriesPerUnitKcalSnapshot = 300.0,
                        proteinPerUnitGSnapshot = 6.0,
                        caloriesKcalSnapshot = 300.0,
                        proteinGSnapshot = 6.0,
                    ),
                    dateKey = "2026-10-10",
                    onDismiss = {},
                    onSave = { draft, _ -> savedDraft = draft },
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithTag("meal_tag_selector").performClick()
        composeRule.onNodeWithTag("meal_tag_option_점심").performClick()
        composeRule.onNodeWithTag("record_save_button").performClick()

        composeRule.runOnIdle {
            assertEquals("점심", savedDraft?.mealTag)
            assertTrue(savedDraft?.tagWasManuallySet == true)
        }
    }

    @Test
    fun changingMealTagInTemplateRecordSavesSelectedTag() {
        var savedDraft: RecordDraft? = null
        composeRule.setContent {
            MaterialTheme {
                TemplateRecordDialog(
                    template = FoodTemplateEntity(
                        id = 1L,
                        name = "밥",
                        unit = "공기",
                        caloriesPerUnitKcal = 300.0,
                        proteinPerUnitG = 6.0,
                    ),
                    dateKey = "2026-10-10",
                    onDismiss = {},
                    onSave = { savedDraft = it },
                )
            }
        }

        composeRule.onNodeWithTag("meal_tag_selector").performClick()
        composeRule.onNodeWithTag("meal_tag_option_점심").performClick()
        composeRule.onNodeWithTag("record_save_button").performClick()

        composeRule.runOnIdle {
            assertEquals("점심", savedDraft?.mealTag)
            assertTrue(savedDraft?.tagWasManuallySet == true)
        }
    }
}
