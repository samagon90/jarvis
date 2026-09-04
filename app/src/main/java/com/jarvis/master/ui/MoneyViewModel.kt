package com.jarvis.master.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.master.data.RepairRepository
import com.jarvis.master.data.db.DailyAggregate
import com.jarvis.master.data.db.Transaction
import com.jarvis.master.data.db.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class MoneyViewModel(
    private val repository: RepairRepository,
    context: Context
) : ViewModel() {

    private val now = System.currentTimeMillis()
    private val dayStart = startOfDay(now)
    private val dayEnd = now + 24 * 3600_000L
    private val monthStart = startOfMonth(now)

    /** Показатели за месяц: доход, расход, прибыль, разбивка по дням. */
    val uiState: StateFlow<MoneyUi> = combine(
        repository.observeIncomeBetween(monthStart, now),
        repository.observeExpenseBetween(monthStart, now),
        repository.observeIncomeBetween(dayStart, dayEnd),
        repository.observeDaily(monthStart, now),
        repository.observeTransactions()
    ) { income, expense, today, daily, all ->
        MoneyUi(
            monthIncome = income,
            monthExpense = expense,
            monthProfit = income - expense,
            todayIncome = today,
            daily = daily,
            transactions = all.filter { it.date >= monthStart }.sortedByDescending { it.date }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MoneyUi())

    fun addExpense(category: String, amount: Double, description: String) {
        viewModelScope.launch {
            repository.addExpense(category, amount, description)
        }
    }

    fun deleteTransaction(t: Transaction) {
        viewModelScope.launch { repository.deleteTransaction(t) }
    }

    private fun startOfDay(ts: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun startOfMonth(ts: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }
}

data class MoneyUi(
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0,
    val monthProfit: Double = 0.0,
    val todayIncome: Double = 0.0,
    val daily: List<DailyAggregate> = emptyList(),
    val transactions: List<Transaction> = emptyList()
)
