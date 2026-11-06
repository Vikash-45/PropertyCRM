package com.propertycrm.app.data.model

data class BackupData(
    val version: Int = 2,
    val exportedAt: Long = System.currentTimeMillis(),
    val appVersion: String = "1.1.0",
    val customers: List<Customer> = emptyList(),
    val followUps: List<FollowUp> = emptyList(),
    val siteVisits: List<SiteVisit> = emptyList(),
    val interactions: List<CustomerInteraction> = emptyList()
)

data class ImportResult(
    val success: Boolean,
    val customersImported: Int = 0,
    val followUpsImported: Int = 0,
    val siteVisitsImported: Int = 0,
    val interactionsImported: Int = 0,
    val customersSkipped: Int = 0,
    val errorMessage: String? = null
)
