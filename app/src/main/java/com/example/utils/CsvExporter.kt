package com.example.utils

import android.content.Context
import android.net.Uri
import com.example.data.PatientEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {
    suspend fun exportToCsv(context: Context, uri: Uri, patients: List<PatientEntity>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        // Write CSV Header
                        writer.write("ID,Name,Age,Gender,Phone,Area,Occupation,Height(cm),Weight(kg),BMI,Pain Level,Stiffness,Risk Score,Risk Tier,Doctor,Facility,Date\n")
                        
                        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        
                        // Write Data Rows
                        patients.forEach { p ->
                            val dateStr = formatter.format(Date(p.timestamp))
                            // Escape fields that might contain commas
                            val name = escapeCsv(p.name)
                            val phone = escapeCsv(p.phoneNumber)
                            val area = escapeCsv(p.area)
                            val occ = escapeCsv(p.occupation)
                            val doctor = escapeCsv(p.doctorName)
                            val facility = escapeCsv(p.facilityName)
                            val stiffness = escapeCsv(p.stiffness)
                            
                            writer.write("${p.id},$name,${p.age},${p.gender},$phone,$area,$occ,${p.heightCm},${p.weightKg},${p.bmi},${p.painLevel},$stiffness,${p.riskScore},${p.riskTier},$doctor,$facility,$dateStr\n")
                        }
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }
}
