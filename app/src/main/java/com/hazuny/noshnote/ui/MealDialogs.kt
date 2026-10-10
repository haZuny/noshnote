package com.hazuny.noshnote.ui

import com.hazuny.noshnote.R

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.shape.RoundedCornerShape
import com.hazuny.noshnote.data.FoodTemplateEntity
import com.hazuny.noshnote.data.MealEntryEntity
import com.hazuny.noshnote.data.MealRules
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

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
private fun TimePickerField(
    time: String,
    onTimeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val pickerVisibility = remember { MutableTransitionState(false) }
    LaunchedEffect(showPicker) {
        pickerVisibility.targetState = showPicker
    }
    Column(modifier = modifier) {
        AutoFitText(uiText(R.string.eaten_time), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        OutlinedButton(
            onClick = { showPicker = true },
            modifier = Modifier.fillMaxWidth().testTag("time_picker_button"),
        ) {
            AutoFitText(time, maxLines = 1)
        }
    }

    if (showPicker || pickerVisibility.currentState || pickerVisibility.targetState) {
        val parsedTime = runCatching { LocalTime.parse(time) }.getOrElse { LocalTime.now() }
        val roundedMinute = ((parsedTime.minute + 2) / 5) * 5
        val initialTime = if (roundedMinute == 60) {
            parsedTime.plusHours(1).withMinute(0)
        } else {
            parsedTime.withMinute(roundedMinute)
        }
        var periodIndex by remember { mutableIntStateOf(if (initialTime.hour < 12) 0 else 1) }
        var hourIndex by remember { mutableIntStateOf((initialTime.hour + 11) % 12) }
        var minuteIndex by remember { mutableIntStateOf(initialTime.minute / 5) }
        Dialog(
            onDismissRequest = { showPicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                AnimatedVisibility(
                    visibleState = pickerVisibility,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(durationMillis = 280),
                    ) + fadeIn(animationSpec = tween(durationMillis = 180)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(durationMillis = 240),
                    ) + fadeOut(animationSpec = tween(durationMillis = 160)),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 24.dp)
                                .padding(top = 8.dp, bottom = 12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 12.dp)
                                    .width(32.dp)
                                    .height(4.dp)
                                    .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(50)),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                TimeWheel(
                                    label = uiText(R.string.period_label),
                                    values = listOf(uiText(R.string.period_am), uiText(R.string.period_pm)),
                                    selectedIndex = periodIndex,
                                    onSelectedIndexChange = { periodIndex = it },
                                    testTag = "time_wheel_period",
                                    modifier = Modifier.weight(1.15f),
                                )
                                TimeWheel(
                                    label = uiText(R.string.hour_label),
                                    values = (1..12).map(Int::toString),
                                    selectedIndex = hourIndex,
                                    onSelectedIndexChange = { hourIndex = it },
                                    testTag = "time_wheel_hour",
                                    modifier = Modifier.weight(0.85f),
                                )
                                TimeWheel(
                                    label = uiText(R.string.minute_label),
                                    values = (0..55 step 5).map { it.toString().padStart(2, '0') },
                                    selectedIndex = minuteIndex,
                                    onSelectedIndexChange = { minuteIndex = it },
                                    testTag = "time_wheel_minute",
                                    modifier = Modifier.weight(0.85f),
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TextButton(
                                    onClick = { showPicker = false },
                                    modifier = Modifier.testTag("time_picker_cancel"),
                                ) { AutoFitText(uiText(R.string.cancel), maxLines = 1) }
                                TextButton(
                                    onClick = {
                                        val hour12 = hourIndex + 1
                                        val hour24 = when {
                                            periodIndex == 0 && hour12 == 12 -> 0
                                            periodIndex == 0 -> hour12
                                            hour12 == 12 -> 12
                                            else -> hour12 + 12
                                        }
                                        onTimeChange(
                                            LocalTime.of(hour24, minuteIndex * 5)
                                                .format(DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)),
                                        )
                                        showPicker = false
                                    },
                                    modifier = Modifier.testTag("time_picker_confirm"),
                                ) { AutoFitText(uiText(R.string.confirm), maxLines = 1) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeWheel(
    label: String,
    values: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val scope = rememberCoroutineScope()
    val centeredIndex by remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo
                .minByOrNull { item -> abs(item.offset + item.size / 2 - viewportCenter) }
                ?.index ?: selectedIndex
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { centeredIndex }
            .distinctUntilChanged()
            .collect { index -> onSelectedIndexChange(index.coerceIn(values.indices)) }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AutoFitText(
            label,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(144.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                    ),
            )
            LazyColumn(
                state = listState,
                flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Center),
                contentPadding = PaddingValues(vertical = 48.dp),
                modifier = Modifier.fillMaxWidth().height(144.dp).testTag(testTag),
            ) {
                itemsIndexed(values) { index, value ->
                    val distance = abs(index - centeredIndex)
                    Text(
                        text = value,
                        color = if (distance == 0) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = if (distance == 1) 0.65f else 0.3f)
                        },
                        fontWeight = if (distance == 0) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = if (distance == 0) 26.sp else 20.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .wrapContentHeight(Alignment.CenterVertically)
                            .clickable {
                                onSelectedIndexChange(index)
                                scope.launch {
                                    listState.animateScrollToItem(index)
                                }
                            }
                            .testTag("${testTag}_option_$index"),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
fun RecordEditorDialog(
    entry: MealEntryEntity?,
    dateKey: String,
    onDismiss: () -> Unit,
    onSave: (RecordDraft, Boolean) -> Unit,
    onDelete: (MealEntryEntity) -> Unit,
) {
    val context = LocalContext.current
    val initialTime = entry?.let {
        Instant.ofEpochMilli(it.eatenAtEpochMillis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    } ?: LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
    var foodName by remember(entry?.id) { mutableStateOf(entry?.foodNameSnapshot.orEmpty()) }
    var quantity by remember(entry?.id) { mutableStateOf(entry?.quantity?.toString() ?: "1") }
    val defaultServingUnit = uiText(R.string.unit_serving)
    var unit by remember(entry?.id) { mutableStateOf(entry?.unitSnapshot ?: defaultServingUnit) }
    var calories by remember(entry?.id) { mutableStateOf(entry?.caloriesKcalSnapshot?.toString().orEmpty()) }
    var protein by remember(entry?.id) { mutableStateOf(entry?.proteinGSnapshot?.toString().orEmpty()) }
    var time by remember(entry?.id) { mutableStateOf(initialTime) }
    var mealTag by remember(entry?.id) { mutableStateOf(entry?.mealTag ?: inferMealTag(initialTime)) }
    var tagManual by remember(entry?.id) { mutableStateOf(entry?.tagSource == "manual") }
    var saveTemplate by remember(entry?.id) { mutableStateOf(false) }
    var nutritionBasis by remember(entry?.id) { mutableStateOf<String?>(null) }
    var showNutritionScanner by remember { mutableStateOf(false) }
    var tagMenuExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val tags = listOf("아침", "점심", "저녁", "간식", "태그 없음")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AutoFitText(if (entry == null) uiText(R.string.record_now) else uiText(R.string.edit_record), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(foodName, { foodName = it }, label = { AutoFitText(uiText(R.string.food_name), maxLines = 1) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        quantity,
                        { quantity = it },
                        label = { AutoFitText(uiText(R.string.food_quantity), maxLines = 1) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(unit, { unit = it }, label = { AutoFitText(uiText(R.string.unit_label), maxLines = 1) }, singleLine = true, modifier = Modifier.weight(1f))
                }
                AutoFitText(uiText(R.string.nutrition_total_amount), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        calories,
                        { calories = it },
                        label = { AutoFitText(uiText(R.string.calories_protein_unit), maxLines = 1) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        protein,
                        { protein = it },
                        label = { AutoFitText(uiText(R.string.protein_unit), maxLines = 1) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedButton(onClick = { showNutritionScanner = true }, modifier = Modifier.fillMaxWidth()) {
                    AutoFitText(uiText(R.string.nutrition_from_label))
                }
                nutritionBasis?.let { basis ->
                    AutoFitText(uiText(R.string.nutrition_basis, basis), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TimePickerField(
                        time = time,
                        onTimeChange = {
                            time = it
                            mealTag = MealRules.tagAfterTimeChange(mealTag, tagManual, it)
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        AutoFitText(uiText(R.string.meal_tag), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(onClick = { tagMenuExpanded = true }, modifier = Modifier.fillMaxWidth().testTag("meal_tag_selector")) {
                            AutoFitText(mealTagText(mealTag), maxLines = 1)
                        }
                        DropdownMenu(expanded = tagMenuExpanded, onDismissRequest = { tagMenuExpanded = false }) {
                            tags.forEach { tag ->
                                DropdownMenuItem(
                                    text = { Text(mealTagText(tag), maxLines = 1) },
                                    modifier = Modifier.testTag("meal_tag_option_$tag"),
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
                        AutoFitText(uiText(R.string.save_as_food), style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (errorMessage != null) AutoFitText(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(modifier = Modifier.testTag("record_save_button"), onClick = {
                val parsedQuantity = quantity.toDoubleOrNull()
                val parsedCalories = calories.toDoubleOrNull()
                val parsedProtein = protein.toDoubleOrNull()
                val parsedTime = runCatching { LocalTime.parse(time) }.getOrNull()
                errorMessage = when {
                    foodName.isBlank() -> context.getString(R.string.food_name_required)
                    unit.isBlank() -> context.getString(R.string.unit_required)
                    parsedQuantity == null || parsedQuantity <= 0 -> context.getString(R.string.quantity_invalid)
                    parsedCalories == null || parsedCalories < 0 -> context.getString(R.string.calories_invalid)
                    parsedProtein == null || parsedProtein < 0 -> context.getString(R.string.protein_invalid)
                    parsedTime == null -> context.getString(R.string.time_invalid)
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
            }) { AutoFitText(uiText(R.string.save), maxLines = 1) }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (entry != null) TextButton(onClick = { onDelete(entry) }) { AutoFitText(uiText(R.string.delete), color = MaterialTheme.colorScheme.error, maxLines = 1) }
                TextButton(onClick = onDismiss) { AutoFitText(uiText(R.string.cancel), maxLines = 1) }
            }
        },
    )

    if (showNutritionScanner) {
        NutritionLabelScannerDialog(
            reviewNote = uiText(R.string.confirm_values),
            onDismiss = { showNutritionScanner = false },
            onApply = { values ->
                calories = formatInputAmount(values.caloriesKcal)
                protein = formatInputAmount(values.proteinG)
                nutritionBasis = values.basisDescription ?: context.getString(R.string.basis_unknown)
                showNutritionScanner = false
            },
        )
    }
}

@Composable
fun TemplateRecordDialog(
    template: FoodTemplateEntity,
    dateKey: String,
    onDismiss: () -> Unit,
    onSave: (RecordDraft) -> Unit,
) {
    val context = LocalContext.current
    var quantity by remember(template.id) { mutableStateOf("1") }
    val timeNow = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
    var time by remember(template.id) { mutableStateOf(timeNow) }
    var tag by remember(template.id) { mutableStateOf(inferMealTag(timeNow)) }
    var tagManual by remember(template.id) { mutableStateOf(false) }
    var tagMenuExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val tags = listOf("아침", "점심", "저녁", "간식", "태그 없음")
    val amount = quantity.toDoubleOrNull() ?: 0.0
    val parsedQuantity = quantity.toDoubleOrNull()
    val decreaseQuantityLabel = uiText(R.string.decrease_quantity)
    val increaseQuantityLabel = uiText(R.string.increase_quantity)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AutoFitText(uiText(R.string.template_record_title, template.name), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AutoFitText(uiText(R.string.template_nutrition, template.unit, formatAmount(template.caloriesPerUnitKcal), formatAmount(template.proteinPerUnitG)), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(
                        onClick = {
                            val current = quantity.toDoubleOrNull()
                            if (current != null && current > 1.0) {
                                quantity = formatInputAmount(current - 1.0)
                            }
                        },
                        enabled = parsedQuantity != null && parsedQuantity > 1.0,
                        modifier = Modifier
                            .semantics { contentDescription = decreaseQuantityLabel }
                            .testTag("template_quantity_decrease"),
                    ) {
                        Text("−", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedTextField(
                        quantity,
                        { quantity = it },
                        label = { AutoFitText(uiText(R.string.template_quantity, template.unit), maxLines = 1) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("template_quantity_input"),
                    )
                    IconButton(
                        onClick = {
                            val current = quantity.toDoubleOrNull()?.takeIf { it >= 0.0 } ?: 0.0
                            quantity = formatInputAmount(current + 1.0)
                        },
                        modifier = Modifier
                            .semantics { contentDescription = increaseQuantityLabel }
                            .testTag("template_quantity_increase"),
                    ) {
                        Text("+", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                AutoFitText(uiText(R.string.current_intake, formatAmount(amount * template.caloriesPerUnitKcal), formatAmount(amount * template.proteinPerUnitG)), fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TimePickerField(
                        time = time,
                        onTimeChange = {
                            time = it
                            tag = MealRules.tagAfterTimeChange(tag, tagManual, it)
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        AutoFitText(uiText(R.string.meal_tag), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(onClick = { tagMenuExpanded = true }, modifier = Modifier.fillMaxWidth().testTag("meal_tag_selector")) { AutoFitText(mealTagText(tag), maxLines = 1) }
                        DropdownMenu(expanded = tagMenuExpanded, onDismissRequest = { tagMenuExpanded = false }) {
                            tags.forEach { option -> DropdownMenuItem(text = { Text(mealTagText(option), maxLines = 1) }, modifier = Modifier.testTag("meal_tag_option_$option"), onClick = {
                                tag = if (option == "태그 없음") "" else option
                                tagManual = true
                                tagMenuExpanded = false
                            }) }
                        }
                    }
                }
                if (errorMessage != null) AutoFitText(errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(modifier = Modifier.testTag("record_save_button"), onClick = {
                val parsed = quantity.toDoubleOrNull()
                val validTime = runCatching { LocalTime.parse(time) }.isSuccess
                errorMessage = when {
                    parsed == null || parsed <= 0 -> context.getString(R.string.quantity_invalid)
                    !validTime -> context.getString(R.string.time_invalid)
                    else -> null
                }
                if (errorMessage == null) {
                    val count = parsed!!
                    onSave(RecordDraft(template.name, count, template.unit, count * template.caloriesPerUnitKcal, count * template.proteinPerUnitG, time, tag, tagManual))
                }
            }) { AutoFitText(uiText(R.string.apply_record), maxLines = 1) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { AutoFitText(uiText(R.string.cancel), maxLines = 1) } },
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
        title = { AutoFitText(uiText(R.string.load_registered_food), fontWeight = FontWeight.Bold) },
        text = {
            if (templates.isEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(uiText(R.string.no_templates_help))
                    OutlinedButton(onClick = onOpenSettings) { AutoFitText(uiText(R.string.go_add_food), maxLines = 1) }
                }
            } else {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    templates.forEach { food ->
                        OutlinedButton(onClick = { onSelect(food) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                AutoFitText(food.name, fontWeight = FontWeight.SemiBold)
                                AutoFitText(uiText(R.string.template_nutrition, food.unit, formatAmount(food.caloriesPerUnitKcal), formatAmount(food.proteinPerUnitG)), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { AutoFitText(uiText(R.string.close), maxLines = 1) } },
    )
}

@Composable
fun FoodTemplateEditorDialog(
    template: FoodTemplateEntity?,
    onDismiss: () -> Unit,
    onSave: (FoodTemplateEntity) -> Unit,
    onDelete: ((FoodTemplateEntity) -> Unit)? = null,
) {
    val context = LocalContext.current
    var name by remember(template?.id) { mutableStateOf(template?.name.orEmpty()) }
    val defaultPieceUnit = uiText(R.string.unit_piece)
    var unit by remember(template?.id) { mutableStateOf(template?.unit ?: defaultPieceUnit) }
    var calories by remember(template?.id) { mutableStateOf(template?.caloriesPerUnitKcal?.toString().orEmpty()) }
    var protein by remember(template?.id) { mutableStateOf(template?.proteinPerUnitG?.toString().orEmpty()) }
    var nutritionBasis by remember(template?.id) { mutableStateOf<String?>(null) }
    var showNutritionScanner by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AutoFitText(if (template == null) uiText(R.string.new_food) else uiText(R.string.edit_food), fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { AutoFitText(uiText(R.string.food_name), maxLines = 1) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(unit, { unit = it }, label = { AutoFitText(uiText(R.string.record_unit), maxLines = 1) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(calories, { calories = it }, label = { AutoFitText(uiText(R.string.calories_per_unit), maxLines = 1) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(protein, { protein = it }, label = { AutoFitText(uiText(R.string.protein_per_unit), maxLines = 1) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedButton(onClick = { showNutritionScanner = true }, modifier = Modifier.fillMaxWidth()) {
                    AutoFitText(uiText(R.string.nutrition_from_label))
                }
                nutritionBasis?.let { basis ->
                    AutoFitText(uiText(R.string.nutrition_basis, basis), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                Text(uiText(R.string.template_quantity_note), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                if (errorMessage != null) AutoFitText(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsedCalories = calories.toDoubleOrNull()
                val parsedProtein = protein.toDoubleOrNull()
                errorMessage = when {
                    name.isBlank() -> context.getString(R.string.food_name_required)
                    unit.isBlank() -> context.getString(R.string.record_unit_required)
                    parsedCalories == null || parsedCalories < 0 -> context.getString(R.string.calories_invalid)
                    parsedProtein == null || parsedProtein < 0 -> context.getString(R.string.protein_invalid)
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
            }) { AutoFitText(uiText(R.string.save), maxLines = 1) }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (template != null && onDelete != null) TextButton(onClick = { onDelete(template) }) { AutoFitText(uiText(R.string.delete), color = MaterialTheme.colorScheme.error, maxLines = 1) }
                TextButton(onClick = onDismiss) { AutoFitText(uiText(R.string.cancel), maxLines = 1) }
            }
        },
    )

    if (showNutritionScanner) {
        NutritionLabelScannerDialog(
            reviewNote = uiText(R.string.template_basis_note),
            onDismiss = { showNutritionScanner = false },
            onApply = { values ->
                calories = formatInputAmount(values.caloriesKcal)
                protein = formatInputAmount(values.proteinG)
                nutritionBasis = values.basisDescription ?: context.getString(R.string.basis_unknown)
                showNutritionScanner = false
            },
        )
    }
}
