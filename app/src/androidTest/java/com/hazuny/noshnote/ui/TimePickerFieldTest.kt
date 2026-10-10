package com.hazuny.noshnote.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hazuny.noshnote.data.FoodTemplateEntity
import com.hazuny.noshnote.data.MealEntryEntity
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimePickerFieldTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectingTimeInRecordEditorUpdatesTimeAndAutomaticTag() {
        var savedDraft: RecordDraft? = null
        composeRule.setContent {
            MaterialTheme {
                RecordEditorDialog(
                    entry = mealEntry("아침", "auto"),
                    dateKey = dateKey,
                    onDismiss = {},
                    onSave = { draft, _ -> savedDraft = draft },
                    onDelete = {},
                )
            }
        }

        chooseRecordTime()
        composeRule.onNodeWithTag("record_save_button").performClick()

        composeRule.runOnIdle {
            assertEquals("17:35", savedDraft?.time)
            assertEquals("저녁", savedDraft?.mealTag)
            assertFalse(savedDraft?.tagWasManuallySet ?: true)
        }
    }

    @Test
    fun selectingTimeInTemplateRecordUpdatesTimeAndAutomaticTag() {
        var savedDraft: RecordDraft? = null
        val initialTime = LocalTime.now().format(timeFormatter)
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
                    dateKey = dateKey,
                    onDismiss = {},
                    onSave = { savedDraft = it },
                )
            }
        }

        selectTime(initialTime, targetPeriod = 1, targetHour = 11, targetMinute = 8)
        composeRule.onNodeWithTag("time_picker_confirm").performClick()
        composeRule.onNodeWithTag("record_save_button").performClick()

        composeRule.runOnIdle {
            assertEquals("12:40", savedDraft?.time)
            assertEquals("점심", savedDraft?.mealTag)
        }
    }

    @Test
    fun dismissingTimePickerDoesNotChangeTheRecord() {
        var savedDraft: RecordDraft? = null
        composeRule.setContent {
            MaterialTheme {
                RecordEditorDialog(
                    entry = mealEntry("아침", "auto"),
                    dateKey = dateKey,
                    onDismiss = {},
                    onSave = { draft, _ -> savedDraft = draft },
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithTag("time_picker_button").performClick()
        selectTime(initialTime = "09:20", targetPeriod = 1, targetHour = 4, targetMinute = 7, openPicker = false)
        composeRule.onNodeWithTag("time_picker_cancel").performClick()
        composeRule.onNodeWithTag("record_save_button").performClick()

        composeRule.runOnIdle {
            assertEquals("09:20", savedDraft?.time)
            assertEquals("아침", savedDraft?.mealTag)
        }
    }

    private fun chooseRecordTime() {
        composeRule.onNodeWithTag("time_picker_button").performClick()
        selectTime(initialTime = "09:20", targetPeriod = 1, targetHour = 4, targetMinute = 7, openPicker = false)
        composeRule.onNodeWithTag("time_picker_confirm").performClick()
    }

    private fun selectTime(
        initialTime: String,
        targetPeriod: Int,
        targetHour: Int,
        targetMinute: Int,
        openPicker: Boolean = true,
    ) {
        if (openPicker) composeRule.onNodeWithTag("time_picker_button").performClick()
        val parsedTime = LocalTime.parse(initialTime)
        val roundedMinute = ((parsedTime.minute + 2) / 5) * 5
        val roundedTime = if (roundedMinute == 60) {
            parsedTime.plusHours(1).withMinute(0)
        } else {
            parsedTime.withMinute(roundedMinute)
        }
        val initialPeriod = if (roundedTime.hour < 12) 0 else 1
        if (initialPeriod != targetPeriod) {
            composeRule.onNodeWithTag("time_wheel_period_option_$targetPeriod").performClick()
        }
        moveWheel("time_wheel_hour", (roundedTime.hour + 11) % 12, targetHour)
        moveWheel("time_wheel_minute", roundedTime.minute / 5, targetMinute)
    }

    private fun moveWheel(tag: String, initialIndex: Int, targetIndex: Int) {
        var currentIndex = initialIndex
        while (currentIndex != targetIndex) {
            val distance = min(1, kotlin.math.abs(targetIndex - currentIndex))
            currentIndex += if (targetIndex > currentIndex) distance else -distance
            composeRule.onNodeWithTag("${tag}_option_$currentIndex").performClick()
        }
    }

    private fun mealEntry(mealTag: String, tagSource: String) = MealEntryEntity(
        id = 1L,
        dateKey = dateKey,
        eatenAtEpochMillis = LocalDate.parse(dateKey)
            .atTime(9, 20)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli(),
        mealTag = mealTag,
        tagSource = tagSource,
        foodNameSnapshot = "밥",
        quantity = 1.0,
        unitSnapshot = "공기",
        caloriesPerUnitKcalSnapshot = 300.0,
        proteinPerUnitGSnapshot = 6.0,
        caloriesKcalSnapshot = 300.0,
        proteinGSnapshot = 6.0,
    )

    private companion object {
        const val dateKey = "2026-10-10"
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
    }
}
