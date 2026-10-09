package com.hazuny.mealtracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.hazuny.mealtracker.MealViewModel
import com.hazuny.mealtracker.data.FoodTemplateEntity

@Composable
fun SettingsScreen(
    viewModel: MealViewModel,
    goalCalories: Double?,
    goalProtein: Double?,
    foodTemplates: List<FoodTemplateEntity>,
) {
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var editingTemplate by remember { mutableStateOf<FoodTemplateEntity?>(null) }
    var creatingTemplate by remember { mutableStateOf(false) }
    var deletingTemplate by remember { mutableStateOf<FoodTemplateEntity?>(null) }

    LaunchedEffect(goalCalories, goalProtein) {
        calories = goalCalories?.let(::formatAmount).orEmpty()
        protein = goalProtein?.let(::formatAmount).orEmpty()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text("설정", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("하루 목표와 내가 등록한 음식을 관리해요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("일일 목표", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = calories,
                            onValueChange = { calories = it; message = null },
                            label = { Text("칼로리 (kcal)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = protein,
                            onValueChange = { protein = it; message = null },
                            label = { Text("단백질 (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (message != null) Text(message!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = {
                            val parsedCalories = calories.toDoubleOrNull()
                            val parsedProtein = protein.toDoubleOrNull()
                            if (parsedCalories == null || parsedProtein == null || parsedCalories <= 0 || parsedProtein <= 0) {
                                message = "두 목표 모두 0보다 큰 숫자로 입력해 주세요."
                            } else {
                                viewModel.saveGoal(parsedCalories, parsedProtein)
                                message = "목표를 저장했어요."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("목표 저장") }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("내 음식", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("반복해서 먹는 음식을 선택할 때 사용해요.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = { creatingTemplate = true }) { Text("음식 등록") }
            }
        }
        if (foodTemplates.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Text("등록한 음식이 아직 없어요. 바로 기록에는 음식 등록이 필요하지 않아요.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(foodTemplates, key = { it.id }) { template ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(template.name, fontWeight = FontWeight.SemiBold)
                            Text("1 ${template.unit}당 ${formatAmount(template.caloriesPerUnitKcal)} kcal · 단백질 ${formatAmount(template.proteinPerUnitG)}g", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { editingTemplate = template }) { Text("수정") }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(5.dp)) }
    }

    if (creatingTemplate || editingTemplate != null) {
        val template = editingTemplate
        FoodTemplateEditorDialog(
            template = template,
            onDismiss = { creatingTemplate = false; editingTemplate = null },
            onSave = {
                viewModel.saveTemplate(it)
                creatingTemplate = false
                editingTemplate = null
            },
            onDelete = { deletingTemplate = it },
        )
    }
    deletingTemplate?.let { template ->
        AlertDialog(
            onDismissRequest = { deletingTemplate = null },
            title = { Text("음식을 삭제할까요?") },
            text = { Text("${template.name} 템플릿만 삭제하고, 기존 식단 기록은 유지해요.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTemplate(template.id)
                    deletingTemplate = null
                    editingTemplate = null
                }) { Text("삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deletingTemplate = null }) { Text("취소") } },
        )
    }
}
