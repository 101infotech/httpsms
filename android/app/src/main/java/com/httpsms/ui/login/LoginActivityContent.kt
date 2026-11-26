package com.httpsms.ui.login

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.httpsms.R
import com.httpsms.ui.components.LotusBadge
import com.httpsms.ui.theme.LocalSgColors

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onQrScanClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val sg = LocalSgColors.current
    var editServer by rememberSaveable { mutableStateOf(false) }
    val showServerField = editServer || uiState.serverUrlError != null || uiState.serverUrl.isBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 30.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LotusBadge(size = 64.dp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "SAUBHAGYA SMS GATEWAY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Connect this\nphone",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            ApiKeyHelpText()

            Spacer(modifier = Modifier.height(6.dp))

            // Primary path: scan the pairing code from the dashboard.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(sg.hero)
                    .clickable(enabled = !uiState.isLoading, onClick = onQrScanClick)
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = sg.onHero, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Scan to pair", color = sg.onHero, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Use the QR code on the web dashboard", color = sg.onHeroMuted, fontSize = 13.sp)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = sg.onHero)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
                Text(
                    text = "or enter your API key",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
            }

            SgTextField(
                value = uiState.apiKey,
                onValueChange = { viewModel.onApiKeyChange(it) },
                label = stringResource(id = R.string.text_area_api_key),
                icon = Icons.Filled.Key,
                error = uiState.apiKeyError,
                keyboardType = KeyboardType.Text,
                enabled = !uiState.isLoading
            )

            SgTextField(
                value = uiState.phoneNumberSIM1,
                onValueChange = { viewModel.onPhoneNumberSIM1Change(it) },
                label = stringResource(id = R.string.login_phone_number_sim1),
                placeholder = stringResource(id = R.string.login_phone_number_hint),
                icon = Icons.Filled.PhoneAndroid,
                error = uiState.phoneNumberSIM1Error,
                keyboardType = KeyboardType.Phone,
                enabled = !uiState.isLoading
            )

            if (uiState.isDualSim) {
                SgTextField(
                    value = uiState.phoneNumberSIM2,
                    onValueChange = { viewModel.onPhoneNumberSIM2Change(it) },
                    label = stringResource(id = R.string.login_phone_number_sim2),
                    placeholder = stringResource(id = R.string.login_phone_number_hint),
                    icon = Icons.Filled.PhoneAndroid,
                    error = uiState.phoneNumberSIM2Error,
                    keyboardType = KeyboardType.Phone,
                    enabled = !uiState.isLoading
                )
            }

            if (showServerField) {
                SgTextField(
                    value = uiState.serverUrl,
                    onValueChange = { viewModel.onServerUrlChange(it) },
                    label = stringResource(id = R.string.server_url),
                    placeholder = stringResource(id = R.string.login_server_url_hint),
                    icon = Icons.Filled.Language,
                    error = uiState.serverUrlError,
                    keyboardType = KeyboardType.Uri,
                    enabled = !uiState.isLoading
                )
            } else {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Server: " + uiState.serverUrl.removePrefix("https://").removePrefix("http://"),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Change",
                        modifier = Modifier.clickable { editServer = true },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Button(
            onClick = onLoginClick,
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(text = "Connect phone", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ApiKeyHelpText() {
    val uriHandler = LocalUriHandler.current
    val text = stringResource(id = R.string.get_your_api_key).replace("\n", " ")
    val linkText = "sms.saubhagyagroup.com/settings"
    val start = text.indexOf(linkText)
    val annotated = buildAnnotatedString {
        if (start >= 0) {
            append(text.substring(0, start))
            withLink(
                LinkAnnotation.Url(
                    url = "https://sms.saubhagyagroup.com/settings",
                    styles = TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)),
                    linkInteractionListener = { uriHandler.openUri("https://sms.saubhagyagroup.com/settings") }
                )
            ) { append(linkText) }
            append(text.substring(start + linkText.length))
        } else {
            append(text)
        }
    }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SgTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    error: String?,
    keyboardType: KeyboardType,
    enabled: Boolean,
    placeholder: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = { Icon(icon, contentDescription = null) },
        modifier = Modifier.fillMaxWidth(),
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        enabled = enabled
    )
}
