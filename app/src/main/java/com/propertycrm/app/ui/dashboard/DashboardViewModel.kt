package com.propertycrm.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.propertycrm.app.data.model.FollowUpWithCustomer
import com.propertycrm.app.data.repository.CustomerRepository
import com.propertycrm.app.data.repository.FollowUpRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val activeCustomersCount: Int = 0,
    val todayDueCount: Int = 0,
    val overdueCount: Int = 0,
    val upcomingCount: Int = 0,
    val completedCount: Int = 0,
    val todayFollowUps: List<FollowUpWithCustomer> = emptyList(),
    val isLoading: Boolean = false
)

class DashboardViewModel(
    private val customerRepository: CustomerRepository,
    private val followUpRepository: FollowUpRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        customerRepository.countActiveCustomers(),
        followUpRepository.countTodayPending(),
        followUpRepository.countOverdue(),
        followUpRepository.countUpcoming(),
        followUpRepository.countCompleted(),
        followUpRepository.getTodayFollowUps()
    ) { activeCust, todayDue, overdue, upcoming, completed, todayList ->
        DashboardUiState(
            activeCustomersCount = activeCust,
            todayDueCount = todayDue,
            overdueCount = overdue,
            upcomingCount = upcoming,
            completedCount = completed,
            todayFollowUps = todayList,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun markFollowUpCompleted(id: Long, notes: String = "") {
        viewModelScope.launch {
            followUpRepository.markCompleted(id, notes)
        }
    }

    fun rescheduleFollowUp(id: Long, newDate: Long, newTime: String?, reason: String) {
        viewModelScope.launch {
            followUpRepository.rescheduleFollowUp(id, newDate, newTime, reason)
        }
    }
}
