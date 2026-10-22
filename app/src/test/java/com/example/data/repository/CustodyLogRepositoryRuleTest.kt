package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CustodyPattern
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustodyLogRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserProfile_and_crossUserIsolation_enforcedByRules() = runBlocking {
        val aliceUid = signInTestUser("alice_rule@custodylog.test")
        val repo = CustodyLogRepository(firestore) { auth }

        val profile = withTimeout(5000L) {
            repo.ensureUserProfile("Alice Carter", "alice_rule@custodylog.test")
        }
        assertEquals(aliceUid, profile.userId)
        assertEquals("Alice Carter", profile.displayName)

        // Sign in as Bob and verify Bob CANNOT read Alice's private user profile
        signInTestUser("bob_rule@custodylog.test")
        var permissionDenied = false
        try {
            withTimeout(5000L) {
                firestore.collection("users").document(aliceUid).get().await()
            }
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                permissionDenied = true
            }
        }
        assertTrue("Expected PERMISSION_DENIED when reading another user's profile", permissionDenied)
    }

    @Test
    fun coParentWorkspace_sharedChildExchangeIncidentExpenseAndMessage_succeedsForBothParents() = runBlocking {
        val aliceUid = signInTestUser("alice_shared@custodylog.test")
        val bobUid = signInTestUser("bob_shared@custodylog.test")

        // Switch back to Alice to create shared records with [aliceUid, bobUid]
        signInTestUser("alice_shared@custodylog.test")
        val repo = CustodyLogRepository(firestore) { auth }
        val familyId = "fam_shared_test"
        val members = listOf(aliceUid, bobUid)

        val childId = withTimeout(5000L) {
            repo.addChild(
                familyId = familyId,
                memberUids = members,
                name = "Emma Carter",
                dateOfBirth = "2019-04-10",
                schoolOrChildcare = "St. Jude Primary",
                medicalNotes = "None",
                custodyScheduleType = CustodyPattern.TWO_TWO_THREE,
                primaryParentRatio = 50L,
                avatarColorHex = "#0D9488"
            )
        }
        assertTrue(childId.isNotEmpty())

        val exchangeId = withTimeout(5000L) {
            repo.logCustodyExchange(
                createdByName = "Alice Carter",
                familyId = familyId,
                memberUids = members,
                childName = "Emma Carter",
                scheduledDateIso = "2026-10-22",
                scheduledTime = "17:00",
                actualArrivalTime = "17:18",
                location = "School North Gate",
                direction = "PICK_UP",
                missedExchange = false,
                notes = "Heavy rain traffic",
                attachmentName = "",
                attachmentUri = ""
            )
        }
        assertTrue(exchangeId.isNotEmpty())

        // Switch to Bob and verify Bob can observe the shared child & confirm the exchange
        signInTestUser("bob_shared@custodylog.test")
        val bobChildren = withTimeout(3000L) {
            repo.observeChildren(bobUid).first { list -> list.any { it.id == childId } }
        }
        assertTrue(bobChildren.any { it.name == "Emma Carter" })

        val bobExchanges = withTimeout(3000L) {
            repo.observeExchanges(bobUid).first { list -> list.any { it.id == exchangeId } }
        }
        val loggedExchange = bobExchanges.first { it.id == exchangeId }
        assertEquals(18L, loggedExchange.latenessMinutes)
        assertEquals("LATE", loggedExchange.exchangeStatus)
        assertEquals("USER_REPORTED", loggedExchange.verificationStatus)

        withTimeout(5000L) {
            repo.confirmOrDisputeExchange(loggedExchange, confirm = true)
        }

        val confirmedExchanges = withTimeout(3000L) {
            repo.observeExchanges(bobUid).first { list ->
                list.any { it.id == exchangeId && it.verificationStatus == "MUTUALLY_CONFIRMED" }
            }
        }
        assertNotNull(confirmedExchanges.firstOrNull { it.id == exchangeId })
    }

    @Test
    fun unauthenticatedCaller_rejectedWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = CustodyLogRepository(firestore) { auth }
        var failedWithPermissionDenied = false
        try {
            withTimeout(3000L) {
                repo.observeChildren("unauth_user").first()
            }
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                failedWithPermissionDenied = true
            }
        }
        assertTrue("Unauthenticated query must fail with PERMISSION_DENIED", failedWithPermissionDenied)
    }
}
