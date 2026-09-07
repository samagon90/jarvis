package com.jarvis.master.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.jarvis.master.data.db.RepairPhoto
import java.io.File
import com.jarvis.master.data.db.RepairStatus
import com.jarvis.master.ui.RepairDetailViewModel
import com.jarvis.master.ui.ViewModelFactory
import com.jarvis.master.ui.components.LabelValue
import com.jarvis.master.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepairDetailScreen(
    navController: NavController,
    viewModel: RepairDetailViewModel = viewModel(
        factory = ViewModelFactory(LocalContext.current.applicationContext)
    )
) {
    val state by viewModel.uiState.collectAsState()
    val repair = state.repair
    val client = state.client

    var showIssueDialog by remember { mutableStateOf(false) }

    // Пикер фото из галереи
    val context = LocalContext.current
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val resolver = context.contentResolver
            val mime = resolver.getType(uri) ?: "image/jpeg"
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                viewModel.addPhoto(bytes, mime, extOf(mime))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(repair?.deviceName ?: "Ремонт") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        repair?.let {
                            navController.navigate("repair_edit?repairId=${it.id}")
                        }
                    }) { Icon(Icons.Filled.Edit, contentDescription = "Редактировать") }
                    IconButton(onClick = { viewModel.delete { navController.popBackStack() } }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                    }
                }
            )
        }
    ) { padding ->
        if (repair == null) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(repair.statusEnum.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    LabelValue("Устройство", repair.deviceName)
                    if (repair.serialNumber.isNotBlank()) LabelValue("Серийный №", repair.serialNumber)
                    LabelValue("Принят", Formatters.dateTime(repair.receivedDate))
                    if (client != null) {
                        LabelValue("Клиент", client.name)
                        if (client.phone.isNotBlank()) LabelValue("Телефон", client.phone)
                    }
                }
            }

            if (repair.issue.isNotBlank()) {
                Text("Неисправность", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(repair.issue)
            }
            if (repair.diagnosis.isNotBlank()) {
                Text("Диагноз", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(repair.diagnosis)
            }

            // Фото техники
            PhotoSection(
                photos = state.photos,
                onAdd = { pickImage.launch("image/*") }
            )

            // Финансы
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    LabelValue("Цена ремонта", Formatters.money(repair.price))
                    LabelValue("Себестоимость запчастей", Formatters.money(repair.partsCost))
                    LabelValue("Прибыль", Formatters.money(repair.price - repair.partsCost))
                    LabelValue("Оплачено", Formatters.money(repair.receivedPayment))
                    if (repair.debt > 0) {
                        LabelValue("Долг", Formatters.money(repair.debt))
                    }
                }
            }

            // Действия по статусу
            when (repair.statusEnum) {
                RepairStatus.NEW, RepairStatus.DIAGNOSED, RepairStatus.IN_WORK, RepairStatus.AWAITING_PART -> {
                    OutlinedButton(onClick = { viewModel.markReady() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Отметить готовым")
                    }
                }
                RepairStatus.READY -> {
                    Button(
                        onClick = { showIssueDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text("Выдать клиенту и принять оплату") }
                }
                else -> { /* завершённые заказы */ }
            }

            if (repair.statusEnum == RepairStatus.ISSUED && repair.issuedDate != null) {
                Text(
                    "Выдан ${Formatters.dateTime(repair.issuedDate!!)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showIssueDialog) {
        val r = repair
        var received by remember(r?.id) { mutableStateOf((r?.price ?: 0.0).toString()) }
        AlertDialog(
            onDismissRequest = { showIssueDialog = false },
            title = { Text("Выдать устройство") },
            text = {
                Column {
                    Text("Введите полученную сумму (макс. ${r?.price ?: 0.0} ₽):")
                    OutlinedTextField(
                        value = received,
                        onValueChange = { received = it.filter { c -> c.isDigit() || c == '.' } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.issue(received.toDoubleOrNull() ?: 0.0)
                    showIssueDialog = false
                }) { Text("Выдать") }
            },
            dismissButton = {
                TextButton(onClick = { showIssueDialog = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun PhotoSection(photos: List<RepairPhoto>, onAdd: () -> Unit) {
    Text("Фото техники", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    OutlinedButton(onClick = onAdd) { Text("+ Добавить фото") }
    if (photos.isEmpty()) {
        Text(
            "Фото хранятся на телефоне и привязаны к ремонту.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(photos) { photo ->
                val file = remember(photo.url) { File(photo.url) }
                AsyncImage(
                    model = file,
                    contentDescription = "Фото ремонта",
                    modifier = Modifier
                        .width(110.dp)
                        .height(90.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

private fun extOf(mime: String): String = when (mime.lowercase()) {
    "image/png" -> "png"
    "image/webp" -> "webp"
    "image/gif" -> "gif"
    else -> "jpg"
}
