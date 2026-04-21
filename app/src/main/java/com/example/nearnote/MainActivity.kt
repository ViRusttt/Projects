package com.example.nearnote

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.nearnote.domain.model.Reminder
import com.example.nearnote.service.GeofenceManager
import com.example.nearnote.ui.ReminderViewModel
import com.example.nearnote.ui.add.AddReminderScreen
import com.example.nearnote.ui.detail.DetailScreen
import com.example.nearnote.ui.home.HomeScreen
import com.example.nearnote.ui.theme.NearNoteTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: ReminderViewModel by viewModels()

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* handled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationPermission.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        ))

        val targetId = intent.getLongExtra("reminder_id", -1L)

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            NearNoteTheme(darkTheme = isDarkMode) {
                NearNoteApp(viewModel = viewModel, openReminderId = targetId, isDarkMode = isDarkMode)
            }
        }
    }
}

@Composable
fun NearNoteApp(viewModel: ReminderViewModel, openReminderId: Long = -1L, isDarkMode: Boolean) {
    val navController = rememberNavController()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val geofenceManager = remember { GeofenceManager(context) }
    val scope = rememberCoroutineScope()

    val startDest = if (openReminderId != -1L) "detail/$openReminderId" else "home"

    var showBgLocationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            showBgLocationDialog = true
        }
    }

    if (showBgLocationDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showBgLocationDialog = false },
            title = { androidx.compose.material3.Text("Permission Required") },
            text = { androidx.compose.material3.Text("\"Always Allow\" location permission is required for reminders to work in the background.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showBgLocationDialog = false
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    androidx.compose.material3.Text("Open Settings", color = com.example.nearnote.ui.theme.Green)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showBgLocationDialog = false }) {
                    androidx.compose.material3.Text("Cancel")
                }
            }
        )
    }

    NavHost(navController, startDestination = startDest) {
        composable("home") {
            HomeScreen(
                reminders = reminders,
                isDarkMode = isDarkMode,
                onAddClick = { navController.navigate("add") },
                onReminderClick = { id -> navController.navigate("detail/$id") },
                onToggle = { viewModel.toggleActive(it) },
                onDelete = {
                    geofenceManager.removeGeofence(it.id)
                    viewModel.deleteReminder(it)
                },
                onToggleTheme = { viewModel.toggleDarkMode() }
            )
        }
        composable("add") {
            AddReminderScreen(
                onBack = { navController.popBackStack() },
                onSave = { reminder ->
                    scope.launch {
                        viewModel.addReminder(reminder)
                        geofenceManager.addGeofence(reminder)
                    }
                    navController.popBackStack()
                }
            )
        }
        composable(
            "detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStack ->
            val id = backStack.arguments?.getLong("id") ?: return@composable
            val reminder = reminders.find { it.id == id }
            if (reminder != null) {
                DetailScreen(
                    reminder = reminder,
                    onBack = { navController.popBackStack() },
                    onDelete = {
                        geofenceManager.removeGeofence(reminder.id)
                        viewModel.deleteReminder(reminder)
                        navController.popBackStack()
                    },
                    onSnooze = {
                        val twoHours = System.currentTimeMillis() + 2 * 60 * 60 * 1000
                        scope.launch {
                            viewModel.updateReminder(reminder.copy(cooldownUntil = twoHours))
                        }
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
