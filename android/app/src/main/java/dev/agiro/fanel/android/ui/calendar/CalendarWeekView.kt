package dev.agiro.fanel.android.ui.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.agiro.fanel.android.R
import dev.agiro.fanel.android.data.local.CalendarEventEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun CalendarWeekView(
    referenceDate: LocalDate,
    events: List<CalendarEventEntity>,
    onDayClick: (LocalDate) -> Unit,
    onEventClick: (CalendarEventEntity) -> Unit,
    onEventLongClick: (CalendarEventEntity) -> Unit
) {
    val monday = remember(referenceDate) {
        referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    val days = remember(monday) { (0 until 7).map { monday.plusDays(it.toLong()) } }
    val today = remember { LocalDate.now() }
    val eventsByDay = remember(events) { events.groupBy { it.date } }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(40.dp))
            days.forEach { date ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onDayClick(date) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal,
                        color = if (date == today) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        val allDayEvents = days.flatMap { date ->
            eventsByDay[date.toString()].orEmpty().filter { it.time == null }.map { date to it }
        }
        if (allDayEvents.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.all_day),
                    modifier = Modifier.width(40.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                days.forEach { date ->
                    Column(modifier = Modifier.weight(1f)) {
                        eventsByDay[date.toString()].orEmpty()
                            .filter { it.time == null }
                            .forEach { event -> EventChip(event, onEventClick, onEventLongClick) }
                    }
                }
            }
        }
        HorizontalDivider()

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(24) { hour ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "%02d:00".format(hour),
                        modifier = Modifier.width(40.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    days.forEach { date ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .padding(horizontal = 1.dp)
                        ) {
                            eventsByDay[date.toString()].orEmpty()
                                .filter { it.time?.startsWith("%02d:".format(hour)) == true }
                                .forEach { event -> EventChip(event, onEventClick, onEventLongClick) }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EventChip(
    event: CalendarEventEntity,
    onClick: (CalendarEventEntity) -> Unit,
    onLongClick: (CalendarEventEntity) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clip(MaterialTheme.shapes.small)
            .background(
                if (event.pendingStatus != null) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
            )
            .combinedClickable(onClick = { onClick(event) }, onLongClick = { onLongClick(event) })
            .padding(2.dp)
    ) {
        Text(
            text = event.title,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
