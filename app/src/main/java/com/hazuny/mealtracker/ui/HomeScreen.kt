package com.hazuny.mealtracker.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazuny.mealtracker.MealViewModel
import com.hazuny.mealtracker.data.FoodTemplateEntity
import com.hazuny.mealtracker.data.MealEntryEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class EntryGroup(val tag: String, val items: List<MealEntryEntity>)

@Composable
fun HomeScreen(
    viewModel: MealViewModel,
    dateKey: String,
    entries: List<MealEntryEntity>,
    foodTemplates: List<FoodTemplateEntity>,
    goalCalories: Double?,
    goalProtein: Double?,
    onDateChange: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val date = LocalDate.parse(dateKey)
    val dateEntries = remember(entries, dateKey) { entries.filter { it.dateKey == dateKey }.sortedBy { it.eatenAtEpochMillis } }
    val groups = remember(dateEntries) { groupAdjacentEntries(dateEntries) }
    var showRecordDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<MealEntryEntity?>(null) }
    var showFoodPicker by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf<FoodTemplateEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(if (date == LocalDate.now()) "오늘 기록" else "${date.monthValue}월 ${date.dayOfMonth}일 기록", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("먹은 것을 간단히 기록해요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(onClick = { onDateChange(date.minusDays(1).toString()) }) { Text("‹") }
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = { onDateChange(date.plusDays(1).toString()) }) { Text("›") }
                TextButton(onClick = { onDateChange(LocalDate.now().toString()) }) { Text("오늘") }
            }
        }
        item {
            DailySummaryCard(
                calories = dateEntries.sumOf { it.caloriesKcalSnapshot },
                protein = dateEntries.sumOf { it.proteinGSnapshot },
                goalCalories = goalCalories,
                goalProtein = goalProtein,
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("기록", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text("${dateEntries.size}개", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { editingEntry = null; showRecordDialog = true },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3C627A)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp),
                ) { Text("＋  바로 기록", fontWeight = FontWeight.SemiBold) }
                Button(
                    onClick = { showFoodPicker = true },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3C627A)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp),
                ) { Text("≡  음식 불러오기", fontWeight = FontWeight.SemiBold) }
            }
        }
        if (dateEntries.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 25.dp, horizontal = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("아직 기록이 없어요", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("음식을 등록하지 않아도 바로 기록할 수 있어요.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            groups.forEach { group ->
                item(key = "tag-${group.items.first().id}") {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                            Spacer(Modifier.width(7.dp))
                            Text(group.tag, color = Color(0xFF326F8B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .65f)),
                        ) {
                            group.items.forEachIndexed { index, entry ->
                                EntryRow(entry = entry, onClick = { editingEntry = entry; showRecordDialog = true })
                                if (index != group.items.lastIndex) HorizontalDivider(color = Color(0xFFE5E9EC), thickness = .7.dp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRecordDialog) {
        RecordEditorDialog(
            entry = editingEntry,
            dateKey = dateKey,
            onDismiss = { showRecordDialog = false },
            onSave = { draft, saveTemplate ->
                viewModel.saveEntry(editingEntry, dateKey, draft.time, draft.mealTag, draft.tagWasManuallySet, draft.foodName, draft.quantity, draft.unit, draft.calories, draft.protein)
                if (saveTemplate && editingEntry == null) {
                    viewModel.saveTemplate(
                        FoodTemplateEntity(
                            name = draft.foodName,
                            unit = draft.unit,
                            caloriesPerUnitKcal = draft.calories / draft.quantity,
                            proteinPerUnitG = draft.protein / draft.quantity,
                        ),
                    )
                }
                showRecordDialog = false
            },
            onDelete = { entry ->
                viewModel.deleteEntry(entry.id)
                showRecordDialog = false
            },
        )
    }
    if (showFoodPicker) {
        FoodTemplatePickerDialog(
            templates = foodTemplates,
            onDismiss = { showFoodPicker = false },
            onSelect = { template ->
                showFoodPicker = false
                selectedTemplate = template
            },
            onOpenSettings = {
                showFoodPicker = false
                onOpenSettings()
            },
        )
    }
    selectedTemplate?.let { template ->
        TemplateRecordDialog(
            template = template,
            dateKey = dateKey,
            onDismiss = { selectedTemplate = null },
            onSave = { draft ->
                viewModel.saveEntry(null, dateKey, draft.time, draft.mealTag, draft.tagWasManuallySet, draft.foodName, draft.quantity, draft.unit, draft.calories, draft.protein)
                selectedTemplate = null
            },
        )
    }
}

@Composable
private fun DailySummaryCard(calories: Double, protein: Double, goalCalories: Double?, goalProtein: Double?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFEFF)),
        border = BorderStroke(2.dp, Color(0xFF64B3D3)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("오늘 섭취 현황", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            NutrientProgress("칼로리", calories, goalCalories, "kcal", Color(0xFF329AC6))
            NutrientProgress("단백질", protein, goalProtein, "g", Color(0xFF78B7D4))
        }
    }
}

@Composable
private fun NutrientProgress(label: String, value: Double, goal: Double?, unit: String, color: Color) {
    val targetText = goal?.let { formatAmount(it) } ?: "목표 미설정"
    val progress = if (goal != null && goal > 0) (value / goal).coerceIn(0.0, 1.0).toFloat() else 0f
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(label, modifier = Modifier.weight(1f), color = Color(0xFF326F8B), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("${formatAmount(value)} / $targetText${if (goal != null) " $unit" else ""}", fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(Color(0xFFDCEBF2))) {
            Box(Modifier.fillMaxWidth(progress).height(7.dp).clip(CircleShape).background(color))
        }
        if (goal != null) {
            val remaining = (goal - value).coerceAtLeast(0.0)
            Text("${if (value >= goal) "목표에 도달했어요" else "남은 양 ${formatAmount(remaining)} $unit"}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EntryRow(entry: MealEntryEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val time = Instant.ofEpochMilli(entry.eatenAtEpochMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
        Text(time, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Column(Modifier.weight(1f)) {
            Text(entry.foodNameSnapshot, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text("${formatAmount(entry.quantity)} ${entry.unitSnapshot} · 단백질 ${formatAmount(entry.proteinGSnapshot)}g", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Text("${formatAmount(entry.caloriesKcalSnapshot)} kcal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun groupAdjacentEntries(entries: List<MealEntryEntity>): List<EntryGroup> = buildList {
    entries.forEach { entry ->
        val tag = entry.mealTag.ifBlank { "태그 없음" }
        val last = lastOrNull()
        if (last?.tag == tag) {
            removeAt(lastIndex)
            add(last.copy(items = last.items + entry))
        } else {
            add(EntryGroup(tag, listOf(entry)))
        }
    }
}

fun formatAmount(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.getDefault(), "%.1f", value)

fun inferMealTag(time: String): String {
    val parsed = runCatching { LocalTime.parse(time) }.getOrNull() ?: return "간식"
    return when (parsed.hour) {
        in 5..10 -> "아침"
        in 11..15 -> "점심"
        in 16..21 -> "저녁"
        else -> "간식"
    }
}
