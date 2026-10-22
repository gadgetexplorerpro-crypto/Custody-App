package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ChildProfile
import com.example.data.model.CoParentInvitation
import com.example.data.model.CoParentMessage
import com.example.data.model.CustodyExchangeRecord
import com.example.data.model.CustodyPattern
import com.example.data.model.CustodyScheduleEvent
import com.example.data.model.ExchangeLatenessCalculator
import com.example.data.model.IncidentRecord
import com.example.data.model.SharedExpense
import com.example.data.model.SubscriptionTier
import com.example.data.model.UserProfile
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CustodyLogRepository(
    private val db: FirebaseFirestore,
    private val authProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() }
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return authProvider().currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun unwrapAndThrow(error: Throwable, op: OperationType, path: String): Nothing {
        val firestoreEx = (error as? com.google.firebase.firestore.FirebaseFirestoreException)
            ?: (error.cause as? com.google.firebase.firestore.FirebaseFirestoreException)
            ?: (error.cause?.cause as? com.google.firebase.firestore.FirebaseFirestoreException)
        if (firestoreEx != null) {
            handleFirestoreError(firestoreEx, op, path)
            throw firestoreEx
        }
        if (error is Exception && error !is CancellationException) {
            handleFirestoreError(error, op, path)
        }
        throw error
    }

    // --- 1. USER PROFILE & PRIVACY ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                unwrapAndThrow(error, OperationType.GET, path)
            }
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        val path = "users/$userId"
        return try {
            val snap = db.collection("users").document(userId).get().await()
            if (snap.exists()) {
                snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    suspend fun ensureUserProfile(displayName: String, email: String): UserProfile {
        val uid = requireUserId()
        val existing = getUserProfile(uid)
        if (existing != null) return existing

        val familyId = "fam_${uid.take(12).replace(Regex("[^a-zA-Z0-9_\\-]"), "")}"
        val profile = UserProfile(
            id = uid,
            userId = uid,
            displayName = displayName.ifBlank { "Parent" }.take(100),
            email = email.ifBlank { "parent@custodylog.app" }.take(150),
            familyId = familyId,
            subscriptionTier = SubscriptionTier.FREE.name,
            custodyPattern = CustodyPattern.TWO_TWO_THREE.name,
            defaultExpenseSplitPercent = 50L,
            privacyRetentionDays = 1825L,
            notificationsEnabled = true
        )
        val docRef = db.collection("users").document(uid)
        try {
            docRef.set(profile.toCreateMap()).await()
            return getUserProfile(uid) ?: profile
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateSubscriptionTier(tier: SubscriptionTier) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            docRef.update(
                mapOf(
                    "subscriptionTier" to tier.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateAccountSettings(
        displayName: String,
        custodyPattern: CustodyPattern,
        defaultExpenseSplitPercent: Long,
        privacyRetentionDays: Long,
        notificationsEnabled: Boolean
    ) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            docRef.update(
                mapOf(
                    "displayName" to displayName.trim().ifBlank { "Parent" }.take(100),
                    "custodyPattern" to custodyPattern.name,
                    "defaultExpenseSplitPercent" to defaultExpenseSplitPercent.coerceIn(0L, 100L),
                    "privacyRetentionDays" to privacyRetentionDays.coerceIn(30L, 3650L),
                    "notificationsEnabled" to notificationsEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun linkCoParentToProfile(
        familyId: String,
        coParentUid: String,
        coParentName: String
    ) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            docRef.update(
                mapOf(
                    "familyId" to familyId,
                    "coParentUid" to coParentUid,
                    "coParentName" to coParentName.take(100),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteUserProfileDocument() {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            // Delete user-created child profiles
            runCatching {
                val childSnaps = db.collection("children")
                    .whereArrayContains("memberUids", uid)
                    .get()
                    .await()
                for (doc in childSnaps.documents) {
                    if (doc.getString("createdByUid") == uid) {
                        doc.reference.delete().await()
                    }
                }
            }
            // Delete user-created schedule events
            runCatching {
                val eventSnaps = db.collection("schedule_events")
                    .whereArrayContains("memberUids", uid)
                    .get()
                    .await()
                for (doc in eventSnaps.documents) {
                    if (doc.getString("createdByUid") == uid) {
                        doc.reference.delete().await()
                    }
                }
            }
            // Delete user-created invitations
            runCatching {
                val inviteSnaps = db.collection("invitations")
                    .whereEqualTo("inviterUid", uid)
                    .get()
                    .await()
                for (doc in inviteSnaps.documents) {
                    doc.reference.delete().await()
                }
            }
            // Delete private user profile document
            docRef.delete().await()
            // Attempt to delete the Firebase Authentication user record
            runCatching {
                authProvider().currentUser?.delete()?.await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- 2. CO-PARENT INVITATIONS ---

    fun observeSentInvitations(userId: String): Flow<List<CoParentInvitation>> {
        val path = "invitations"
        return db.collection(path)
            .whereEqualTo("inviterUid", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CoParentInvitation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                unwrapAndThrow(error, OperationType.LIST, path)
            }
    }

    suspend fun createCoParentInvitation(inviterName: String, familyId: String): String {
        val uid = requireUserId()
        val rawToken = UUID.randomUUID().toString().replace("-", "").uppercase().take(8)
        val inviteCode = "CL-$rawToken"
        val docRef = db.collection("invitations").document(inviteCode)
        val invitation = CoParentInvitation(
            id = inviteCode,
            inviteCode = inviteCode,
            inviterUid = uid,
            inviterName = inviterName.ifBlank { "Co-Parent" }.take(100),
            familyId = familyId,
            status = "PENDING"
        )
        try {
            docRef.set(invitation.toCreateMap()).await()
            return inviteCode
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun acceptCoParentInvitation(inviteCode: String, accepterName: String): CoParentInvitation {
        val uid = requireUserId()
        val normalizedCode = inviteCode.trim().uppercase()
        val docRef = db.collection("invitations").document(normalizedCode)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                throw IllegalArgumentException("Invitation code '$normalizedCode' was not found.")
            }
            val invite = snap.toObject(CoParentInvitation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: throw IllegalArgumentException("Invalid invitation record.")
            if (invite.status != "PENDING") {
                throw IllegalStateException("This invitation has already been ${invite.status.lowercase()}.")
            }
            docRef.update(
                mapOf(
                    "status" to "ACCEPTED",
                    "acceptedByUid" to uid,
                    "acceptedByName" to accepterName.ifBlank { "Co-Parent" }.take(100),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            linkCoParentToProfile(
                familyId = invite.familyId,
                coParentUid = invite.inviterUid,
                coParentName = invite.inviterName
            )
            return invite.copy(
                status = "ACCEPTED",
                acceptedByUid = uid,
                acceptedByName = accepterName
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- 3. CHILDREN ---

    fun observeChildren(userId: String): Flow<List<ChildProfile>> {
        val path = "children"
        return db.collection(path)
            .whereArrayContains("memberUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ChildProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.name }
            }
            .catch { error ->
                unwrapAndThrow(error, OperationType.LIST, path)
            }
    }

    suspend fun addChild(
        familyId: String,
        memberUids: List<String>,
        name: String,
        dateOfBirth: String,
        schoolOrChildcare: String,
        medicalNotes: String,
        custodyScheduleType: CustodyPattern,
        primaryParentRatio: Long,
        avatarColorHex: String
    ): String {
        val uid = requireUserId()
        val childId = "child_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("children").document(childId)
        val distinctMembers = (listOf(uid) + memberUids).filter { it.isNotBlank() }.distinct().take(2)
        val child = ChildProfile(
            id = childId,
            createdByUid = uid,
            familyId = familyId,
            memberUids = distinctMembers,
            name = name.trim().take(100),
            dateOfBirth = dateOfBirth.trim().take(30),
            schoolOrChildcare = schoolOrChildcare.trim().take(150),
            medicalNotes = medicalNotes.trim().take(500),
            custodyScheduleType = custodyScheduleType.name,
            primaryParentRatio = primaryParentRatio.coerceIn(0L, 100L),
            avatarColorHex = avatarColorHex.ifBlank { "#0D9488" }.take(20)
        )
        try {
            docRef.set(child.toCreateMap()).await()
            return childId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteChild(childId: String) {
        requireUserId()
        val docRef = db.collection("children").document(childId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- 4. SMART CUSTODY CALENDAR & SCHEDULE REQUESTS ---

    fun observeScheduleEvents(userId: String): Flow<List<CustodyScheduleEvent>> {
        val path = "schedule_events"
        return db.collection(path)
            .whereArrayContains("memberUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CustodyScheduleEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareBy<CustodyScheduleEvent> { it.dateIso }.thenBy { it.startTime })
            }
            .catch { error ->
                unwrapAndThrow(error, OperationType.LIST, path)
            }
    }

    suspend fun addScheduleEvent(
        familyId: String,
        memberUids: List<String>,
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
        reminderMinutesBefore: Long = 60L
    ): String {
        val uid = requireUserId()
        val eventId = "evt_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("schedule_events").document(eventId)
        val distinctMembers = (listOf(uid) + memberUids).filter { it.isNotBlank() }.distinct().take(2)
        val status = if (isChangeRequest || eventType == "CHANGE_REQUEST") "PROPOSED_CHANGE" else "SCHEDULED"
        val history = if (status == "PROPOSED_CHANGE") {
            "Change requested on $dateIso ($startTime-$endTime) — awaiting co-parent confirmation."
        } else {
            "Scheduled on $dateIso ($startTime-$endTime)."
        }

        val event = CustodyScheduleEvent(
            id = eventId,
            createdByUid = uid,
            familyId = familyId,
            memberUids = distinctMembers,
            childName = childName.ifBlank { "All Children" }.take(100),
            title = title.trim().take(150),
            eventType = eventType,
            assignedParent = assignedParent,
            dateIso = dateIso.trim().take(30),
            startTime = startTime.trim().ifBlank { "09:00" }.take(20),
            endTime = endTime.trim().ifBlank { "17:00" }.take(20),
            location = location.trim().take(200),
            notes = notes.trim().take(1000),
            verificationStatus = status,
            changeHistorySummary = history.take(1000),
            reminderMinutesBefore = reminderMinutesBefore.coerceIn(0L, 10080L)
        )
        try {
            docRef.set(event.toCreateMap()).await()
            return eventId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun respondToScheduleChangeRequest(
        event: CustodyScheduleEvent,
        approve: Boolean,
        responderName: String
    ) {
        val uid = requireUserId()
        require(event.id.isNotBlank()) { "Event ID must not be blank" }
        val docRef = db.collection("schedule_events").document(event.id)
        val newStatus = if (approve) "MUTUALLY_CONFIRMED" else "DECLINED"
        val actionWord = if (approve) "Mutually confirmed" else "Declined"
        val updatedHistory = "${event.changeHistorySummary} | $actionWord by $responderName.".take(1000)

        try {
            docRef.update(
                mapOf(
                    "verificationStatus" to newStatus,
                    "confirmedByUid" to uid,
                    "changeHistorySummary" to updatedHistory,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- 5. CUSTODY EXCHANGE TRACKING ---

    fun observeExchanges(userId: String): Flow<List<CustodyExchangeRecord>> {
        val path = "exchanges"
        return db.collection(path)
            .whereArrayContains("memberUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CustodyExchangeRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareByDescending<CustodyExchangeRecord> { it.scheduledDateIso }
                            .thenByDescending { it.scheduledTime }
                    )
            }
            .catch { error ->
                unwrapAndThrow(error, OperationType.LIST, path)
            }
    }

    suspend fun logCustodyExchange(
        createdByName: String,
        familyId: String,
        memberUids: List<String>,
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
    ): String {
        val uid = requireUserId()
        val exchangeId = "exch_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("exchanges").document(exchangeId)
        val distinctMembers = (listOf(uid) + memberUids).filter { it.isNotBlank() }.distinct().take(2)

        val lateness = if (missedExchange) {
            0L
        } else {
            ExchangeLatenessCalculator.calculateLatenessMinutes(scheduledTime, actualArrivalTime)
        }

        val status = when {
            missedExchange -> "MISSED"
            lateness > 0L -> "LATE"
            else -> "COMPLETED_ON_TIME"
        }

        val record = CustodyExchangeRecord(
            id = exchangeId,
            createdByUid = uid,
            createdByName = createdByName.ifBlank { "Parent" }.take(100),
            familyId = familyId,
            memberUids = distinctMembers,
            childName = childName.ifBlank { "Child" }.take(100),
            scheduledDateIso = scheduledDateIso.trim().take(30),
            scheduledTime = scheduledTime.trim().take(20),
            actualArrivalTime = if (missedExchange) "No Arrival" else actualArrivalTime.trim().take(20),
            latenessMinutes = lateness,
            location = location.trim().ifBlank { "Agreed Exchange Location" }.take(200),
            direction = direction,
            exchangeStatus = status,
            verificationStatus = "USER_REPORTED",
            notes = notes.trim().take(1000),
            attachmentName = attachmentName.trim().take(200),
            attachmentUri = attachmentUri.trim().take(500),
            disputeReason = "",
            confirmedByUids = listOf(uid)
        )

        try {
            docRef.set(record.toCreateMap()).await()
            return exchangeId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun confirmOrDisputeExchange(
        exchange: CustodyExchangeRecord,
        confirm: Boolean,
        disputeReason: String = ""
    ) {
        val uid = requireUserId()
        require(exchange.id.isNotBlank()) { "Exchange ID must not be blank" }
        val docRef = db.collection("exchanges").document(exchange.id)
        val updatedConfirmers = (exchange.confirmedByUids + uid).distinct().take(2)
        val newVerification = if (confirm) "MUTUALLY_CONFIRMED" else "DISPUTED"
        val newExchangeStatus = if (!confirm) "DISPUTED" else exchange.exchangeStatus

        try {
            docRef.update(
                mapOf(
                    "exchangeStatus" to newExchangeStatus,
                    "verificationStatus" to newVerification,
                    "disputeReason" to disputeReason.trim().take(1000),
                    "confirmedByUids" to updatedConfirmers,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- 6. EVIDENCE & INCIDENT RECORDS ---

    fun observeIncidents(userId: String): Flow<List<IncidentRecord>> {
        val path = "incidents"
        return db.collection(path)
            .whereArrayContains("memberUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(IncidentRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareByDescending<IncidentRecord> { it.incidentDateIso }
                            .thenByDescending { it.incidentTime }
                    )
            }
            .catch { error ->
                unwrapAndThrow(error, OperationType.LIST, path)
            }
    }

    suspend fun addIncidentRecord(
        createdByName: String,
        familyId: String,
        memberUids: List<String>,
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
    ): String {
        val uid = requireUserId()
        val incidentId = "inc_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("incidents").document(incidentId)
        val distinctMembers = (listOf(uid) + memberUids).filter { it.isNotBlank() }.distinct().take(2)
        val auditSummary = "v1 logged by $createdByName on $incidentDateIso at $incidentTime (User-Reported)."

        val incident = IncidentRecord(
            id = incidentId,
            createdByUid = uid,
            createdByName = createdByName.ifBlank { "Parent" }.take(100),
            familyId = familyId,
            memberUids = distinctMembers,
            childName = childName.ifBlank { "Child" }.take(100),
            category = category,
            title = title.trim().take(150),
            description = description.trim().take(2000),
            incidentDateIso = incidentDateIso.trim().take(30),
            incidentTime = incidentTime.trim().take(20),
            attachmentType = attachmentType,
            attachmentName = attachmentName.trim().take(200),
            attachmentUri = attachmentUri.trim().take(500),
            originalFileHash = originalFileHash.ifBlank { "sha256_none" }.take(128),
            verificationStatus = "USER_REPORTED",
            disputeResponse = "",
            version = 1L,
            auditHistorySummary = auditSummary.take(2000),
            isArchived = false
        )

        try {
            docRef.set(incident.toCreateMap()).await()
            return incidentId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun amendOrRespondToIncident(
        incident: IncidentRecord,
        actorName: String,
        newTitle: String = incident.title,
        newDescription: String = incident.description,
        newVerificationStatus: String = incident.verificationStatus,
        disputeResponse: String = incident.disputeResponse,
        archive: Boolean = incident.isArchived
    ) {
        requireUserId()
        require(incident.id.isNotBlank()) { "Incident ID must not be blank" }
        val docRef = db.collection("incidents").document(incident.id)
        val nextVersion = incident.version + 1L
        val actionLabel = when {
            archive && !incident.isArchived -> "Archived (soft-delete)"
            newVerificationStatus == "MUTUALLY_CONFIRMED" -> "Confirmed"
            newVerificationStatus == "DISPUTED" -> "Disputed"
            else -> "Amended"
        }
        val nextAudit = "${incident.auditHistorySummary} | v$nextVersion $actionLabel by $actorName".take(2000)

        try {
            docRef.update(
                mapOf(
                    "title" to newTitle.trim().take(150),
                    "description" to newDescription.trim().take(2000),
                    "verificationStatus" to newVerificationStatus,
                    "disputeResponse" to disputeResponse.trim().take(1000),
                    "version" to nextVersion,
                    "auditHistorySummary" to nextAudit,
                    "isArchived" to archive,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- 7. SHARED EXPENSE TRACKER ---

    fun observeExpenses(userId: String): Flow<List<SharedExpense>> {
        val path = "expenses"
        return db.collection(path)
            .whereArrayContains("memberUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SharedExpense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.dateIso }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun addSharedExpense(
        createdByName: String,
        familyId: String,
        memberUids: List<String>,
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
    ): String {
        val uid = requireUserId()
        val expenseId = "exp_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("expenses").document(expenseId)
        val distinctMembers = (listOf(uid) + memberUids).filter { it.isNotBlank() }.distinct().take(2)
        val clampedSplit = creatorSharePercent.coerceIn(0L, 100L)
        val coParentShareCents = ((totalAmountCents * (100L - clampedSplit)) / 100L).coerceAtLeast(0L)

        val expense = SharedExpense(
            id = expenseId,
            createdByUid = uid,
            createdByName = createdByName.ifBlank { "Parent" }.take(100),
            familyId = familyId,
            memberUids = distinctMembers,
            childName = childName.ifBlank { "Child" }.take(100),
            category = category,
            title = title.trim().take(150),
            totalAmountCents = totalAmountCents.coerceAtLeast(1L),
            currency = if (currency == "GBP") "GBP" else "USD",
            creatorSharePercent = clampedSplit,
            coParentShareCents = coParentShareCents,
            dateIso = dateIso.trim().take(30),
            receiptName = receiptName.trim().take(200),
            receiptUri = receiptUri.trim().take(500),
            paymentStatus = "UNPAID_REQUESTED",
            statusNote = statusNote.trim().take(500),
            paidAtIso = ""
        )

        try {
            docRef.set(expense.toCreateMap()).await()
            return expenseId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateExpensePaymentStatus(
        expense: SharedExpense,
        newStatus: String,
        statusNote: String,
        paidAtIso: String
    ) {
        requireUserId()
        require(expense.id.isNotBlank()) { "Expense ID must not be blank" }
        val docRef = db.collection("expenses").document(expense.id)
        try {
            docRef.update(
                mapOf(
                    "paymentStatus" to newStatus,
                    "statusNote" to statusNote.trim().take(500),
                    "paidAtIso" to paidAtIso.trim().take(40),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- 8. SECURE CO-PARENT MESSAGING ---

    fun observeMessages(userId: String): Flow<List<CoParentMessage>> {
        val path = "messages"
        return db.collection(path)
            .whereArrayContains("memberUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CoParentMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun sendMessage(
        senderName: String,
        familyId: String,
        memberUids: List<String>,
        body: String,
        attachmentName: String = "",
        attachmentUri: String = "",
        toneAssisted: Boolean = false
    ): String {
        val uid = requireUserId()
        val messageId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("messages").document(messageId)
        val distinctMembers = (listOf(uid) + memberUids).filter { it.isNotBlank() }.distinct().take(2)
        val trimmedBody = body.trim().take(2000)

        val message = CoParentMessage(
            id = messageId,
            senderUid = uid,
            senderName = senderName.ifBlank { "Parent" }.take(100),
            familyId = familyId,
            memberUids = distinctMembers,
            body = trimmedBody,
            originalBody = trimmedBody,
            attachmentName = attachmentName.trim().take(200),
            attachmentUri = attachmentUri.trim().take(500),
            isEdited = false,
            isRetracted = false,
            auditNote = "Sent by $senderName",
            readByUids = listOf(uid),
            toneAssisted = toneAssisted
        )

        try {
            docRef.set(message.toCreateMap()).await()
            return messageId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun editOrRetractMessage(
        message: CoParentMessage,
        newBody: String,
        retract: Boolean,
        actorName: String
    ) {
        val uid = requireUserId()
        require(message.id.isNotBlank()) { "Message ID must not be blank" }
        require(message.senderUid == uid) { "Only the sender can edit or retract their message" }
        val docRef = db.collection("messages").document(message.id)
        val updatedBody = if (retract) "[Message retracted by sender — original preserved in audit log]" else newBody.trim().take(2000)
        val updatedAudit = if (retract) {
            "${message.auditNote} | Retracted by $actorName (Original preserved)"
        } else {
            "${message.auditNote} | Edited by $actorName (Original preserved)"
        }.take(500)

        try {
            docRef.update(
                mapOf(
                    "body" to updatedBody,
                    "isEdited" to !retract,
                    "isRetracted" to retract,
                    "auditNote" to updatedAudit,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun markMessageRead(message: CoParentMessage) {
        val uid = requireUserId()
        if (uid in message.readByUids || message.id.isBlank()) return
        val docRef = db.collection("messages").document(message.id)
        val updatedReaders = (message.readByUids + uid).distinct().take(2)
        try {
            docRef.update(
                mapOf(
                    "readByUids" to updatedReaders,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }
}
