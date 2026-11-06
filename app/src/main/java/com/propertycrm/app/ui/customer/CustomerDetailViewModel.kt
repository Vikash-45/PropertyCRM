package com.propertycrm.app.ui.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.propertycrm.app.data.model.CustomerStage
import com.propertycrm.app.data.model.CustomerWithFollowUps
import com.propertycrm.app.data.repository.CustomerRepository
import com.propertycrm.app.data.repository.FollowUpRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
