package com.hazuny.noshnote.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NutritionLabelParserTest {
    @Test
    fun parsesLabeledCaloriesAndProtein() {
        val values = NutritionLabelParser.parse("Calories 230 kcal\nProtein 12 g")
            ?: error("완전한 영양 후보를 찾지 못했습니다.")

        assertEquals(230.0, values.caloriesKcal, 0.0)
        assertEquals(12.0, values.proteinG, 0.0)
    }

    @Test
    fun parsesKoreanDecimalCommaValues() {
        val values = NutritionLabelParser.parse("열량 125,5 kcal\n단백질 3,2 g")
            ?: error("완전한 영양 후보를 찾지 못했습니다.")

        assertEquals(125.5, values.caloriesKcal, 0.0)
        assertEquals(3.2, values.proteinG, 0.0)
    }

    @Test
    fun rejectsDailyReferenceCaloriesAsMealValue() {
        val candidates = NutritionLabelParser.parseCandidates(
            "Nutrition Facts\nDaily Value 2,000 kcal\nProtein 12 g",
        )

        assertNull(candidates.caloriesKcal)
        assertEquals(12.0, candidates.proteinG ?: error("단백질 후보가 없습니다."), 0.0)
    }

    @Test
    fun requiresBothNutritionValuesForCompleteResult() {
        assertNull(NutritionLabelParser.parse("Calories 230 kcal"))
    }
}
