package com.httpsms.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.PhoneMissed
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.httpsms.R
import com.httpsms.Settings
import com.httpsms.ui.components.FloatingTabBar
import com.httpsms.ui.components.GroupLabel
import com.httpsms.ui.components.IconTile
import com.httpsms.ui.components.LotusBadge
import com.httpsms.ui.components.SgCard
import com.httpsms.ui.components.SgRow
import com.httpsms.ui.components.SgSwitch
import com.httpsms.ui.components.StatusPill
import com.httpsms.ui.main.SgMainTabs
import com.httpsms.ui.theme.BricolageGrotesque

private const val TAB_SIMS = 1

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    initialTab: Int,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(1, 2)) }
    val scroll = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (tab == TAB_SIMS) {
                SimsTab(viewModel)
            } else {
                PreferencesTab(viewModel, onLogoutClick)
            }
        }

        FloatingTabBar(
            tabs = SgMainTabs,
            selectedIndex = tab,
            onSelect = { if (it == 0) onBackClick() else tab = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ColumnScope.SimsTab(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Text(
        text = "SIM cards",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground
    )

    SimSettingsCard(
        slot = 1,
        phoneNumber = uiState.phoneNumberSIM1,
        isDefault = true,
        outgoing = uiState.isActiveSIM1,
        onOutgoing = { viewModel.setActiveSIM1(context, it) },
        incoming = uiState.isIncomingSIM1Enabled,
        onIncoming = { viewModel.setIncomingSIM1Enabled(context, it) },
        calls = uiState.isIncomingCallEventsSIM1Enabled,
        onCalls = { viewModel.setIncomingCallEventsSIM1Enabled(context, it) }
    )

    if (uiState.isDualSim) {
        SimSettingsCard(
            slot = 2,
            phoneNumber = uiState.phoneNumberSIM2,
            isDefault = false,
            outgoing = uiState.isActiveSIM2,
            onOutgoing = { viewModel.setActiveSIM2(context, it) },
            incoming = uiState.isIncomingSIM2Enabled,
            onIncoming = { viewModel.setIncomingSIM2Enabled(context, it) },
            calls = uiState.isIncomingCallEventsSIM2Enabled,
            onCalls = { viewModel.setIncomingCallEventsSIM2Enabled(context, it) }
        )
    }
}

@Composable
private fun ColumnScope.PreferencesTab(viewModel: SettingsViewModel, onLogoutClick: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val serverHost = remember { Settings.getServerUrlOrDefault(context).host ?: "" }

    Text(
        text = "Settings",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground
    )

    SgCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LotusBadge(size = 52.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Saubhagya Group", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(serverHost, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusPill(
                text = "Connected",
                background = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }

    Column {
        GroupLabel("Security")
        SgCard {
            OutlinedTextField(
                value = uiState.encryptionKey,
                onValueChange = { viewModel.setEncryptionKey(context, it) },
                label = { Text(stringResource(id = R.string.encryption_key)) },
                leadingIcon = {
                    Icon(Icons.Filled.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            SgRow(
                title = stringResource(id = R.string.encrypt_received_messages),
                subtitle = if (uiState.encryptionKey.isEmpty()) "Add an encryption key first" else "End-to-end with your key",
                enabled = uiState.encryptionKey.isNotEmpty(),
                leading = {
                    IconTile(
                        Icons.Filled.Lock,
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.onPrimaryContainer,
                        36.dp
                    )
                },
                trailing = {
                    SgSwitch(
                        checked = uiState.isEncryptReceivedMessagesEnabled,
                        onCheckedChange = { viewModel.setEncryptReceivedMessagesEnabled(context, it) },
                        enabled = uiState.encryptionKey.isNotEmpty()
                    )
                }
            )
        }
    }

    Column {
        GroupLabel("Device")
        SgCard(contentPadding = 0.dp) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                SgRow(
                    title = stringResource(id = R.string.enable_debug_logs),
                    subtitle = "Share logs with support",
                    leading = {
                        IconTile(
                            Icons.Filled.BugReport,
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                            36.dp
                        )
                    },
                    trailing = {
                        SgSwitch(
                            checked = uiState.isDebugLogEnabled,
                            onCheckedChange = { viewModel.setDebugLogEnabled(context, it) }
                        )
                    }
                )
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onLogoutClick)
            .padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Logout,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Disconnect this phone",
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SimSettingsCard(
    slot: Int,
    phoneNumber: String,
    isDefault: Boolean,
    outgoing: Boolean,
    onOutgoing: (Boolean) -> Unit,
    incoming: Boolean,
    onIncoming: (Boolean) -> Unit,
    calls: Boolean,
    onCalls: (Boolean) -> Unit,
) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SIM $slot", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    if (isDefault) {
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusPill(
                            text = "Default",
                            background = MaterialTheme.colorScheme.tertiaryContainer,
                            content = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                Text(
                    text = phoneNumber.ifEmpty { "No number" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Filled.SignalCellularAlt,
                contentDescription = null,
                tint = if (outgoing || incoming) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
        SgRow(
            title = "Send messages",
            subtitle = "API messages go out on this SIM",
            showDivider = true,
            leading = {
                IconTile(
                    Icons.AutoMirrored.Filled.Send,
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                    36.dp
                )
            },
            trailing = { SgSwitch(checked = outgoing, onCheckedChange = onOutgoing) }
        )
        SgRow(
            title = "Receive messages",
            subtitle = "Forward incoming SMS to the server",
            showDivider = true,
            leading = {
                IconTile(
                    Icons.AutoMirrored.Filled.CallReceived,
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.onSecondaryContainer,
                    36.dp
                )
            },
            trailing = { SgSwitch(checked = incoming, onCheckedChange = onIncoming) }
        )
        SgRow(
            title = "Missed calls",
            subtitle = "Send call events to webhooks",
            leading = {
                IconTile(
                    Icons.AutoMirrored.Filled.PhoneMissed,
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.onTertiaryContainer,
                    36.dp
                )
            },
            trailing = { SgSwitch(checked = calls, onCheckedChange = onCalls) }
        )
    }
}
