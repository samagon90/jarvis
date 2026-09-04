package com.jarvis.master.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jarvis.master.ui.PartEditViewModel
import com.jarvis.master.ui.ViewModelFactory

private val categories = listOf(
    "Дисплей", "Аккумулятор", "Разъём зарядки", "Кнопки", "Камера", "Динамик/Микрофон",
    "Плата", "Корпус/стекло", "Кабель/шлейф", "Прочее"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartEditScreen(
    navController: NavController,
    viewModel: PartEditViewModel = viewModel(
        factory = ViewModelFactory(LocalContext.current.applicationContext)
    )
) {
    val form by viewModel.form.collectAsState()
    val partId = navController.currentBackStackEntry?.arguments?.getLong("partId") ?: 0L
    var expandedCat by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (partId == 0L) "Новая запчасть" else "Редактирование") },
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
                value = form.name,
                onValueChange = { v -> viewModel.update { f -> f.copy(name = v) } },
                label = { Text("Название *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            ExposedDropdownMenuBox(expanded = expandedCat, onExpandedChange = { expandedCat = it }) {
                OutlinedTextField(
                    value = form.category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Категория") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedCat) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) {
                    categories.forEach { c ->
                        DropdownMenuItem(text = { Text(c) }, onClick = {
                            viewModel.update { f -> f.copy(category = c) }
                            expandedCat = false
                        })
                    }
                }
            }
            OutlinedTextField(
                value = form.barcode,
                onValueChange = { v -> viewModel.update { f -> f.copy(barcode = v) } },
                label = { Text("Артикул / штрих-код") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = form.quantity.let { if (it == 0) "" else it.toString() },
                    onValueChange = { v ->
                        viewModel.update { f -> f.copy(quantity = v.filter { c -> c.isDigit() }.toIntOrNull() ?: 0) }
                    },
                    label = { Text("Остаток") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = form.minQuantity.toString(),
                    onValueChange = { v ->
                        viewModel.update { f -> f.copy(minQuantity = v.filter { c -> c.isDigit() }.toIntOrNull() ?: 0) }
                    },
                    label = { Text("Мин. остаток") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = form.purchase.let { if (it == 0.0) "" else it.toString() },
                    onValueChange = { v ->
                        viewModel.update { f -> f.copy(purchase = v.toDoubleOrNull() ?: 0.0) }
                    },
                    label = { Text("Закупка, ₽") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = form.sell.let { if (it == 0.0) "" else it.toString() },
                    onValueChange = { v ->
                        viewModel.update { f -> f.copy(sell = v.toDoubleOrNull() ?: 0.0) }
                    },
                    label = { Text("Продажа, ₽") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = form.location,
                onValueChange = { v -> viewModel.update { f -> f.copy(location = v) } },
                label = { Text("Место на складе") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = form.note,
                onValueChange = { v -> viewModel.update { f -> f.copy(note = v) } },
                label = { Text("Примечание") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Button(
                onClick = { viewModel.save { navController.popBackStack() } },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) { Text("Сохранить") }
        }
    }
}
