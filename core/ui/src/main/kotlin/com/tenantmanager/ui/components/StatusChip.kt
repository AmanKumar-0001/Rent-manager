package com.tenantmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tenantmanager.domain.model.PeriodStatus
import com.tenantmanager.domain.model.TenantStatus
import com.tenantmanager.ui.theme.StatusDueSoonBg
import com.tenantmanager.ui.theme.StatusDueSoonBgDark
import com.tenantmanager.ui.theme.StatusDueSoonText
import com.tenantmanager.ui.theme.StatusDueSoonTextDark
import com.tenantmanager.ui.theme.StatusLeftBg
import com.tenantmanager.ui.theme.StatusLeftBgDark
import com.tenantmanager.ui.theme.StatusLeftText
import com.tenantmanager.ui.theme.StatusLeftTextDark
import com.tenantmanager.ui.theme.StatusOverdueBg
import com.tenantmanager.ui.theme.StatusOverdueBgDark
import com.tenantmanager.ui.theme.StatusOverdueText
import com.tenantmanager.ui.theme.StatusOverdueTextDark
import com.tenantmanager.ui.theme.StatusPaidBg
import com.tenantmanager.ui.theme.StatusPaidBgDark
import com.tenantmanager.ui.theme.StatusPaidText
import com.tenantmanager.ui.theme.StatusPaidTextDark
import com.tenantmanager.ui.theme.StatusUpcomingBg
import com.tenantmanager.ui.theme.StatusUpcomingBgDark
import com.tenantmanager.ui.theme.StatusUpcomingText
import com.tenantmanager.ui.theme.StatusUpcomingTextDark

data class StatusChipConfig(
    val bgColor: androidx.compose.ui.graphics.Color,
    val textColor: androidx.compose.ui.graphics.Color,
    val label: String,
    val icon: ImageVector
)

@Composable
fun StatusChip(
    status: TenantStatus,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val config = when (status) {
        TenantStatus.PAID -> StatusChipConfig(
            bgColor = if (isDark) StatusPaidBgDark else StatusPaidBg,
            textColor = if (isDark) StatusPaidTextDark else StatusPaidText,
            label = "PAID",
            icon = Icons.Default.CheckCircle
        )
        TenantStatus.DUE_SOON -> StatusChipConfig(
            bgColor = if (isDark) StatusDueSoonBgDark else StatusDueSoonBg,
            textColor = if (isDark) StatusDueSoonTextDark else StatusDueSoonText,
            label = "DUE SOON",
            icon = Icons.Default.Schedule
        )
        TenantStatus.OVERDUE -> StatusChipConfig(
            bgColor = if (isDark) StatusOverdueBgDark else StatusOverdueBg,
            textColor = if (isDark) StatusOverdueTextDark else StatusOverdueText,
            label = "OVERDUE",
            icon = Icons.Default.Warning
        )
        TenantStatus.LEFT -> StatusChipConfig(
            bgColor = if (isDark) StatusLeftBgDark else StatusLeftBg,
            textColor = if (isDark) StatusLeftTextDark else StatusLeftText,
            label = "LEFT",
            icon = Icons.AutoMirrored.Filled.Logout
        )
        TenantStatus.UPCOMING -> StatusChipConfig(
            bgColor = if (isDark) StatusUpcomingBgDark else StatusUpcomingBg,
            textColor = if (isDark) StatusUpcomingTextDark else StatusUpcomingText,
            label = "UPCOMING",
            icon = Icons.Default.Event
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(config.bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Status: ${config.label}"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = config.icon,
            contentDescription = null,
            tint = config.textColor,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = config.label,
            color = config.textColor,
            fontSize = 11.sp,
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
    val isDark = isSystemInDarkTheme()
    val (bgColor, textColor, label, icon) = when (status) {
        PeriodStatus.PAID -> listOf(
            if (isDark) StatusPaidBgDark else StatusPaidBg,
            if (isDark) StatusPaidTextDark else StatusPaidText,
            "PAID",
            Icons.Default.CheckCircle
        )
        PeriodStatus.PARTIALLY_PAID -> listOf(
            if (isDark) StatusDueSoonBgDark else StatusDueSoonBg,
            if (isDark) StatusDueSoonTextDark else StatusDueSoonText,
            "PARTIAL",
            Icons.Default.HourglassBottom
        )
        PeriodStatus.UNPAID -> listOf(
            if (isDark) StatusOverdueBgDark else StatusOverdueBg,
            if (isDark) StatusOverdueTextDark else StatusOverdueText,
            "UNPAID",
            Icons.Default.Warning
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor as androidx.compose.ui.graphics.Color)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Period status: $label"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon as ImageVector,
            contentDescription = null,
            tint = textColor as androidx.compose.ui.graphics.Color,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label as String,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
