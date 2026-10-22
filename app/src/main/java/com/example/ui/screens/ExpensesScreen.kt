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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ChildProfile
import com.example.data.model.SharedExpense
import com.example.ui.components.EmptyRecordsIllustration
import com.example.ui.components.SectionHeader
import com.example.ui.components.VerificationBadge
import com.example.ui.theme.TealSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun ExpensesScreen(
    children: List<ChildProfile>,
    expenses: List<SharedExpense>,
    defaultSplitPercent: Long,
    openAddExpenseInitially: Boolean = false,
    onAddExpense: (
        childName: String,
        category: String,
        title: String,
        totalAmountCents: Long,
        currency: String,
        creatorSharePercent: Long,
        dateIso: String,
        receiptName: String,
        receiptUri: String,
        statusNote: String
    ) -> Unit,
    onUpdateExpenseStatus: (SharedExpense, String, String) -> Unit,
    onOpenExportReports: () -> Unit
) {
    var showAddModal by remember(openAddExpenseInitially) { mutableStateOf(openAddExpenseInitially) }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }

    val filteredExpenses = remember(expenses, selectedCategoryFilter) {
        if (selectedCategoryFilter == "ALL") expenses
        else expenses.filter { it.category == selectedCategoryFilter }
    }

    val totalSpentCents = remember(expenses) { expenses.sumOf { it.totalAmountCents } }
    val unpaidCoParentCents = remember(expenses) {
        expenses.filter { it.paymentStatus == "UNPAID_REQUESTED" }.sumOf { it.coParentShareCents }
    }
    val paidCoParentCents = remember(expenses) {
        expenses.filter { it.paymentStatus == "PAID" }.sumOf { it.coParentShareCents }
    }
    val activeCurrencySymbol = if (expenses.firstOrNull()?.currency == "GBP") "£" else "$"

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddModal = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("expenses_add_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Add Shared Expense", fontWeight = FontWeight.SemiBold)
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
                    title = "Shared Expense Tracker",
                    subtitle = "Receipts, custom split percentages & reimbursement history (USD / GBP)",
                    actionText = "PDF / CSV Report",
                    onAction = onOpenExportReports
                )
            }

            // Monthly Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "OUTSTANDING REIMBURSEMENT OWED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = String.format(Locale.US, "%s%.2f", activeCurrencySymbol, unpaidCoParentCents / 100.0),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedButton(
                                onClick = onOpenExportReports,
                                modifier = Modifier.testTag("expenses_export_statement_button")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export")
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Logged: ${String.format(Locale.US, "%s%.2f", activeCurrencySymbol, totalSpentCents / 100.0)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Settled / Paid: ${String.format(Locale.US, "%s%.2f", activeCurrencySymbol, paidCoParentCents / 100.0)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TealSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Category Filter Chips
            item {
                val filterOptions = listOf(
                    "ALL" to "All",
                    "SCHOOL_FEES" to "School Fees",
                    "MEDICAL_BILLS" to "Medical",
                    "CHILDCARE" to "Childcare",
                    "CLOTHING" to "Clothing",
                    "FOOD" to "Food",
                    "ACTIVITIES" to "Activities",
                    "TRANSPORTATION" to "Transport",
                    "OTHER" to "Other"
                )
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filterOptions.forEach { (code, label) ->
                        FilterChip(
                            selected = selectedCategoryFilter == code,
                            onClick = { selectedCategoryFilter = code },
                            label = { Text(label) }
                        )
                    }
                }
            }

            if (filteredExpenses.isEmpty()) {
                item {
                    EmptyRecordsIllustration(
                        title = "No Shared Expenses Logged",
                        subtitle = "Log school fees, medical bills, childcare, activities, or clothing with custom split ratios and receipt attachments.",
                        actionLabel = "Add First Expense",
                        testTag = "empty_add_expense_button",
                        onAction = { showAddModal = true }
                    )
                }
            } else {
                items(filteredExpenses, key = { it.id }) { exp ->
                    ExpenseItemCard(
                        expense = exp,
                        onMarkPaid = {
                            onUpdateExpenseStatus(exp, "PAID", "Marked paid in full")
                        },
                        onMarkDisputed = { reason ->
                            onUpdateExpenseStatus(exp, "DISPUTED", reason)
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(84.dp)) }
        }
    }

    if (showAddModal) {
        AddExpenseModal(
            children = children,
            defaultSplitPercent = defaultSplitPercent,
            onDismiss = { showAddModal = false },
            onSave = { child, cat, title, cents, curr, split, dateIso, recName, recUri, note ->
                onAddExpense(child, cat, title, cents, curr, split, dateIso, recName, recUri, note)
                showAddModal = false
            }
        )
    }
}

@Composable
private fun ExpenseItemCard(
    expense: SharedExpense,
    onMarkPaid: () -> Unit,
    onMarkDisputed: (String) -> Unit
) {
    val sym = if (expense.currency == "GBP") "£" else "$"
    var showDisputeField by remember { mutableStateOf(false) }
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
                        text = expense.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${expense.dateIso} • ${expense.category.replace("_", " ")} • Child: ${expense.childName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                VerificationBadge(status = expense.paymentStatus)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("TOTAL RECEIPT", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = String.format(Locale.US, "%s%.2f", sym, expense.totalAmountCents / 100.0),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text("SPLIT RATIO", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${expense.creatorSharePercent}% / ${100 - expense.creatorSharePercent}%",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("CO-PARENT SHARE", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = String.format(Locale.US, "%s%.2f", sym, expense.coParentShareCents / 100.0),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TealSecondary
                        )
                    }
                }
            }

            if (expense.receiptName.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp), tint = TealSecondary)
                    Text(
                        text = "Receipt attached: ${expense.receiptName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TealSecondary
                    )
                }
            }

            if (expense.statusNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Note: ${expense.statusNote}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Logged by ${expense.createdByName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (expense.paymentStatus != "PAID") {
                        TextButton(onClick = onMarkPaid) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark Paid")
                        }
                    }
                    TextButton(onClick = { showDisputeField = !showDisputeField }) {
                        Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dispute")
                    }
                }
            }

            if (showDisputeField) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = disputeNote,
                        onValueChange = { disputeNote = it },
                        label = { Text("Reason for expense dispute") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (disputeNote.isNotBlank()) {
                                onMarkDisputed(disputeNote)
                                showDisputeField = false
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddExpenseModal(
    children: List<ChildProfile>,
    defaultSplitPercent: Long,
    onDismiss: () -> Unit,
    onSave: (
        childName: String,
        category: String,
        title: String,
        totalAmountCents: Long,
        currency: String,
        creatorSharePercent: Long,
        dateIso: String,
        receiptName: String,
        receiptUri: String,
        statusNote: String
    ) -> Unit
) {
    val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var childName by remember { mutableStateOf(children.firstOrNull()?.name ?: "Child") }
    var category by remember { mutableStateOf("SCHOOL_FEES") }
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }
    var splitSlider by remember { mutableFloatStateOf(defaultSplitPercent.toFloat()) }
    var dateIso by remember { mutableStateOf(todayIso) }
    var receiptName by remember { mutableStateOf("") }
    var receiptUri by remember { mutableStateOf("") }
    var statusNote by remember { mutableStateOf("") }

    val receiptPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            receiptUri = uri.toString()
            receiptName = "receipt_${System.currentTimeMillis() % 10000}.jpg"
        }
    }

    val categories = listOf(
        "SCHOOL_FEES" to "School Fees",
        "MEDICAL_BILLS" to "Medical Bills",
        "CHILDCARE" to "Childcare",
        "CLOTHING" to "Clothing",
        "FOOD" to "Food",
        "ACTIVITIES" to "Activities",
        "TRANSPORTATION" to "Transportation",
        "OTHER" to "Other"
    )

    val parsedCents = remember(amountText) {
        val d = amountText.toDoubleOrNull() ?: 0.0
        (d * 100.0).roundToLong()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Shared Child Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    label = { Text("Merchant / Expense Description") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_title_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Total Amount (e.g. 120.50)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("expense_amount_input")
                    )
                    FilterChip(
                        selected = currency == "USD",
                        onClick = { currency = "USD" },
                        label = { Text("USD ($)") }
                    )
                    FilterChip(
                        selected = currency == "GBP",
                        onClick = { currency = "GBP" },
                        label = { Text("GBP (£)") }
                    )
                }
                Text(
                    text = "Your Share: ${splitSlider.toInt()}% • Co-Parent Reimbursement: ${100 - splitSlider.toInt()}%",
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = splitSlider,
                    onValueChange = { splitSlider = it },
                    valueRange = 0f..100f,
                    steps = 9
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = childName,
                        onValueChange = { childName = it },
                        label = { Text("Child") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dateIso,
                        onValueChange = { dateIso = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = statusNote,
                    onValueChange = { statusNote = it },
                    label = { Text("Payment Request Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = {
                        receiptPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (receiptName.isBlank()) "Upload Receipt Photo" else "Attached: $receiptName")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && parsedCents > 0L) {
                        onSave(
                            childName,
                            category,
                            title,
                            parsedCents,
                            currency,
                            splitSlider.toLong(),
                            dateIso,
                            receiptName,
                            receiptUri,
                            statusNote
                        )
                    }
                },
                enabled = title.isNotBlank() && parsedCents > 0L,
                modifier = Modifier.testTag("save_expense_button")
            ) {
                Text("Save & Request Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
