package com.jarvis.master.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jarvis.master.data.db.Repair
import com.jarvis.master.data.db.RepairStatus
import com.jarvis.master.ui.RepairsViewModel
import com.jarvis.master.ui.ViewModelFactory
import com.jarvis.master.ui.components.EmptyState
import com.jarvis.master.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepairsScreen(
    navController: NavController,
    viewModel: RepairsViewModel = viewModel(
        factory = ViewModelFactory(LocalContext.current.applicationContext)
    )
) {
    val repairs by viewModel.repairs.collectAsState()
    val filters by viewModel.filters.collectAsState()
    val query by viewModel.queryState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ремонты") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("repair_edit?repairId=0") }) {
                Icon(Icons.Filled.Add, contentDescription = "Новый ремонт")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Поиск: устройство, IMEI, неисправность") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Фильтры по статусам
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        FilterChip(
                            selected = filters == null,
                            onClick = { viewModel.setStatusFilter(null) },
                            label = { Text("Все") }
                        )
                        RepairStatus.entries.forEach { s ->
                            FilterChip(
                                selected = filters == s,
                                onClick = { viewModel.setStatusFilter(s) },
                                label = { Text(s.label) }
                            )
                        }
                    }
                }

                if (repairs.isEmpty()) {
                    item { EmptyState("Ремонтов пока нет") }
                }
                items(repairs, key = { it.id }) { r ->
                    RepairCard(r) {
                        navController.navigate("repair_detail?repairId=${r.id}")
                    }
                }
            }
        }
    }
}

@Composable
private fun RepairCard(repair: Repair, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    repair.deviceName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(repair.statusEnum)
            }
            if (repair.serialNumber.isNotBlank()) {
                Text(repair.serialNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(repair.issue, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(
                    Formatters.relativeDay(repair.receivedDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (repair.price > 0) {
                    Text(
                        Formatters.money(repair.price),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: RepairStatus) {
    val color = when (status) {
        RepairStatus.NEW -> MaterialTheme.colorScheme.primary
        RepairStatus.DIAGNOSED -> MaterialTheme.colorScheme.secondary
        RepairStatus.IN_WORK -> MaterialTheme.colorScheme.tertiary
        RepairStatus.AWAITING_PART -> MaterialTheme.colorScheme.tertiary
        RepairStatus.READY -> MaterialTheme.colorScheme.secondary
        RepairStatus.ISSUED -> MaterialTheme.colorScheme.primary
        RepairStatus.CANCELLED -> MaterialTheme.colorScheme.error
    }
    Text(
        status.label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier.padding(4.dp)
    )
}
