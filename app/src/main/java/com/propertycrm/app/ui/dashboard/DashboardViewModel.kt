package com.propertycrm.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.propertycrm.app.data.model.CallOutcome
import com.propertycrm.app.data.model.FollowUpWithCustomer
import com.propertycrm.app.data.repository.CustomerRepository
import com.propertycrm.app.data.repository.FollowUpRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val activeCustomersCount: Int = 0,
    val uniqueTodayCustomersCount: Int = 0,
    val totalTodayTasksCount: Int = 0,
    val uniqueOverdueCustomersCount: Int = 0,
    val totalOverdueTasksCount: Int = 0,
    val upcomingTasksCount: Int = 0,
    val completedTasksCount: Int = 0,
    val upcomingSiteVisitsCount: Int = 0,
    val todayFollowUps: List<FollowUpWithCustomer> = emptyList(),
    val isLoading: Boolean = false
)

class DashboardViewModel(
    private val customerRepository: CustomerRepository,
    private val followUpRepository: FollowUpRepository
) : ViewModel() {

    private val countsPart1 = combine(
        customerRepository.countActiveCustomers(),
        followUpRepository.countTodayUniqueCustomers(),
        followUpRepository.countTodayTotalTasks(),
        followUpRepository.countOverdueUniqueCustomers()
    ) { a, b, c, d -> listOf(a, b, c, d) }

    private val countsPart2 = combine(
        followUpRepository.countOverdueTotalTasks(),
        followUpRepository.countUpcomingTasks(),
        followUpRepository.countCompletedTasks(),
        customerRepository.countUpcomingSiteVisits()
    ) { e, f, g, h -> listOf(e, f, g, h) }

    val uiState: StateFlow<DashboardUiState> = combine(
        countsPart1,
        countsPart2,
        followUpRepository.getTodayFollowUps()
    ) { p1, p2, todayList ->
        DashboardUiState(
            activeCustomersCount = p1[0],
            uniqueTodayCustomersCount = p1[1],
            totalTodayTasksCount = p1[2],
            uniqueOverdueCustomersCount = p1[3],
            totalOverdueTasksCount = p2[0],
            upcomingTasksCount = p2[1],
            completedTasksCount = p2[2],
            upcomingSiteVisitsCount = p2[3],
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

    fun recordCallOutcome(
        customerId: Long,
        outcome: CallOutcome,
        notes: String,
        nextDate: Long?,
        nextTime: String?
    ) {
        viewModelScope.launch {
            customerRepository.recordCallOutcome(customerId, outcome, notes, nextDate, nextTime)
        }
    }
}
