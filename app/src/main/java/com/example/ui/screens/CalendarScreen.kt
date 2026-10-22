package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.ChildProfile
import com.example.data.model.CustodyPattern
import com.example.data.model.CustodyScheduleEvent
import com.example.ui.components.EmptyRecordsIllustration
import com.example.ui.components.SectionHeader
import com.example.ui.components.VerificationBadge
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TealSecondary
import com.example.util.NotificationHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class CalendarViewMode(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(
    children: List<ChildProfile>,
    scheduleEvents: List<CustodyScheduleEvent>,
    onAddEvent: (
        childName: String,
        title: String,
        eventType: String,
        assignedParent: String,
        dateIso: String,
        startTime: String,
        endTime: String,
        location: String,
        notes: String,
        isChangeRequest: Boolean,
        reminderMinutes: Long
    ) -> Unit,
    onGeneratePattern14Days: (CustodyPattern, String) -> Unit,
    onRespondToChangeRequest: (CustodyScheduleEvent, Boolean) -> Unit
) {
    val context = LocalContext.current
    val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var selectedDateIso by remember { mutableStateOf(todayIso) }
    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTHLY) }
    var selectedPattern by remember { mutableStateOf(CustodyPattern.TWO_TWO_THREE) }
    var showAddEventDialog by remember { mutableStateOf(false) }

    val next14Dates = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        List(28) {
            val d = sdf.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            d
        }
    }

    val displayedEvents = remember(scheduleEvents, selectedDateIso, viewMode) {
        when (viewMode) {
            CalendarViewMode.DAILY -> scheduleEvents.filter { it.dateIso == selectedDateIso }
            CalendarViewMode.WEEKLY -> {
                val weekDates = next14Dates.take(7).toSet()
                scheduleEvents.filter { it.dateIso in weekDates || it.dateIso == selectedDateIso }
            }
            CalendarViewMode.MONTHLY -> scheduleEvents
        }
    }

    val changeHistoryEvents = remember(scheduleEvents) {
        scheduleEvents.filter {
            it.eventType == "CHANGE_REQUEST" || it.verificationStatus in listOf("PROPOSED_CHANGE", "MUTUALLY_CONFIRMED", "DECLINED")
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddEventDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("calendar_add_event_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Calendar Event")
                    Text("Add Event / Request", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                SectionHeader(
                    title = "Smart Custody Calendar",
                    subtitle = "2-2-3, 2-2-5-5, Alternating Weekends, Holidays & Change History"
                )
            }

            // Daily / Weekly / Monthly view switcher
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    CalendarViewMode.entries.forEachIndexed { idx, mode ->
                        SegmentedButton(
                            selected = viewMode == mode,
                            onClick = { viewMode = mode },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = CalendarViewMode.entries.size),
                            modifier = Modifier.testTag("calendar_mode_${mode.name.lowercase()}")
                        ) {
                            Text(mode.label)
                        }
                    }
                }
            }

            // Interactive Month/Week Grid
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Selected Date: $selectedDateIso",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                LegendDot(color = NavyPrimary, label = "Primary")
                                LegendDot(color = TealSecondary, label = "Co-Parent")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        val datesToShow = when (viewMode) {
                            CalendarViewMode.DAILY -> listOf(selectedDateIso)
                            CalendarViewMode.WEEKLY -> next14Dates.take(7)
                            CalendarViewMode.MONTHLY -> next14Dates.take(21)
                        }

                        val chunkedDates = datesToShow.chunked(7)
                        chunkedDates.forEach { rowDates ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowDates.forEach { dateStr ->
                                    val dayEvents = scheduleEvents.filter { it.dateIso == dateStr }
                                    val isSelected = dateStr == selectedDateIso
                                    val assignedParent = dayEvents.firstOrNull()?.assignedParent
                                    val dayNumber = dateStr.takeLast(2)

                                    val cellColor = when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        assignedParent == "PRIMARY" -> NavyPrimary.copy(alpha = 0.14f)
                                        assignedParent == "CO_PARENT" -> TealSecondary.copy(alpha = 0.18f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    }
                                    val textColor = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(cellColor)
                                            .clickable { selectedDateIso = dateStr },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = dayNumber,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = textColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (dayEvents.isNotEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                            else TealSecondary
                                                        )
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

            // Parenting Schedule Template Generator Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = TealSecondary
                            )
                            Text(
                                text = "Apply Parenting Schedule Template",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Generate 14 days of structured custody blocks using standard US & UK co-parenting templates:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CustodyPattern.entries.forEach { pattern ->
                                FilterChip(
                                    selected = selectedPattern == pattern,
                                    onClick = { selectedPattern = pattern },
                                    label = { Text(pattern.label) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = selectedPattern.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val defaultChild = children.firstOrNull()?.name ?: "All Children"
                                onGeneratePattern14Days(selectedPattern, defaultChild)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("generate_schedule_pattern_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate 14-Day ${selectedPattern.label}")
                        }
                    }
                }
            }

            // Scheduled Events List
            item {
                SectionHeader(
                    title = "${viewMode.label} Schedule (${displayedEvents.size})",
                    subtitle = "Custody blocks, holidays, school events & appointments"
                )
            }

            if (displayedEvents.isEmpty()) {
                item {
                    EmptyRecordsIllustration(
                        title = "No Events for This ${viewMode.label} View",
                        subtitle = "Generate a 2-2-3 or 2-2-5-5 template above, or tap '+ Add Event / Request' to schedule a holiday, school event, or custody change.",
                        actionLabel = "Add Calendar Event",
                        testTag = "calendar_empty_add_event",
                        onAction = { showAddEventDialog = true }
                    )
                }
            } else {
                items(displayedEvents, key = { it.id }) { ev ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ev.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${ev.dateIso} • ${ev.startTime} – ${ev.endTime} • ${ev.childName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                VerificationBadge(status = ev.verificationStatus)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Type: ${ev.eventType.replace("_", " ")} | Responsibility: ${ev.assignedParent.replace("_", " ")}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(
                                    onClick = {
                                        NotificationHelper.sendNotification(
                                            context = context,
                                            title = "Custody Exchange Reminder: ${ev.title}",
                                            message = "Scheduled on ${ev.dateIso} at ${ev.startTime} (${ev.location.ifBlank { "Agreed Location" }})"
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "Send Reminder Notification",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remind")
                                }
                            }

                            if (ev.verificationStatus == "PROPOSED_CHANGE") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onRespondToChangeRequest(ev, true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = TealSecondary)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Confirm Request")
                                    }
                                    OutlinedButton(onClick = { onRespondToChangeRequest(ev, false) }) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Decline")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Automatic History of Agreed Schedule Changes
            if (changeHistoryEvents.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Schedule Change Audit History",
                        subtitle = "Automatic history of requested, confirmed, and declined modifications"
                    )
                }
                items(changeHistoryEvents, key = { "hist_${it.id}" }) { ch ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${ch.title} (${ch.dateIso})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = ch.changeHistorySummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            VerificationBadge(status = ch.verificationStatus)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(84.dp)) }
        }
    }

    if (showAddEventDialog) {
        AddScheduleEventDialog(
            defaultDateIso = selectedDateIso,
            children = children,
            onDismiss = { showAddEventDialog = false },
            onSave = { child, title, type, parent, dateIso, start, end, loc, notes, isRequest, reminder ->
                onAddEvent(child, title, type, parent, dateIso, start, end, loc, notes, isRequest, reminder)
                showAddEventDialog = false
            }
        )
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddScheduleEventDialog(
    defaultDateIso: String,
    children: List<ChildProfile>,
    onDismiss: () -> Unit,
    onSave: (
        childName: String,
        title: String,
        eventType: String,
        assignedParent: String,
        dateIso: String,
        startTime: String,
        endTime: String,
        location: String,
        notes: String,
        isChangeRequest: Boolean,
        reminderMinutes: Long
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var childName by remember { mutableStateOf(children.firstOrNull()?.name ?: "All Children") }
    var eventType by remember { mutableStateOf("CUSTODY_TIME") }
    var assignedParent by remember { mutableStateOf("PRIMARY") }
    var dateIso by remember { mutableStateOf(defaultDateIso) }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("18:00") }
    var location by remember { mutableStateOf("School / Residence") }
    var notes by remember { mutableStateOf("") }
    var isChangeRequest by remember { mutableStateOf(false) }

    val eventTypes = listOf(
        "CUSTODY_TIME" to "Custody Block",
        "HOLIDAY" to "Holiday",
        "VACATION" to "Vacation",
        "SCHOOL_EVENT" to "School Event",
        "MEDICAL_APPOINTMENT" to "Appointment",
        "CHANGE_REQUEST" to "Change Request"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custody Event or Change Request") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event Title (e.g., Weekend Custody, Half-Term Holiday)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calendar_event_title_input")
                )
                OutlinedTextField(
                    value = childName,
                    onValueChange = { childName = it },
                    label = { Text("Child Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Category", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    eventTypes.forEach { (code, label) ->
                        FilterChip(
                            selected = eventType == code,
                            onClick = {
                                eventType = code
                                if (code == "CHANGE_REQUEST") isChangeRequest = true
                            },
                            label = { Text(label) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = assignedParent == "PRIMARY",
                        onClick = { assignedParent = "PRIMARY" },
                        label = { Text("Primary Parent") }
                    )
                    FilterChip(
                        selected = assignedParent == "CO_PARENT",
                        onClick = { assignedParent = "CO_PARENT" },
                        label = { Text("Co-Parent") }
                    )
                    FilterChip(
                        selected = assignedParent == "SHARED",
                        onClick = { assignedParent = "SHARED" },
                        label = { Text("Shared") }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateIso,
                        onValueChange = { dateIso = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start") },
                        singleLine = true,
                        modifier = Modifier.weight(0.85f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End") },
                        singleLine = true,
                        modifier = Modifier.weight(0.85f)
                    )
                }
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Reason for Change") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Require Co-Parent Confirmation",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Switch(
                        checked = isChangeRequest,
                        onCheckedChange = { isChangeRequest = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && dateIso.length >= 8) {
                        onSave(
                            childName,
                            title,
                            eventType,
                            assignedParent,
                            dateIso,
                            startTime,
                            endTime,
                            location,
                            notes,
                            isChangeRequest,
                            60L
                        )
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("save_calendar_event_button")
            ) {
                Text("Save to Calendar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
