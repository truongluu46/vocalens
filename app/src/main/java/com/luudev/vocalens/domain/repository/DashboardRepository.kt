package com.luudev.vocalens.domain.repository

import com.luudev.vocalens.domain.model.Dashboard
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun observeDashboard(userId: String): Flow<Dashboard>
}
