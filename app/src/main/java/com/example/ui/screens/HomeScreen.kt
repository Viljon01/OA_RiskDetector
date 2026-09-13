package com.example.ui.screens

import com.example.utils.translateAsState

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PatientEntity
import com.example.ui.theme.*
import com.example.utils.Language
import com.example.viewmodel.MainViewModel


@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNewScreening: () -> Unit,
    onPatientClick: (Int) -> Unit,
    onPendingPatientClick: (Int) -> Unit,
    onOrgProfileClick: () -> Unit,
    onRegistryClick: () -> Unit
) {
    val allPatients by viewModel.allPatients.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val syncedCount by viewModel.syncedCount.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val pendingCount = totalCount - syncedCount
    
    val currentDoctorName by viewModel.currentDoctorName.collectAsState()
    val currentFacilityName by viewModel.currentFacilityName.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf(viewModel.translateAsState("all_patients"), viewModel.translateAsState("high_risk"))
    val filteredPatients = remember(selectedTab, allPatients) {
        if (selectedTab == 1) allPatients.filter { it.riskTier == "HIGH RISK" } else allPatients
    }

    Scaffold(
        bottomBar = { 
            CustomBottomNav(
                onOrgProfileClick = onOrgProfileClick, 
                onRegistryClick = onRegistryClick, 
                labels = mapOf(
                    "home" to viewModel.translateAsState("home"),
                    "patients" to viewModel.translateAsState("patients"),
                    "profile" to viewModel.translateAsState("profile")
                )
            ) 
        },
        containerColor = Color(0xFFE1F5FE).copy(alpha = 0.6f)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = paddingValues.calculateBottomPadding() + 24.dp
            )
        ) {
            item {
                HeaderSection(
                    facilityName = currentFacilityName, 
                    currentLanguage = currentLanguage, 
                    onSetLanguage = viewModel::setLanguage,
                    topPadding = paddingValues.calculateTopPadding(),
                    viewModel = viewModel
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    DoctorAndSyncStatus(
                        doctorName = currentDoctorName,
                        isOnline = isOnline,
                        syncedCount = syncedCount,
                        pendingCount = pendingCount,
                        viewModel = viewModel
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    PrimaryActions(onNewScreening, onRegistryClick, viewModel)
                    Spacer(modifier = Modifier.height(24.dp))
                    TodaysOverview(allPatients, pendingCount, viewModel)
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
            
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = PrimaryTeal,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.Indicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = PrimaryTeal,
                                height = 3.dp
                            )
                        }
                    },
                    divider = { HorizontalDivider(color = BorderLight) }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                            selectedContentColor = PrimaryTeal,
                            unselectedContentColor = OnSurfaceVariantText
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            if (filteredPatients.isEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderItem)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(viewModel.translateAsState("no_records_found"), color = OnSurfaceVariantText)
                            }
                        }
                    }
                }
            } else {
                items(filteredPatients.take(10), key = { it.id }) { patient ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        PatientCard(patient, viewModel) {
                if (patient.riskTier == "PENDING") {
                    onPendingPatientClick(patient.id)
                } else {
                    onPatientClick(patient.id)
                }
            }
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderSection(
    facilityName: String, 
    currentLanguage: String, 
    onSetLanguage: (String) -> Unit,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp,
    viewModel: MainViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    Surface(
        color = PrimaryTeal,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding)
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    coil.compose.AsyncImage(
                        model = com.example.R.drawable.custom_logo,
                        contentDescription = "Logo",
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = viewModel.translateAsState("operational_facility"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha=0.8f)
                        )
                        Text(
                            text = facilityName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha=0.2f), RoundedCornerShape(8.dp))
                        .clickable { expanded = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(currentLanguage.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        Language.values().forEach { lang ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = (currentLanguage == lang.code),
                                            onClick = null
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("${lang.nativeName} (${lang.code})")
                                    }
                                },
                                onClick = {
                                    onSetLanguage(lang.code)
                                    expanded = false
                                    (context as? Activity)?.recreate()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DoctorAndSyncStatus(doctorName: String, isOnline: Boolean, syncedCount: Int, pendingCount: Int, viewModel: MainViewModel) {
    val isPending = pendingCount > 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderItem),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Doctor info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(PrimaryTealLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(doctorName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = OnSurfaceText)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isOnline) Color(0xFF4CAF50) else Color(0xFF9E9E9E), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isOnline) viewModel.translateAsState("online") else viewModel.translateAsState("offline_mode"),
                            fontSize = 12.sp,
                            color = OnSurfaceVariantText
                        )
                    }
                }
            }

            // Sync Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(if (isPending) Color(0xFFFFF8E1) else Color(0xFFE8F5E9), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = if (isPending) Icons.Default.CloudSync else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isPending) Color(0xFFF57F17) else Color(0xFF388E3C),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(viewModel.translateAsState("sync_status"), fontSize = 10.sp, color = OnSurfaceVariantText)
                    Text("$syncedCount ${viewModel.translateAsState("sync_short")} / $pendingCount ${viewModel.translateAsState("pen_short")}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OnSurfaceText)
                }
            }
        }
    }
}

@Composable
fun PrimaryActions(onNewScreening: () -> Unit, onRegistryClick: () -> Unit, viewModel: MainViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = {
                viewModel.clearCurrentPatient()
                onNewScreening()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.Start) {
                Text(viewModel.translateAsState("new_screening"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(viewModel.translateAsState("start_new_oa_assessment"), fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
        }

        OutlinedButton(
            onClick = onRegistryClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryTeal),
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryTeal),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(viewModel.translateAsState("existing_patient"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun TodaysOverview(patients: List<PatientEntity>, pendingCount: Int, viewModel: MainViewModel) {
    val totalScreened = patients.size
    val highRiskCount = patients.count { it.riskTier == "HIGH RISK" }
    
    Column {
        Text(viewModel.translateAsState("todays_overview"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceText)
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewCard(
                title = viewModel.translateAsState("patients_screened"),
                value = totalScreened.toString(),
                modifier = Modifier.weight(1f)
            )
            OverviewCard(
                title = viewModel.translateAsState("high_risk"),
                value = highRiskCount.toString(),
                valueColor = Color(0xFFD32F2F),
                modifier = Modifier.weight(1f)
            )
            OverviewCard(
                title = viewModel.translateAsState("pending_sync"),
                value = pendingCount.toString(),
                valueColor = if (pendingCount > 0) Color(0xFFFFA000) else OnSurfaceText,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun OverviewCard(title: String, value: String, valueColor: Color = OnSurfaceText, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderItem)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = valueColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, fontSize = 11.sp, color = OnSurfaceVariantText, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun PatientCard(patient: PatientEntity, viewModel: MainViewModel, onClick: () -> Unit) {
    val (bgColor, textColor, label) = when (patient.riskTier) {
        "HIGH RISK" -> Triple(Color(0xFFFFEBEE), Color(0xFFD32F2F), viewModel.translateAsState("high_risk_badge"))
        "MODERATE RISK" -> Triple(Color(0xFFFFF8E1), Color(0xFFF57F17), viewModel.translateAsState("moderate_risk_badge"))
        "LOW RISK", "NORMAL" -> Triple(Color(0xFFE8F5E9), Color(0xFF388E3C), viewModel.translateAsState("low_risk_badge"))
        else -> Triple(Color(0xFFF5F5F5), Color(0xFF757575), viewModel.translateAsState("pending_badge"))
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderItem),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = patient.name.ifBlank { viewModel.translateAsState("unknown_patient") },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "ID: NER-2024-${patient.id.toString().padStart(3, '0')}",
                        fontSize = 12.sp,
                        color = OnSurfaceVariantText
                    )
                }
                
                Box(
                    modifier = Modifier.background(bgColor, RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = label,
                        color = textColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = BorderLight)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(viewModel.translateAsState("oa_risk_score"), fontSize = 11.sp, color = OnSurfaceVariantText)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${patient.riskScore.toInt()}/100", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OnSurfaceText)
                }
                
                if (patient.area.isNotBlank()) {
                    Column {
                        Text(viewModel.translateAsState("area_location"), fontSize = 11.sp, color = OnSurfaceVariantText)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(patient.area, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceText)
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(viewModel.translateAsState("view_assessment"), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = PrimaryTeal)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun CustomBottomNav(onOrgProfileClick: () -> Unit, onRegistryClick: () -> Unit, labels: Map<String, String>) {
    Surface(
        color = PrimaryTeal,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(labels["home"] ?: "Home", Icons.Default.Home, true, {}, Color.White, Color.White.copy(alpha=0.5f))
            NavItem(labels["patients"] ?: "Patients", Icons.Default.Folder, false, onRegistryClick, Color.White, Color.White.copy(alpha=0.5f))
            NavItem(labels["profile"] ?: "Profile", Icons.Default.Person, false, onOrgProfileClick, Color.White, Color.White.copy(alpha=0.5f))
        }
    }
}

@Composable
fun NavItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, activeColor: Color, inactiveColor: Color) {
    val color = if (selected) activeColor else inactiveColor
    Column(
        modifier = Modifier.clickable { onClick() }.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = color
        )
    }
}
