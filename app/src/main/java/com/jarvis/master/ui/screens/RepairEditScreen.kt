package com.jarvis.master.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jarvis.master.data.db.RepairStatus
import com.jarvis.master.ui.RepairEditViewModel
import com.jarvis.master.ui.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepairEditScreen(
    navController: NavController,
    viewModel: RepairEditViewModel = viewModel(
        factory = ViewModelFactory(LocalContext.current.applicationContext)
    )
) {
    val form by viewModel.form.collectAsState()
    val clients by viewModel.clients.collectAsState()
    var expandedClient by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (form.deviceName.isBlank() && form.issue.isBlank() && form.serialNumber.isBlank()) "Новый ремонт" else "Редактирование") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
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
            OutlinedTextField(
                value = form.deviceName,
                onValueChange = { viewModel.update { f -> f.copy(deviceName = it) } },
                label = { Text("Устройство *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = form.serialNumber,
                onValueChange = { viewModel.update { f -> f.copy(serialNumber = it) } },
                label = { Text("Серийный номер / IMEI") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = form.issue,
                onValueChange = { viewModel.update { f -> f.copy(issue = it) } },
                label = { Text("Заявленная неисправность") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            OutlinedTextField(
                value = form.diagnosis,
                onValueChange = { viewModel.update { f -> f.copy(diagnosis = it) } },
                label = { Text("Диагноз") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            OutlinedTextField(
                value = form.price,
                onValueChange = { viewModel.update { f -> f.copy(price = it.filter { c -> c.isDigit() || c == '.' }) } },
                label = { Text("Цена ремонта, ₽") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            // Выбор клиента
            ExposedDropdownMenuBox(
                expanded = expandedClient,
                onExpandedChange = { expandedClient = it }
            ) {
                OutlinedTextField(
                    value = clients.firstOrNull { it.id == form.clientId }?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Клиент") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedClient) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedClient,
                    onDismissRequest = { expandedClient = false }
                ) {
                    clients.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c.name) },
                            onClick = {
                                viewModel.update { f -> f.copy(clientId = c.id) }
                                expandedClient = false
                            }
                        )
                    }
                }
            }

            // Статус
            var expandedStatus by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedStatus,
                onExpandedChange = { expandedStatus = it }
            ) {
                OutlinedTextField(
                    value = form.status.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Статус") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStatus) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedStatus,
                    onDismissRequest = { expandedStatus = false }
                ) {
                    RepairStatus.entries.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s.label) },
                            onClick = {
                                viewModel.update { f -> f.copy(status = s) }
                                expandedStatus = false
                            }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.save { id ->
                        navController.navigate("repair_detail?repairId=$id") {
                            popUpTo("repairs")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("Сохранить")
            }
        }
    }
}
