package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DisputedCrimson
import com.example.ui.theme.DisputedCrimsonSoft
import com.example.ui.theme.TealSecondary

enum class LegalDocumentTab(val title: String) {
    PRIVACY_POLICY("Privacy Policy"),
    TERMS_OF_SERVICE("Terms of Service"),
    DELETE_ACCOUNT_AND_DATA("Delete Account & Data")
}

object CustodyLegalLinks {
    const val PRIVACY_POLICY_URL = "https://custodylog.app/privacy-policy"
    const val TERMS_OF_SERVICE_URL = "https://custodylog.app/terms-of-service"
    const val DELETE_ACCOUNT_WEB_URL = "https://custodylog.app/account/delete-data"
    const val PRIVACY_SUPPORT_EMAIL = "privacy@custodylog.app"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginPrivacyTermsAndDeletionSection(
    acceptedTerms: Boolean,
    onAcceptedTermsChange: (Boolean) -> Unit,
    onOpenLegalTab: (LegalDocumentTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = acceptedTerms,
                    onCheckedChange = onAcceptedTermsChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = TealSecondary
                    ),
                    modifier = Modifier.testTag("login_accept_privacy_terms_checkbox")
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "I have read and agree to the CustodyLog Privacy Policy and Terms of Service.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Records are user-authored or mutually confirmed; CustodyLog is not a law firm.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TextButton(
                onClick = { onOpenLegalTab(LegalDocumentTab.PRIVACY_POLICY) },
                modifier = Modifier.testTag("login_privacy_policy_link")
            ) {
                Icon(
                    imageVector = Icons.Default.PrivacyTip,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline
                )
            }

            TextButton(
                onClick = { onOpenLegalTab(LegalDocumentTab.TERMS_OF_SERVICE) },
                modifier = Modifier.testTag("login_terms_of_service_link")
            ) {
                Icon(
                    imageVector = Icons.Default.Gavel,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Terms of Service",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline
                )
            }

            TextButton(
                onClick = { onOpenLegalTab(LegalDocumentTab.DELETE_ACCOUNT_AND_DATA) },
                modifier = Modifier.testTag("login_delete_account_data_link")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = DisputedCrimson
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Delete Account & Data",
                    style = MaterialTheme.typography.labelMedium,
                    color = DisputedCrimson,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}

@Composable
fun LegalAndDataSafetyDialog(
    initialTab: LegalDocumentTab,
    isAuthenticated: Boolean,
    userEmail: String?,
    onDismiss: () -> Unit,
    onConfirmDeleteAccountAndData: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    var confirmPermanentDeletionCheckbox by remember { mutableStateOf(false) }
    var copiedNotice by remember { mutableStateOf<String?>(null) }

    fun openExternalUrl(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun openDeletionEmailRequest() {
        runCatching {
            val subject = Uri.encode("CustodyLog Account and Data Deletion Request")
            val body = Uri.encode(
                "Please permanently delete my CustodyLog account and associated personal data.\n" +
                    "Account Email: ${userEmail ?: "[Enter your Google Account Email]"}"
            )
            val mailUri = Uri.parse("mailto:${CustodyLegalLinks.PRIVACY_SUPPORT_EMAIL}?subject=$subject&body=$body")
            val intent = Intent(Intent.ACTION_SENDTO, mailUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (selectedTab) {
                            LegalDocumentTab.PRIVACY_POLICY -> Icons.Default.PrivacyTip
                            LegalDocumentTab.TERMS_OF_SERVICE -> Icons.Default.Gavel
                            LegalDocumentTab.DELETE_ACCOUNT_AND_DATA -> Icons.Default.DeleteForever
                        },
                        contentDescription = null,
                        tint = if (selectedTab == LegalDocumentTab.DELETE_ACCOUNT_AND_DATA) DisputedCrimson else TealSecondary
                    )
                    Text(
                        text = "Legal, Privacy & Data Controls",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    edgePadding = 0.dp
                ) {
                    LegalDocumentTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = {
                                selectedTab = tab
                                copiedNotice = null
                            },
                            text = { Text(tab.title, style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.testTag("legal_dialog_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (copiedNotice != null) {
                    Surface(
                        color = TealSecondary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = copiedNotice ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            color = TealSecondary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                when (selectedTab) {
                    LegalDocumentTab.PRIVACY_POLICY -> {
                        Text(
                            text = "CustodyLog Privacy Policy (US & UK)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Effective Date: October 2026 • Complies with US State Privacy Laws (CCPA/CPRA), UK GDPR, and Google Play User Data Policy.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LegalBulletSection(
                            heading = "1. Information We Collect",
                            body = "• Account Identity: Google Sign-In UID, display name, and email address used exclusively for authentication and workspace security.\n" +
                                "• Co-Parenting Records: Child profile names/DOBs, parenting schedules, timestamped exchange check-ins, incident descriptions, shared expenses, and co-parent chat messages."
                        )
                        LegalBulletSection(
                            heading = "2. Zero-Trust Co-Parent Isolation",
                            body = "• Each parent maintains a separate account. Private account preferences are isolated to your UID.\n" +
                                "• Shared family records are restricted strictly to the 2 parent UIDs linked via an explicit invitation token (CL-XXXXXXXX)."
                        )
                        LegalBulletSection(
                            heading = "3. Evidence Integrity & AI Tone Assistant Privacy",
                            body = "• Uploaded photos and PDFs are hashed on-device with SHA-256 to preserve original file integrity.\n" +
                                "• The optional Calm Tone Assistant only processes draft message text when you explicitly tap 'Rewrite in Calm, Child-Focused Tone' and never automatically sends messages or sells personal data."
                        )
                        LegalBulletSection(
                            heading = "4. Data Retention & Deletion Rights",
                            body = "• You may export your records to PDF or CSV at any time and permanently delete your account and personal data in-app or via our web/email deletion portal."
                        )
                        HorizontalDivider()
                        ExternalLinkRow(
                            label = "Official Privacy Policy Link",
                            url = CustodyLegalLinks.PRIVACY_POLICY_URL,
                            onOpen = { openExternalUrl(CustodyLegalLinks.PRIVACY_POLICY_URL) },
                            onCopy = {
                                clipboard.setText(AnnotatedString(CustodyLegalLinks.PRIVACY_POLICY_URL))
                                copiedNotice = "Copied Privacy Policy URL to clipboard."
                            }
                        )
                    }

                    LegalDocumentTab.TERMS_OF_SERVICE -> {
                        Text(
                            text = "CustodyLog Terms of Service",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Governing Co-Parenting Documentation, Evidentiary Neutrality & Subscriptions",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LegalBulletSection(
                            heading = "1. Non-Legal Advice Disclaimer",
                            body = "• CustodyLog is a personal recordkeeping, scheduling, and communication platform. CustodyLog is NOT a law firm, court agency, or legal representative, and does not provide legal advice."
                        )
                        LegalBulletSection(
                            heading = "2. Neutral Recordkeeping & Verification Status",
                            body = "• Every record clearly distinguishes between Scheduled Events, User-Reported (Unconfirmed) entries, Mutually Confirmed events, and Disputed records.\n" +
                                "• An unconfirmed report logged by one parent is never treated or certified as an established fact unless mutually confirmed by the co-parent."
                        )
                        LegalBulletSection(
                            heading = "3. Tamper-Evident Audit Trail",
                            body = "• Edits to incidents or retractions of messages preserve revision numbers and audit timestamps so neither party can silently alter historical communications."
                        )
                        LegalBulletSection(
                            heading = "4. Subscriptions & Google Play Billing",
                            body = "• Free Plan includes 1 child profile and core scheduling. Premium ($12.99/mo) and Family Premium ($19.99/mo) are billed via Google Play and can be managed or canceled anytime in Google Play Subscriptions."
                        )
                        HorizontalDivider()
                        ExternalLinkRow(
                            label = "Official Terms of Service Link",
                            url = CustodyLegalLinks.TERMS_OF_SERVICE_URL,
                            onOpen = { openExternalUrl(CustodyLegalLinks.TERMS_OF_SERVICE_URL) },
                            onCopy = {
                                clipboard.setText(AnnotatedString(CustodyLegalLinks.TERMS_OF_SERVICE_URL))
                                copiedNotice = "Copied Terms of Service URL to clipboard."
                            }
                        )
                    }

                    LegalDocumentTab.DELETE_ACCOUNT_AND_DATA -> {
                        Surface(
                            color = DisputedCrimsonSoft,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Delete Account & Personal Data",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DisputedCrimson,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "You can permanently delete your CustodyLog account and associated personal data at any time—either directly inside the app or via our external account deletion link.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        LegalBulletSection(
                            heading = "What Is Deleted Immediately",
                            body = "• Your private User Profile document (users/{uid}), email, display name, and preferences.\n" +
                                "• All pending Co-Parent Invitation tokens created by your account.\n" +
                                "• All Child Profiles and Custody Schedule Events created solely by your account.\n" +
                                "• Your Firebase Authentication login credentials."
                        )

                        LegalBulletSection(
                            heading = "Shared Co-Parent Audit Records",
                            body = "• If you have connected a co-parent, mutually shared expense statements, exchange logs, and chat transcripts retain tamper-evident audit integrity for the remaining co-parent in accordance with legal recordkeeping standards, while your account profile and login are permanently removed."
                        )

                        HorizontalDivider()

                        Text(
                            text = "Option A: External Web / Email Data Deletion Link (No Login Required)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        ExternalLinkRow(
                            label = "Account & Data Deletion Portal",
                            url = CustodyLegalLinks.DELETE_ACCOUNT_WEB_URL,
                            onOpen = { openExternalUrl(CustodyLegalLinks.DELETE_ACCOUNT_WEB_URL) },
                            onCopy = {
                                clipboard.setText(AnnotatedString(CustodyLegalLinks.DELETE_ACCOUNT_WEB_URL))
                                copiedNotice = "Copied Account & Data Deletion URL to clipboard."
                            }
                        )

                        OutlinedButton(
                            onClick = { openDeletionEmailRequest() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_data_deletion_request_button")
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Request Data Deletion via Email (${CustodyLegalLinks.PRIVACY_SUPPORT_EMAIL})")
                        }

                        if (isAuthenticated && onConfirmDeleteAccountAndData != null) {
                            HorizontalDivider()
                            Text(
                                text = "Option B: Instant In-App Account & Data Deletion",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = DisputedCrimson
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = confirmPermanentDeletionCheckbox,
                                    onCheckedChange = { confirmPermanentDeletionCheckbox = it },
                                    modifier = Modifier.testTag("confirm_permanent_delete_checkbox")
                                )
                                Text(
                                    text = "I understand that deleting my account and personal data is permanent and cannot be undone.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Button(
                                onClick = {
                                    onDismiss()
                                    onConfirmDeleteAccountAndData()
                                },
                                enabled = confirmPermanentDeletionCheckbox,
                                colors = ButtonDefaults.buttonColors(containerColor = DisputedCrimson),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("confirm_delete_account_and_data_button")
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Permanently Delete Account & Data Now")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_legal_dialog_button")
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun LegalBulletSection(
    heading: String,
    body: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = heading,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ExternalLinkRow(
    label: String,
    url: String,
    onOpen: () -> Unit,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = url,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onOpen,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open Link", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Link", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LegalPrivacyFooterCard(
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTermsOfService: () -> Unit,
    onOpenDeleteAccountAndData: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = TealSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Legal, Privacy & Account Data Controls",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Review our privacy practices, terms of use, or delete your account and personal data.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(
                    onClick = onOpenPrivacyPolicy,
                    modifier = Modifier.testTag("footer_privacy_policy_button")
                ) {
                    Icon(Icons.Default.PrivacyTip, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Privacy Policy")
                }
                TextButton(
                    onClick = onOpenTermsOfService,
                    modifier = Modifier.testTag("footer_terms_of_service_button")
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Terms of Service")
                }
                TextButton(
                    onClick = onOpenDeleteAccountAndData,
                    colors = ButtonDefaults.textButtonColors(contentColor = DisputedCrimson),
                    modifier = Modifier.testTag("footer_delete_account_data_button")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Account & Data")
                }
            }
        }
    }
}
