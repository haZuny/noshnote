package com.hazuny.noshnote.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hazuny.noshnote.data.FoodTemplateEntity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TemplateQuantityControlsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun quantityButtonsAdjustByOneAndDoNotGoBelowOne() {
        var savedDraft: RecordDraft? = null
        composeRule.setContent {
            MaterialTheme {
                TemplateRecordDialog(
                    template = template,
                    dateKey = "2026-10-10",
                    onDismiss = {},
                    onSave = { savedDraft = it },
                )
            }
        }

        composeRule.onNodeWithTag("template_quantity_decrease").assertIsNotEnabled()
        composeRule.onNodeWithTag("template_quantity_increase").performClick()
        composeRule.onNodeWithTag("template_quantity_increase").performClick()
        composeRule.onNodeWithTag("template_quantity_decrease").performClick()
        composeRule.onNodeWithTag("record_save_button").performClick()

        composeRule.runOnIdle {
            assertEquals(2.0, savedDraft?.quantity ?: 0.0, 0.0)
            assertEquals(600.0, savedDraft?.calories ?: 0.0, 0.0)
        }
    }

    @Test
    fun directDecimalEntryStillWorksWithQuantityButtons() {
        var savedDraft: RecordDraft? = null
        composeRule.setContent {
            MaterialTheme {
                TemplateRecordDialog(
                    template = template,
                    dateKey = "2026-10-10",
                    onDismiss = {},
                    onSave = { savedDraft = it },
                )
            }
        }

        composeRule.onNodeWithTag("template_quantity_input").performTextReplacement("2.5")
        composeRule.onNodeWithTag("template_quantity_increase").performClick()
        composeRule.onNodeWithTag("template_quantity_decrease").performClick()
        composeRule.onNodeWithTag("record_save_button").performClick()

        composeRule.runOnIdle {
            assertEquals(2.5, savedDraft?.quantity ?: 0.0, 0.0)
            assertEquals(750.0, savedDraft?.calories ?: 0.0, 0.0)
        }
    }

    private companion object {
        val template = FoodTemplateEntity(
            id = 1L,
            name = "밥",
            unit = "공기",
            caloriesPerUnitKcal = 300.0,
            proteinPerUnitG = 6.0,
        )
    }
}
