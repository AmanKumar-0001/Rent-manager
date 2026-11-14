package com.tenantmanager.domain.sync

import com.tenantmanager.domain.model.Money
import com.tenantmanager.domain.model.Payment
import com.tenantmanager.domain.model.Property
import com.tenantmanager.domain.model.Tenant

object SheetRowMapper {

    const val TAB_TENANTS = "Tenants"
    const val TAB_PAYMENTS = "Payments"
    const val TAB_PROPERTIES = "Properties"

    fun mapToTabs(snapshot: SyncSnapshot): List<SheetTabData> {
        val propertyMap = snapshot.properties.associateBy { it.id }
        val tenantMap = snapshot.tenants.associateBy { it.id }

        val tenantsTab = mapTenantsTab(snapshot.tenants, propertyMap, snapshot.includeNotes, snapshot.currencySymbol)
        val paymentsTab = mapPaymentsTab(snapshot.payments, tenantMap, snapshot.includeNotes, snapshot.currencySymbol)
        val propertiesTab = mapPropertiesTab(snapshot.properties, snapshot.currencySymbol)

        return listOf(tenantsTab, paymentsTab, propertiesTab)
    }

    private fun mapTenantsTab(
        tenants: List<Tenant>,
        propertyMap: Map<String, Property>,
        includeNotes: Boolean,
        currencySymbol: String
    ): SheetTabData {
        val headers = mutableListOf(
            "Tenant ID",
            "Property",
            "Unit",
            "Name",
            "Phone",
            "Email",
            "Monthly Rent",
            "Due Day",
            "Join Date",
            "Leave Date",
            "Archived",
            "Security Deposit",
            "Deposit Returned Date"
        )
        if (includeNotes) {
            headers.add("Notes")
        }

        val rows = tenants.map { tenant ->
            val propName = tenant.propertyId?.let { propertyMap[it]?.name } ?: "—"
            val row = mutableListOf(
                tenant.id,
                propName,
                tenant.unitName,
                tenant.name,
                tenant.phone,
                tenant.email ?: "—",
                tenant.monthlyRent.formatINR(currencySymbol),
                tenant.dueDay.toString(),
                tenant.joinDate.toString(),
                tenant.leaveDate?.toString() ?: "—",
                if (tenant.isArchived) "YES" else "NO",
                tenant.securityDeposit.formatINR(currencySymbol),
                tenant.depositReturnedDate?.toString() ?: "—"
            )
            if (includeNotes) {
                row.add(tenant.notes ?: "")
            }
            row
        }

        return SheetTabData(
            title = TAB_TENANTS,
            headers = headers,
            rows = rows
        )
    }

    private fun mapPaymentsTab(
        payments: List<Payment>,
        tenantMap: Map<String, Tenant>,
        includeNotes: Boolean,
        currencySymbol: String
    ): SheetTabData {
        val headers = mutableListOf(
            "Payment ID",
            "Tenant Name",
            "Unit",
            "Period Month",
            "Amount",
            "Paid Date"
        )
        if (includeNotes) {
            headers.add("Notes")
        }

        val rows = payments.map { payment ->
            val tenant = tenantMap[payment.tenantId]
            val tenantName = tenant?.name ?: "Unknown"
            val unitName = tenant?.unitName ?: "—"

            val row = mutableListOf(
                payment.id,
                tenantName,
                unitName,
                payment.periodMonth.toString(),
                payment.amount.formatINR(currencySymbol),
                payment.paidDate.toString()
            )
            if (includeNotes) {
                row.add(payment.notes ?: "")
            }
            row
        }

        return SheetTabData(
            title = TAB_PAYMENTS,
            headers = headers,
            rows = rows
        )
    }

    private fun mapPropertiesTab(
        properties: List<Property>,
        currencySymbol: String
    ): SheetTabData {
        val headers = listOf(
            "Property ID",
            "Property Name",
            "Address",
            "Default Rent"
        )

        val rows = properties.map { property ->
            listOf(
                property.id,
                property.name,
                property.address ?: "—",
                property.defaultRent.formatINR(currencySymbol)
            )
        }

        return SheetTabData(
            title = TAB_PROPERTIES,
            headers = headers,
            rows = rows
        )
    }
}

