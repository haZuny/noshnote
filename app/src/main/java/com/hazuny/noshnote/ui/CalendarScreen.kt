package com.hazuny.noshnote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.navigationBarsPadding
import com.hazuny.noshnote.data.MealEntryEntity
import com.hazuny.noshnote.ui.theme.NoshNoteSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    entries: List<MealEntryEntity>,
    goalCalories: Double?,
    goalProtein: Double?,
    onSelectDate: (String) -> Unit,
) {
    var monthKey by rememberSaveable { mutableStateOf(LocalDate.now().withDayOfMonth(1).toString()) }
    var showMonthPicker by rememberSaveable { mutableStateOf(false) }
    var pickerYear by rememberSaveable { mutableStateOf(LocalDate.now().year) }
    var pickerMonth by rememberSaveable { mutableStateOf(LocalDate.now().monthValue) }
    val month = LocalDate.parse(monthKey)
    val today = LocalDate.now()
    val datesWithRecords = remember(entries) { entries.map { it.dateKey }.toSet() }
    val entriesByDate = remember(entries) { entries.groupBy { it.dateKey } }
    val monthRecordDays = remember(entries, monthKey) {
        entries.asSequence().filter { it.dateKey.startsWith(monthKey.take(7)) }.map { it.dateKey }.toSet().size
    }
    val streak = remember(datesWithRecords) {
        var cursor = if (today.toString() in datesWithRecords) today else today.minusDays(1)
        var days = 0
        while (cursor.toString() in datesWithRecords) {
            days += 1
            cursor = cursor.minusDays(1)
        }
        days
    }
    val leadingDays = month.dayOfWeek.value % 7
    val calendarCells: List<LocalDate?> = buildList {
        repeat(leadingDays) { add(null) }
        for (day in 1..month.lengthOfMonth()) add(month.withDayOfMonth(day))
        while (size % 7 != 0) add(null)
    }
    val calendarWeeks = calendarCells.chunked(7)
    val fontScale = LocalDensity.current.fontScale

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compactWidth = maxWidth < 360.dp
        val horizontalPadding = if (compactWidth) 12.dp else NoshNoteSpacing.screenHorizontal
        val gridGap = if (compactWidth || fontScale >= 1.3f) 3.dp else 5.dp
        val calendarWidth = (maxWidth - horizontalPadding * 2).coerceAtMost(420.dp)
        val cellWidth = (calendarWidth - gridGap * 6) / 7
        // Keep each date's tap area at least 48dp tall while capping tablet-sized cells.
        val cellHeight = cellWidth.coerceIn(48.dp, 56.dp)
        val dayNumberFontSize = (cellWidth.value * 0.62f / fontScale).coerceIn(10f, 14f).sp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding, vertical = NoshNoteSpacing.screenTop),
            verticalArrangement = Arrangement.spacedBy(NoshNoteSpacing.section),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (compactWidth) 14.dp else 16.dp),
                ) {
                    val stackStats = maxWidth < 280.dp || fontScale >= 1.6f
                    if (stackStats) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            CalendarStat("이번 달 기록", "${monthRecordDays}일", Modifier.fillMaxWidth())
                            CalendarStat("현재 연속 기록", "${streak}일", Modifier.fillMaxWidth())
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CalendarStat("이번 달 기록", "${monthRecordDays}일", Modifier.weight(1f))
                            CalendarStat("현재 연속 기록", "${streak}일", Modifier.weight(1f))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { monthKey = month.minusMonths(1).toString() },
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = "이전 달" },
                ) {
                    Text("‹", fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                BoxWithConstraints(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    TextButton(
                        onClick = {
                            pickerYear = month.year
                            pickerMonth = month.monthValue
                            showMonthPicker = true
                        },
                        modifier = Modifier.semantics { contentDescription = "월 선택" },
                    ) {
                        Text(
                            month.format(DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text("⌄", modifier = Modifier.padding(start = 6.dp), fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                IconButton(
                    onClick = { monthKey = month.plusMonths(1).toString() },
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = "다음 달" },
                ) {
                    Text("›", fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Card(
                modifier = Modifier.width(calendarWidth).align(Alignment.CenterHorizontally),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(gridGap),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gridGap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        listOf("일", "월", "화", "수", "목", "금", "토").forEach { label ->
                            Text(
                                label,
                                modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(gridGap)) {
                        calendarWeeks.forEach { week ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(gridGap),
                            ) {
                                week.forEach { date ->
                                    if (date == null) {
                                        Spacer(Modifier.weight(1f).height(cellHeight))
                                    } else {
                                        CalendarDay(
                                            date = date,
                                            entriesForDate = entriesByDate[date.toString()].orEmpty(),
                                            goalCalories = goalCalories,
                                            goalProtein = goalProtein,
                                            modifier = Modifier.weight(1f).height(cellHeight),
                                            dayNumberFontSize = dayNumberFontSize,
                                            onClick = { onSelectDate(date.toString()) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMonthPicker) {
        val minimumYear = 1900
        val maximumYear = 2100
        ModalBottomSheet(
            onDismissRequest = { showMonthPicker = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("월 선택", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    WheelPickerColumn(
                        value = pickerYear,
                        range = minimumYear..maximumYear,
                        wrap = false,
                        label = { "${it}년" },
                        onValueChange = { pickerYear = it },
                        modifier = Modifier.weight(1f),
                    )
                    WheelPickerColumn(
                        value = pickerMonth,
                        range = 1..12,
                        wrap = true,
                        label = { "${it}월" },
                        onValueChange = { pickerMonth = it },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(
                        onClick = { showMonthPicker = false },
                        modifier = Modifier.weight(1f),
                    ) { Text("취소") }
                    Button(
                        onClick = {
                            monthKey = LocalDate.of(pickerYear.coerceIn(minimumYear, maximumYear), pickerMonth, 1).toString()
                            showMonthPicker = false
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("이 달 보기") }
                }
            }
        }
    }
}

@Composable
private fun WheelPickerColumn(
    value: Int,
    range: IntRange,
    wrap: Boolean,
    label: (Int) -> String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dragThreshold = with(LocalDensity.current) { 30.dp.toPx() }
    val latestValue = rememberUpdatedState(value)
    Column(
        modifier = modifier
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(range.first, range.last, wrap) {
                var currentValue = value
                var dragDistance = 0f
                detectVerticalDragGestures(
                    onDragStart = {
                        currentValue = latestValue.value
                        dragDistance = 0f
                    },
                    onDragEnd = { dragDistance = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        dragDistance += dragAmount
                        while (dragDistance <= -dragThreshold) {
                            currentValue = nextWheelValue(currentValue, 1, range, wrap)
                            onValueChange(currentValue)
                            dragDistance += dragThreshold
                        }
                        while (dragDistance >= dragThreshold) {
                            currentValue = nextWheelValue(currentValue, -1, range, wrap)
                            onValueChange(currentValue)
                            dragDistance -= dragThreshold
                        }
                    },
                )
            },
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        (-2..2).forEach { offset ->
            val itemValue = nextWheelValue(value, offset, range, wrap)
            val selected = offset == 0
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable(enabled = !selected) { onValueChange(itemValue) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(itemValue),
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (kotlin.math.abs(offset) == 1) 0.72f else 0.42f),
                    style = if (selected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

private fun nextWheelValue(value: Int, delta: Int, range: IntRange, wrap: Boolean): Int {
    val next = value + delta
    return when {
        wrap && next > range.last -> range.first
        wrap && next < range.first -> range.last
        else -> next.coerceIn(range.first, range.last)
    }
}

@Composable
private fun CalendarStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate,
    entriesForDate: List<MealEntryEntity>,
    goalCalories: Double?,
    goalProtein: Double?,
    modifier: Modifier = Modifier,
    dayNumberFontSize: androidx.compose.ui.unit.TextUnit,
    onClick: () -> Unit,
) {
    val isToday = date == LocalDate.now()
    val hasRecord = entriesForDate.isNotEmpty()
    val calories = entriesForDate.sumOf { it.caloriesKcalSnapshot }
    val protein = entriesForDate.sumOf { it.proteinGSnapshot }
    val caloriesGoalValue = goalCalories?.takeIf { it > 0 }
    val proteinGoalValue = goalProtein?.takeIf { it > 0 }
    val goalCount = (if (caloriesGoalValue != null) 1 else 0) + (if (proteinGoalValue != null) 1 else 0)
    val achievedGoalCount =
        (if (caloriesGoalValue != null && calories <= caloriesGoalValue) 1 else 0) +
            (if (proteinGoalValue != null && protein >= proteinGoalValue) 1 else 0)
    val backgroundColor = when {
        !hasRecord -> Color(0xFFF0F3F5)
        goalCount == 0 -> Color(0xFFE4F4FA)
        achievedGoalCount == 0 -> Color(0xFFFCE8E6)
        achievedGoalCount < goalCount -> Color(0xFFB8E0EF)
        else -> Color(0xFF2588B2)
    }
    val contentColor = if (goalCount > 0 && achievedGoalCount == goalCount) {
        Color.White
    } else {
        Color(0xFF194E66)
    }
    val statusDescription = when {
        isToday && !hasRecord -> "오늘 진행 중, 아직 기록 없음"
        isToday && goalCount == 0 -> "오늘 진행 중, 목표 미설정"
        isToday -> "오늘 진행 중, 현재 $achievedGoalCount/$goalCount 목표 달성"
        !hasRecord -> "기록 없음"
        goalCount == 0 -> "기록 있음, 목표 미설정"
        achievedGoalCount == 0 -> "미달성, 0/$goalCount 목표"
        achievedGoalCount < goalCount -> "일부 달성, $achievedGoalCount/$goalCount 목표"
        else -> "성공, $achievedGoalCount/$goalCount 목표"
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .then(if (isToday) Modifier.border(1.5.dp, Color(0xFF287FA5), RoundedCornerShape(10.dp)) else Modifier)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "${date.monthValue}월 ${date.dayOfMonth}일, $statusDescription" }
            .then(if (isToday) Modifier.padding(1.dp) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            date.dayOfMonth.toString(),
            maxLines = 1,
            softWrap = false,
            fontSize = dayNumberFontSize,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            color = if (hasRecord) contentColor else MaterialTheme.colorScheme.onSurface,
        )
    }
}
