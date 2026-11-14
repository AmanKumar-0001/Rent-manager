package com.tenantmanager.ui.navigation

sealed class Destination(val route: String) {
    data object Dashboard : Destination("dashboard")
    data object Tenants : Destination("tenants")
    data object Payments : Destination("payments")
    data object Settings : Destination("settings")

    data object Properties : Destination("properties")
    data object TenantDetail : Destination("tenant_detail/{tenantId}") {
        fun createRoute(tenantId: String) = "tenant_detail/$tenantId"
    }
    data object AddEditTenant : Destination("add_edit_tenant?tenantId={tenantId}") {
        fun createRoute(tenantId: String? = null) = if (tenantId != null) "add_edit_tenant?tenantId=$tenantId" else "add_edit_tenant"
    }
}

