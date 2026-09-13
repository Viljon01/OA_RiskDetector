package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object AdminDashboard : Screen("admin_dashboard")
    object OrgProfile : Screen("org_profile")
    object Registry : Screen("registry")
    object PatientTimeline : Screen("patient_timeline")
    object Registration : Screen("registration")
    object QuickAssessment : Screen("quick_assessment")
    object PostureAssessment : Screen("posture_assessment")
    object PainAssessment : Screen("pain_assessment")
    object HardwareData : Screen("hardware_data")
    object Results : Screen("results")
}
