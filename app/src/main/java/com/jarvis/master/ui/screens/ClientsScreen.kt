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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import com.jarvis.master.data.db.Client
import com.jarvis.master.ui.ClientsViewModel
import com.jarvis.master.ui.ViewModelFactory
import com.jarvis.master.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    navController: NavController,
    viewModel: ClientsViewModel = viewModel(factory = ViewModelFactory(LocalContext.current.applicationContext))
) {
    val clients by viewModel.clients.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Клиенты") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // Быстрое создание клиента через простой диалог не реализовано — открываем экран создания.
                // Для MVP используем навигацию к detail с clientId=0 как режим создания.
                navController.navigate("client_detail?clientId=0")
            }) { Icon(Icons.Filled.Add, contentDescription = "Новый клиент") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (clients.isEmpty()) {
                item { EmptyState("Клиентов пока нет") }
            }
            items(clients, key = { it.id }) { c ->
                ClientCard(c) { navController.navigate("client_detail?clientId=${c.id}") }
            }
        }
    }
}

@Composable
private fun ClientCard(client: Client, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(client.name, fontWeight = FontWeight.Bold)
                if (client.phone.isNotBlank()) {
                    Text(client.phone, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (client.telegram.isNotBlank()) {
                Text("@${client.telegram}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
