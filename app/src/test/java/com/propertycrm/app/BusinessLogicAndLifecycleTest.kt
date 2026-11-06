package com.propertycrm.app

import com.propertycrm.app.data.model.AppointmentStatus
import com.propertycrm.app.data.model.CallOutcome
import com.propertycrm.app.data.model.Customer
import com.propertycrm.app.data.model.CustomerInteraction
import com.propertycrm.app.data.model.CustomerStage
import com.propertycrm.app.data.model.FollowUp
import com.propertycrm.app.data.model.FollowUpStatus
import com.propertycrm.app.data.model.InteractionType
import com.propertycrm.app.data.model.PropertyType
import com.propertycrm.app.data.model.RequirementType
import com.propertycrm.app.data.model.SiteVisit
import com.propertycrm.app.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessLogicAndLifecycleTest {

    @Test
    fun testDoNotCallExclusionLogic() {
        val activeCustomer = Customer(
            id = 1L,
            name = "Anil Sharma",
            phone = "9876500001",
            inquiryReason = "Plot in Sector 150",
            isDoNotCall = false,
            stage = CustomerStage.CONTACTED
        )

        val dncCustomer = Customer(
            id = 2L,
            name = "Pooja Verma",
            phone = "9876500002",
            inquiryReason = "Flat in Greater Noida",
            isDoNotCall = true,
            stage = CustomerStage.NEW
        )

        // Helper filter simulating SQL WHERE c.is_do_not_call = 0 AND c.stage NOT IN ('WON', 'LOST')
        fun isEligibleForCalling(c: Customer): Boolean {
            return !c.isArchived && !c.isDoNotCall && c.stage != CustomerStage.WON && c.stage != CustomerStage.LOST
        }

        assertTrue("Active customer should be eligible for reminders", isEligibleForCalling(activeCustomer))
        assertFalse("DNC customer must be excluded from reminders", isEligibleForCalling(dncCustomer))
    }

    @Test
    fun testWonAndLostExclusionLogic() {
        val wonCustomer = Customer(
            id = 3L,
            name = "Sunil Gupta",
            phone = "9876500003",
            inquiryReason = "Villa registered",
            stage = CustomerStage.WON
        )

        val lostCustomer = Customer(
            id = 4L,
            name = "Ravi Mehta",
            phone = "9876500004",
            inquiryReason = "Budget mismatched",
            stage = CustomerStage.LOST
        )

        fun isEligibleForCalling(c: Customer): Boolean {
            return !c.isArchived && !c.isDoNotCall && c.stage != CustomerStage.WON && c.stage != CustomerStage.LOST
        }

        assertFalse("Won lead should be excluded from calling reminders", isEligibleForCalling(wonCustomer))
        assertFalse("Lost lead should be excluded from calling reminders", isEligibleForCalling(lostCustomer))
    }

    @Test
    fun testMultipleFollowUpsAndUniqueCounts() {
        // Customer 1 has 2 follow-up tasks due today
        val task1 = FollowUp(id = 1L, customerId = 101L, followUpDate = DateUtils.getStartOfDay(), status = FollowUpStatus.PENDING)
        val task2 = FollowUp(id = 2L, customerId = 101L, followUpDate = DateUtils.getStartOfDay(), status = FollowUpStatus.PENDING)

        // Customer 2 has 1 follow-up task due today
        val task3 = FollowUp(id = 3L, customerId = 102L, followUpDate = DateUtils.getStartOfDay(), status = FollowUpStatus.PENDING)

        val todayTasks = listOf(task1, task2, task3)

        val totalTasksCount = todayTasks.size
        val uniqueCustomersCount = todayTasks.map { it.customerId }.distinct().size

        assertEquals(3, totalTasksCount)
        assertEquals(2, uniqueCustomersCount)
    }

    @Test
    fun testReschedulingLifecyclePreservesHistory() {
        val initialFollowUp = FollowUp(
            id = 10L,
            customerId = 50L,
            followUpDate = 1730851200000L,
            notes = "First follow-up",
            status = FollowUpStatus.PENDING
        )

        // Simulating reschedule: old task marked CANCELLED with note, new task created with PENDING
        val rescheduledOldTask = initialFollowUp.copy(
            status = FollowUpStatus.CANCELLED,
            notes = "Rescheduled: Customer asked for weekend morning call"
        )

        val newPendingTask = FollowUp(
            id = 11L,
            customerId = 50L,
            followUpDate = 1731024000000L,
            notes = "Customer asked for weekend morning call",
            status = FollowUpStatus.PENDING
        )

        // Verify history integrity
        assertEquals(FollowUpStatus.CANCELLED, rescheduledOldTask.status)
        assertTrue(rescheduledOldTask.notes.contains("Rescheduled"))
        assertEquals(FollowUpStatus.PENDING, newPendingTask.status)
        assertEquals(initialFollowUp.customerId, newPendingTask.customerId)
        assertNotNull(rescheduledOldTask)
    }

    @Test
    fun testMultipleSiteVisitsPerCustomer() {
        val visit1 = SiteVisit(
            id = 1L,
            customerId = 200L,
            propertyLocation = "Sector 137, Noida",
            visitDate = DateUtils.getStartOfDay(),
            visitTime = "11:00 AM",
            status = AppointmentStatus.COMPLETED
        )

        val visit2 = SiteVisit(
            id = 2L,
            customerId = 200L,
            propertyLocation = "Sector 143, Noida",
            visitDate = DateUtils.getStartOfDay() + 86400000L,
            visitTime = "04:30 PM",
            status = AppointmentStatus.SCHEDULED
        )

        val visits = listOf(visit1, visit2)
        assertEquals(2, visits.size)
        assertEquals(AppointmentStatus.COMPLETED, visits[0].status)
        assertEquals(AppointmentStatus.SCHEDULED, visits[1].status)
    }

    @Test
    fun testCallOutcomeRecording() {
        val interaction = CustomerInteraction(
            id = 1L,
            customerId = 300L,
            type = InteractionType.CALL,
            callOutcome = CallOutcome.SITE_VISIT_REQUESTED,
            notes = "Customer liked pictures of corner plot, requested site inspection tomorrow",
            timestamp = System.currentTimeMillis()
        )

        assertEquals(InteractionType.CALL, interaction.type)
        assertEquals(CallOutcome.SITE_VISIT_REQUESTED, interaction.callOutcome)
        assertTrue(interaction.notes.contains("corner plot"))
    }
}
