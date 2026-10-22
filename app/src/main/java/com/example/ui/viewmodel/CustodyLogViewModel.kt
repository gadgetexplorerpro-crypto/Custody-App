package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.example.data.remote.CalmToneAssistantService
import com.example.data.remote.ToneSuggestionResult
import com.example.data.repository.CustodyLogRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class CustodyLogViewModel(
    private val repository: CustodyLogRepository,
    val currentUserId: String,
    private val currentUserDisplayName: String,
    private val currentUserEmail: String
) : ViewModel() {

    private val toneAssistant = CalmToneAssistantService()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _toneSuggestion = MutableStateFlow<ToneSuggestionResult?>(null)
    val toneSuggestion: StateFlow<ToneSuggestionResult?> = _toneSuggestion.asStateFlow()

    private val _isToneLoading = MutableStateFlow(false)
    val isToneLoading: StateFlow<Boolean> = _isToneLoading.asStateFlow()

    // Two-tier Kotlin Flow error handling on all repository streams
    val userProfileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing user profile", err)
            emit(UiState.Error(err.message ?: "Failed to load profile"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val invitationsState: StateFlow<UiState<List<CoParentInvitation>>> = repository.observeSentInvitations(currentUserId)
        .map<List<CoParentInvitation>, UiState<List<CoParentInvitation>>> { UiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing invitations", err)
            emit(UiState.Error(err.message ?: "Failed to load invitations"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val childrenState: StateFlow<UiState<List<ChildProfile>>> = repository.observeChildren(currentUserId)
        .map<List<ChildProfile>, UiState<List<ChildProfile>>> { UiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing children", err)
            emit(UiState.Error(err.message ?: "Failed to load children"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val scheduleEventsState: StateFlow<UiState<List<CustodyScheduleEvent>>> = repository.observeScheduleEvents(currentUserId)
        .map<List<CustodyScheduleEvent>, UiState<List<CustodyScheduleEvent>>> { UiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing schedule events", err)
            emit(UiState.Error(err.message ?: "Failed to load custody calendar"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val exchangesState: StateFlow<UiState<List<CustodyExchangeRecord>>> = repository.observeExchanges(currentUserId)
        .map<List<CustodyExchangeRecord>, UiState<List<CustodyExchangeRecord>>> { UiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing exchanges", err)
            emit(UiState.Error(err.message ?: "Failed to load exchange records"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val incidentsState: StateFlow<UiState<List<IncidentRecord>>> = repository.observeIncidents(currentUserId)
        .map<List<IncidentRecord>, UiState<List<IncidentRecord>>> { UiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing incidents", err)
            emit(UiState.Error(err.message ?: "Failed to load incident records"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val expensesState: StateFlow<UiState<List<SharedExpense>>> = repository.observeExpenses(currentUserId)
        .map<List<SharedExpense>, UiState<List<SharedExpense>>> { UiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing expenses", err)
            emit(UiState.Error(err.message ?: "Failed to load expenses"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val messagesState: StateFlow<UiState<List<CoParentMessage>>> = repository.observeMessages(currentUserId)
        .map<List<CoParentMessage>, UiState<List<CoParentMessage>>> { UiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing messages", err)
            emit(UiState.Error(err.message ?: "Failed to load messages"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    init {
        viewModelScope.launch {
            try {
                repository.ensureUserProfile(
                    displayName = currentUserDisplayName,
                    email = currentUserEmail
                )
            } catch (e: Exception) {
                Log.w(TAG, "Profile initialization failed", e)
            }
        }
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun showBanner(message: String) {
        _statusBanner.value = message
    }

    private fun currentProfileOrDefault(): UserProfile {
        val loaded = (userProfileState.value as? UiState.Success)?.data
        val fallbackFamilyId = "fam_${currentUserId.take(12).replace(Regex("[^a-zA-Z0-9_\\-]"), "")}"
        return loaded ?: UserProfile(
            id = currentUserId,
            userId = currentUserId,
            displayName = currentUserDisplayName.ifBlank { "Parent" },
            email = currentUserEmail.ifBlank { "parent@custodylog.app" },
            familyId = fallbackFamilyId
        )
    }

    fun activeMemberUids(): List<String> {
        val profile = currentProfileOrDefault()
        return listOfNotNull(currentUserId, profile.coParentUid?.takeIf { it.isNotBlank() }).distinct()
    }

    fun currentTier(): SubscriptionTier {
        val tierStr = currentProfileOrDefault().subscriptionTier
        return SubscriptionTier.entries.firstOrNull { it.name == tierStr } ?: SubscriptionTier.FREE
    }

    // --- Entitlement Checks ---
    fun canAddAnotherChild(existingCount: Int): Boolean {
        val tier = currentTier()
        return existingCount == 0 || tier == SubscriptionTier.FAMILY_PREMIUM
    }

    fun canAddMoreIncidents(existingCount: Int): Boolean {
        val tier = currentTier()
        return tier != SubscriptionTier.FREE || existingCount < 5
    }

    fun canExportReports(): Boolean {
        return currentTier() != SubscriptionTier.FREE
    }

    // --- Account & Co-Parent Actions ---

    fun generateInvitationLink(onResult: (String) -> Unit = {}) {
        val profile = currentProfileOrDefault()
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val code = repository.createCoParentInvitation(
                    inviterName = profile.displayName,
                    familyId = profile.familyId
                )
                _statusBanner.value = "Invitation token $code created. Share with your co-parent."
                onResult(code)
            } catch (e: Exception) {
                _statusBanner.value = "Could not create invitation: ${e.localizedMessage ?: "Error"}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun acceptInvitationCode(code: String) {
        val profile = currentProfileOrDefault()
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val invite = repository.acceptCoParentInvitation(
                    inviteCode = code,
                    accepterName = profile.displayName
                )
                _statusBanner.value = "Connected with ${invite.inviterName} in shared workspace ${invite.familyId}!"
            } catch (e: Exception) {
                _statusBanner.value = e.localizedMessage ?: "Failed to accept invitation code."
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun syncAcceptedInvitationToProfile(invitation: CoParentInvitation) {
        val acceptedUid = invitation.acceptedByUid ?: return
        val acceptedName = invitation.acceptedByName ?: "Co-Parent"
        val profile = currentProfileOrDefault()
        if (profile.coParentUid == acceptedUid) return
        viewModelScope.launch {
            try {
                repository.linkCoParentToProfile(
                    familyId = invitation.familyId,
                    coParentUid = acceptedUid,
                    coParentName = acceptedName
                )
                _statusBanner.value = "Co-parent $acceptedName linked to your workspace."
            } catch (e: Exception) {
                Log.w(TAG, "Failed linking co-parent", e)
            }
        }
    }

    fun saveAccountSettings(
        displayName: String,
        custodyPattern: CustodyPattern,
        defaultExpenseSplitPercent: Long,
        privacyRetentionDays: Long,
        notificationsEnabled: Boolean
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                repository.updateAccountSettings(
                    displayName = displayName,
                    custodyPattern = custodyPattern,
                    defaultExpenseSplitPercent = defaultExpenseSplitPercent,
                    privacyRetentionDays = privacyRetentionDays,
                    notificationsEnabled = notificationsEnabled
                )
                _statusBanner.value = "Account & privacy preferences saved."
            } catch (e: Exception) {
                _statusBanner.value = "Failed to save settings: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun onVerifiedSubscriptionPurchased(tier: SubscriptionTier) {
        viewModelScope.launch {
            try {
                repository.updateSubscriptionTier(tier)
                _statusBanner.value = "Subscription updated to ${tier.label}."
            } catch (e: Exception) {
                _statusBanner.value = "Failed to update subscription record: ${e.localizedMessage}"
            }
        }
    }

    fun deleteAccountProfile(onComplete: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                repository.deleteUserProfileDocument()
                onComplete()
            } catch (e: Exception) {
                _statusBanner.value = "Could not delete profile document: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    // --- Child Profile Actions ---

    fun addChildProfile(
        name: String,
        dateOfBirth: String,
        schoolOrChildcare: String,
        medicalNotes: String,
        custodyScheduleType: CustodyPattern,
        primaryParentRatio: Long,
        avatarColorHex: String
    ) {
        val profile = currentProfileOrDefault()
        viewModelScope.launch {
            _isBusy.value = true
            try {
                repository.addChild(
                    familyId = profile.familyId,
                    memberUids = activeMemberUids(),
                    name = name,
                    dateOfBirth = dateOfBirth,
                    schoolOrChildcare = schoolOrChildcare,
                    medicalNotes = medicalNotes,
                    custodyScheduleType = custodyScheduleType,
                    primaryParentRatio = primaryParentRatio,
                    avatarColorHex = avatarColorHex
                )
                _statusBanner.value = "Child profile '$name' added."
            } catch (e: Exception) {
                _statusBanner.value = "Failed to add child: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    // --- Smart Custody Calendar Actions ---

    fun addCalendarEvent(
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
        reminderMinutesBefore: Long
    ) {
        val profile = currentProfileOrDefault()
        viewModelScope.launch {
            _isBusy.value = true
            try {
                repository.addScheduleEvent(
                    familyId = profile.familyId,
                    memberUids = activeMemberUids(),
                    childName = childName,
                    title = title,
                    eventType = eventType,
                    assignedParent = assignedParent,
                    dateIso = dateIso,
                    startTime = startTime,
                    endTime = endTime,
                    location = location,
                    notes = notes,
                    isChangeRequest = isChangeRequest,
                    reminderMinutesBefore = reminderMinutesBefore
                )
                _statusBanner.value = if (isChangeRequest) {
                    "Schedule change request submitted for co-parent confirmation."
                } else {
                    "Event '$title' added to custody calendar."
                }
            } catch (e: Exception) {
                _statusBanner.value = "Failed to save calendar event: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun generatePatternScheduleNext14Days(pattern: CustodyPattern, childName: String) {
        val profile = currentProfileOrDefault()
        val members = activeMemberUids()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()

        // 14-day cycle assignments for 2-2-3, 2-2-5-5, and Alternating Weekends
        val cycleAssignments: List<String> = when (pattern) {
            CustodyPattern.TWO_TWO_THREE -> listOf(
                "PRIMARY", "PRIMARY", "CO_PARENT", "CO_PARENT", "PRIMARY", "PRIMARY", "PRIMARY",
                "CO_PARENT", "CO_PARENT", "PRIMARY", "PRIMARY", "CO_PARENT", "CO_PARENT", "CO_PARENT"
            )
            CustodyPattern.TWO_TWO_FIVE_FIVE -> listOf(
                "PRIMARY", "PRIMARY", "CO_PARENT", "CO_PARENT", "PRIMARY", "PRIMARY", "PRIMARY",
                "PRIMARY", "PRIMARY", "CO_PARENT", "CO_PARENT", "CO_PARENT", "CO_PARENT", "CO_PARENT"
            )
            CustodyPattern.ALTERNATING_WEEKENDS -> listOf(
                "PRIMARY", "PRIMARY", "PRIMARY", "PRIMARY", "CO_PARENT", "CO_PARENT", "CO_PARENT",
                "PRIMARY", "PRIMARY", "PRIMARY", "PRIMARY", "PRIMARY", "PRIMARY", "PRIMARY"
            )
            CustodyPattern.CUSTOM -> listOf(
                "PRIMARY", "PRIMARY", "PRIMARY", "CO_PARENT", "CO_PARENT", "PRIMARY", "CO_PARENT",
                "PRIMARY", "PRIMARY", "PRIMARY", "CO_PARENT", "CO_PARENT", "PRIMARY", "CO_PARENT"
            )
        }

        viewModelScope.launch {
            _isBusy.value = true
            try {
                for (dayOffset in 0 until 14) {
                    val dateStr = sdf.format(cal.time)
                    val assigned = cycleAssignments[dayOffset % cycleAssignments.size]
                    val parentLabel = if (assigned == "PRIMARY") profile.displayName else (profile.coParentName ?: "Co-Parent")
                    repository.addScheduleEvent(
                        familyId = profile.familyId,
                        memberUids = members,
                        childName = childName.ifBlank { "All Children" },
                        title = "${pattern.label}: $parentLabel Custody Day",
                        eventType = "CUSTODY_TIME",
                        assignedParent = assigned,
                        dateIso = dateStr,
                        startTime = "08:00",
                        endTime = "20:00",
                        location = "Home / School",
                        notes = "Generated from ${pattern.label} (${pattern.summary})",
                        isChangeRequest = false,
                        reminderMinutesBefore = 60L
                    )
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                _statusBanner.value = "Generated 14-day ${pattern.label} custody blocks."
            } catch (e: Exception) {
                _statusBanner.value = "Could not generate schedule blocks: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun respondToScheduleChange(event: CustodyScheduleEvent, approve: Boolean) {
        val profile = currentProfileOrDefault()
        viewModelScope.launch {
            _isBusy.value = true
            try {
                repository.respondToScheduleChangeRequest(
                    event = event,
                    approve = approve,
                    responderName = profile.displayName
                )
                _statusBanner.value = if (approve) {
                    "Schedule change mutually confirmed and logged in history."
                } else {
                    "Schedule change request declined."
                }
            } catch (e: Exception) {
                _statusBanner.value = "Could not update schedule request: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    // --- Custody Exchange Tracking Actions ---

    fun logExchange(
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
    ) {
        val profile = currentProfileOrDefault()
        viewModelScope.launch {
            _isBusy.value = true
            try {
                repository.logCustodyExchange(
                    createdByName = profile.displayName,
                    familyId = profile.familyId,
                    memberUids = activeMemberUids(),
                    childName = childName,
                    scheduledDateIso = scheduledDateIso,
                    scheduledTime = scheduledTime,
                    actualArrivalTime = actualArrivalTime,
                    location = location,
                    direction = direction,
                    missedExchange = missedExchange,
                    notes = notes,
                    attachmentName = attachmentName,
                    attachmentUri = attachmentUri
                )
                _statusBanner.value = "Custody exchange logged (User-Reported until co-parent confirms)."
            } catch (e: Exception) {
                _statusBanner.value = "Failed to log exchange: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun confirmOrDisputeExchange(exchange: CustodyExchangeRecord, confirm: Boolean, disputeReason: String = "") {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                repository.confirmOrDisputeExchange(exchange, confirm, disputeReason)
                _statusBanner.value = if (confirm) {
                    "Exchange mutually confirmed."
                } else {
                    "Exchange marked as disputed with neutral note."
                }
            } catch (e: Exception) {
                _statusBanner.value = "Could not update exchange record: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    // --- Evidence & Incident Records Actions ---

    fun addIncident(
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
    ) {
        val profile = currentProfileOrDefault()
        viewModelScope.launch {
            _isBusy.value = true
            try {
                repository.addIncidentRecord(
                    createdByName = profile.displayName,
                    familyId = profile.familyId,
                    memberUids = activeMemberUids(),
                    childName = childName,
                    category = category,
                    title = title,
                    description = description,
                    incidentDateIso = incidentDateIso,
                    incidentTime = incidentTime,
                    attachmentType = attachmentType,
                    attachmentName = attachmentName,
                    attachmentUri = attachmentUri,
                    originalFileHash = originalFileHash
                )
                _statusBanner.value = "Incident documented in chronological journal (v1)."
            } catch (e: Exception) {
                _statusBanner.value = "Failed to document incident: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun updateIncidentVerificationOrArchive(
        incident: IncidentRecord,
        newTitle: String = incident.title,
        newDescription: String = incident.description,
        newStatus: String = incident.verificationStatus,
        disputeResponse: String = incident.disputeResponse,
        archive: Boolean = incident.isArchived
    ) {
        val profile = currentProfileOrDefault()
        viewModelScope.launch {
            _isBusy.value = true
            try {
                repository.amendOrRespondToIncident(
                    incident = incident,
                    actorName = profile.displayName,
                    newTitle = newTitle,
                    newDescription = newDescription,
                    newVerificationStatus = newStatus,
                    disputeResponse = disputeResponse,
                    archive = archive
                )
                _statusBanner.value = "Incident updated (revision v${incident.version + 1} recorded in audit log)."
            } catch (e: Exception) {
                _statusBanner.value = "Could not update incident: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    // --- Shared Expense Actions ---

    fun addExpense(
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
    ) {
        val profile = currentProfileOrDefault()
        viewModelScope.launch {
            _isBusy.value = true
            try {
                repository.addSharedExpense(
                    createdByName = profile.displayName,
                    familyId = profile.familyId,
                    memberUids = activeMemberUids(),
                    childName = childName,
                    category = category,
                    title = title,
                    totalAmountCents = totalAmountCents,
                    currency = currency,
                    creatorSharePercent = creatorSharePercent,
                    dateIso = dateIso,
                    receiptName = receiptName,
                    receiptUri = receiptUri,
                    statusNote = statusNote
                )
                _statusBanner.value = "Shared expense '$title' logged and reimbursement calculated."
            } catch (e: Exception) {
                _statusBanner.value = "Failed to save expense: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun updateExpenseStatus(expense: SharedExpense, newStatus: String, note: String) {
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        viewModelScope.launch {
            _isBusy.value = true
            try {
                repository.updateExpensePaymentStatus(
                    expense = expense,
                    newStatus = newStatus,
                    statusNote = note,
                    paidAtIso = if (newStatus == "PAID") todayIso else ""
                )
                _statusBanner.value = "Expense status updated to $newStatus."
            } catch (e: Exception) {
                _statusBanner.value = "Failed to update expense: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    // --- Co-Parent Messaging & AI Tone Assistant Actions ---

    fun requestCalmToneRewrite(draft: String, childName: String) {
        viewModelScope.launch {
            _isToneLoading.value = true
            try {
                val result = toneAssistant.rewriteForCalmCoParenting(draft, childName.ifBlank { "our child" })
                _toneSuggestion.value = result
            } catch (e: Exception) {
                _statusBanner.value = "Tone assistant error: ${e.localizedMessage}"
            } finally {
                _isToneLoading.value = false
            }
        }
    }

    fun dismissToneSuggestion() {
        _toneSuggestion.value = null
    }

    fun sendCoParentMessage(
        body: String,
        attachmentName: String = "",
        attachmentUri: String = "",
        toneAssisted: Boolean = false
    ) {
        if (body.isBlank()) return
        val profile = currentProfileOrDefault()
        viewModelScope.launch {
            try {
                repository.sendMessage(
                    senderName = profile.displayName,
                    familyId = profile.familyId,
                    memberUids = activeMemberUids(),
                    body = body,
                    attachmentName = attachmentName,
                    attachmentUri = attachmentUri,
                    toneAssisted = toneAssisted
                )
                _toneSuggestion.value = null
            } catch (e: Exception) {
                _statusBanner.value = "Could not send message: ${e.localizedMessage}"
            }
        }
    }

    fun editOrRetractMessage(message: CoParentMessage, newBody: String, retract: Boolean) {
        val profile = currentProfileOrDefault()
        viewModelScope.launch {
            try {
                repository.editOrRetractMessage(
                    message = message,
                    newBody = newBody,
                    retract = retract,
                    actorName = profile.displayName
                )
                _statusBanner.value = if (retract) {
                    "Message retracted (original text retained in audit trail)."
                } else {
                    "Message edited (original text retained in audit trail)."
                }
            } catch (e: Exception) {
                _statusBanner.value = "Could not modify message: ${e.localizedMessage}"
            }
        }
    }

    fun markMessageReadIfNeeded(message: CoParentMessage) {
        if (currentUserId in message.readByUids) return
        viewModelScope.launch {
            try {
                repository.markMessageRead(message)
            } catch ( ignored: Exception) {
            }
        }
    }

    private companion object {
        const val TAG = "CustodyLogVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
