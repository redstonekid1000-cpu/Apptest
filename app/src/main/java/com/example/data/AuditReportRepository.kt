package com.example.data

import kotlinx.coroutines.flow.Flow

class AuditReportRepository(private val auditReportDao: AuditReportDao) {
    fun getReportsForUser(userId: Long): Flow<List<AuditReport>> = auditReportDao.getReportsForUser(userId)
    suspend fun insertReport(report: AuditReport): Long = auditReportDao.insertReport(report)
    suspend fun deleteReport(id: Long) = auditReportDao.deleteReport(id)
}
