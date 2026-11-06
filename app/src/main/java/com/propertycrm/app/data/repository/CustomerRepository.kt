package com.propertycrm.app.data.repository

import com.propertycrm.app.data.local.CustomerDao
import com.propertycrm.app.data.local.FollowUpDao
import com.propertycrm.app.data.model.Customer
import com.propertycrm.app.data.model.CustomerStage
import com.propertycrm.app.data.model.CustomerWithFollowUps
import com.propertycrm.app.data.model.FollowUp
import com.propertycrm.app.data.model.FollowUpStatus
import com.propertycrm.app.util.DateUtils
import com.propertycrm.app.util.PhoneUtils
import kotlinx.coroutines.flow.Flow

class CustomerRepository(
    private val customerDao: CustomerDao,
    private val followUpDao: FollowUpDao
) {

    fun getActiveCustomers(): Flow<List<Customer>> = customerDao.getAllActiveCustomers()

    fun getArchivedCustomers(): Flow<List<Customer>> = customerDao.getAllArchivedCustomers()

    fun getCustomerById(id: Long): Flow<Customer?> = customerDao.getCustomerById(id)

    suspend fun getCustomerByIdSync(id: Long): Customer? = customerDao.getCustomerByIdSync(id)

    fun getCustomerWithFollowUps(id: Long): Flow<CustomerWithFollowUps?> =
        customerDao.getCustomerWithFollowUps(id)

    fun countActiveCustomers(): Flow<Int> = customerDao.countActiveCustomers()

    fun searchCustomers(query: String): Flow<List<Customer>> = customerDao.searchCustomers(query)

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
    }

    suspend fun updateCustomerStage(id: Long, stage: CustomerStage) {
        customerDao.updateStage(id, stage)
    }
}
