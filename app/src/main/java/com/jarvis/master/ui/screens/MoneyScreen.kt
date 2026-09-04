package com.jarvis.master.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jarvis.master.data.db.DailyAggregate
import com.jarvis.master.data.db.Transaction
import com.jarvis.master.data.db.TransactionType
import com.jarvis.master.ui.MoneyViewModel
import com.jarvis.master.ui.ViewModelFactory
import com.jarvis.master.ui.components.EmptyState
import com.jarvis.master.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyScreen(
    navController: NavController,
    viewModel: MoneyViewModel = viewModel(factory = ViewModelFactory(LocalContext.current.applicationContext))
) {
    val state by viewModel.uiState.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Финансы") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить расход")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MiniStat("Доход", Formatters.money(state.monthIncome), Color(0xFF2E7D32), Modifier.weight(1f))
                    MiniStat("Расход", Formatters.money(state.monthExpense), Color(0xFFC62828), Modifier.weight(1f))
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Прибыль за месяц", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            Formatters.money(state.monthProfit),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Сегодня: ${Formatters.money(state.todayIncome)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (state.daily.isNotEmpty()) {
                item {
                    Text("Доход по дням", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    DailyBarChart(state.daily)
                }
            }

            item {
                Text("Операции", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            }
            if (state.transactions.isEmpty()) {
                item { EmptyState("Операций за месяц нет") }
            }
            items(state.transactions, key = { it.id }) { t ->
                TransactionRow(t) { viewModel.deleteTransaction(t) }
            }
        }
    }

    if (showAdd) {
        ExpenseDialog(
            onDismiss = { showAdd = false },
            onAdd = { cat, amt, desc ->
                viewModel.addExpense(cat, amt, desc)
                showAdd = false
            }
        )
    }
}

@Composable
private fun MiniStat(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun DailyBarChart(daily: List<DailyAggregate>) {
    // Группируем по дням, берём только доход
    val byDay = daily.filter { it.type == TransactionType.INCOME.name }
        .groupBy { Formatters.relativeDay(it.date) }
        .map { (day, list) -> day to list.sumOf { it.sum } }
        .sortedByDescending { it.first }
        .take(7)
    val max = byDay.maxOfOrNull { it.second } ?: 1.0
    Row(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        byDay.forEach { (day, value) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((if (max == 0.0) 0 else (value / max) * 100).dp)
                        .background(Color(0xFF1565C0), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                )
                Text(day.take(3), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TransactionRow(t: Transaction, onDelete: () -> Unit) {
    val income = t.typeEnum == TransactionType.INCOME
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (income) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
            contentDescription = null,
            tint = if (income) Color(0xFF2E7D32) else Color(0xFFC62828)
        )
        Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
            Text(t.category, fontWeight = FontWeight.Medium)
            if (t.description.isNotBlank()) {
                Text(t.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            "${if (income) "+" else "−"}${Formatters.money(t.amount)}",
            fontWeight = FontWeight.Bold,
            color = if (income) Color(0xFF2E7D32) else Color(0xFFC62828)
        )
        TextButton(onClick = onDelete) { Text("✕", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun ExpenseDialog(onDismiss: () -> Unit, onAdd: (String, Double, String) -> Unit) {
    var cat by remember { mutableStateOf("Запчасти") }
    var amt by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить расход") },
        text = {
            Column {
                OutlinedTextField(
                    value = cat, onValueChange = { cat = it }, label = { Text("Категория") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = amt,
                    onValueChange = { amt = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Сумма, ₽") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true
                )
                OutlinedTextField(
                    value = desc, onValueChange = { desc = it }, label = { Text("Комментарий") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = amt.toDoubleOrNull()
                if (a != null && a > 0) onAdd(cat, a, desc)
            }) { Text("Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
