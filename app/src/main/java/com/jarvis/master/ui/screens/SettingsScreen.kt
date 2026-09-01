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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jarvis.master.ui.SettingsViewModel
import com.jarvis.master.ui.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel(factory = ViewModelFactory(LocalContext.current.applicationContext))
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
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
            Text("Мастерская", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            OutlinedTextField(
                value = settings.shopName,
                onValueChange = { v -> viewModel.save { it.copy(shopName = v) } },
                label = { Text("Название") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = settings.shopPhone,
                onValueChange = { v -> viewModel.save { it.copy(shopPhone = v) } },
                label = { Text("Телефон мастерской") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = settings.shopAddress,
                onValueChange = { v -> viewModel.save { it.copy(shopAddress = v) } },
                label = { Text("Адрес") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = settings.warrantyDays.toString(),
                onValueChange = { v -> viewModel.save { it.copy(warrantyDays = v.filter { c -> c.isDigit() }.toIntOrNull() ?: 30) } },
                label = { Text("Гарантия, дней") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            Text("Резервная копия", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Text(
                "Экспорт всех данных (клиенты, ремонты, запчасти, финансы) в JSON-файл.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { viewModel.exportBackup() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Backup, contentDescription = null)
                Text("Экспортировать данные", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
