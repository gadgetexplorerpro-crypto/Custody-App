package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.billing.BillingUiState
import com.example.data.billing.PlayBillingManager
import com.example.data.model.ChildProfile
import com.example.data.model.CoParentInvitation
import com.example.data.model.CoParentMessage
import com.example.data.model.CustodyExchangeRecord
import com.example.data.model.CustodyPattern
import com.example.data.model.CustodyScheduleEvent
import com.example.data.model.IncidentRecord
import com.example.data.model.SharedExpense
import com.example.data.model.SubscriptionTier
import com.example.data.model.UserProfile
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.PrivacyTip
import com.example.ui.components.CustodyLegalLinks
import com.example.ui.components.LegalAndDataSafetyDialog
import com.example.ui.components.LegalDocumentTab
import com.example.ui.components.SectionHeader
import com.example.ui.components.VerificationBadge
import com.example.ui.theme.DisputedCrimson
import com.example.ui.theme.TealSecondary
import com.example.util.ReportExportManager

@Composable
fun ReportsAndDocumentationScreen(
    userProfile: UserProfile?,
    scheduleEvents: List<CustodyScheduleEvent>,
    exchanges: List<CustodyExchangeRecord>,
    incidents: List<IncidentRecord>,
    expenses: List<SharedExpense>,
    messages: List<CoParentMessage>,
    onBack: () -> Unit,
    onShowBanner: (String) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var startDateIso by remember { mutableStateOf("2026-01-01") }
    var endDateIso by remember { mutableStateOf("2026-12-31") }

    val missedExchangeCount = exchanges.count { it.exchangeStatus == "MISSED" }
    val lateExchangeCount = exchanges.count { it.exchangeStatus == "LATE" }
    val confirmedExchangeCount = exchanges.count { it.verificationStatus == "MUTUALLY_CONFIRMED" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("reports_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Reports & Legal Documentation",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Custom Date Range & Export Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Every exported report clearly identifies the source and verification status (Scheduled, User-Reported, or Mutually Confirmed) of each record for mediation or legal review.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = startDateIso,
                            onValueChange = { startDateIso = it },
                            label = { Text("Start Date (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endDateIso,
                            onValueChange = { endDateIso = it },
                            label = { Text("End Date (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val exported = ReportExportManager.generateComprehensivePdfReport(
                                    context = context,
                                    familyId = userProfile?.familyId ?: "fam_default",
                                    parentName = userProfile?.displayName ?: "Parent",
                                    coParentName = userProfile?.coParentName ?: "",
                                    startDateIso = startDateIso,
                                    endDateIso = endDateIso,
                                    scheduleEvents = scheduleEvents,
                                    exchanges = exchanges,
                                    incidents = incidents,
                                    expenses = expenses,
                                    messages = messages
                                )
                                onShowBanner(exported.summaryText)
                                ReportExportManager.shareExportedFile(context, exported)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_pdf_report_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export PDF")
                        }
                        OutlinedButton(
                            onClick = {
                                val exported = ReportExportManager.generateCsvReport(
                                    context = context,
                                    startDateIso = startDateIso,
                                    endDateIso = endDateIso,
                                    exchanges = exchanges,
                                    incidents = incidents,
                                    expenses = expenses
                                )
                                onShowBanner(exported.summaryText)
                                ReportExportManager.shareExportedFile(context, exported)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_csv_report_button")
                        ) {
                            Icon(Icons.Default.TableView, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV")
                        }
                    }
                }
            }
        }

        // Report Breakdown Preview Cards
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Included Documentation Summaries",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("• Custody Exchange History: ${exchanges.size} total ($confirmedExchangeCount mutually confirmed)")
                    Text("• Missed & Late Exchange Summary: $missedExchangeCount missed, $lateExchangeCount late arrivals")
                    Text("• Parenting Time Calculations: ${scheduleEvents.size} scheduled custody & holiday blocks")
                    Text("• Shared Expense Statements: ${expenses.size} itemized receipts & split calculations")
                    Text("• Communication Records: ${messages.size} timestamped messages with edit/retraction logs")
                    Text("• Incident Timelines: ${incidents.count { !it.isArchived }} neutral incident entries with SHA-256 hashes")
                }
            }
        }
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
fun PremiumSubscriptionScreen(
    currentTier: SubscriptionTier,
    billingState: BillingUiState,
    billingManager: PlayBillingManager,
    onBack: () -> Unit,
    onShowBanner: (String) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("subscription_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "CustodyLog Subscriptions",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = TealSecondary)
                        Text(
                            text = "Current Plan: ${currentTier.label} (${currentTier.priceLabel})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = billingState.statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Plan 1: Free Plan
        item {
            PlanComparisonCard(
                tier = SubscriptionTier.FREE,
                isCurrent = currentTier == SubscriptionTier.FREE,
                features = listOf(
                    "One child profile",
                    "Basic custody calendar",
                    "Limited incident records (up to 5)",
                    "Basic expense tracking"
                ),
                buttonLabel = "Included Free Tier",
                enabled = false,
                onSelect = {}
            )
        }

        // Plan 2: Premium ($12.99/month)
        item {
            PlanComparisonCard(
                tier = SubscriptionTier.PREMIUM,
                isCurrent = currentTier == SubscriptionTier.PREMIUM,
                features = listOf(
                    "Unlimited custody & exchange records",
                    "Advanced custody schedules (2-2-3, 2-2-5-5, Alternating Weekends)",
                    "Full chronological incident journal & evidence attachments",
                    "Professional PDF & CSV mediation/legal reports",
                    "Advanced expense management & custom splits",
                    "Searchable co-parent messaging history & smart reminders"
                ),
                buttonLabel = if (currentTier == SubscriptionTier.PREMIUM) "Active Plan" else "Subscribe via Google Play ($12.99/mo)",
                enabled = currentTier != SubscriptionTier.PREMIUM,
                onSelect = {
                    val activity = context as? Activity
                    if (activity != null) {
                        val err = billingManager.launchSubscriptionFlow(activity, SubscriptionTier.PREMIUM)
                        if (err != null) onShowBanner(err)
                    }
                }
            )
        }

        // Plan 3: Family Premium ($19.99/month)
        item {
            PlanComparisonCard(
                tier = SubscriptionTier.FAMILY_PREMIUM,
                isCurrent = currentTier == SubscriptionTier.FAMILY_PREMIUM,
                features = listOf(
                    "All Premium features included",
                    "Multiple child profiles with independent ratios",
                    "Shared co-parent workspace synchronization",
                    "Advanced multi-child legal & financial reporting",
                    "Expanded secure evidence metadata & retention controls"
                ),
                buttonLabel = if (currentTier == SubscriptionTier.FAMILY_PREMIUM) "Active Plan" else "Subscribe via Google Play ($19.99/mo)",
                enabled = currentTier != SubscriptionTier.FAMILY_PREMIUM,
                onSelect = {
                    val activity = context as? Activity
                    if (activity != null) {
                        val err = billingManager.launchSubscriptionFlow(activity, SubscriptionTier.FAMILY_PREMIUM)
                        if (err != null) onShowBanner(err)
                    }
                }
            )
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun PlanComparisonCard(
    tier: SubscriptionTier,
    isCurrent: Boolean,
    features: List<String>,
    buttonLabel: String,
    enabled: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isCurrent) 2.dp else 1.dp,
            color = if (isCurrent) TealSecondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
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
                        text = tier.label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tier.priceLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = TealSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (isCurrent) {
                    VerificationBadge(status = "MUTUALLY_CONFIRMED")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            features.forEach { feat ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = TealSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(text = feat, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onSelect,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("subscribe_plan_${tier.name.lowercase()}")
            ) {
                Text(buttonLabel)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkspaceAndPrivacySettingsScreen(
    userProfile: UserProfile?,
    children: List<ChildProfile>,
    invitations: List<CoParentInvitation>,
    onBack: () -> Unit,
    onAddChild: (
        name: String,
        dob: String,
        school: String,
        medical: String,
        scheduleType: CustodyPattern,
        ratio: Long
    ) -> Unit,
    onGenerateInviteCode: ((String) -> Unit) -> Unit,
    onAcceptInviteCode: (String) -> Unit,
    onSaveSettings: (
        displayName: String,
        custodyPattern: CustodyPattern,
        splitPercent: Long,
        retentionDays: Long,
        notificationsEnabled: Boolean
    ) -> Unit,
    onExportAllData: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    BackHandler { onBack() }
    val clipboard = LocalClipboardManager.current

    var displayName by remember(userProfile) { mutableStateOf(userProfile?.displayName ?: "Parent") }
    var selectedPattern by remember(userProfile) {
        mutableStateOf(
            CustodyPattern.entries.firstOrNull { it.name == userProfile?.custodyPattern }
                ?: CustodyPattern.TWO_TWO_THREE
        )
    }
    var splitPercentText by remember(userProfile) {
        mutableStateOf((userProfile?.defaultExpenseSplitPercent ?: 50L).toString())
    }
    var retentionDaysText by remember(userProfile) {
        mutableStateOf((userProfile?.privacyRetentionDays ?: 1825L).toString())
    }
    var notificationsEnabled by remember(userProfile) {
        mutableStateOf(userProfile?.notificationsEnabled ?: true)
    }

    // Child form
    var childName by remember { mutableStateOf("") }
    var childDob by remember { mutableStateOf("2018-05-15") }
    var childSchool by remember { mutableStateOf("") }
    var childMedical by remember { mutableStateOf("") }

    // Co-parent invite code
    var inviteCodeInput by remember { mutableStateOf("") }
    var latestGeneratedCode by remember { mutableStateOf(invitations.firstOrNull()?.inviteCode ?: "") }
    var activeLegalDialogTab by remember { mutableStateOf<LegalDocumentTab?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Family, Co-Parent & Privacy Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Co-Parent Invitation & Connection Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = "Secure Co-Parent Connection",
                        subtitle = "Each parent keeps a private account with controlled access to shared workspace data"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onGenerateInviteCode { code -> latestGeneratedCode = code }
                            },
                            modifier = Modifier.testTag("generate_invite_token_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate Invite Token")
                        }
                        if (latestGeneratedCode.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(latestGeneratedCode))
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(latestGeneratedCode)
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inviteCodeInput,
                            onValueChange = { inviteCodeInput = it },
                            label = { Text("Enter Co-Parent Invite Token (e.g. CL-XXXX)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("accept_invite_token_input")
                        )
                        Button(
                            onClick = {
                                if (inviteCodeInput.isNotBlank()) {
                                    onAcceptInviteCode(inviteCodeInput)
                                    inviteCodeInput = ""
                                }
                            },
                            enabled = inviteCodeInput.isNotBlank(),
                            modifier = Modifier.testTag("accept_invite_token_button")
                        ) {
                            Text("Connect")
                        }
                    }
                }
            }
        }

        // Child Profiles Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = "Child Profiles (${children.size})",
                        subtitle = "Manage custody arrangements, school info & medical notes"
                    )
                    children.forEach { child ->
                        Text(
                            text = "• ${child.name} (DOB: ${child.dateOfBirth}) — ${child.schoolOrChildcare.ifBlank { "School not set" }}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    OutlinedTextField(
                        value = childName,
                        onValueChange = { childName = it },
                        label = { Text("Child Full Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("child_name_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = childDob,
                            onValueChange = { childDob = it },
                            label = { Text("DOB (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = childSchool,
                            onValueChange = { childSchool = it },
                            label = { Text("School / Childcare") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = childMedical,
                        onValueChange = { childMedical = it },
                        label = { Text("Medical Notes / Pediatrician") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (childName.isNotBlank()) {
                                onAddChild(childName, childDob, childSchool, childMedical, selectedPattern, 50L)
                                childName = ""
                                childSchool = ""
                                childMedical = ""
                            }
                        },
                        enabled = childName.isNotBlank(),
                        modifier = Modifier.testTag("add_child_profile_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Child Profile")
                    }
                }
            }
        }

        // Account, Custody Defaults & Privacy Retention Controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = "Privacy, Retention & Custody Defaults",
                        subtitle = "Private account settings are isolated to your UID via zero-trust Firestore rules"
                    )
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Your Legal / Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Default Parenting Schedule", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CustodyPattern.entries.forEach { pat ->
                            FilterChip(
                                selected = selectedPattern == pat,
                                onClick = { selectedPattern = pat },
                                label = { Text(pat.label) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = splitPercentText,
                            onValueChange = { splitPercentText = it },
                            label = { Text("Default Expense Split (%)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = retentionDaysText,
                            onValueChange = { retentionDaysText = it },
                            label = { Text("Retention Window (Days)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Exchange & Message Notifications", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
                    }
                    Button(
                        onClick = {
                            onSaveSettings(
                                displayName,
                                selectedPattern,
                                splitPercentText.toLongOrNull() ?: 50L,
                                retentionDaysText.toLongOrNull() ?: 1825L,
                                notificationsEnabled
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_privacy_settings_button")
                    ) {
                        Text("Save Account & Privacy Settings")
                    }
                }
            }
        }

        // Legal, Privacy Policy, Terms of Service & Account/Data Deletion
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Legal, Privacy Policy, Terms & Account Deletion",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Review our US/UK privacy policy, terms of service, or permanently delete your account and personal data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { activeLegalDialogTab = LegalDocumentTab.PRIVACY_POLICY },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_privacy_policy_button")
                        ) {
                            Icon(Icons.Default.PrivacyTip, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Privacy Policy")
                        }
                        OutlinedButton(
                            onClick = { activeLegalDialogTab = LegalDocumentTab.TERMS_OF_SERVICE },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_terms_of_service_button")
                        ) {
                            Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Terms of Service")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onExportAllData,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Data")
                        }
                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sign Out")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(CustodyLegalLinks.DELETE_ACCOUNT_WEB_URL))
                            activeLegalDialogTab = LegalDocumentTab.DELETE_ACCOUNT_AND_DATA
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_delete_account_data_link_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Account & Data Deletion Link (${CustodyLegalLinks.DELETE_ACCOUNT_WEB_URL})")
                    }

                    Button(
                        onClick = { activeLegalDialogTab = LegalDocumentTab.DELETE_ACCOUNT_AND_DATA },
                        colors = ButtonDefaults.buttonColors(containerColor = DisputedCrimson),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_account_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Account & Personal Data")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }

    val currentLegalTab = activeLegalDialogTab
    if (currentLegalTab != null) {
        LegalAndDataSafetyDialog(
            initialTab = currentLegalTab,
            isAuthenticated = true,
            userEmail = userProfile?.email,
            onDismiss = { activeLegalDialogTab = null },
            onConfirmDeleteAccountAndData = onDeleteAccount
        )
    }
}
