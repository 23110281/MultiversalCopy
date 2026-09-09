package com.multiversalcopy

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkOverlayPermission()
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            launchMediaProjection()
        }
    }

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val apiUrl = prefs.getString("api_base_url", "") ?: ""

            val intent = Intent(this, CaptureService::class.java).apply {
                action = CaptureService.ACTION_START
                putExtra(CaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(CaptureService.EXTRA_RESULT_DATA, result.data)
                putExtra(CaptureService.EXTRA_API_URL, apiUrl)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        setContent {
            MaterialTheme {
                val prefs = remember { getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
                var showSettings by remember { mutableStateOf(false) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(if (showSettings) "Settings" else "MultiversalCopy") },
                            navigationIcon = {
                                if (showSettings) {
                                    IconButton(onClick = { showSettings = false }) {
                                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                                    }
                                }
                            },
                            actions = {
                                if (!showSettings) {
                                    IconButton(onClick = { showSettings = true }) {
                                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.primary,
                            )
                        )
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (showSettings) {
                            val scrollState = rememberScrollState()
                            Column(modifier = Modifier.verticalScroll(scrollState)) {
                                SettingsMenu(prefs)
                            }
                        } else {
                            AppContent(prefs)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun AppContent(prefs: android.content.SharedPreferences) {
        var apiUrl by remember { mutableStateOf(prefs.getString("api_base_url", "") ?: "") }

        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {

            OutlinedTextField(
                value = apiUrl,
                onValueChange = {
                    apiUrl = it
                    prefs.edit().putString("api_base_url", it).apply()
                },
                label = { Text("API Server URL") },
                placeholder = { Text("https://xxx.ngrok-free.app") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (apiUrl.isNotBlank()) {
                        startCaptureFlow()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = apiUrl.isNotBlank()
            ) {
                Text("Start Capture Mode")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    val intent = Intent(this@MainActivity, CaptureService::class.java).apply {
                        action = CaptureService.ACTION_STOP
                    }
                    startService(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Stop Capture Mode")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Instructions:\n\n" +
                        "1. Enter your API server URL above.\n" +
                        "2. Configure OCR settings below.\n" +
                        "3. Tap 'Start Capture Mode'.\n" +
                        "4. Grant necessary permissions.\n" +
                        "5. Go to any app you want to copy from.\n" +
                        "6. Pull down the notification shade and tap the MultiversalCopy notification.\n" +
                        "7. Wait for the boxes to appear, then tap one to copy its text.",
                style = MaterialTheme.typography.bodyMedium
            )

        }
    }

    private fun startCaptureFlow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                checkOverlayPermission()
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            checkOverlayPermission()
        }
    }

    private fun checkOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            launchMediaProjection()
        } else {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }

    private fun launchMediaProjection() {
        mediaProjectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }
}
