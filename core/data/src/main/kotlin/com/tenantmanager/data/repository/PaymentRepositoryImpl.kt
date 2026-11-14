package com.tenantmanager.data.repository

import com.tenantmanager.data.local.dao.PaymentDao
import com.tenantmanager.data.mapper.toDomain
import com.tenantmanager.data.mapper.toEntity
import com.tenantmanager.domain.model.Payment
import com.tenantmanager.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PaymentRepositoryImpl(
    private val paymentDao: PaymentDao
) : PaymentRepository {

    override fun getAllPayments(): Flow<List<Payment>> {
        return paymentDao.getAllPayments().map { list -> list.map { it.toDomain() } }
    }

    override fun getPaymentsForTenant(tenantId: String): Flow<List<Payment>> {
        return paymentDao.getPaymentsForTenant(tenantId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getPaymentById(id: String): Payment? {
        return paymentDao.getPaymentById(id)?.toDomain()
    }

    override suspend fun insertPayment(payment: Payment) {
        paymentDao.insertPayment(payment.toEntity())
    }

    override suspend fun deletePayment(id: String) {
        paymentDao.deletePayment(id)
    }
}

