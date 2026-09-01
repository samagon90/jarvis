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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.jarvis.master.data.db.Part
import com.jarvis.master.ui.PartsViewModel
import com.jarvis.master.ui.ViewModelFactory
import com.jarvis.master.ui.components.EmptyState
import com.jarvis.master.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartsScreen(
    navController: NavController,
    viewModel: PartsViewModel = viewModel(factory = ViewModelFactory(LocalContext.current.applicationContext))
) {
    val parts by viewModel.parts.collectAsState()
    val lowStock by viewModel.lowStock.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Запчасти") },
                actions = {
                    IconButton(onClick = { viewModel.exportCsv() }) {
                        Icon(Icons.Filled.FileDownload, contentDescription = "Экспорт CSV")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("part_edit?partId=0") }) {
                Icon(Icons.Filled.Add, contentDescription = "Новая запчасть")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (lowStock.isNotEmpty()) {
                Text(
                    "Заканчивается: ${lowStock.size}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (parts.isEmpty()) {
                    item { EmptyState("Запчастей нет. Добавьте склад.") }
                }
                items(parts, key = { it.id }) { p ->
                    PartCard(p, lowStock.any { it.id == p.id }) {
                        navController.navigate("part_edit?partId=${p.id}")
                    }
                }
            }
        }
    }
}

@Composable
private fun PartCard(part: Part, isLow: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isLow) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(part.name, fontWeight = FontWeight.Bold)
                Text(
                    "${part.category}${if (part.barcode.isNotBlank()) " · " + part.barcode else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (part.location.isNotBlank()) {
                    Text("Место: ${part.location}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Остаток: ${part.quantity} шт.",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                if (part.sellPrice > 0) {
                    Text(Formatters.money(part.sellPrice), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
