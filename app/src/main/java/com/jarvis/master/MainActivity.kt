package com.jarvis.master

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jarvis.master.ui.JarvisRoot
import com.jarvis.master.ui.SettingsViewModel
import com.jarvis.master.ui.ViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val factory = ViewModelFactory(LocalContext.current)
            val settingsVm: SettingsViewModel = viewModel(factory = factory)
            val themeMode by settingsVm.settings.collectAsState()
            JarvisRoot(themeMode = themeMode.themeMode)
        }
    }
}
