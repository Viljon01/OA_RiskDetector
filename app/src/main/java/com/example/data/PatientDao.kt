package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients ORDER BY timestamp DESC")
    fun getAllPatients(): Flow<List<PatientEntity>>
    
    @Query("SELECT * FROM patients WHERE isSynced = 0")
    fun getUnsyncedPatients(): Flow<List<PatientEntity>>
    
    @Query("SELECT COUNT(*) FROM patients")
    fun getTotalCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM patients WHERE isSynced = 1")
    fun getSyncedCount(): Flow<Int>

    @Query("SELECT * FROM patients WHERE name = :name ORDER BY timestamp ASC")
    fun getPatientHistoryByName(name: String): Flow<List<PatientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity): Long

    @Update
    suspend fun updatePatient(patient: PatientEntity)

    @Query("SELECT * FROM patients WHERE id = :id LIMIT 1")
    suspend fun getPatientById(id: Int): PatientEntity?
}
