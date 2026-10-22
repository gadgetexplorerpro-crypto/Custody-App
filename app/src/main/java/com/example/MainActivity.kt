package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.billing.PlayBillingManager
import com.example.data.model.ChildProfile
import com.example.data.model.CoParentInvitation
import com.example.data.model.CoParentMessage
import com.example.data.model.CustodyExchangeRecord
import com.example.data.model.CustodyScheduleEvent
import com.example.data.model.IncidentRecord
import com.example.data.model.SharedExpense
import com.example.data.repository.CustodyLogRepository
import com.example.ui.auth.AuthAndOnboardingScreen
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.auth.authStateFlow
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.PremiumSubscriptionScreen
import com.example.ui.screens.RecordsAndExchangesScreen
import com.example.ui.screens.ReportsAndDocumentationScreen
import com.example.ui.screens.WorkspaceAndPrivacySettingsScreen
import com.example.ui.theme.CustodyLogTheme
import com.example.ui.theme.TealSecondary
import com.example.ui.viewmodel.CustodyLogViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

enum class MainNavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    CALENDAR("calendar", "Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    RECORDS("records", "Records", Icons.Filled.Gavel, Icons.Outlined.Gavel),
    EXPENSES("expenses", "Expenses", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    MESSAGES("messages", "Messages", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat)
}

enum class OverlaySubScreen {
    NONE,
    REPORTS,
    SUBSCRIPTIONS,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.ensureChannel(this)

        setContent {
            CustodyLogTheme {
                CustodyLogAppRoot()
            }
        }
    }
}

@Composable
fun CustodyLogAppRoot(auth: FirebaseAuth = FirebaseAuth.getInstance()) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthAndOnboardingScreen(
            onAuthSuccess = {}
        )
    } else {
        AuthenticatedCustodyWorkspace(
            currentUserId = user.uid,
            currentUserDisplayName = user.displayName ?: "Parent",
            currentUserEmail = user.email ?: "parent@custodylog.app"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedCustodyWorkspace(
    currentUserId: String,
    currentUserDisplayName: String,
    currentUserEmail: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: CustodyLogViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                CustodyLogViewModel(
                    repository = CustodyLogRepository(db),
                    currentUserId = currentUserId,
                    currentUserDisplayName = currentUserDisplayName,
                    currentUserEmail = currentUserEmail
                )
            }
        }
    )

    val billingManager = remember(currentUserId) {
        PlayBillingManager(
            context = context,
            onEntitlementVerified = { tier ->
                viewModel.onVerifiedSubscriptionPurchased(tier)
            }
        )
    }

    DisposableEffect(billingManager) {
        billingManager.startConnection()
        onDispose {
            billingManager.endConnection()
        }
    }

    // Request POST_NOTIFICATIONS on Android 13+ when entering authenticated workspace
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val invitationsState by viewModel.invitationsState.collectAsStateWithLifecycle()
    val childrenState by viewModel.childrenState.collectAsStateWithLifecycle()
    val scheduleState by viewModel.scheduleEventsState.collectAsStateWithLifecycle()
    val exchangesState by viewModel.exchangesState.collectAsStateWithLifecycle()
    val incidentsState by viewModel.incidentsState.collectAsStateWithLifecycle()
    val expensesState by viewModel.expensesState.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val toneSuggestion by viewModel.toneSuggestion.collectAsStateWithLifecycle()
    val isToneLoading by viewModel.isToneLoading.collectAsStateWithLifecycle()
    val billingState by billingManager.billingState.collectAsStateWithLifecycle()

    val userProfile = (userProfileState as? UiState.Success)?.data
    val invitations = (invitationsState as? UiState.Success)?.data ?: emptyList<CoParentInvitation>()
    val children = (childrenState as? UiState.Success)?.data ?: emptyList<ChildProfile>()
    val scheduleEvents = (scheduleState as? UiState.Success)?.data ?: emptyList<CustodyScheduleEvent>()
    val exchanges = (exchangesState as? UiState.Success)?.data ?: emptyList<CustodyExchangeRecord>()
    val incidents = (incidentsState as? UiState.Success)?.data ?: emptyList<IncidentRecord>()
    val expenses = (expensesState as? UiState.Success)?.data ?: emptyList<SharedExpense>()
    val messages = (messagesState as? UiState.Success)?.data ?: emptyList<CoParentMessage>()

    // Automatically link accepted co-parent invitations back to inviter's profile
    LaunchedEffect(invitations) {
        invitations.firstOrNull { it.status == "ACCEPTED" && !it.acceptedByUid.isNullOrBlank() }?.let { accepted ->
            viewModel.syncAcceptedInvitationToProfile(accepted)
        }
    }

    var currentTab by remember { mutableStateOf(MainNavDestination.HOME) }
    var overlaySubScreen by remember { mutableStateOf(OverlaySubScreen.NONE) }
    var recordsSubTabIndex by remember { mutableIntStateOf(0) }
    var triggerLogExchangeModal by remember { mutableStateOf(false) }
    var triggerAddIncidentModal by remember { mutableStateOf(false) }
    var triggerAddExpenseModal by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    if (overlaySubScreen != OverlaySubScreen.NONE) {
        BackHandler { overlaySubScreen = OverlaySubScreen.NONE }
    } else if (currentTab != MainNavDestination.HOME) {
        BackHandler { currentTab = MainNavDestination.HOME }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 680.dp

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = TealSecondary,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "CustodyLog",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = userProfile?.coParentName?.takeIf { it.isNotBlank() }
                                        ?.let { "Co-Parent: $it" }
                                        ?: "Verified Co-Parenting Workspace",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { overlaySubScreen = OverlaySubScreen.REPORTS },
                            modifier = Modifier.testTag("top_bar_reports_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Reports & PDF Export"
                            )
                        }
                        IconButton(
                            onClick = { overlaySubScreen = OverlaySubScreen.SUBSCRIPTIONS },
                            modifier = Modifier.testTag("top_bar_subscriptions_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "Premium Subscriptions",
                                tint = TealSecondary
                            )
                        }
                        IconButton(
                            onClick = { overlaySubScreen = OverlaySubScreen.SETTINGS },
                            modifier = Modifier.testTag("top_bar_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Family & Privacy Settings"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!isExpandedScreen && overlaySubScreen == OverlaySubScreen.NONE) {
                    NavigationBar {
                        MainNavDestination.entries.forEach { dest ->
                            val selected = currentTab == dest
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    triggerLogExchangeModal = false
                                    triggerAddIncidentModal = false
                                    triggerAddExpenseModal = false
                                    currentTab = dest
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag("bottom_nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen && overlaySubScreen == OverlaySubScreen.NONE) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        MainNavDestination.entries.forEach { dest ->
                            val selected = currentTab == dest
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    triggerLogExchangeModal = false
                                    triggerAddIncidentModal = false
                                    triggerAddExpenseModal = false
                                    currentTab = dest
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag("rail_nav_${dest.route}")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    if (isBusy) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    when (overlaySubScreen) {
                        OverlaySubScreen.REPORTS -> {
                            ReportsAndDocumentationScreen(
                                userProfile = userProfile,
                                scheduleEvents = scheduleEvents,
                                exchanges = exchanges,
                                incidents = incidents,
                                expenses = expenses,
                                messages = messages,
                                onBack = { overlaySubScreen = OverlaySubScreen.NONE },
                                onShowBanner = { viewModel.showBanner(it) }
                            )
                        }
                        OverlaySubScreen.SUBSCRIPTIONS -> {
                            PremiumSubscriptionScreen(
                                currentTier = viewModel.currentTier(),
                                billingState = billingState,
                                billingManager = billingManager,
                                onBack = { overlaySubScreen = OverlaySubScreen.NONE },
                                onShowBanner = { viewModel.showBanner(it) }
                            )
                        }
                        OverlaySubScreen.SETTINGS -> {
                            WorkspaceAndPrivacySettingsScreen(
                                userProfile = userProfile,
                                children = children,
                                invitations = invitations,
                                onBack = { overlaySubScreen = OverlaySubScreen.NONE },
                                onAddChild = { name, dob, school, medical, pattern, ratio ->
                                    if (!viewModel.canAddAnotherChild(children.size)) {
                                        viewModel.showBanner("Multiple child profiles require Family Premium ($19.99/month).")
                                        overlaySubScreen = OverlaySubScreen.SUBSCRIPTIONS
                                    } else {
                                        viewModel.addChildProfile(
                                            name = name,
                                            dateOfBirth = dob,
                                            schoolOrChildcare = school,
                                            medicalNotes = medical,
                                            custodyScheduleType = pattern,
                                            primaryParentRatio = ratio,
                                            avatarColorHex = "#0D9488"
                                        )
                                    }
                                },
                                onGenerateInviteCode = { cb -> viewModel.generateInvitationLink(cb) },
                                onAcceptInviteCode = { code -> viewModel.acceptInvitationCode(code) },
                                onSaveSettings = { name, pat, split, ret, notif ->
                                    viewModel.saveAccountSettings(name, pat, split, ret, notif)
                                },
                                onExportAllData = { overlaySubScreen = OverlaySubScreen.REPORTS },
                                onSignOut = {
                                    GoogleAuthHelper.signOut(
                                        credentialManager = credentialManager,
                                        onSignOutComplete = {},
                                        scope = scope
                                    )
                                },
                                onDeleteAccount = {
                                    viewModel.deleteAccountProfile {
                                        GoogleAuthHelper.signOut(
                                            credentialManager = credentialManager,
                                            onSignOutComplete = {},
                                            scope = scope
                                        )
                                    }
                                }
                            )
                        }
                        OverlaySubScreen.NONE -> {
                            when (currentTab) {
                                MainNavDestination.HOME -> {
                                    HomeDashboardScreen(
                                        userProfile = userProfile,
                                        children = children,
                                        scheduleEvents = scheduleEvents,
                                        exchanges = exchanges,
                                        incidents = incidents,
                                        expenses = expenses,
                                        onQuickAddIncident = {
                                            recordsSubTabIndex = 1
                                            triggerAddIncidentModal = true
                                            triggerLogExchangeModal = false
                                            currentTab = MainNavDestination.RECORDS
                                        },
                                        onQuickLogExchange = {
                                            recordsSubTabIndex = 0
                                            triggerLogExchangeModal = true
                                            triggerAddIncidentModal = false
                                            currentTab = MainNavDestination.RECORDS
                                        },
                                        onQuickAddExpense = {
                                            triggerAddExpenseModal = true
                                            currentTab = MainNavDestination.EXPENSES
                                        },
                                        onQuickMessageCoParent = {
                                            currentTab = MainNavDestination.MESSAGES
                                        },
                                        onOpenCalendar = { currentTab = MainNavDestination.CALENDAR },
                                        onOpenReports = { overlaySubScreen = OverlaySubScreen.REPORTS },
                                        onOpenSubscription = { overlaySubScreen = OverlaySubScreen.SUBSCRIPTIONS },
                                        onOpenWorkspaceSettings = { overlaySubScreen = OverlaySubScreen.SETTINGS },
                                        onApproveScheduleRequest = { req -> viewModel.respondToScheduleChange(req, true) },
                                        onDeclineScheduleRequest = { req -> viewModel.respondToScheduleChange(req, false) },
                                        onDeleteAccountAndData = {
                                            viewModel.deleteAccountProfile {
                                                GoogleAuthHelper.signOut(
                                                    credentialManager = credentialManager,
                                                    onSignOutComplete = {},
                                                    scope = scope
                                                )
                                            }
                                        }
                                    )
                                }
                                MainNavDestination.CALENDAR -> {
                                    CalendarScreen(
                                        children = children,
                                        scheduleEvents = scheduleEvents,
                                        onAddEvent = { child, title, type, parent, dateIso, start, end, loc, notes, isReq, reminder ->
                                            viewModel.addCalendarEvent(
                                                childName = child,
                                                title = title,
                                                eventType = type,
                                                assignedParent = parent,
                                                dateIso = dateIso,
                                                startTime = start,
                                                endTime = end,
                                                location = loc,
                                                notes = notes,
                                                isChangeRequest = isReq,
                                                reminderMinutesBefore = reminder
                                            )
                                        },
                                        onGeneratePattern14Days = { pattern, childName ->
                                            viewModel.generatePatternScheduleNext14Days(pattern, childName)
                                        },
                                        onRespondToChangeRequest = { ev, approve ->
                                            viewModel.respondToScheduleChange(ev, approve)
                                        }
                                    )
                                }
                                MainNavDestination.RECORDS -> {
                                    RecordsAndExchangesScreen(
                                        children = children,
                                        exchanges = exchanges,
                                        incidents = incidents,
                                        initialTab = recordsSubTabIndex,
                                        openLogExchangeInitially = triggerLogExchangeModal,
                                        openAddIncidentInitially = triggerAddIncidentModal,
                                        onLogExchange = { child, dateIso, sched, actual, loc, dir, missed, notes, attName, attUri ->
                                            triggerLogExchangeModal = false
                                            viewModel.logExchange(
                                                childName = child,
                                                scheduledDateIso = dateIso,
                                                scheduledTime = sched,
                                                actualArrivalTime = actual,
                                                location = loc,
                                                direction = dir,
                                                missedExchange = missed,
                                                notes = notes,
                                                attachmentName = attName,
                                                attachmentUri = attUri
                                            )
                                        },
                                        onConfirmOrDisputeExchange = { ex, confirm, reason ->
                                            viewModel.confirmOrDisputeExchange(ex, confirm, reason)
                                        },
                                        onAddIncident = { child, cat, title, desc, dateIso, time, attType, attName, attUri, hash ->
                                            triggerAddIncidentModal = false
                                            if (!viewModel.canAddMoreIncidents(incidents.size)) {
                                                viewModel.showBanner("Free Plan includes up to 5 incident records. Upgrade to Premium for unlimited records.")
                                                overlaySubScreen = OverlaySubScreen.SUBSCRIPTIONS
                                            } else {
                                                viewModel.addIncident(
                                                    childName = child,
                                                    category = cat,
                                                    title = title,
                                                    description = desc,
                                                    incidentDateIso = dateIso,
                                                    incidentTime = time,
                                                    attachmentType = attType,
                                                    attachmentName = attName,
                                                    attachmentUri = attUri,
                                                    originalFileHash = hash
                                                )
                                            }
                                        },
                                        onUpdateIncident = { inc, newStatus, disputeResp, archive ->
                                            viewModel.updateIncidentVerificationOrArchive(
                                                incident = inc,
                                                newStatus = newStatus,
                                                disputeResponse = disputeResp,
                                                archive = archive
                                            )
                                        }
                                    )
                                }
                                MainNavDestination.EXPENSES -> {
                                    ExpensesScreen(
                                        children = children,
                                        expenses = expenses,
                                        defaultSplitPercent = userProfile?.defaultExpenseSplitPercent ?: 50L,
                                        openAddExpenseInitially = triggerAddExpenseModal,
                                        onAddExpense = { child, cat, title, cents, curr, split, dateIso, recName, recUri, note ->
                                            triggerAddExpenseModal = false
                                            viewModel.addExpense(
                                                childName = child,
                                                category = cat,
                                                title = title,
                                                totalAmountCents = cents,
                                                currency = curr,
                                                creatorSharePercent = split,
                                                dateIso = dateIso,
                                                receiptName = recName,
                                                receiptUri = recUri,
                                                statusNote = note
                                            )
                                        },
                                        onUpdateExpenseStatus = { exp, status, note ->
                                            viewModel.updateExpenseStatus(exp, status, note)
                                        },
                                        onOpenExportReports = {
                                            overlaySubScreen = OverlaySubScreen.REPORTS
                                        }
                                    )
                                }
                                MainNavDestination.MESSAGES -> {
                                    MessagesScreen(
                                        currentUserId = currentUserId,
                                        coParentName = userProfile?.coParentName,
                                        defaultChildName = children.firstOrNull()?.name ?: "our child",
                                        messages = messages,
                                        toneSuggestion = toneSuggestion,
                                        isToneLoading = isToneLoading,
                                        onSendMessage = { body, attName, attUri, toneAssisted ->
                                            viewModel.sendCoParentMessage(body, attName, attUri, toneAssisted)
                                        },
                                        onRequestCalmRewrite = { draft, childName ->
                                            viewModel.requestCalmToneRewrite(draft, childName)
                                        },
                                        onDismissToneSuggestion = {
                                            viewModel.dismissToneSuggestion()
                                        },
                                        onEditOrRetractMessage = { msg, newBody, retract ->
                                            viewModel.editOrRetractMessage(msg, newBody, retract)
                                        },
                                        onMarkMessageRead = { msg ->
                                            viewModel.markMessageReadIfNeeded(msg)
                                        },
                                        onExportTranscript = {
                                            overlaySubScreen = OverlaySubScreen.REPORTS
                                        }
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
