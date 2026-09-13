package com.example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.ui.theme.MyApplicationTheme

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val navController = rememberNavController()
        val mainViewModel: MainViewModel = viewModel()
        
        NavHost(navController = navController, startDestination = Screen.Login.route) {
            composable(Screen.AdminDashboard.route) {
                AdminDashboardScreen(
                    onLogout = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.AdminDashboard.route) { inclusive = true }
                        }
                    },
                    onManageDoctors = { /* TODO */ },
                    onManagePatients = { /* TODO */ }
                )
            }
                        composable(Screen.Login.route) {
                LoginScreen(
                    viewModel = mainViewModel,
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = mainViewModel,
                    onNewScreening = { navController.navigate(Screen.Registration.route) },
                    onPatientClick = { id -> 
                        mainViewModel.loadPatient(id)
                        navController.navigate(Screen.Results.route)
                    },
                    onPendingPatientClick = { id ->
                        mainViewModel.loadPatient(id)
                        val p = mainViewModel.allPatients.value.find { it.id == id }
                        if (p != null) {
                            if (p.quickAssessmentData.isEmpty()) {
                                navController.navigate(Screen.QuickAssessment.route)
                            } else if (p.jointAngle == 0.0) {
                                navController.navigate(Screen.PostureAssessment.route)
                            } else if (p.hardwareData.isEmpty()) {
                                navController.navigate(Screen.HardwareData.route)
                            } else {
                                navController.navigate(Screen.Results.route)
                            }
                        } else {
                            navController.navigate(Screen.QuickAssessment.route)
                        }
                    },
                    onOrgProfileClick = { navController.navigate(Screen.OrgProfile.route) },
                    onRegistryClick = { navController.navigate(Screen.Registry.route) }
                )
            }
            composable(Screen.OrgProfile.route) {
                OrganizationProfileScreen(
                    viewModel = mainViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Registry.route) {
                PatientRegistryScreen(
                    viewModel = mainViewModel,
                    onBack = { navController.popBackStack() },
                    onPatientSelected = { name ->
                        navController.navigate("${Screen.PatientTimeline.route}/${name}")
                    }
                )
            }
            composable("${Screen.PatientTimeline.route}/{patientName}") { backStackEntry ->
                val patientName = backStackEntry.arguments?.getString("patientName") ?: ""
                PatientTimelineScreen(
                    viewModel = mainViewModel,
                    patientName = patientName,
                    onBack = { navController.popBackStack() },
                    onViewReport = { id ->
                        mainViewModel.loadPatient(id)
                        navController.navigate(Screen.Results.route)
                    },
                    onStartAssessment = {
                        navController.navigate(Screen.QuickAssessment.route)
                    }
                )
            }
            composable(Screen.Registration.route) {
                PatientRegistrationScreen(
                    viewModel = mainViewModel,
                    onProceed = { navController.navigate(Screen.QuickAssessment.route) },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(Screen.QuickAssessment.route) {
                QuickAssessmentScreen(
                    viewModel = mainViewModel,
                    onProceed = { navController.navigate(Screen.PostureAssessment.route) },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(Screen.PostureAssessment.route) {
                PostureAssessmentScreen(
                    viewModel = mainViewModel,
                    onAnalysisComplete = { navController.navigate(Screen.HardwareData.route) },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(Screen.PainAssessment.route) {
                PainAssessmentScreen(
                    viewModel = mainViewModel,
                    onGenerateReport = { navController.navigate(Screen.HardwareData.route) },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(Screen.HardwareData.route) {
                HardwareDataScreen(
                    viewModel = mainViewModel,
                    onGenerateReport = { navController.navigate(Screen.Results.route) },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(Screen.Results.route) {
                ResultsScreen(
                    viewModel = mainViewModel,
                    onFinish = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
      }
    }
  }
}
