package com.example.data

import kotlinx.coroutines.flow.Flow

class PatientRepository(private val patientDao: PatientDao) {
    val allPatients: Flow<List<PatientEntity>> = patientDao.getAllPatients()
    val unsyncedPatients: Flow<List<PatientEntity>> = patientDao.getUnsyncedPatients()
    val totalCount: Flow<Int> = patientDao.getTotalCount()
    val syncedCount: Flow<Int> = patientDao.getSyncedCount()

    fun getPatientHistoryByName(name: String): Flow<List<PatientEntity>> {
        return patientDao.getPatientHistoryByName(name)
    }

    suspend fun insert(patient: PatientEntity): Long {
        return patientDao.insertPatient(patient)
    }

    suspend fun update(patient: PatientEntity) {
        patientDao.updatePatient(patient)
    }

    suspend fun getById(id: Int): PatientEntity? {
        return patientDao.getPatientById(id)
    }
}
