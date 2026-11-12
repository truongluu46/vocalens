package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.repository.DashboardRepository

class GetDashboard(private val repository: DashboardRepository) {
    operator fun invoke(userId: String) = repository.observeDashboard(userId)
}
