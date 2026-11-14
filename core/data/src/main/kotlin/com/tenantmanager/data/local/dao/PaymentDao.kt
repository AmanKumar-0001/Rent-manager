package com.tenantmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tenantmanager.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY paidDate DESC, createdAt DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE tenantId = :tenantId ORDER BY paidDate ASC")
    fun getPaymentsForTenant(tenantId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE id = :id LIMIT 1")
    suspend fun getPaymentById(id: String): PaymentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<PaymentEntity>)

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deletePayment(id: String)

    @Query("DELETE FROM payments")
    suspend fun clearAll()
}

