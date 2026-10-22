package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class SubscriptionTier(val label: String, val priceLabel: String, val skuId: String) {
    FREE("Free Plan", "$0/month", "free_plan"),
    PREMIUM("Premium", "$12.99/month", "custodylog_premium_monthly"),
    FAMILY_PREMIUM("Family Premium", "$19.99/month", "custodylog_family_premium_monthly")
}

enum class CustodyPattern(val label: String, val summary: String) {
    TWO_TWO_THREE("2-2-3 Schedule", "Mon-Tue Parent A, Wed-Thu Parent B, Fri-Sun alternating"),
    TWO_TWO_FIVE_FIVE("2-2-5-5 Schedule", "2 days Parent A, 2 days Parent B, 5-day alternating blocks"),
    ALTERNATING_WEEKENDS("Alternating Weekends", "Weekdays primary parent, alternating Fri-Sun weekends"),
    CUSTOM("Custom Arrangement", "Tailored custody blocks and holiday rotation")
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val familyId: String = "",
    val coParentUid: String? = null,
    val coParentName: String? = null,
    val subscriptionTier: String = SubscriptionTier.FREE.name,
    val custodyPattern: String = CustodyPattern.TWO_TWO_THREE.name,
    val defaultExpenseSplitPercent: Long = 50L,
    val privacyRetentionDays: Long = 1825L,
    val notificationsEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "displayName" to displayName,
            "email" to email,
            "familyId" to familyId,
            "coParentUid" to coParentUid,
            "coParentName" to coParentName,
            "subscriptionTier" to subscriptionTier,
            "custodyPattern" to custodyPattern,
            "defaultExpenseSplitPercent" to defaultExpenseSplitPercent,
            "privacyRetentionDays" to privacyRetentionDays,
            "notificationsEnabled" to notificationsEnabled,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class CoParentInvitation(
    @DocumentId val id: String = "",
    val inviteCode: String = "",
    val inviterUid: String = "",
    val inviterName: String = "",
    val familyId: String = "",
    val status: String = "PENDING",
    val acceptedByUid: String? = null,
    val acceptedByName: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "inviteCode" to inviteCode,
            "inviterUid" to inviterUid,
            "inviterName" to inviterName,
            "familyId" to familyId,
            "status" to status,
            "acceptedByUid" to acceptedByUid,
            "acceptedByName" to acceptedByName,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class ChildProfile(
    @DocumentId val id: String = "",
    val createdByUid: String = "",
    val familyId: String = "",
    val memberUids: List<String> = emptyList(),
    val name: String = "",
    val dateOfBirth: String = "",
    val schoolOrChildcare: String = "",
    val medicalNotes: String = "",
    val custodyScheduleType: String = CustodyPattern.TWO_TWO_THREE.name,
    val primaryParentRatio: Long = 50L,
    val avatarColorHex: String = "#0D9488",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "createdByUid" to createdByUid,
            "familyId" to familyId,
            "memberUids" to memberUids,
            "name" to name,
            "dateOfBirth" to dateOfBirth,
            "schoolOrChildcare" to schoolOrChildcare,
            "medicalNotes" to medicalNotes,
            "custodyScheduleType" to custodyScheduleType,
            "primaryParentRatio" to primaryParentRatio,
            "avatarColorHex" to avatarColorHex,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@IgnoreExtraProperties
data class CustodyScheduleEvent(
    @DocumentId val id: String = "",
    val createdByUid: String = "",
    val familyId: String = "",
    val memberUids: List<String> = emptyList(),
    val childName: String = "",
    val title: String = "",
    val eventType: String = "CUSTODY_TIME", // CUSTODY_TIME, HOLIDAY, VACATION, SCHOOL_EVENT, MEDICAL_APPOINTMENT, CHANGE_REQUEST
    val assignedParent: String = "PRIMARY", // PRIMARY, CO_PARENT, SHARED
    val dateIso: String = "",
    val startTime: String = "09:00",
    val endTime: String = "17:00",
    val location: String = "",
    val notes: String = "",
    val verificationStatus: String = "SCHEDULED", // SCHEDULED, PROPOSED_CHANGE, MUTUALLY_CONFIRMED, DECLINED
    val confirmedByUid: String? = null,
    val changeHistorySummary: String = "",
    val reminderMinutesBefore: Long = 60L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "createdByUid" to createdByUid,
            "familyId" to familyId,
            "memberUids" to memberUids,
            "childName" to childName,
            "title" to title,
            "eventType" to eventType,
            "assignedParent" to assignedParent,
            "dateIso" to dateIso,
            "startTime" to startTime,
            "endTime" to endTime,
            "location" to location,
            "notes" to notes,
            "verificationStatus" to verificationStatus,
            "confirmedByUid" to confirmedByUid,
            "changeHistorySummary" to changeHistorySummary,
            "reminderMinutesBefore" to reminderMinutesBefore,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class CustodyExchangeRecord(
    @DocumentId val id: String = "",
    val createdByUid: String = "",
    val createdByName: String = "",
    val familyId: String = "",
    val memberUids: List<String> = emptyList(),
    val childName: String = "",
    val scheduledDateIso: String = "",
    val scheduledTime: String = "17:00",
    val actualArrivalTime: String = "17:00",
    val latenessMinutes: Long = 0L,
    val location: String = "",
    val direction: String = "PICK_UP", // PICK_UP, DROP_OFF
    val exchangeStatus: String = "COMPLETED_ON_TIME", // COMPLETED_ON_TIME, LATE, MISSED, DISPUTED
    val verificationStatus: String = "USER_REPORTED", // USER_REPORTED, MUTUALLY_CONFIRMED, DISPUTED
    val notes: String = "",
    val attachmentName: String = "",
    val attachmentUri: String = "",
    val disputeReason: String = "",
    val confirmedByUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "createdByUid" to createdByUid,
            "createdByName" to createdByName,
            "familyId" to familyId,
            "memberUids" to memberUids,
            "childName" to childName,
            "scheduledDateIso" to scheduledDateIso,
            "scheduledTime" to scheduledTime,
            "actualArrivalTime" to actualArrivalTime,
            "latenessMinutes" to latenessMinutes,
            "location" to location,
            "direction" to direction,
            "exchangeStatus" to exchangeStatus,
            "verificationStatus" to verificationStatus,
            "notes" to notes,
            "attachmentName" to attachmentName,
            "attachmentUri" to attachmentUri,
            "disputeReason" to disputeReason,
            "confirmedByUids" to confirmedByUids,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@IgnoreExtraProperties
data class IncidentRecord(
    @DocumentId val id: String = "",
    val createdByUid: String = "",
    val createdByName: String = "",
    val familyId: String = "",
    val memberUids: List<String> = emptyList(),
    val childName: String = "",
    val category: String = "OTHER", // MISSED_EXCHANGE, LATE_PICKUP, MISSED_PARENTING_TIME, CANCELED_VISIT, COMMUNICATION_DISAGREEMENT, EXPENSE_DISPUTE, OTHER
    val title: String = "",
    val description: String = "",
    val incidentDateIso: String = "",
    val incidentTime: String = "",
    val attachmentType: String = "NONE", // NONE, PHOTO, PDF_DOCUMENT, SCREENSHOT
    val attachmentName: String = "",
    val attachmentUri: String = "",
    val originalFileHash: String = "sha256_none",
    val verificationStatus: String = "USER_REPORTED", // USER_REPORTED, MUTUALLY_CONFIRMED, DISPUTED
    val disputeResponse: String = "",
    val version: Long = 1L,
    val auditHistorySummary: String = "",
    val isArchived: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "createdByUid" to createdByUid,
            "createdByName" to createdByName,
            "familyId" to familyId,
            "memberUids" to memberUids,
            "childName" to childName,
            "category" to category,
            "title" to title,
            "description" to description,
            "incidentDateIso" to incidentDateIso,
            "incidentTime" to incidentTime,
            "attachmentType" to attachmentType,
            "attachmentName" to attachmentName,
            "attachmentUri" to attachmentUri,
            "originalFileHash" to originalFileHash,
            "verificationStatus" to verificationStatus,
            "disputeResponse" to disputeResponse,
            "version" to version,
            "auditHistorySummary" to auditHistorySummary,
            "isArchived" to isArchived,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@IgnoreExtraProperties
data class SharedExpense(
    @DocumentId val id: String = "",
    val createdByUid: String = "",
    val createdByName: String = "",
    val familyId: String = "",
    val memberUids: List<String> = emptyList(),
    val childName: String = "",
    val category: String = "OTHER", // SCHOOL_FEES, MEDICAL_BILLS, CHILDCARE, CLOTHING, FOOD, ACTIVITIES, TRANSPORTATION, OTHER
    val title: String = "",
    val totalAmountCents: Long = 0L,
    val currency: String = "USD", // USD, GBP
    val creatorSharePercent: Long = 50L,
    val coParentShareCents: Long = 0L,
    val dateIso: String = "",
    val receiptName: String = "",
    val receiptUri: String = "",
    val paymentStatus: String = "UNPAID_REQUESTED", // UNPAID_REQUESTED, PAID, DISPUTED
    val statusNote: String = "",
    val paidAtIso: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "createdByUid" to createdByUid,
            "createdByName" to createdByName,
            "familyId" to familyId,
            "memberUids" to memberUids,
            "childName" to childName,
            "category" to category,
            "title" to title,
            "totalAmountCents" to totalAmountCents,
            "currency" to currency,
            "creatorSharePercent" to creatorSharePercent,
            "coParentShareCents" to coParentShareCents,
            "dateIso" to dateIso,
            "receiptName" to receiptName,
            "receiptUri" to receiptUri,
            "paymentStatus" to paymentStatus,
            "statusNote" to statusNote,
            "paidAtIso" to paidAtIso,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@IgnoreExtraProperties
data class CoParentMessage(
    @DocumentId val id: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val familyId: String = "",
    val memberUids: List<String> = emptyList(),
    val body: String = "",
    val originalBody: String = "",
    val attachmentName: String = "",
    val attachmentUri: String = "",
    val isEdited: Boolean = false,
    val isRetracted: Boolean = false,
    val auditNote: String = "",
    val readByUids: List<String> = emptyList(),
    val toneAssisted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "senderUid" to senderUid,
            "senderName" to senderName,
            "familyId" to familyId,
            "memberUids" to memberUids,
            "body" to body,
            "originalBody" to originalBody,
            "attachmentName" to attachmentName,
            "attachmentUri" to attachmentUri,
            "isEdited" to isEdited,
            "isRetracted" to isRetracted,
            "auditNote" to auditNote,
            "readByUids" to readByUids,
            "toneAssisted" to toneAssisted,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

object ExchangeLatenessCalculator {
    /**
     * Automatically calculates lateness in minutes from scheduledTime (HH:mm) and actualArrivalTime (HH:mm).
     * Returns 0 if on time or early.
     */
    fun calculateLatenessMinutes(scheduledTime: String, actualArrivalTime: String): Long {
        val schedMins = parseMinutesFromMidnight(scheduledTime) ?: return 0L
        val actualMins = parseMinutesFromMidnight(actualArrivalTime) ?: return 0L
        val diff = actualMins - schedMins
        return if (diff > 0) diff.toLong() else 0L
    }

    private fun parseMinutesFromMidnight(hhMm: String): Int? {
        val parts = hhMm.trim().split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }
}
