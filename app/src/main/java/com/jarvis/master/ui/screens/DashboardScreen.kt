package com.jarvis.master.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jarvis.master.ui.DashboardViewModel
import com.jarvis.master.ui.ViewModelFactory
import com.jarvis.master.ui.components.SectionTitle
import com.jarvis.master.ui.components.StatCard
import com.jarvis.master.ui.theme.Amber
import com.jarvis.master.ui.theme.CyanAccent
import com.jarvis.master.ui.theme.Green
import com.jarvis.master.ui.theme.Red
import com.jarvis.master.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = viewModel(
        factory = ViewModelFactory(LocalContext.current.applicationContext)
    )
) {
    val stats by viewModel.stats.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Главная") },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Настройки")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Сводка за неделю",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Финансы
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Доход", Formatters.money(stats.weekIncome), Icons.Filled.ArrowUpward, Green, Modifier.weight(1f))
                    StatCard("Прибыль", Formatters.money(stats.weekProfit), Icons.Filled.Money, CyanAccent, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Сегодня", Formatters.money(stats.todayIncome), Icons.Filled.ArrowUpward, CyanAccent, Modifier.weight(1f))
                    StatCard("Долг", Formatters.money(stats.totalDebt), Icons.Filled.Money, Red, Modifier.weight(1f))
                }
            }

            // Ремонты
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("В работе", stats.inProgress.toString(), Icons.Filled.Build, BlueCard, Modifier.weight(1f))
                    StatCard("Всего", stats.totalRepairs.toString(), Icons.Filled.Info, Color.DarkGray, Modifier.weight(1f))
                }
            }
            item {
                StatCard("Готовы к выдаче", stats.ready.toString(), Icons.Filled.ShoppingCart, Amber)
            }

            // Готовые, но не выданные (ждём клиента)
            if (stats.readyOverdue.isNotEmpty()) {
                item { SectionTitle("Ждут выдачи") }
                stats.readyOverdue.forEach { r ->
                    item {
                        AlertCard(
                            color = Amber,
                            title = r.deviceName,
                            subtitle = "Готов с ${Formatters.date(r.readyDate ?: 0)}"
                        )
                    }
                }
            }

            // Заканчивающиеся запчасти
            if (stats.lowStockCount > 0) {
                item { SectionTitle("Запчасти заканчиваются") }
                stats.lowStockParts.forEach { p ->
                    item {
                        AlertCard(
                            color = Red,
                            title = p.name,
                            subtitle = "Осталось ${p.quantity} шт."
                        )
                    }
                }
            }

            if (stats.readyOverdue.isEmpty() && stats.lowStockCount == 0) {
                item {
                    Text(
                        "Всё в порядке ✅\nГотовых к выдаче нет, склад укомплектован.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            }
        }
    }
}

private val BlueCard = Color(0xFF1565C0)

@Composable
private fun AlertCard(color: Color, title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, RoundedCornerShape(5.dp))
            )
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
