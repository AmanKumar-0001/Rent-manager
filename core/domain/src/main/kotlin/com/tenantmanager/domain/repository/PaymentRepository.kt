package com.tenantmanager.domain.repository

import com.tenantmanager.domain.model.Payment
import kotlinx.coroutines.flow.Flow

interface PaymentRepository {
    fun getAllPayments(): Flow<List<Payment>>
    fun getPaymentsForTenant(tenantId: String): Flow<List<Payment>>
    suspend fun getPaymentById(id: String): Payment?
    suspend fun insertPayment(payment: Payment)
    suspend fun deletePayment(id: String)
}

