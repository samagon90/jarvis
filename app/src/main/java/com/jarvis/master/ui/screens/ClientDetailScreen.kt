package com.jarvis.master.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jarvis.master.data.db.Client
import com.jarvis.master.data.db.RepairStatus
import com.jarvis.master.ui.ClientDetailViewModel
import com.jarvis.master.ui.ViewModelFactory
import com.jarvis.master.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    navController: NavController,
    viewModel: ClientDetailViewModel = viewModel(factory = ViewModelFactory(LocalContext.current.applicationContext))
) {
    val state by viewModel.uiState.collectAsState()
    val client = state.client
    val clientId = navController.currentBackStackEntry?.arguments?.getLong("clientId") ?: 0L
    val isNew = clientId == 0L

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var telegram by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(client?.id) {
        if (client != null) {
            name = client.name
            phone = client.phone
            telegram = client.telegram
            note = client.note
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Новый клиент" else client?.name ?: "Клиент") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Имя *") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Телефон") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = telegram, onValueChange = { telegram = it }, label = { Text("Telegram (без @)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Примечание") },
                modifier = Modifier.fillMaxWidth(), minLines = 2)

            Button(
                onClick = {
                    viewModel.save(Client(id = client?.id ?: 0L, name = name, phone = phone,
                        telegram = telegram, note = note)) { navController.popBackStack() }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Сохранить") }

            if (!isNew && state.repairs.isNotEmpty()) {
                Text("Ремонты клиента", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp))
                state.repairs.forEach { r ->
                    Card(
                        onClick = { navController.navigate("repair_detail?repairId=${r.id}") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(r.deviceName, fontWeight = FontWeight.Bold)
                            Text(r.statusEnum.label, style = MaterialTheme.typography.bodySmall)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(Formatters.relativeDay(r.receivedDate), style = MaterialTheme.typography.bodySmall)
                                Text(Formatters.money(r.price), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить клиента?") },
            text = { Text("История ремонтов клиента сохранится, но связь с ним будет удалена.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete { navController.popBackStack() } }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } }
        )
    }
}
