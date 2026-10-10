package com.hazuny.noshnote.data

import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class MealBackupData(
    val exportedAtEpochMillis: Long,
    val entries: List<MealEntryEntity>,
    val foodTemplates: List<FoodTemplateEntity>,
    val goal: DailyGoalEntity?,
)

class MealBackupFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

object MealBackupJson {
    private const val FORMAT = "noshnote-backup"
    private const val VERSION = 1
    private const val MAX_FOOD_NAME_LENGTH = 200
    private const val MAX_UNIT_LENGTH = 100
    private const val MAX_TAG_LENGTH = 32

    fun encode(
        entries: List<MealEntryEntity>,
        foodTemplates: List<FoodTemplateEntity>,
        goal: DailyGoalEntity?,
        exportedAtEpochMillis: Long = System.currentTimeMillis(),
    ): String {
        val root = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAtEpochMillis", exportedAtEpochMillis)
            .put("entries", JSONArray().apply { entries.forEach { put(it.toJson()) } })
            .put("foodTemplates", JSONArray().apply { foodTemplates.forEach { put(it.toJson()) } })
            .put("goal", goal?.toJson() ?: JSONObject.NULL)
        return root.toString(2)
    }

    @Throws(MealBackupFormatException::class)
    fun decode(json: String): MealBackupData {
        try {
            val root = JSONObject(json)
            if (root.requiredString("format") != FORMAT) fail("지원하지 않는 백업 파일입니다.")
            if (root.requiredLong("version") != VERSION.toLong()) fail("지원하지 않는 백업 버전입니다.")

            val exportedAt = root.requiredLong("exportedAtEpochMillis").requireNonNegative("exportedAtEpochMillis")
            val entries = root.requiredArray("entries").objects().map { it.toMealEntry() }
            val templates = root.requiredArray("foodTemplates").objects().map { it.toFoodTemplate() }
            val goalValue = root.get("goal")
            val goal = if (goalValue == JSONObject.NULL) null else (goalValue as? JSONObject
                ?: fail("목표 데이터 형식이 올바르지 않습니다.")).toGoal()

            return MealBackupData(exportedAt, entries, templates, goal)
        } catch (error: MealBackupFormatException) {
            throw error
        } catch (error: JSONException) {
            throw MealBackupFormatException("JSON 백업 파일을 읽을 수 없습니다.", error)
        } catch (error: IllegalArgumentException) {
            throw MealBackupFormatException("백업 파일의 날짜 또는 값이 올바르지 않습니다.", error)
        } catch (error: ClassCastException) {
            throw MealBackupFormatException("백업 파일의 필드 형식이 올바르지 않습니다.", error)
        }
    }

    private fun MealEntryEntity.toJson() = JSONObject()
        .put("dateKey", dateKey)
        .put("eatenAtEpochMillis", eatenAtEpochMillis)
        .put("mealTag", mealTag)
        .put("tagSource", tagSource)
        .put("foodNameSnapshot", foodNameSnapshot)
        .put("quantity", quantity)
        .put("unitSnapshot", unitSnapshot)
        .put("caloriesPerUnitKcalSnapshot", caloriesPerUnitKcalSnapshot)
        .put("proteinPerUnitGSnapshot", proteinPerUnitGSnapshot)
        .put("caloriesKcalSnapshot", caloriesKcalSnapshot)
        .put("proteinGSnapshot", proteinGSnapshot)
        .put("createdAtEpochMillis", createdAtEpochMillis)

    private fun FoodTemplateEntity.toJson() = JSONObject()
        .put("name", name)
        .put("unit", unit)
        .put("caloriesPerUnitKcal", caloriesPerUnitKcal)
        .put("proteinPerUnitG", proteinPerUnitG)
        .put("createdAtEpochMillis", createdAtEpochMillis)
        .put("updatedAtEpochMillis", updatedAtEpochMillis)

    private fun DailyGoalEntity.toJson() = JSONObject()
        .put("caloriesKcal", caloriesKcal)
        .put("proteinG", proteinG)
        .put("effectiveFromDate", effectiveFromDate)
        .put("updatedAtEpochMillis", updatedAtEpochMillis)

    private fun JSONObject.toMealEntry(): MealEntryEntity {
        val dateKey = requiredString("dateKey")
        LocalDate.parse(dateKey)
        val tag = requiredString("mealTag")
        val tagSource = requiredString("tagSource")
        val name = requiredString("foodNameSnapshot").requireLength(MAX_FOOD_NAME_LENGTH, "foodNameSnapshot")
        val unit = requiredString("unitSnapshot").requireLength(MAX_UNIT_LENGTH, "unitSnapshot")
        if (tag.length > MAX_TAG_LENGTH || tagSource !in setOf("auto", "manual")) {
            fail("식사 태그 데이터가 올바르지 않습니다.")
        }
        if (tag !in setOf("", "아침", "점심", "저녁", "간식")) fail("식사 태그 데이터가 올바르지 않습니다.")

        val quantity = requiredDouble("quantity").requirePositive("quantity")
        val caloriesPerUnit = requiredDouble("caloriesPerUnitKcalSnapshot").requireNonNegative("caloriesPerUnitKcalSnapshot")
        val proteinPerUnit = requiredDouble("proteinPerUnitGSnapshot").requireNonNegative("proteinPerUnitGSnapshot")
        val calories = requiredDouble("caloriesKcalSnapshot").requireNonNegative("caloriesKcalSnapshot")
        val protein = requiredDouble("proteinGSnapshot").requireNonNegative("proteinGSnapshot")

        return MealEntryEntity(
            dateKey = dateKey,
            eatenAtEpochMillis = requiredLong("eatenAtEpochMillis"),
            mealTag = tag,
            tagSource = tagSource,
            foodNameSnapshot = name,
            quantity = quantity,
            unitSnapshot = unit,
            caloriesPerUnitKcalSnapshot = caloriesPerUnit,
            proteinPerUnitGSnapshot = proteinPerUnit,
            caloriesKcalSnapshot = calories,
            proteinGSnapshot = protein,
            createdAtEpochMillis = requiredLong("createdAtEpochMillis").requireNonNegative("createdAtEpochMillis"),
        )
    }

    private fun JSONObject.toFoodTemplate(): FoodTemplateEntity {
        val name = requiredString("name").trim().requireLength(MAX_FOOD_NAME_LENGTH, "name")
        val unit = requiredString("unit").trim().requireLength(MAX_UNIT_LENGTH, "unit")
        if (name.isBlank() || unit.isBlank()) fail("음식 이름과 단위는 비워둘 수 없습니다.")
        return FoodTemplateEntity(
            name = name,
            unit = unit,
            caloriesPerUnitKcal = requiredDouble("caloriesPerUnitKcal").requireNonNegative("caloriesPerUnitKcal"),
            proteinPerUnitG = requiredDouble("proteinPerUnitG").requireNonNegative("proteinPerUnitG"),
            createdAtEpochMillis = requiredLong("createdAtEpochMillis").requireNonNegative("createdAtEpochMillis"),
            updatedAtEpochMillis = requiredLong("updatedAtEpochMillis").requireNonNegative("updatedAtEpochMillis"),
        )
    }

    private fun JSONObject.toGoal(): DailyGoalEntity {
        val calories = requiredDouble("caloriesKcal").requirePositive("caloriesKcal")
        val protein = requiredDouble("proteinG").requirePositive("proteinG")
        val effectiveDate = requiredString("effectiveFromDate")
        LocalDate.parse(effectiveDate)
        return DailyGoalEntity(
            caloriesKcal = calories,
            proteinG = protein,
            effectiveFromDate = effectiveDate,
            updatedAtEpochMillis = requiredLong("updatedAtEpochMillis").requireNonNegative("updatedAtEpochMillis"),
        )
    }

    private fun JSONObject.requiredString(key: String): String = get(key) as? String
        ?: fail("백업 파일의 $key 항목이 올바르지 않습니다.")

    private fun JSONObject.requiredLong(key: String): Long {
        val value = get(key) as? Number ?: fail("백업 파일의 $key 항목이 올바르지 않습니다.")
        val longValue = value.toLong()
        if (value.toDouble() != longValue.toDouble()) fail("백업 파일의 $key 항목이 정수가 아닙니다.")
        return longValue
    }

    private fun JSONObject.requiredDouble(key: String): Double {
        val value = (get(key) as? Number)?.toDouble()
            ?: fail("백업 파일의 $key 항목이 올바르지 않습니다.")
        if (!value.isFinite()) fail("백업 파일의 $key 항목이 올바르지 않습니다.")
        return value
    }

    private fun JSONObject.requiredArray(key: String): JSONArray = get(key) as? JSONArray
        ?: fail("백업 파일의 $key 항목이 올바르지 않습니다.")

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { index ->
        get(index) as? JSONObject ?: fail("백업 목록의 $index 항목 형식이 올바르지 않습니다.")
    }

    private fun String.requireLength(maxLength: Int, field: String): String {
        if (length > maxLength) fail("백업 파일의 $field 항목이 너무 깁니다.")
        return this
    }

    private fun Double.requirePositive(field: String): Double {
        if (this <= 0.0) fail("백업 파일의 $field 값은 0보다 커야 합니다.")
        return this
    }

    private fun Double.requireNonNegative(field: String): Double {
        if (this < 0.0) fail("백업 파일의 $field 값은 음수일 수 없습니다.")
        return this
    }

    private fun Long.requireNonNegative(field: String): Long {
        if (this < 0L) fail("백업 파일의 $field 값은 음수일 수 없습니다.")
        return this
    }

    private fun fail(message: String): Nothing = throw MealBackupFormatException(message)
}
