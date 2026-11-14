package com.example.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.model.MedicationEntry
import com.example.data.model.SeizureLog
import com.example.data.repository.AuraCareRepository
import com.example.notifications.DoseReminderScheduler
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutUser
import com.example.ui.components.ClinicalReportDialog
import com.example.ui.components.EmergencyAlertDialog
import com.example.ui.components.MedicationEditorDialog
import com.example.ui.components.SeizureEditorDialog
import com.example.ui.screens.CareProfileScreen
import com.example.ui.screens.DailyWellnessScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MedicationsScreen
import com.example.ui.screens.SeizuresScreen
import com.example.ui.viewmodel.AuraCareViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val NAV_ITEMS = listOf(
    NavDestinationItem("home", R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    NavDestinationItem("seizures", R.string.nav_seizures, Icons.Filled.Timeline, Icons.Outlined.Timeline),
    NavDestinationItem("meds", R.string.nav_meds, Icons.Filled.Medication, Icons.Outlined.Medication),
    NavDestinationItem("daily", R.string.nav_daily, Icons.Filled.Restaurant, Icons.Outlined.Restaurant),
    NavDestinationItem("care", R.string.nav_care, Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
)

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            MainAuraCareShell(
                currentUserId = user.uid,
                defaultDisplayName = user.displayName ?: user.email?.substringBefore("@") ?: "Patient"
            )
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAuraCareShell(
    currentUserId: String,
    defaultDisplayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: AuraCareViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                AuraCareViewModel(AuraCareRepository(db, Firebase.auth), currentUserId)
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val seizuresState by viewModel.seizuresState.collectAsStateWithLifecycle()
    val medsState by viewModel.medicationsState.collectAsStateWithLifecycle()
    val doseLogsState by viewModel.doseLogsState.collectAsStateWithLifecycle()
    val dailyLogsState by viewModel.dailyLogsState.collectAsStateWithLifecycle()
    val apptsState by viewModel.appointmentsState.collectAsStateWithLifecycle()
    val contactsState by viewModel.contactsState.collectAsStateWithLifecycle()
    val recordsState by viewModel.recordsState.collectAsStateWithLifecycle()
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val seizures = (seizuresState as? UiState.Success)?.data ?: emptyList()
    val medications = (medsState as? UiState.Success)?.data ?: emptyList()
    val doseLogs = (doseLogsState as? UiState.Success)?.data ?: emptyList()
    val dailyLogs = (dailyLogsState as? UiState.Success)?.data ?: emptyList()
    val appointments = (apptsState as? UiState.Success)?.data ?: emptyList()
    val contacts = (contactsState as? UiState.Success)?.data ?: emptyList()
    val records = (recordsState as? UiState.Success)?.data ?: emptyList()

    // Sync local dose alarms whenever medication list updates
    LaunchedEffect(medications) {
        DoseReminderScheduler.syncMedicationReminders(context, medications)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    var showEmergencyDialog by remember { mutableStateOf(false) }
    var reportRecipientEmail by remember { mutableStateOf<String?>(null) }
    var editingSeizure by remember { mutableStateOf<SeizureLog?>(null) }
    var showNewSeizure by remember { mutableStateOf(false) }
    var prefilledSeconds by remember { mutableStateOf<Long?>(null) }
    var prefilledStartMs by remember { mutableStateOf<Long?>(null) }
    var editingMedication by remember { mutableStateOf<MedicationEntry?>(null) }
    var newMedDefaultStatus by remember { mutableStateOf<String?>(null) }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"
    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    fun navigateTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openSeizureFromTimer() {
        val (elapsed, startedMs) = viewModel.stopTimer()
        prefilledSeconds = elapsed
        prefilledStartMs = startedMs
        editingSeizure = null
        showNewSeizure = true
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { reportRecipientEmail = profile?.neurologistEmail ?: "" },
                            modifier = Modifier.testTag("topbar_pdf_button")
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = "PDF & Email Report")
                        }
                        FilledTonalButton(
                            onClick = { showEmergencyDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("topbar_sos_button")
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = "Emergency Alert")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SOS")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        windowInsets = WindowInsets(0, 0, 0, 0).let {
                            androidx.compose.material3.NavigationBarDefaults.windowInsets
                        }
                    ) {
                        NAV_ITEMS.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(id = item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(id = item.labelRes)) },
                                modifier = Modifier.testTag("nav_${item.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail {
                        NAV_ITEMS.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(id = item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(id = item.labelRes)) }
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable("home") {
                            DashboardScreen(
                                profile = profile,
                                seizures = seizures,
                                medications = medications,
                                doseLogs = doseLogs,
                                dailyLogs = dailyLogs,
                                appointments = appointments,
                                records = records,
                                timerState = timerState,
                                onStartTimer = { viewModel.startTimer() },
                                onStopAndLogTimer = { openSeizureFromTimer() },
                                onResetTimer = { viewModel.resetTimer() },
                                onOpenEmergencyDialog = { showEmergencyDialog = true },
                                onOpenSeizureEditor = {
                                    prefilledSeconds = null
                                    prefilledStartMs = null
                                    editingSeizure = null
                                    showNewSeizure = true
                                },
                                onOpenReportDialog = { reportRecipientEmail = profile?.neurologistEmail ?: "" },
                                onToggleDose = { med, time, taken ->
                                    viewModel.setDoseTaken(med.id, med.name, med.dosage, time, todayKey, taken)
                                },
                                onMarkAppointmentLinked = { id -> viewModel.markAppointmentLinked(id) },
                                onNavigateToTab = { route -> navigateTab(route) }
                            )
                        }

                        composable("seizures") {
                            SeizuresScreen(
                                seizures = seizures,
                                timerState = timerState,
                                onStartTimer = { viewModel.startTimer() },
                                onStopAndLogTimer = { openSeizureFromTimer() },
                                onResetTimer = { viewModel.resetTimer() },
                                onOpenEmergencyDialog = { showEmergencyDialog = true },
                                onAddSeizureClick = {
                                    prefilledSeconds = null
                                    prefilledStartMs = null
                                    editingSeizure = null
                                    showNewSeizure = true
                                },
                                onEditSeizureClick = { log ->
                                    prefilledSeconds = null
                                    prefilledStartMs = null
                                    editingSeizure = log
                                },
                                onDeleteSeizureClick = { id -> viewModel.deleteSeizure(id) },
                                onOpenReportDialog = { reportRecipientEmail = profile?.neurologistEmail ?: "" }
                            )
                        }

                        composable("meds") {
                            MedicationsScreen(
                                medications = medications,
                                doseLogs = doseLogs,
                                onAddMedication = { status ->
                                    editingMedication = null
                                    newMedDefaultStatus = status
                                },
                                onEditMedication = { med ->
                                    newMedDefaultStatus = null
                                    editingMedication = med
                                },
                                onToggleReminder = { id, enabled -> viewModel.toggleMedicationReminder(id, enabled) },
                                onArchiveToPast = { med, reason ->
                                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                    viewModel.saveMedication(
                                        med.copy(
                                            status = "past",
                                            endDate = today,
                                            stopReason = reason,
                                            reminderEnabled = false
                                        )
                                    )
                                },
                                onDeleteMedication = { id -> viewModel.deleteMedication(id) },
                                onToggleDose = { med, time, taken ->
                                    viewModel.setDoseTaken(med.id, med.name, med.dosage, time, todayKey, taken)
                                },
                                onFeedback = { msg -> viewModel.showMessage(msg) }
                            )
                        }

                        composable("daily") {
                            DailyWellnessScreen(
                                dailyLogs = dailyLogs,
                                initialDietPreference = profile?.dietaryPreference ?: "Balanced",
                                onSaveDailyLog = { log -> viewModel.saveDailyLog(log) },
                                onDeleteDailyLog = { id -> viewModel.deleteDailyLog(id) }
                            )
                        }

                        composable("care") {
                            CareProfileScreen(
                                uid = currentUserId,
                                defaultDisplayName = defaultDisplayName,
                                profile = profile,
                                appointments = appointments,
                                contacts = contacts,
                                records = records,
                                onSaveProfile = { updated -> viewModel.saveProfile(updated) },
                                onSaveAppointment = { appt -> viewModel.saveAppointment(appt) },
                                onMarkAppointmentLinked = { id -> viewModel.markAppointmentLinked(id) },
                                onDeleteAppointment = { id -> viewModel.deleteAppointment(id) },
                                onSaveContact = { contact -> viewModel.saveEmergencyContact(contact) },
                                onDeleteContact = { id -> viewModel.deleteEmergencyContact(id) },
                                onSaveRecord = { rec -> viewModel.saveMedicalRecord(rec) },
                                onDeleteRecord = { id -> viewModel.deleteMedicalRecord(id) },
                                onOpenReportDialog = { email -> reportRecipientEmail = email },
                                onOpenEmergencyDialog = { showEmergencyDialog = true },
                                onSignOut = { signOutUser(credentialManager, scope) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewSeizure || editingSeizure != null) {
        SeizureEditorDialog(
            initialLog = editingSeizure,
            prefilledSeconds = prefilledSeconds,
            prefilledTimestampMillis = prefilledStartMs,
            onDismiss = {
                showNewSeizure = false
                editingSeizure = null
            },
            onSave = { entry, openEmergency ->
                viewModel.saveSeizure(entry) {
                    viewModel.resetTimer()
                }
                showNewSeizure = false
                editingSeizure = null
                if (openEmergency) {
                    showEmergencyDialog = true
                }
            }
        )
    }

    if (newMedDefaultStatus != null || editingMedication != null) {
        MedicationEditorDialog(
            initialMed = editingMedication,
            defaultStatus = newMedDefaultStatus ?: editingMedication?.status ?: "current",
            onDismiss = {
                newMedDefaultStatus = null
                editingMedication = null
            },
            onSave = { med ->
                viewModel.saveMedication(med)
                newMedDefaultStatus = null
                editingMedication = null
            }
        )
    }

    if (showEmergencyDialog) {
        EmergencyAlertDialog(
            patientName = profile?.fullName?.ifBlank { defaultDisplayName } ?: defaultDisplayName,
            timerSeconds = timerState.elapsedSeconds,
            contacts = contacts,
            onDismiss = { showEmergencyDialog = false },
            onLocalAlertTriggered = { viewModel.showMessage("On-device emergency alert notification posted") }
        )
    }

    if (reportRecipientEmail != null) {
        ClinicalReportDialog(
            initialEmail = reportRecipientEmail!!,
            profile = profile,
            seizures = seizures,
            medications = medications,
            dailyLogs = dailyLogs,
            records = records,
            appointments = appointments,
            onDismiss = { reportRecipientEmail = null },
            onFeedback = { msg -> viewModel.showMessage(msg) }
        )
    }
}
