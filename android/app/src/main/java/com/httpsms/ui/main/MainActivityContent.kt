package com.httpsms.ui.main

import android.telephony.PhoneNumberUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.httpsms.R
import com.httpsms.ui.components.FloatingTabBar
import com.httpsms.ui.components.IconTile
import com.httpsms.ui.components.LotusBadge
import com.httpsms.ui.components.SgCard
import com.httpsms.ui.components.SgRow
import com.httpsms.ui.components.SgTab
import com.httpsms.ui.components.StatusPill
import com.httpsms.ui.theme.BricolageGrotesque
import com.httpsms.ui.theme.LocalSgColors
import java.util.Locale

/** Tabs shared by the Home and Settings screens. */
val SgMainTabs = listOf(
    SgTab("Home", Icons.Outlined.Home, Icons.Filled.Home),
    SgTab("SIMs", Icons.Outlined.SimCard, Icons.Filled.SimCard),
    SgTab("Settings", Icons.Outlined.Settings, Icons.Filled.Settings),
)

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onSettingsClick: () -> Unit,
    onSimsClick: () -> Unit,
    onSmsPermissionClick: () -> Unit,
    onBatteryOptimizationClick: () -> Unit,
    onHeartbeatClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val needsAttention = !uiState.isSmsPermissionGranted || !uiState.isBatteryOptimizationDisabled

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Header: lotus avatar, greeting, title.
            Row(verticalAlignment = Alignment.CenterVertically) {
                LotusBadge()
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Namaste",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Saubhagya SMS",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            GatewayCard(
                isActive = uiState.isActiveSIM1 || uiState.isActiveSIM2,
                lastHeartbeat = uiState.lastHeartbeatTime,
                isHeartbeatLoading = uiState.isHeartbeatLoading,
                onHeartbeatClick = onHeartbeatClick
            )

            if (needsAttention) {
                Column {
                    Text(
                        text = "Needs attention",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    SgCard(contentPadding = 0.dp) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            if (!uiState.isSmsPermissionGranted) {
                                AttentionRow(
                                    title = stringResource(id = R.string.enable_sms_permission),
                                    subtitle = "The gateway can't send or read SMS without it.",
                                    iconTile = {
                                        IconTile(
                                            Icons.Filled.Sms,
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    },
                                    showDivider = !uiState.isBatteryOptimizationDisabled,
                                    onClick = onSmsPermissionClick
                                )
                            }
                            if (!uiState.isBatteryOptimizationDisabled) {
                                AttentionRow(
                                    title = stringResource(id = R.string.disable_battery_optimization),
                                    subtitle = "Android may pause the gateway in the background.",
                                    iconTile = {
                                        IconTile(
                                            Icons.Filled.BatteryAlert,
                                            MaterialTheme.colorScheme.tertiaryContainer,
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    },
                                    showDivider = false,
                                    onClick = onBatteryOptimizationClick
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SIM cards",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Manage",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onSimsClick)
                    )
                }
                SimCard(slot = 1, phoneNumber = uiState.phoneNumberSIM1, isActive = uiState.isActiveSIM1)
                if (uiState.isDualSim) {
                    SimCard(slot = 2, phoneNumber = uiState.phoneNumberSIM2, isActive = uiState.isActiveSIM2)
                }
            }

            Text(
                text = uiState.appVersion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        FloatingTabBar(
            tabs = SgMainTabs,
            selectedIndex = 0,
            onSelect = {
                when (it) {
                    1 -> onSimsClick()
                    2 -> onSettingsClick()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/** Maroon hero card: gateway status, last heartbeat, heartbeat action. */
@Composable
private fun GatewayCard(
    isActive: Boolean,
    lastHeartbeat: String,
    isHeartbeatLoading: Boolean,
    onHeartbeatClick: () -> Unit
) {
    val sg = LocalSgColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(sg.hero)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.16f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isActive) Color(0xFF7CE0B5) else sg.gold)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isActive) "GATEWAY ACTIVE" else "SENDING PAUSED",
                color = sg.onHero,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.4.sp
            )
        }

        Column {
            Text(text = "Last heartbeat", color = sg.onHeroMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = lastHeartbeat,
                color = sg.onHero,
                fontFamily = BricolageGrotesque,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.16f))
                .clickable(enabled = !isHeartbeatLoading, onClick = onHeartbeatClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Favorite, contentDescription = null, tint = sg.onHero, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(id = R.string.send_heartbeat),
                color = sg.onHero,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (isHeartbeatLoading) {
                CircularProgressIndicator(color = sg.onHero, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = sg.onHero,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AttentionRow(
    title: String,
    subtitle: String,
    iconTile: @Composable () -> Unit,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.clickable(onClick = onClick)) {
        Column {
            SgRow(
                title = title,
                subtitle = subtitle,
                leading = iconTile,
                showDivider = showDivider,
                trailing = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
    }
}

@Composable
private fun SimCard(slot: Int, phoneNumber: String, isActive: Boolean) {
    val formatted = PhoneNumberUtils.formatNumber(phoneNumber, Locale.getDefault().country) ?: phoneNumber
    SgCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = slot.toString(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontFamily = BricolageGrotesque,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SIM $slot",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatted,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isActive) {
                StatusPill(
                    text = "Sending",
                    background = MaterialTheme.colorScheme.secondaryContainer,
                    content = MaterialTheme.colorScheme.onSecondaryContainer
                )
            } else {
                StatusPill(
                    text = "Paused",
                    background = MaterialTheme.colorScheme.tertiaryContainer,
                    content = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Filled.SignalCellularAlt,
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(0.dp))
    }
}
