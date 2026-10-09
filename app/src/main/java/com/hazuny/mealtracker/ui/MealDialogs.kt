package com.hazuny.mealtracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hazuny.mealtracker.data.FoodTemplateEntity
import com.hazuny.mealtracker.data.MealEntryEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class RecordDraft(
    val foodName: String,
    val quantity: Double,
    val unit: String,
    val calories: Double,
    val protein: Double,
    val time: String,
    val mealTag: String,
    val tagWasManuallySet: Boolean,
)

@Composable
fun RecordEditorDialog(
    entry: MealEntryEntity?,
    dateKey: String,
    onDismiss: () -> Unit,
    onSave: (RecordDraft, Boolean) -> Unit,
    onDelete: (MealEntryEntity) -> Unit,
) {
    val initialTime = entry?.let {
        Instant.ofEpochMilli(it.eatenAtEpochMillis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    } ?: LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
    var foodName by remember(entry?.id) { mutableStateOf(entry?.foodNameSnapshot.orEmpty()) }
    var quantity by remember(entry?.id) { mutableStateOf(entry?.quantity?.toString() ?: "1") }
    var unit by remember(entry?.id) { mutableStateOf(entry?.unitSnapshot ?: "인분") }
    var calories by remember(entry?.id) { mutableStateOf(entry?.caloriesKcalSnapshot?.toString().orEmpty()) }
    var protein by remember(entry?.id) { mutableStateOf(entry?.proteinGSnapshot?.toString().orEmpty()) }
    var time by remember(entry?.id) { mutableStateOf(initialTime) }
    var mealTag by remember(entry?.id) { mutableStateOf(entry?.mealTag ?: inferMealTag(initialTime)) }
    var tagManual by remember(entry?.id) { mutableStateOf(entry?.tagSource == "manual") }
    var saveTemplate by remember(entry?.id) { mutableStateOf(false) }
    var tagMenuExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val tags = listOf("아침", "점심", "저녁", "간식", "태그 없음")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry == null) "바로 기록" else "기록 수정", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(foodName, { foodName = it }, label = { Text("음식 이름") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        quantity,
                        { quantity = it },
                        label = { Text("이번에 먹은 양") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(unit, { unit = it }, label = { Text("단위") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Text("영양값은 이번에 먹은 전체 양 기준이에요.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        calories,
                        { calories = it },
                        label = { Text("칼로리 (kcal)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        protein,
                        { protein = it },
                        label = { Text("단백질 (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = time,
                        onValueChange = {
                            time = it
                            if (!tagManual) mealTag = inferMealTag(it)
                        },
                        label = { Text("먹은 시간") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("식사 태그", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(onClick = { tagMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(mealTag.ifBlank { "태그 없음" })
                        }
                        DropdownMenu(expanded = tagMenuExpanded, onDismissRequest = { tagMenuExpanded = false }) {
                            tags.forEach { tag ->
                                DropdownMenuItem(
                                    text = { Text(tag) },
                                    onClick = {
                                        mealTag = if (tag == "태그 없음") "" else tag
                                        tagManual = true
                                        tagMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
                if (entry == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = saveTemplate, onCheckedChange = { saveTemplate = it })
                        Text("다음에도 쓰도록 음식 목록에 저장", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (errorMessage != null) Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsedQuantity = quantity.toDoubleOrNull()
                val parsedCalories = calories.toDoubleOrNull()
                val parsedProtein = protein.toDoubleOrNull()
                val parsedTime = runCatching { LocalTime.parse(time) }.getOrNull()
                errorMessage = when {
                    foodName.isBlank() -> "음식 이름을 입력해 주세요."
                    unit.isBlank() -> "단위를 입력해 주세요."
                    parsedQuantity == null || parsedQuantity <= 0 -> "먹은 양을 확인해 주세요."
                    parsedCalories == null || parsedCalories < 0 -> "칼로리를 확인해 주세요."
                    parsedProtein == null || parsedProtein < 0 -> "단백질을 확인해 주세요."
                    parsedTime == null -> "시간은 24시간 형식으로 입력해 주세요."
                    else -> null
                }
                if (errorMessage == null) {
                    onSave(
                        RecordDraft(
                            foodName = foodName.trim(),
                            quantity = parsedQuantity!!,
                            unit = unit.trim(),
                            calories = parsedCalories!!,
                            protein = parsedProtein!!,
                            time = time,
                            mealTag = mealTag,
                            tagWasManuallySet = tagManual,
                        ),
                        saveTemplate,
                    )
                }
            }) { Text("저장") }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (entry != null) TextButton(onClick = { onDelete(entry) }) { Text("삭제", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("취소") }
            }
        },
    )
}

@Composable
fun TemplateRecordDialog(
    template: FoodTemplateEntity,
    dateKey: String,
    onDismiss: () -> Unit,
    onSave: (RecordDraft) -> Unit,
) {
    var quantity by remember(template.id) { mutableStateOf("1") }
    val timeNow = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
    var time by remember(template.id) { mutableStateOf(timeNow) }
    var tag by remember(template.id) { mutableStateOf(inferMealTag(timeNow)) }
    var tagManual by remember(template.id) { mutableStateOf(false) }
    var tagMenuExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val tags = listOf("아침", "점심", "저녁", "간식", "태그 없음")
    val amount = quantity.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${template.name} 기록", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("1 ${template.unit}당 ${formatAmount(template.caloriesPerUnitKcal)} kcal · 단백질 ${formatAmount(template.proteinPerUnitG)}g", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    quantity,
                    { quantity = it },
                    label = { Text("이번에 먹은 양 (${template.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("이번 섭취량 · ${formatAmount(amount * template.caloriesPerUnitKcal)} kcal · 단백질 ${formatAmount(amount * template.proteinPerUnitG)}g", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = time,
                        onValueChange = {
                            time = it
                            if (!tagManual) tag = inferMealTag(it)
                        },
                        label = { Text("먹은 시간") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("식사 태그", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(onClick = { tagMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(tag.ifBlank { "태그 없음" }) }
                        DropdownMenu(expanded = tagMenuExpanded, onDismissRequest = { tagMenuExpanded = false }) {
                            tags.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = {
                                tag = if (option == "태그 없음") "" else option
                                tagManual = true
                                tagMenuExpanded = false
                            }) }
                        }
                    }
                }
                if (errorMessage != null) Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsed = quantity.toDoubleOrNull()
                val validTime = runCatching { LocalTime.parse(time) }.isSuccess
                errorMessage = when {
                    parsed == null || parsed <= 0 -> "먹은 양을 확인해 주세요."
                    !validTime -> "시간은 24시간 형식으로 입력해 주세요."
                    else -> null
                }
                if (errorMessage == null) {
                    val count = parsed!!
                    onSave(RecordDraft(template.name, count, template.unit, count * template.caloriesPerUnitKcal, count * template.proteinPerUnitG, time, tag, tagManual))
                }
            }) { Text("기록 추가") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
fun FoodTemplatePickerDialog(
    templates: List<FoodTemplateEntity>,
    onDismiss: () -> Unit,
    onSelect: (FoodTemplateEntity) -> Unit,
    onOpenSettings: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("등록 음식 불러오기", fontWeight = FontWeight.Bold) },
        text = {
            if (templates.isEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("등록한 음식이 아직 없어요. 바로 기록을 사용하거나 자주 먹는 음식을 등록해 보세요.")
                    OutlinedButton(onClick = onOpenSettings) { Text("음식 등록으로 이동") }
                }
            } else {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    templates.forEach { food ->
                        OutlinedButton(onClick = { onSelect(food) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                Text(food.name, fontWeight = FontWeight.SemiBold)
                                Text("1 ${food.unit}당 ${formatAmount(food.caloriesPerUnitKcal)} kcal · 단백질 ${formatAmount(food.proteinPerUnitG)}g", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
    )
}

@Composable
fun FoodTemplateEditorDialog(
    template: FoodTemplateEntity?,
    onDismiss: () -> Unit,
    onSave: (FoodTemplateEntity) -> Unit,
    onDelete: ((FoodTemplateEntity) -> Unit)? = null,
) {
    var name by remember(template?.id) { mutableStateOf(template?.name.orEmpty()) }
    var unit by remember(template?.id) { mutableStateOf(template?.unit ?: "개") }
    var calories by remember(template?.id) { mutableStateOf(template?.caloriesPerUnitKcal?.toString().orEmpty()) }
    var protein by remember(template?.id) { mutableStateOf(template?.proteinPerUnitG?.toString().orEmpty()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (template == null) "음식 등록" else "음식 수정", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("음식 이름") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(unit, { unit = it }, label = { Text("기록 단위") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(calories, { calories = it }, label = { Text("단위당 칼로리") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(protein, { protein = it }, label = { Text("단위당 단백질") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                }
                Text("등록할 때 수량은 입력하지 않아요. 기록할 때 먹은 양을 입력해요.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                if (errorMessage != null) Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsedCalories = calories.toDoubleOrNull()
                val parsedProtein = protein.toDoubleOrNull()
                errorMessage = when {
                    name.isBlank() -> "음식 이름을 입력해 주세요."
                    unit.isBlank() -> "기록 단위를 입력해 주세요."
                    parsedCalories == null || parsedCalories < 0 -> "칼로리를 확인해 주세요."
                    parsedProtein == null || parsedProtein < 0 -> "단백질을 확인해 주세요."
                    else -> null
                }
                if (errorMessage == null) {
                    onSave(FoodTemplateEntity(
                        id = template?.id ?: 0,
                        name = name.trim(),
                        unit = unit.trim(),
                        caloriesPerUnitKcal = parsedCalories!!,
                        proteinPerUnitG = parsedProtein!!,
                        createdAtEpochMillis = template?.createdAtEpochMillis ?: System.currentTimeMillis(),
                    ))
                }
            }) { Text("저장") }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (template != null && onDelete != null) TextButton(onClick = { onDelete(template) }) { Text("삭제", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("취소") }
            }
        },
    )
}
