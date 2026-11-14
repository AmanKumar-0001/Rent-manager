package com.tenantmanager.domain.model

data class Property(
    val id: String,
    val name: String,
    val address: String? = null,
    val defaultRent: Money = Money.ZERO
)

