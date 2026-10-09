package com.hazuny.noshnote.ui

import com.hazuny.noshnote.R

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
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
import com.hazuny.noshnote.MealViewModel
import com.hazuny.noshnote.data.FoodTemplateEntity
import com.hazuny.noshnote.data.MealEntryEntity
import com.hazuny.noshnote.ui.theme.NoshNoteSpacing
import java.math.BigDecimal
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = NoshNoteSpacing.screenHorizontal,
            end = NoshNoteSpacing.screenHorizontal,
            top = NoshNoteSpacing.screenTop,
            bottom = NoshNoteSpacing.screenBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(NoshNoteSpacing.section),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(
                    onClick = { onDateChange(date.minusDays(1).toString()) },
                    modifier = Modifier.size(48.dp),
                ) { Text("‹", fontSize = 36.sp, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center) }
                AutoFitText(
                    text = date.format(DateTimeFormatter.ofPattern(uiText(R.string.date_pattern), Locale.getDefault())),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                IconButton(
                    onClick = { onDateChange(date.plusDays(1).toString()) },
                    modifier = Modifier.size(48.dp),
                ) { Text("›", fontSize = 36.sp, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center) }
                Button(
                    onClick = { onDateChange(LocalDate.now().toString()) },
                    modifier = Modifier.height(40.dp),
                    shape = MaterialTheme.shapes.small,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 13.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) { AutoFitText(uiText(R.string.today), fontWeight = FontWeight.Bold, maxLines = 1) }
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
                AutoFitText(uiText(R.string.record), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(Modifier.width(8.dp))
                AutoFitText(uiText(R.string.item_count, dateEntries.size), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { editingEntry = null; showRecordDialog = true },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    AutoFitText(uiText(R.string.quick_record), textAlign = TextAlign.Center)
                }
                Button(
                    onClick = { showFoodPicker = true },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    AutoFitText(uiText(R.string.load_food), textAlign = TextAlign.Center)
                }
            }
        }
        if (dateEntries.isEmpty()) {
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 25.dp, horizontal = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AutoFitText(uiText(R.string.no_records_yet), fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(uiText(R.string.quick_record_no_template), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
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
                            AutoFitText(mealTagText(group.tag), color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            group.items.forEachIndexed { index, entry ->
                                EntryRow(entry = entry, onClick = { editingEntry = entry; showRecordDialog = true })
                                if (index != group.items.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .65f), thickness = .7.dp)
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
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(NoshNoteSpacing.cardPadding), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AutoFitText(uiText(R.string.today_intake), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            NutrientProgress(R.string.calories, calories, goalCalories, "kcal", MaterialTheme.colorScheme.primary)
            NutrientProgress(R.string.protein, protein, goalProtein, "g", MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun NutrientProgress(labelRes: Int, value: Double, goal: Double?, unit: String, color: Color) {
    val label = uiText(labelRes)
    val targetText = goal?.let { formatAmount(it) } ?: uiText(R.string.goal_not_set)
    val progress = if (goal != null && goal > 0) (value / goal).coerceIn(0.0, 1.0).toFloat() else 0f
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            AutoFitText(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, contentAlignment = Alignment.CenterStart)
            AutoFitText(
                "${formatAmount(value)} / $targetText${if (goal != null) " $unit" else ""}",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                contentAlignment = Alignment.CenterEnd,
            )
        }
        Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)) {
            Box(Modifier.fillMaxWidth(progress).height(7.dp).clip(CircleShape).background(color))
        }
        if (goal != null) {
            val remaining = (goal - value).coerceAtLeast(0.0)
            AutoFitText(if (value >= goal) uiText(R.string.goal_reached) else uiText(R.string.amount_remaining, formatAmount(remaining), unit), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
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
            AutoFitText(entry.foodNameSnapshot, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            AutoFitText(uiText(R.string.entry_detail, formatAmount(entry.quantity), entry.unitSnapshot, formatAmount(entry.proteinGSnapshot)), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, minFontSize = 8.sp)
        }
        Text("${formatAmount(entry.caloriesKcalSnapshot)} kcal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun groupAdjacentEntries(entries: List<MealEntryEntity>): List<EntryGroup> = buildList {
    entries.forEach { entry ->
        val tag = entry.mealTag
        val last = lastOrNull()
        if (last?.tag == tag) {
            removeAt(lastIndex)
            add(last.copy(items = last.items + entry))
        } else {
            add(EntryGroup(tag, listOf(entry)))
        }
    }
}

@Composable
internal fun mealTagText(tag: String): String = when (tag) {
    "아침" -> uiText(R.string.meal_breakfast)
    "점심" -> uiText(R.string.meal_lunch)
    "저녁" -> uiText(R.string.meal_dinner)
    "간식" -> uiText(R.string.meal_snack)
    else -> uiText(R.string.tag_none)
}

fun formatAmount(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.getDefault(), "%.1f", value)

fun formatInputAmount(value: Double): String = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

fun inferMealTag(time: String): String {
    val parsed = runCatching { LocalTime.parse(time) }.getOrNull() ?: return "간식"
    return when (parsed.hour) {
        in 5..10 -> "아침"
        in 11..15 -> "점심"
        in 16..21 -> "저녁"
        else -> "간식"
    }
}
