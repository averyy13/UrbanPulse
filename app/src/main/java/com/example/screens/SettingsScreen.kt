package com.example.screens

import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.AuthState
import com.example.ui.theme.ThemeConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val isDarkThemeActive by ThemeConfig.isDarkTheme.collectAsState()
    val authState by authViewModel.authState.collectAsState()

    var notificationsEnabled by remember { mutableStateOf(true) }
    var nearbyIncidentsEnabled by remember { mutableStateOf(true) }

    var showNameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var newDisplayName by remember { mutableStateOf("") }

    // Synchronize local state name when authState is loaded
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            newDisplayName = (authState as AuthState.Success).displayName ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                SettingsSectionHeader("Notification Preferences", Icons.Default.Notifications)
                SettingsSwitchItem(
                    title = "Report Updates", 
                    checked = notificationsEnabled, 
                    onCheckedChange = { 
                        notificationsEnabled = it 
                        Toast.makeText(context, if (it) "Report updates enabled" else "Report updates disabled", Toast.LENGTH_SHORT).show()
                    }
                )
                SettingsSwitchItem(
                    title = "Nearby Incidents", 
                    checked = nearbyIncidentsEnabled, 
                    onCheckedChange = { 
                        nearbyIncidentsEnabled = it 
                        Toast.makeText(context, if (it) "Nearby alerts enabled" else "Nearby alerts disabled", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                SettingsSectionHeader("Theme Settings")
                SettingsSwitchItem(
                    title = "Dark Mode", 
                    checked = isDarkThemeActive, 
                    onCheckedChange = { 
                        ThemeConfig.isDarkTheme.value = it 
                    }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                SettingsSectionHeader("Account Management", Icons.Default.Person)
                val currentName = if (authState is AuthState.Success) (authState as AuthState.Success).displayName ?: "User" else "User"
                SettingsClickableItem(
                    title = "Update Display Name", 
                    subtitle = "Current: $currentName"
                ) { 
                    showNameDialog = true
                }
                SettingsClickableItem(
                    title = "Delete Account", 
                    subtitle = "Permanently remove your account and data", 
                    color = MaterialTheme.colorScheme.error
                ) { 
                    showDeleteDialog = true
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                SettingsSectionHeader("App Info", Icons.Default.Info)
                SettingsClickableItem(title = "Version", subtitle = "1.0.0 (Build 10)") { 
                    Toast.makeText(context, "UrbanPulse v1.0.0 is up-to-date", Toast.LENGTH_SHORT).show()
                }
                SettingsClickableItem(title = "About UrbanPulse", subtitle = "Learn more about our mission") { 
                    showAboutDialog = true
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                SettingsSectionHeader("Privacy Settings", Icons.Default.Policy)
                SettingsClickableItem(title = "Privacy Policy") { 
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://urbanpulse.org/privacy"))
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Opening Privacy Policy link", Toast.LENGTH_SHORT).show()
                    }
                }
                SettingsClickableItem(title = "Terms of Service") { 
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://urbanpulse.org/terms"))
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Opening Terms of Service link", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Update Display Name Dialog
    if (showNameDialog) {
        var nameInput by remember { mutableStateOf(newDisplayName) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Update Display Name") },
            text = {
                Column {
                    Text("Enter your new display name below:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        singleLine = true,
                        placeholder = { Text("Full Name") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            authViewModel.updateDisplayName(nameInput) { result ->
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Display name updated successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                            showNameDialog = false
                        } else {
                            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Account Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account", color = MaterialTheme.colorScheme.error) },
            text = {
                Text("Are you sure you want to permanently delete your UrbanPulse account? This action is irreversible and all your reports, contributions, and account preferences will be deleted forever.")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        authViewModel.deleteAccount { result ->
                            if (result.isSuccess) {
                                Toast.makeText(context, "Account successfully deleted.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete Forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About UrbanPulse") },
            text = {
                Text("UrbanPulse empowers citizens to report and track infrastructure issues (potholes, street light failures, water leakage) in real-time. By bridging the gap between communities and local administration, we aim to build safer, cleaner, and more resilient cities together.")
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun SettingsSwitchItem(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsClickableItem(title: String, subtitle: String? = null, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(text = title, fontSize = 16.sp, color = color, fontWeight = if (color == MaterialTheme.colorScheme.error) FontWeight.SemiBold else FontWeight.Normal)
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
