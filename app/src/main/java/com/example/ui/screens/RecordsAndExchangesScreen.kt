package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ChildProfile
import com.example.data.model.CustodyExchangeRecord
import com.example.data.model.ExchangeLatenessCalculator
import com.example.data.model.IncidentRecord
import com.example.ui.components.EmptyRecordsIllustration
import com.example.ui.components.SectionHeader
import com.example.ui.components.VerificationBadge
import com.example.ui.theme.DisputedCrimson
import com.example.ui.theme.TealSecondary
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordsAndExchangesScreen(
    children: List<ChildProfile>,
    exchanges: List<CustodyExchangeRecord>,
    incidents: List<IncidentRecord>,
    initialTab: Int = 0,
    openLogExchangeInitially: Boolean = false,
    openAddIncidentInitially: Boolean = false,
    onLogExchange: (
        childName: String,
        scheduledDateIso: String,
        scheduledTime: String,
        actualArrivalTime: String,
        location: String,
        direction: String,
        missedExchange: Boolean,
        notes: String,
        attachmentName: String,
        attachmentUri: String
    ) -> Unit,
    onConfirmOrDisputeExchange: (CustodyExchangeRecord, Boolean, String) -> Unit,
    onAddIncident: (
        childName: String,
        category: String,
        title: String,
        description: String,
        incidentDateIso: String,
        incidentTime: String,
        attachmentType: String,
        attachmentName: String,
        attachmentUri: String,
        originalFileHash: String
    ) -> Unit,
    onUpdateIncident: (
        incident: IncidentRecord,
        newStatus: String,
        disputeResponse: String,
        archive: Boolean
    ) -> Unit
) {
    var activeSubTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var showLogExchangeModal by remember(openLogExchangeInitially) { mutableStateOf(openLogExchangeInitially) }
    var showAddIncidentModal by remember(openAddIncidentInitially) { mutableStateOf(openAddIncidentInitially) }

    val activeIncidents = remember(incidents) { incidents.filter { !it.isArchived } }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (activeSubTab == 0) showLogExchangeModal = true else showAddIncidentModal = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("records_primary_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(
                        text = if (activeSubTab == 0) "Log Exchange" else "Record Incident",
                        fontWeight = FontWeight.SemiBold
                    )
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
                    title = "Custody Exchanges & Evidence Journal",
                    subtitle = "Unconfirmed reports are clearly distinguished from mutually confirmed facts"
                )
            }

            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = activeSubTab == 0,
                        onClick = { activeSubTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("records_tab_exchanges")
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exchanges (${exchanges.size})")
                    }
                    SegmentedButton(
                        selected = activeSubTab == 1,
                        onClick = { activeSubTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("records_tab_incidents")
                    ) {
                        Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Incident Journal (${activeIncidents.size})")
                    }
                }
            }

            if (activeSubTab == 0) {
                if (exchanges.isEmpty()) {
                    item {
                        EmptyRecordsIllustration(
                            title = "No Custody Exchanges Logged",
                            subtitle = "Document pickup and drop-off times, locations, supporting attachments, and automatic lateness calculations.",
                            actionLabel = "Log Custody Exchange",
                            testTag = "empty_log_exchange_button",
                            onAction = { showLogExchangeModal = true }
                        )
                    }
                } else {
                    items(exchanges, key = { it.id }) { ex ->
                        ExchangeRecordCard(
                            exchange = ex,
                            onConfirm = { onConfirmOrDisputeExchange(ex, true, "") },
                            onDispute = { reason -> onConfirmOrDisputeExchange(ex, false, reason) }
                        )
                    }
                }
            } else {
                if (activeIncidents.isEmpty()) {
                    item {
                        EmptyRecordsIllustration(
                            title = "No Incidents Documented",
                            subtitle = "Maintain a chronological, neutral incident journal with photo/PDF attachments, SHA-256 integrity hashes, and versioned audit trails.",
                            actionLabel = "Document Incident",
                            testTag = "empty_add_incident_button",
                            onAction = { showAddIncidentModal = true }
                        )
                    }
                } else {
                    items(activeIncidents, key = { it.id }) { inc ->
                        IncidentRecordCard(
                            incident = inc,
                            onConfirm = {
                                onUpdateIncident(inc, "MUTUALLY_CONFIRMED", inc.disputeResponse, false)
                            },
                            onDispute = { reason ->
                                onUpdateIncident(inc, "DISPUTED", reason, false)
                            },
                            onArchive = {
                                onUpdateIncident(inc, inc.verificationStatus, inc.disputeResponse, true)
                            }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(84.dp)) }
        }
    }

    if (showLogExchangeModal) {
        LogExchangeModal(
            children = children,
            onDismiss = { showLogExchangeModal = false },
            onSave = { child, dateIso, sched, actual, loc, dir, missed, notes, attName, attUri ->
                onLogExchange(child, dateIso, sched, actual, loc, dir, missed, notes, attName, attUri)
                showLogExchangeModal = false
            }
        )
    }

    if (showAddIncidentModal) {
        AddIncidentModal(
            children = children,
            onDismiss = { showAddIncidentModal = false },
            onSave = { child, cat, title, desc, dateIso, time, attType, attName, attUri, hash ->
                onAddIncident(child, cat, title, desc, dateIso, time, attType, attName, attUri, hash)
                showAddIncidentModal = false
            }
        )
    }
}

@Composable
private fun ExchangeRecordCard(
    exchange: CustodyExchangeRecord,
    onConfirm: () -> Unit,
    onDispute: (String) -> Unit
) {
    var showDisputeInput by remember { mutableStateOf(false) }
    var disputeNote by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${exchange.childName} • ${exchange.direction.replace("_", " ")}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Date: ${exchange.scheduledDateIso} • Location: ${exchange.location}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                VerificationBadge(status = exchange.verificationStatus)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("SCHEDULED", style = MaterialTheme.typography.labelSmall)
                        Text(exchange.scheduledTime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("ACTUAL ARRIVAL", style = MaterialTheme.typography.labelSmall)
                        Text(exchange.actualArrivalTime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("AUTO LATENESS", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = when {
                                exchange.exchangeStatus == "MISSED" -> "MISSED"
                                exchange.latenessMinutes > 0L -> "+${exchange.latenessMinutes} min late"
                                else -> "On Time (0m)"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (exchange.latenessMinutes > 0L || exchange.exchangeStatus == "MISSED") DisputedCrimson else TealSecondary
                        )
                    }
                }
            }

            if (exchange.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notes: ${exchange.notes}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (exchange.attachmentName.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Attachment: ${exchange.attachmentName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            if (exchange.disputeReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Dispute Statement: ${exchange.disputeReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DisputedCrimson
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Logged by ${exchange.createdByName} (${exchange.confirmedByUids.size} parent confirmation)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (exchange.verificationStatus != "MUTUALLY_CONFIRMED") {
                        TextButton(onClick = onConfirm) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Confirm")
                        }
                    }
                    TextButton(onClick = { showDisputeInput = !showDisputeInput }) {
                        Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dispute")
                    }
                }
            }

            if (showDisputeInput) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = disputeNote,
                        onValueChange = { disputeNote = it },
                        label = { Text("Neutral explanation of dispute") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (disputeNote.isNotBlank()) {
                                onDispute(disputeNote)
                                showDisputeInput = false
                            }
                        }
                    ) {
                        Text("Submit")
                    }
                }
            }
        }
    }
}

@Composable
private fun IncidentRecordCard(
    incident: IncidentRecord,
    onConfirm: () -> Unit,
    onDispute: (String) -> Unit,
    onArchive: () -> Unit
) {
    var showDisputeBox by remember { mutableStateOf(false) }
    var disputeText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = incident.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${incident.incidentDateIso} at ${incident.incidentTime} • ${incident.category.replace("_", " ")} • Child: ${incident.childName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                VerificationBadge(status = incident.verificationStatus)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = incident.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (incident.attachmentType != "NONE" && incident.attachmentName.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(15.dp))
                        Text(
                            text = "${incident.attachmentType}: ${incident.attachmentName} (Hash: ${incident.originalFileHash.take(16)}…)",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            if (incident.disputeResponse.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Co-Parent Response: ${incident.disputeResponse}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DisputedCrimson
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(15.dp))
                    Text(
                        text = "Audit Trail (v${incident.version}): ${incident.auditHistorySummary}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (incident.verificationStatus != "MUTUALLY_CONFIRMED") {
                    TextButton(onClick = onConfirm) {
                        Text("Confirm Accuracy")
                    }
                }
                TextButton(onClick = { showDisputeBox = !showDisputeBox }) {
                    Text("Add Response / Dispute")
                }
                TextButton(onClick = onArchive) {
                    Text("Archive (Preserve Log)")
                }
            }

            if (showDisputeBox) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = disputeText,
                        onValueChange = { disputeText = it },
                        label = { Text("Neutral co-parent response") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (disputeText.isNotBlank()) {
                                onDispute(disputeText)
                                showDisputeBox = false
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun LogExchangeModal(
    children: List<ChildProfile>,
    onDismiss: () -> Unit,
    onSave: (
        childName: String,
        scheduledDateIso: String,
        scheduledTime: String,
        actualArrivalTime: String,
        location: String,
        direction: String,
        missedExchange: Boolean,
        notes: String,
        attachmentName: String,
        attachmentUri: String
    ) -> Unit
) {
    val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var childName by remember { mutableStateOf(children.firstOrNull()?.name ?: "Child") }
    var scheduledDateIso by remember { mutableStateOf(todayIso) }
    var scheduledTime by remember { mutableStateOf("17:00") }
    var actualArrivalTime by remember { mutableStateOf("17:00") }
    var location by remember { mutableStateOf("School Main Entrance") }
    var direction by remember { mutableStateOf("PICK_UP") }
    var missedExchange by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var attachmentName by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf("") }

    // Zero-permission Android Photo Picker
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            attachmentUri = uri.toString()
            attachmentName = "exchange_photo_${System.currentTimeMillis() % 10000}.jpg"
        }
    }

    val computedLateness = remember(scheduledTime, actualArrivalTime, missedExchange) {
        if (missedExchange) 0L else ExchangeLatenessCalculator.calculateLatenessMinutes(scheduledTime, actualArrivalTime)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Custody Exchange") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = childName,
                    onValueChange = { childName = it },
                    label = { Text("Child Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = direction == "PICK_UP",
                        onClick = { direction = "PICK_UP" },
                        label = { Text("Pickup") }
                    )
                    FilterChip(
                        selected = direction == "DROP_OFF",
                        onClick = { direction = "DROP_OFF" },
                        label = { Text("Drop-off") }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = scheduledDateIso,
                        onValueChange = { scheduledDateIso = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = scheduledTime,
                        onValueChange = { scheduledTime = it },
                        label = { Text("Sched (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = actualArrivalTime,
                        onValueChange = { actualArrivalTime = it },
                        label = { Text("Actual (HH:mm)") },
                        enabled = !missedExchange,
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (missedExchange) {
                            "Status: Missed Exchange (User-Reported until confirmed)"
                        } else if (computedLateness > 0L) {
                            "Auto-Calculated Lateness: $computedLateness minutes late"
                        } else {
                            "Auto-Calculated Lateness: 0 minutes (On time)"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Pickup / Drop-off Location") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exchange_location_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mark as Missed Exchange", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = missedExchange, onCheckedChange = { missedExchange = it })
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Neutral Exchange Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (attachmentName.isBlank()) "Attach Supporting Photo" else "Attached: $attachmentName")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        childName,
                        scheduledDateIso,
                        scheduledTime,
                        actualArrivalTime,
                        location,
                        direction,
                        missedExchange,
                        notes,
                        attachmentName,
                        attachmentUri
                    )
                },
                modifier = Modifier.testTag("save_exchange_log_button")
            ) {
                Text("Save Exchange Log")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddIncidentModal(
    children: List<ChildProfile>,
    onDismiss: () -> Unit,
    onSave: (
        childName: String,
        category: String,
        title: String,
        description: String,
        incidentDateIso: String,
        incidentTime: String,
        attachmentType: String,
        attachmentName: String,
        attachmentUri: String,
        originalFileHash: String
    ) -> Unit
) {
    val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val nowTime = remember { SimpleDateFormat("HH:mm", Locale.US).format(Date()) }
    var childName by remember { mutableStateOf(children.firstOrNull()?.name ?: "Child") }
    var category by remember { mutableStateOf("MISSED_EXCHANGE") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var incidentDateIso by remember { mutableStateOf(todayIso) }
    var incidentTime by remember { mutableStateOf(nowTime) }
    var attachmentType by remember { mutableStateOf("NONE") }
    var attachmentName by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            attachmentType = "PHOTO"
            attachmentUri = uri.toString()
            attachmentName = "evidence_photo_${System.currentTimeMillis() % 10000}.jpg"
        }
    }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            attachmentType = "PDF_DOCUMENT"
            attachmentUri = uri.toString()
            attachmentName = "evidence_doc_${System.currentTimeMillis() % 10000}.pdf"
        }
    }

    val categories = listOf(
        "MISSED_EXCHANGE" to "Missed Exchange",
        "LATE_PICKUP" to "Late Pickup",
        "MISSED_PARENTING_TIME" to "Missed Time",
        "CANCELED_VISIT" to "Canceled Visit",
        "COMMUNICATION_DISAGREEMENT" to "Communication",
        "EXPENSE_DISPUTE" to "Expense Dispute",
        "OTHER" to "Other"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Document Incident Record") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Use calm, factual, objective language. Original timestamps and file hashes are preserved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { (code, label) ->
                        FilterChip(
                            selected = category == code,
                            onClick = { category = code },
                            label = { Text(label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Factual Summary Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("incident_title_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Objective Chronological Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("incident_description_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = incidentDateIso,
                        onValueChange = { incidentDateIso = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = incidentTime,
                        onValueChange = { incidentTime = it },
                        label = { Text("Time (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Attach Photo")
                    }
                    OutlinedButton(
                        onClick = {
                            pdfPicker.launch(arrayOf("application/pdf", "image/*"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Attach PDF")
                    }
                }
                if (attachmentName.isNotBlank()) {
                    Text(
                        text = "Preserved File: $attachmentName",
                        style = MaterialTheme.typography.labelMedium,
                        color = TealSecondary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && description.isNotBlank()) {
                        val rawHashInput = "$title|$incidentDateIso|$incidentTime|$attachmentName|$attachmentUri"
                        val sha256 = MessageDigest.getInstance("SHA-256")
                            .digest(rawHashInput.toByteArray())
                            .joinToString("") { "%02x".format(it) }
                            .take(32)
                        onSave(
                            childName,
                            category,
                            title,
                            description,
                            incidentDateIso,
                            incidentTime,
                            attachmentType,
                            attachmentName,
                            attachmentUri,
                            "sha256_$sha256"
                        )
                    }
                },
                enabled = title.isNotBlank() && description.isNotBlank(),
                modifier = Modifier.testTag("save_incident_button")
            ) {
                Text("Save Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
