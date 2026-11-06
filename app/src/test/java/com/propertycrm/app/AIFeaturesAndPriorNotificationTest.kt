package com.propertycrm.app

import com.propertycrm.app.ai.CallSummaryResult
import com.propertycrm.app.ai.ExtractedCustomerCandidate
import com.propertycrm.app.data.model.Customer
import com.propertycrm.app.data.model.CustomerInteraction
import com.propertycrm.app.data.model.CustomerStage
import com.propertycrm.app.data.model.InteractionType
import com.propertycrm.app.data.model.PropertyType
import com.propertycrm.app.data.model.RequirementType
import com.propertycrm.app.data.model.SiteVisit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AIFeaturesAndPriorNotificationTest {

    @Test
    fun testExtractedCustomerCandidateMapping() {
        val candidate = ExtractedCustomerCandidate(
            name = "Rajesh Sharma",
            phone = "+91 9811223344",
            requirementType = RequirementType.BUY,
            propertyType = PropertyType.FLAT,
            preferredLocation = "Noida Expressway",
            budget = "95 Lakhs",
            inquiryReason = "3BHK high-rise apartment near metro",
            stage = CustomerStage.NEW,
            notes = "Auto-imported lead from document"
        )

        assertEquals("Rajesh Sharma", candidate.name)
        assertEquals("+91 9811223344", candidate.phone)
        assertEquals(RequirementType.BUY, candidate.requirementType)
        assertEquals(PropertyType.FLAT, candidate.propertyType)
        assertEquals("95 Lakhs", candidate.budget)
    }

    @Test
    fun testCallSummaryResultDataStructure() {
        val summary = CallSummaryResult(
            summaryText = "Client discussed 4BHK duplex options. Wants ready-to-move inventory with covered parking.",
            sentiment = "POSITIVE",
            agreedBudget = "2.5 Cr",
            objections = "Requires loan approval from SBI",
            actionItems = listOf("Share brochure", "Schedule site visit on Sunday"),
            suggestedFollowUpDays = 2
        )

        assertEquals("POSITIVE", summary.sentiment)
        assertEquals("2.5 Cr", summary.agreedBudget)
        assertEquals(2, summary.actionItems.size)
        assertEquals(2, summary.suggestedFollowUpDays)
    }

    @Test
    fun testCustomerCustomNotificationSettingsDefaults() {
        val customer = Customer(
            id = 100L,
            name = "Deepak Verma",
            phone = "9876543210",
            inquiryReason = "Commercial land near Jewar airport"
        )

        // Verify default notification lead time is 60 minutes
        assertEquals(60, customer.reminderLeadTimeMinutes)
        assertTrue("Notifications should be enabled by default", customer.notificationsEnabled)
        assertEquals(null, customer.customNotificationNotes)
    }

    @Test
    fun testPriorVisitAlertTimeComputation() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 10, 15, 0, 0) // Nov 10, 2026, 3:00 PM
            set(Calendar.MILLISECOND, 0)
        }
        val visitTimestamp = cal.timeInMillis

        val leadTimeMinutes = 30
        val alertTriggerMillis = visitTimestamp - (leadTimeMinutes * 60 * 1000L)

        val alertCal = Calendar.getInstance().apply { timeInMillis = alertTriggerMillis }
        assertEquals(14, alertCal.get(Calendar.HOUR_OF_DAY)) // 2:30 PM
        assertEquals(30, alertCal.get(Calendar.MINUTE))
    }

    @Test
    fun testCustomerInteractionAICallSummaryType() {
        val interaction = CustomerInteraction(
            id = 50L,
            customerId = 100L,
            type = InteractionType.AI_CALL_SUMMARY,
            notes = "AI Call Summary: Client confirmed budget of 1.2 Cr.",
            audioFilePath = "/data/user/0/com.propertycrm.app/files/call_recordings/call_100_test.m4a",
            aiSummary = "Client confirmed budget of 1.2 Cr.",
            timestamp = System.currentTimeMillis()
        )

        assertEquals(InteractionType.AI_CALL_SUMMARY, interaction.type)
        assertEquals("AI Call Summary", interaction.type.getDisplayName())
        assertNotNull(interaction.audioFilePath)
        assertNotNull(interaction.aiSummary)
    }
}
