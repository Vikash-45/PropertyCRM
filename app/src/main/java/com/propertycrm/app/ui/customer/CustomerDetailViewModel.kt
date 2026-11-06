package com.propertycrm.app.ui.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.propertycrm.app.data.model.AppointmentStatus
import com.propertycrm.app.data.model.CallOutcome
import com.propertycrm.app.data.model.CustomerInteraction
import com.propertycrm.app.data.model.CustomerStage
import com.propertycrm.app.data.model.CustomerWithFollowUps
import com.propertycrm.app.data.model.InteractionType
import com.propertycrm.app.data.model.SiteVisit
import com.propertycrm.app.data.repository.CustomerRepository
import com.propertycrm.app.data.repository.FollowUpRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class CustomerDetailEvent {
    object CustomerDeleted : CustomerDetailEvent()
}

class CustomerDetailViewModel(
    private val customerId: Long,
    private val customerRepository: CustomerRepository,
    private val followUpRepository: FollowUpRepository
) : ViewModel() {

    private val _customerData = MutableStateFlow<CustomerWithFollowUps?>(null)
    val customerData: StateFlow<CustomerWithFollowUps?> = _customerData.asStateFlow()

    val interactions: StateFlow<List<CustomerInteraction>> =
        customerRepository.getCustomerInteractions(customerId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val siteVisits: StateFlow<List<SiteVisit>> =
        customerRepository.getCustomerSiteVisits(customerId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _eventFlow = MutableSharedFlow<CustomerDetailEvent>()
    val eventFlow: SharedFlow<CustomerDetailEvent> = _eventFlow.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            customerRepository.getCustomerWithFollowUps(customerId).collect {
                _customerData.value = it
            }
        }
    }

    fun updateStage(stage: CustomerStage) {
        viewModelScope.launch {
            customerRepository.updateCustomerStage(customerId, stage)
        }
    }

    fun toggleDoNotCall() {
        val current = _customerData.value?.customer ?: return
        viewModelScope.launch {
            customerRepository.setCustomerDoNotCall(customerId, !current.isDoNotCall)
        }
    }

    fun recordCallOutcome(outcome: CallOutcome, notes: String, nextDate: Long?, nextTime: String?) {
        viewModelScope.launch {
            customerRepository.recordCallOutcome(customerId, outcome, notes, nextDate, nextTime)
        }
    }

    fun scheduleSiteVisit(
        propertyLocation: String,
        visitDate: Long,
        visitTime: String?,
        notes: String,
        onScheduled: ((SiteVisit) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val visit = SiteVisit(
                customerId = customerId,
                propertyLocation = propertyLocation,
                visitDate = visitDate,
                visitTime = visitTime,
                notes = notes,
                status = AppointmentStatus.SCHEDULED
            )
            val id = customerRepository.scheduleSiteVisit(visit)
            onScheduled?.invoke(visit.copy(id = id))
        }
    }

    fun updateNotificationSettings(leadTimeMinutes: Int, enabled: Boolean, notes: String?) {
        viewModelScope.launch {
            customerRepository.updateCustomerNotificationSettings(customerId, leadTimeMinutes, enabled, notes)
        }
    }

    fun saveAiCallSummary(
        notes: String,
        audioPath: String? = null,
        aiSummaryText: String? = null
    ) {
        viewModelScope.launch {
            val interaction = CustomerInteraction(
                customerId = customerId,
                type = InteractionType.AI_CALL_SUMMARY,
                notes = notes,
                audioFilePath = audioPath,
                aiSummary = aiSummaryText,
                timestamp = System.currentTimeMillis()
            )
            customerRepository.recordInteraction(interaction)
        }
    }

    fun updateSiteVisitStatus(visitId: Long, status: AppointmentStatus, notes: String = "") {
        viewModelScope.launch {
            customerRepository.updateSiteVisitStatus(visitId, customerId, status, notes)
        }
    }

    fun addFollowUp(dateMillis: Long, time: String?, notes: String) {
        viewModelScope.launch {
            followUpRepository.addFollowUp(customerId, dateMillis, time, notes)
        }
    }

    fun markFollowUpCompleted(followUpId: Long, notes: String = "") {
        viewModelScope.launch {
            followUpRepository.markCompleted(followUpId, notes)
        }
    }

    fun rescheduleFollowUp(followUpId: Long, newDate: Long, newTime: String?, reason: String) {
        viewModelScope.launch {
            followUpRepository.rescheduleFollowUp(followUpId, newDate, newTime, reason)
        }
    }

    fun toggleArchive() {
        val current = _customerData.value?.customer ?: return
        viewModelScope.launch {
            customerRepository.setCustomerArchived(customerId, !current.isArchived)
        }
    }

    fun deleteCustomer() {
        viewModelScope.launch {
            customerRepository.deleteCustomer(customerId)
            _eventFlow.emit(CustomerDetailEvent.CustomerDeleted)
        }
    }
}
