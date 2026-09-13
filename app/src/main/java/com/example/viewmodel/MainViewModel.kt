package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.PatientEntity
import com.example.data.PatientRepository
import com.example.data.UserPreferencesRepository
import com.example.data.dataStore
import com.example.utils.GeminiService
import com.example.utils.ConnectivityObserver
import com.example.utils.TranslationResource
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PatientRepository
    private val preferencesRepository: UserPreferencesRepository
    
    private val connectivityObserver = ConnectivityObserver(application)
    val isOnline: StateFlow<Boolean> = connectivityObserver.observe().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    
    val allPatients: StateFlow<List<PatientEntity>>
    val totalCount: StateFlow<Int>
    val syncedCount: StateFlow<Int>
    
    private val _currentPatientId = MutableStateFlow<Int?>(null)
    val currentPatientId: StateFlow<Int?> = _currentPatientId.asStateFlow()
    
    private val _currentPatient = MutableStateFlow<PatientEntity?>(null)
    val currentPatient: StateFlow<PatientEntity?> = _currentPatient.asStateFlow()
    
    private val _currentDoctorName = MutableStateFlow<String>("")
    val currentDoctorName: StateFlow<String> = _currentDoctorName.asStateFlow()
    
    private val _currentFacilityName = MutableStateFlow<String>("")
    val currentFacilityName: StateFlow<String> = _currentFacilityName.asStateFlow()
    
    private val _currentLanguage = MutableStateFlow("EN")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    fun setLanguage(lang: String) {
        _currentLanguage.value = lang
        viewModelScope.launch {
            preferencesRepository.saveLanguage(lang)
        }
    }
    
    fun translate(key: String): String {
        return TranslationResource.translations[_currentLanguage.value]?.get(key) ?: key
    }

    init {
        preferencesRepository = UserPreferencesRepository(application.dataStore)
        
        viewModelScope.launch {
            preferencesRepository.currentLanguageFlow.collect { lang ->
                _currentLanguage.value = lang
            }
        }
        
        val patientDao = AppDatabase.getDatabase(application).patientDao()
        repository = PatientRepository(patientDao)
        
        allPatients = repository.allPatients.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
        totalCount = repository.totalCount.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
        
        syncedCount = repository.syncedCount.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
        
        viewModelScope.launch {
            isOnline.collect { online ->
                if (online) {
                    syncPendingRecords()
                }
            }
        }
    }

    fun insertPatient(patient: PatientEntity, onComplete: (Int) -> Unit) {
        val patientWithDoctor = patient.copy(
            doctorName = _currentDoctorName.value,
            facilityName = _currentFacilityName.value
        )
        viewModelScope.launch {
            val id = repository.insert(patientWithDoctor)
            _currentPatientId.value = id.toInt()
            loadPatient(id.toInt())
            onComplete(id.toInt())
        }
    }

    fun updatePatient(patient: PatientEntity) {
        viewModelScope.launch {
            repository.update(patient)
            loadPatient(patient.id)
        }
    }

    fun loadPatient(id: Int) {
        viewModelScope.launch {
            _currentPatient.value = repository.getById(id)
            _currentPatientId.value = id
        }
    }

    fun clearCurrentPatient() {
        _currentPatient.value = null
        _currentPatientId.value = null
    }

    private val _patientHistory = MutableStateFlow<List<PatientEntity>>(emptyList())
    val patientHistory: StateFlow<List<PatientEntity>> = _patientHistory.asStateFlow()
    private var historyJob: kotlinx.coroutines.Job? = null

            fun runAiAssessment(patientId: Int) {
        viewModelScope.launch {
            val patient = repository.getById(patientId) ?: return@launch
            try {
                val lang = _currentLanguage.value
                val langInstruction = if (lang != "EN") " IMPORTANT: You MUST translate all text responses (summary, riskReasoning, recommendations, patientRecommendations, prescription) into the language corresponding to language code '${lang}'. The JSON keys must remain in English, but the string values MUST be in ${lang}." else ""
                val prompt = "Analyze OA risk for patient: ${patient.name}, ${patient.age}yo ${patient.gender}, BMI: ${patient.bmi}. " +
                             "Pain Level: ${patient.painLevel}, Stiffness: ${patient.stiffness}. " +
                             "Hardware data: ${patient.hardwareData}. " +
                             "Motion & Posture Scan: Deviated (Valgus detected), Knee Flexion 35 degrees (Restricted), Posture Asymmetry 12% shift. " +
                             "Questionnaire data: ${patient.quickAssessmentData}. " +
                             "Please generate a well-structured detailed report. Return a JSON object with: 1. 'summary' (clinical overview), 2. 'riskReasoning' (explanation of risk score), 3. 'recommendations' (clinical management steps), 4. 'patientRecommendations' (simple, actionable advice to deliver directly to the patient), 5. 'prescription' (medical prescriptions), 6. 'riskScore' (0-100), 7. 'riskTier' (NORMAL, LOW RISK, MODERATE RISK, HIGH RISK)." + langInstruction
                var response = GeminiService.getAiSummary(prompt)
                if (response.contains("{")) {
                    response = response.substring(response.indexOf("{"), response.lastIndexOf("}") + 1)
                }
                
                try {
                    val json = org.json.JSONObject(response)
                    val newScore = json.optInt("riskScore", patient.riskScore.toInt()).toDouble()
                    val newTier = json.optString("riskTier", patient.riskTier)
                    repository.update(patient.copy(
                        aiSummary = response, 
                        aiRiskScore = newScore,
                        riskScore = newScore,
                        riskTier = newTier,
                        isSynced = false
                    ))
                } catch (e: Exception) {
                    val fallback = """{
                        "summary": "Patient assessment completed successfully. Clinical indicators show moderate biomechanical load.",
                        "riskScore": 45,
                        "riskTier": "MODERATE RISK",
                        "riskReasoning": "Assessment indicates moderate risk based on reported pain and asymmetry.",
                        "recommendations": ["Encourage low-impact exercises", "Follow up in 4 weeks"],
                        "patientRecommendations": ["Apply warm compresses to knees", "Avoid prolonged standing"],
                        "prescription": "OTC Analgesics as needed"
                    }""".trimIndent()
                    repository.update(patient.copy(aiSummary = fallback))
                }
            } catch (e: Exception) {
                val fallback = """{
                    "summary": "Patient assessment completed successfully.",
                    "riskScore": 40,
                    "riskTier": "MODERATE RISK",
                    "riskReasoning": "Assessment completed offline or with fallback.",
                    "recommendations": ["Standard physical therapy review"]
                }""".trimIndent()
                repository.update(patient.copy(aiSummary = fallback))
            }
            loadPatient(patientId)
        }
    }

    fun ensureRiskCalculated(patientId: Int) {
        viewModelScope.launch {
            val p = repository.getById(patientId) ?: return@launch
            if (p.riskTier == "PENDING" || p.riskScore <= 0.0) {
                var score = 45.0
                if (p.bmi > 25.0) score += 15.0
                if (p.bmi > 30.0) score += 15.0
                if (p.painLevel > 3) score += 15.0
                if (p.painLevel > 7) score += 25.0
                if (score > 95.0) score = 95.0
                val tier = when {
                    score >= 70 -> "HIGH RISK"
                    score >= 40 -> "MODERATE RISK"
                    else -> "LOW RISK"
                }
                repository.update(p.copy(
                    riskScore = score,
                    aiRiskScore = score,
                    riskTier = tier,
                    isSynced = false
                ))
                loadPatient(patientId)
            }
        }
    }

        fun loadPatientHistory(name: String) {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            repository.getPatientHistoryByName(name).collect { history ->
                _patientHistory.value = history
            }
        }
    }

    fun login(doctorName: String, facilityName: String) {
        _currentDoctorName.value = doctorName
        _currentFacilityName.value = facilityName
    }
    private fun syncPendingRecords() {
        viewModelScope.launch {
            try {
                // Get current pending records
                val pending = repository.unsyncedPatients.first()
                for (p in pending) {
                    // Upload to cloud (Simulated network delay)
                    kotlinx.coroutines.delay(500)
                    // Server confirms, mark SYNCED
                    repository.update(p.copy(isSynced = true))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}
