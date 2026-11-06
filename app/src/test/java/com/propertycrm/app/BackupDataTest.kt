package com.propertycrm.app

import com.google.gson.Gson
import com.propertycrm.app.data.model.AppointmentStatus
import com.propertycrm.app.data.model.BackupData
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BackupDataTest {

    @Test
    fun testJsonSerializationDeserializationV2() {
        val gson = Gson()
        val customer = Customer(
            id = 1L,
            name = "Ramesh Kumar",
            phone = "9876543210",
            inquiryReason = "Commercial showroom on highway",
            propertyType = PropertyType.COMMERCIAL,
            requirementType = RequirementType.BUY,
            preferredLocation = "Noida Sector 62",
            budget = "1.5 Cr",
            stage = CustomerStage.SITE_VISIT,
            notes = "Customer is ready for token payment upon inspection",
            isDoNotCall = false
        )
        val followUp = FollowUp(
            id = 101L,
            customerId = 1L,
            followUpDate = 1730851200000L,
            followUpTime = "11:00 AM",
            notes = "Meeting scheduled at showroom site",
            status = FollowUpStatus.PENDING
        )
        val siteVisit = SiteVisit(
            id = 201L,
            customerId = 1L,
            propertyLocation = "Plot 42, Highway commercial corridor",
            visitDate = 1730851200000L,
            visitTime = "03:00 PM",
            status = AppointmentStatus.SCHEDULED,
            notes = "Owner will also be present"
        )
        val interaction = CustomerInteraction(
            id = 301L,
            customerId = 1L,
            type = InteractionType.CALL,
            callOutcome = CallOutcome.INTERESTED,
            notes = "Positive response from customer"
        )

        val backup = BackupData(
            version = 2,
            exportedAt = System.currentTimeMillis(),
            appVersion = "1.1.0",
            customers = listOf(customer),
            followUps = listOf(followUp),
            siteVisits = listOf(siteVisit),
            interactions = listOf(interaction)
        )

        val json = gson.toJson(backup)
        assertNotNull(json)

        val parsed = gson.fromJson(json, BackupData::class.java)
        assertEquals(2, parsed.version)
        assertEquals(1, parsed.customers.size)
        assertEquals("Ramesh Kumar", parsed.customers[0].name)
        assertEquals("9876543210", parsed.customers[0].phone)
        assertEquals(PropertyType.COMMERCIAL, parsed.customers[0].propertyType)
        assertEquals(RequirementType.BUY, parsed.customers[0].requirementType)
        assertEquals(1, parsed.followUps.size)
        assertEquals(1, parsed.siteVisits.size)
        assertEquals("Plot 42, Highway commercial corridor", parsed.siteVisits[0].propertyLocation)
        assertEquals(1, parsed.interactions.size)
        assertEquals(CallOutcome.INTERESTED, parsed.interactions[0].callOutcome)
    }
}
