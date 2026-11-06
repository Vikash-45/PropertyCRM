package com.propertycrm.app.data.repository

import com.propertycrm.app.data.local.CustomerDao
import com.propertycrm.app.data.local.CustomerInteractionDao
import com.propertycrm.app.data.local.FollowUpDao
import com.propertycrm.app.data.local.SiteVisitDao
import com.propertycrm.app.data.model.AppointmentStatus
import com.propertycrm.app.data.model.CallOutcome
import com.propertycrm.app.data.model.Customer
import com.propertycrm.app.data.model.CustomerInteraction
import com.propertycrm.app.data.model.CustomerStage
import com.propertycrm.app.data.model.CustomerWithFollowUps
import com.propertycrm.app.data.model.FollowUp
import com.propertycrm.app.data.model.FollowUpStatus
import com.propertycrm.app.data.model.InteractionType
import com.propertycrm.app.data.model.PropertyType
import com.propertycrm.app.data.model.SiteVisit
import com.propertycrm.app.data.model.SiteVisitWithCustomer
import com.propertycrm.app.util.DateUtils
import com.propertycrm.app.util.PhoneUtils
import kotlinx.coroutines.flow.Flow

class CustomerRepository(
    private val customerDao: CustomerDao,
    private val followUpDao: FollowUpDao,
    private val siteVisitDao: SiteVisitDao,
    private val customerInteractionDao: CustomerInteractionDao
) {

    fun getActiveCustomers(): Flow<List<Customer>> = customerDao.getAllActiveCustomers()

    fun getArchivedCustomers(): Flow<List<Customer>> = customerDao.getAllArchivedCustomers()

    fun getCustomerById(id: Long): Flow<Customer?> = customerDao.getCustomerById(id)

    suspend fun getCustomerByIdSync(id: Long): Customer? = customerDao.getCustomerByIdSync(id)

    fun getCustomerWithFollowUps(id: Long): Flow<CustomerWithFollowUps?> =
        customerDao.getCustomerWithFollowUps(id)

    fun countActiveCustomers(): Flow<Int> = customerDao.countActiveCustomers()

    fun searchCustomers(query: String): Flow<List<Customer>> = customerDao.searchCustomers(query)

    fun filterCustomers(
        query: String = "",
        stage: CustomerStage? = null,
        propType: PropertyType? = null,
        location: String = ""
    ): Flow<List<Customer>> = customerDao.filterCustomers(query.trim(), stage, propType, location.trim())

    fun getCustomerInteractions(customerId: Long): Flow<List<CustomerInteraction>> =
        customerInteractionDao.getInteractionsForCustomer(customerId)

    fun getCustomerSiteVisits(customerId: Long): Flow<List<SiteVisit>> =
        siteVisitDao.getSiteVisitsForCustomer(customerId)

    fun getUpcomingSiteVisits(): Flow<List<SiteVisitWithCustomer>> =
        siteVisitDao.getUpcomingSiteVisits(DateUtils.getStartOfDay())

    fun countUpcomingSiteVisits(): Flow<Int> =
        siteVisitDao.countUpcomingSiteVisitsFlow(DateUtils.getStartOfDay())

    suspend fun checkForDuplicatePhone(phone: String, currentCustomerId: Long? = null): Boolean {
        if (!PhoneUtils.isValidPhone(phone)) return false
        val matches = customerDao.findCustomersByPhone(phone.trim())
        return matches.any { it.id != currentCustomerId }
    }

    suspend fun saveCustomerWithInitialFollowUp(
        customer: Customer,
        initialFollowUpDate: Long?,
        initialFollowUpTime: String?,
        initialFollowUpNotes: String?
    ): Long {
        val customerId = customerDao.insertCustomer(customer)

        // Log creation interaction
        customerInteractionDao.insertInteraction(
            CustomerInteraction(
                customerId = customerId,
                type = InteractionType.SYSTEM,
                notes = "Customer registered with initial stage: ${customer.stage.getDisplayName()}"
            )
        )

        // Initial follow-up
        if (initialFollowUpDate != null && initialFollowUpDate > 0L) {
            val followUp = FollowUp(
                customerId = customerId,
                followUpDate = DateUtils.getStartOfDay(initialFollowUpDate),
                followUpTime = initialFollowUpTime?.takeIf { it.isNotBlank() },
                notes = initialFollowUpNotes?.trim() ?: "Initial follow-up",
                status = FollowUpStatus.PENDING,
                createdAt = System.currentTimeMillis()
            )
            followUpDao.insertFollowUp(followUp)

            customerInteractionDao.insertInteraction(
                CustomerInteraction(
                    customerId = customerId,
                    type = InteractionType.FOLLOW_UP_CREATED,
                    notes = "Scheduled initial follow-up for ${DateUtils.formatDate(initialFollowUpDate)}"
                )
            )
        }
        return customerId
    }

    suspend fun updateCustomer(customer: Customer) {
        val updated = customer.copy(updatedAt = System.currentTimeMillis())
        customerDao.updateCustomer(updated)
    }

    suspend fun deleteCustomer(id: Long) {
        customerDao.deleteCustomerById(id)
    }

    suspend fun setCustomerArchived(id: Long, archived: Boolean) {
        customerDao.setArchived(id, archived)
        customerInteractionDao.insertInteraction(
            CustomerInteraction(
                customerId = id,
                type = InteractionType.SYSTEM,
                notes = if (archived) "Customer archived" else "Customer unarchived and reactivated"
            )
        )
    }

    suspend fun setCustomerDoNotCall(id: Long, doNotCall: Boolean) {
        customerDao.setDoNotCall(id, doNotCall)
        customerInteractionDao.insertInteraction(
            CustomerInteraction(
                customerId = id,
                type = InteractionType.DO_NOT_CALL_TOGGLED,
                notes = if (doNotCall) "Marked as Do Not Call (DNC). Removed from calling reminders." else "Do Not Call status removed. Normal follow-ups resumed."
            )
        )
    }

    suspend fun updateCustomerStage(id: Long, stage: CustomerStage) {
        customerDao.updateStage(id, stage)
        customerInteractionDao.insertInteraction(
            CustomerInteraction(
                customerId = id,
                type = InteractionType.STAGE_CHANGE,
                notes = "Customer stage updated to ${stage.getDisplayName()}"
            )
        )
    }

    // Call outcome recording (Item 2)
    suspend fun recordCallOutcome(
        customerId: Long,
        outcome: CallOutcome,
        notes: String,
        nextFollowUpDate: Long? = null,
        nextFollowUpTime: String? = null
    ) {
        // 1. Log immutable interaction
        customerInteractionDao.insertInteraction(
            CustomerInteraction(
                customerId = customerId,
                type = InteractionType.CALL,
                callOutcome = outcome,
                notes = notes.trim(),
                timestamp = System.currentTimeMillis()
            )
        )

        // 2. Schedule next follow up if requested
        if (nextFollowUpDate != null && nextFollowUpDate > 0L) {
            val fu = FollowUp(
                customerId = customerId,
                followUpDate = DateUtils.getStartOfDay(nextFollowUpDate),
                followUpTime = nextFollowUpTime?.takeIf { it.isNotBlank() },
                notes = "Scheduled after ${outcome.getDisplayName()} call: $notes".trim(),
                status = FollowUpStatus.PENDING,
                createdAt = System.currentTimeMillis()
            )
            followUpDao.insertFollowUp(fu)
        }
    }

    // Site Visit / Appointment Management (Item 1)
    suspend fun scheduleSiteVisit(
        customerId: Long,
        propertyLocation: String,
        visitDate: Long,
        visitTime: String?,
        notes: String
    ): Long {
        val visit = SiteVisit(
            customerId = customerId,
            propertyLocation = propertyLocation.trim(),
            visitDate = DateUtils.getStartOfDay(visitDate),
            visitTime = visitTime?.takeIf { it.isNotBlank() },
            status = AppointmentStatus.SCHEDULED,
            notes = notes.trim(),
            createdAt = System.currentTimeMillis()
        )
        val visitId = siteVisitDao.insertSiteVisit(visit)

        customerInteractionDao.insertInteraction(
            CustomerInteraction(
                customerId = customerId,
                type = InteractionType.SITE_VISIT,
                notes = "Scheduled site visit at $propertyLocation on ${DateUtils.formatDate(visitDate)} ${visitTime ?: ""}".trim()
            )
        )
        return visitId
    }

    suspend fun updateSiteVisitStatus(
        id: Long,
        customerId: Long,
        status: AppointmentStatus,
        notes: String = ""
    ) {
        siteVisitDao.updateStatus(id, status, notes)
        customerInteractionDao.insertInteraction(
            CustomerInteraction(
                customerId = customerId,
                type = InteractionType.SITE_VISIT,
                notes = "Site visit marked as ${status.getDisplayName()}${if (notes.isNotBlank()) ": $notes" else ""}"
            )
        )
    }
}
