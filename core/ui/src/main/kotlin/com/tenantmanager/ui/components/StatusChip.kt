package com.tenantmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tenantmanager.domain.model.PeriodStatus
import com.tenantmanager.domain.model.TenantStatus
import com.tenantmanager.ui.theme.StatusDueSoonBg
import com.tenantmanager.ui.theme.StatusDueSoonText
import com.tenantmanager.ui.theme.StatusLeftBg
import com.tenantmanager.ui.theme.StatusLeftText
import com.tenantmanager.ui.theme.StatusOverdueBg
import com.tenantmanager.ui.theme.StatusOverdueText
import com.tenantmanager.ui.theme.StatusPaidBg
import com.tenantmanager.ui.theme.StatusPaidText
import com.tenantmanager.ui.theme.StatusUpcomingBg
import com.tenantmanager.ui.theme.StatusUpcomingText

@Composable
fun StatusChip(
    status: TenantStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status) {
        TenantStatus.PAID -> Triple(StatusPaidBg, StatusPaidText, "PAID")
        TenantStatus.DUE_SOON -> Triple(StatusDueSoonBg, StatusDueSoonText, "DUE SOON")
        TenantStatus.OVERDUE -> Triple(StatusOverdueBg, StatusOverdueText, "OVERDUE")
        TenantStatus.LEFT -> Triple(StatusLeftBg, StatusLeftText, "LEFT")
        TenantStatus.UPCOMING -> Triple(StatusUpcomingBg, StatusUpcomingText, "UPCOMING")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun PeriodStatusChip(
    status: PeriodStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status) {
        PeriodStatus.PAID -> Triple(StatusPaidBg, StatusPaidText, "PAID")
        PeriodStatus.PARTIALLY_PAID -> Triple(StatusDueSoonBg, StatusDueSoonText, "PARTIAL")
        PeriodStatus.UNPAID -> Triple(StatusOverdueBg, StatusOverdueText, "UNPAID")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

