package com.propertycrm.app.ui.followup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.propertycrm.app.data.model.FollowUpWithCustomer
import com.propertycrm.app.data.repository.FollowUpRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FollowUpFilterTab {
    TODAY,
    OVERDUE,
    UPCOMING,
    COMPLETED,
    ALL
}

class FollowUpViewModel(
    private val followUpRepository: FollowUpRepository,
    initialFilter: String = "ALL"
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(
        try {
            FollowUpFilterTab.valueOf(initialFilter.uppercase())
        } catch (e: Exception) {
            FollowUpFilterTab.ALL
        }
    )
    val selectedFilter: StateFlow<FollowUpFilterTab> = _selectedFilter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val followUps: StateFlow<List<FollowUpWithCustomer>> = _selectedFilter.flatMapLatest { filter ->
        when (filter) {
            FollowUpFilterTab.TODAY -> followUpRepository.getTodayFollowUps()
            FollowUpFilterTab.OVERDUE -> followUpRepository.getOverdueFollowUps()
            FollowUpFilterTab.UPCOMING -> followUpRepository.getUpcomingFollowUps()
            FollowUpFilterTab.COMPLETED -> followUpRepository.getCompletedFollowUps()
            FollowUpFilterTab.ALL -> followUpRepository.getAllFollowUps()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setFilter(filter: FollowUpFilterTab) {
        _selectedFilter.value = filter
    }

    fun markCompleted(id: Long, notes: String = "") {
        viewModelScope.launch {
            followUpRepository.markCompleted(id, notes)
        }
    }

    fun reschedule(id: Long, newDate: Long, newTime: String?, reason: String) {
        viewModelScope.launch {
            followUpRepository.rescheduleFollowUp(id, newDate, newTime, reason)
        }
    }

    fun addFollowUp(customerId: Long, date: Long, time: String?, notes: String) {
        viewModelScope.launch {
            followUpRepository.addFollowUp(customerId, date, time, notes)
        }
    }
}
