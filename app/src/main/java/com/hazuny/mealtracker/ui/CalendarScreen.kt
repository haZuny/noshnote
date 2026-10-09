package com.hazuny.mealtracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import com.hazuny.mealtracker.data.MealEntryEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(
    entries: List<MealEntryEntity>,
    onSelectDate: (String) -> Unit,
) {
    var monthKey by rememberSaveable { mutableStateOf(LocalDate.now().withDayOfMonth(1).toString()) }
    val month = LocalDate.parse(monthKey)
    val today = LocalDate.now()
    val datesWithRecords = remember(entries) { entries.map { it.dateKey }.toSet() }
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
        val horizontalPadding = if (compactWidth) 12.dp else 18.dp
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
                .padding(horizontal = horizontalPadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("달력", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "기록한 날짜와 연속 기록을 확인해요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
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
                Text(
                    month.format(DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN)),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(
                    onClick = { monthKey = month.plusMonths(1).toString() },
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = "다음 달" },
                ) {
                    Text("›", fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Row(
                modifier = Modifier.width(calendarWidth).align(Alignment.CenterHorizontally),
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

            Column(
                modifier = Modifier.width(calendarWidth).align(Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(gridGap),
            ) {
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
                                    hasRecord = date.toString() in datesWithRecords,
                                    modifier = Modifier.weight(1f).height(cellHeight),
                                    dayNumberFontSize = dayNumberFontSize,
                                    onClick = { onSelectDate(date.toString()) },
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Spacer(
                    Modifier
                        .size(11.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF9FD2E8)),
                )
                Text(
                    "기록한 날 · 오늘은 테두리로 표시",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
    hasRecord: Boolean,
    modifier: Modifier = Modifier,
    dayNumberFontSize: androidx.compose.ui.unit.TextUnit,
    onClick: () -> Unit,
) {
    val isToday = date == LocalDate.now()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (hasRecord) Color(0xFF9FD2E8) else Color(0xFFF0F3F5))
            .then(if (isToday) Modifier.border(1.5.dp, Color(0xFF287FA5), RoundedCornerShape(10.dp)) else Modifier)
            .clickable(onClick = onClick)
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
            color = if (hasRecord) Color(0xFF194E66) else MaterialTheme.colorScheme.onSurface,
        )
        if (hasRecord) {
            Spacer(
                Modifier
                    .padding(top = 3.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF194E66)),
            )
        }
    }
}
