package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_reports")
data class AuditReport(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val appName: String,
    val screenDescription: String,
    val overallScore: Int, // Out of 10
    val hierarchyStatus: String, // e.g. "Critical", "Good", "Excellent"
    val hierarchyText: String,
    val contrastStatus: String, // e.g. "AA standard missed", "Good", "Passed"
    val contrastText: String,
    val targetsStatus: String, // e.g. "44px threshold", "48dp verified", "Excellent"
    val targetsText: String,
    val recommendations: String, // Additional Markdown text to display
    val timestamp: Long = System.currentTimeMillis()
)
