package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ExchangeLatenessCalculator
import com.example.data.model.SharedExpense
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CustodyLog", appName)
    }

    @Test
    fun `calculate lateness from HH_mm timestamps`() {
        assertEquals(0L, ExchangeLatenessCalculator.calculateLatenessMinutes("17:00", "16:55"))
        assertEquals(0L, ExchangeLatenessCalculator.calculateLatenessMinutes("17:00", "17:00"))
        assertEquals(22L, ExchangeLatenessCalculator.calculateLatenessMinutes("17:00", "17:22"))
        assertEquals(75L, ExchangeLatenessCalculator.calculateLatenessMinutes("09:00", "10:15"))
    }

    @Test
    fun `shared expense serializes split and status fields`() {
        val expense = SharedExpense(
            createdByUid = "uid_parent_1",
            createdByName = "Alex Carter",
            familyId = "fam_123",
            memberUids = listOf("uid_parent_1", "uid_parent_2"),
            childName = "Maya Carter",
            category = "MEDICAL_BILLS",
            title = "Pediatric Dental Visit",
            totalAmountCents = 24000L,
            currency = "USD",
            creatorSharePercent = 60L,
            coParentShareCents = 9600L,
            dateIso = "2026-05-10"
        )
        val map = expense.toCreateMap()
        assertEquals(24000L, map["totalAmountCents"])
        assertEquals(9600L, map["coParentShareCents"])
        assertEquals("UNPAID_REQUESTED", map["paymentStatus"])
    }
}
