package com.propertycrm.app

import com.google.gson.Gson
import com.propertycrm.app.data.model.BackupData
import com.propertycrm.app.data.model.Customer
import com.propertycrm.app.data.model.CustomerStage
import com.propertycrm.app.data.model.FollowUp
import com.propertycrm.app.data.model.FollowUpStatus
import com.propertycrm.app.data.model.PropertyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BackupDataTest {

    @Test
    fun testJsonSerializationDeserialization() {
        val gson = Gson()
        val customer = Customer(
            id = 1L,
            name = "Ramesh Kumar",
            phone = "9876543210",
            inquiryReason = "Commercial showroom on highway",
            propertyType = PropertyType.COMMERCIAL,
            preferredLocation = "Noida Sector 62",
            budget = "1.5 Cr",
            stage = CustomerStage.SITE_VISIT,
            notes = "Customer is ready for token payment upon inspection"
        )
        val followUp = FollowUp(
            id = 101L,
            customerId = 1L,
            followUpDate = 1730851200000L,
            followUpTime = "11:00 AM",
            notes = "Meeting scheduled at showroom site",
            status = FollowUpStatus.PENDING
        )

        val backup = BackupData(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            appVersion = "1.0.0",
            customers = listOf(customer),
            followUps = listOf(followUp)
        )

        val json = gson.toJson(backup)
        assertNotNull(json)

        val parsed = gson.fromJson(json, BackupData::class.java)
        assertEquals(1, parsed.version)
        assertEquals(1, parsed.customers.size)
        assertEquals("Ramesh Kumar", parsed.customers[0].name)
        assertEquals("9876543210", parsed.customers[0].phone)
        assertEquals(PropertyType.COMMERCIAL, parsed.customers[0].propertyType)
        assertEquals(1, parsed.followUps.size)
        assertEquals("11:00 AM", parsed.followUps[0].followUpTime)
    }
}
