package com.jarvis.master.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
    val repairs by viewModel.repairs.collectAsState()

    // Список уже вводившихся устройств и серийников — для подсказок
    val knownDevices = remember(repairs) {
        repairs.map { it.deviceName }.filter { it.isNotBlank() }.distinct()
    }
    val knownSerials = remember(repairs) {
        repairs.map { it.serialNumber }.filter { it.isNotBlank() }.distinct()
    }
    val knownIssues = remember(repairs) {
        repairs.map { it.issue }.filter { it.isNotBlank() }.distinct()
    }

    val deviceSugg = suggestions(form.deviceName, knownDevices)
    val serialSugg = suggestions(form.serialNumber, knownSerials)
    val issueSugg = suggestions(form.issue, knownIssues)
    val clientSugg = clients.filter {
        form.clientName.isNotBlank() &&
            it.name.contains(form.clientName.trim(), true) &&
            !it.name.equals(form.clientName.trim(), true)
    }.take(6)

    // Если открыт существующий ремонт с клиентом — подставляем имя и телефон клиента
    LaunchedEffect(form.clientId, clients) {
        val id = form.clientId
        if (id != null && form.clientName.isBlank()) {
            clients.firstOrNull { it.id == id }?.let { viewModel.onClientSelect(it) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новый ремонт") },
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SuggestionField(
                value = form.deviceName,
                onValueChange = { viewModel.update { f -> f.copy(deviceName = it) } },
                label = "Устройство *",
                suggestions = deviceSugg,
                onPick = { viewModel.update { f -> f.copy(deviceName = it) } }
            )
            SuggestionField(
                value = form.serialNumber,
                onValueChange = { viewModel.update { f -> f.copy(serialNumber = it) } },
                label = "Серийный номер / IMEI",
                suggestions = serialSugg,
                onPick = { viewModel.update { f -> f.copy(serialNumber = it) } }
            )
            SuggestionField(
                value = form.issue,
                onValueChange = { viewModel.update { f -> f.copy(issue = it) } },
                label = "Заявленная неисправность",
                suggestions = issueSugg,
                onPick = { viewModel.update { f -> f.copy(issue = it) } },
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

            // ---------- Клиент ----------
            Text("Клиент (необязательно)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            SuggestionField(
                value = form.clientName,
                onValueChange = viewModel::onClientNameChange,
                label = "Имя клиента",
                suggestions = clientSugg.map { it.name },
                onPick = { name ->
                    clients.firstOrNull { it.name.equals(name, true) }?.let { viewModel.onClientSelect(it) }
                }
            )
            OutlinedTextField(
                value = form.clientPhone,
                onValueChange = { viewModel.update { f -> f.copy(clientPhone = it) } },
                label = { Text("Телефон клиента") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            Text(
                "Если клиента нет в базе — просто введите имя и телефон, и он сохранится в клиентскую базу.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ---------- Статус ----------
            var expandedStatus by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expandedStatus, onExpandedChange = { expandedStatus = it }) {
                OutlinedTextField(
                    value = form.status.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Статус") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStatus) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) {
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
                        // id == 0 — облако недоступно, ошибка уже показана Snackbar'ом.
                        if (id != 0L) {
                            navController.navigate("repair_detail?repairId=$id") {
                                popUpTo("repairs")
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) { Text("Сохранить") }
        }
    }
}

/** Подбирает до 5 подсказок, содержащих введённый текст (кроме точного совпадения). */
private fun suggestions(text: String, known: List<String>): List<String> {
    if (text.isBlank()) return emptyList()
    val t = text.trim()
    return known.filter { it.contains(t, true) && it != t }.take(5)
}

@Composable
private fun SuggestionField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suggestions: List<String>,
    onPick: (String) -> Unit,
    minLines: Int = 1
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            minLines = minLines,
            singleLine = minLines == 1
        )
        if (suggestions.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
                suggestions.forEach { s ->
                    Text(
                        text = s,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(s) }
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                    )
                }
            }
        }
    }
}
