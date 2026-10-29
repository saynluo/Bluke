package dev.arnv.bluke.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.arnv.bluke.R
import dev.arnv.bluke.bluetooth.LanHidLink

enum class ConnectionMode(val preferenceValue: String) {
    BLUETOOTH("bluetooth"),
    LAN("lan");

    companion object {
        const val PREFERENCE = "connection_mode"
        fun fromPreference(value: String?) = entries.firstOrNull { it.preferenceValue == value } ?: BLUETOOTH
    }
}

@Composable
internal fun ConnectionModeTabs(selected: ConnectionMode, onSelect: (ConnectionMode) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        ConnectionMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, ConnectionMode.entries.size),
                icon = {
                    SegmentedButtonDefaults.Icon(active = mode == selected) {
                        Icon(
                            if (mode == ConnectionMode.LAN) Icons.Default.Wifi else Icons.Default.Bluetooth,
                            contentDescription = null,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                        )
                    }
                }
            ) {
                Text(stringResource(if (mode == ConnectionMode.LAN) R.string.mode_lan else R.string.mode_bluetooth))
            }
        }
    }
}

internal fun LazyListScope.LanSection(
    state: LanHidLink.State,
    hosts: List<LanHidLink.Host>,
    isScanning: Boolean,
    lastFailure: LanHidLink.Failure?,
    onScan: () -> Unit,
    onPickHost: (LanHidLink.Host) -> Unit,
    onDisconnect: () -> Unit,
) {
    item { LanHeaderCard(state, isScanning, lastFailure, onScan, onDisconnect) }
    val activeAddress = when (state) {
        is LanHidLink.State.Connected -> state.host.address
        is LanHidLink.State.Connecting -> state.host.address
        LanHidLink.State.Idle -> null
    }
    val others = hosts.filter { it.address != activeAddress }
    item {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionHeader(
                stringResource(R.string.lan_computers_header),
                Modifier.padding(start = 12.dp, bottom = 8.dp)
            )
            if (others.isEmpty()) {
                LanEmptyState(isScanning)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    others.forEachIndexed { index, host ->
                        LanHostRow(host, listShape(index, others.lastIndex), onClick = { onPickHost(host) })
                    }
                }
            }
        }
    }
}

@Composable
private fun LanHeaderCard(
    state: LanHidLink.State,
    isScanning: Boolean,
    lastFailure: LanHidLink.Failure?,
    onScan: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val isConnected = state is LanHidLink.State.Connected
    val content = if (isConnected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = content
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (state is LanHidLink.State.Connecting) {
                        CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 3.dp)
                    } else {
                        Icon(
                            Icons.Default.Wifi,
                            contentDescription = null,
                            tint = if (isConnected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    StatusPill(
                        label = stringResource(
                            when (state) {
                                is LanHidLink.State.Connected -> R.string.hid_status_connected
                                is LanHidLink.State.Connecting -> R.string.hid_status_connecting
                                LanHidLink.State.Idle -> R.string.lan_status_not_connected
                            }
                        ),
                        dotColor = if (isConnected) ConnectedGreen else MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when (state) {
                            is LanHidLink.State.Connected -> state.host.name
                            is LanHidLink.State.Connecting -> state.host.name
                            LanHidLink.State.Idle -> stringResource(R.string.lan_header_title)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            lastFailure == LanHidLink.Failure.WRONG_PIN -> stringResource(R.string.lan_error_wrong_pin)
                            lastFailure == LanHidLink.Failure.UNREACHABLE -> stringResource(R.string.lan_error_unreachable)
                            isConnected -> (state as LanHidLink.State.Connected).host.address
                            else -> stringResource(R.string.lan_header_hint)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (lastFailure != null) MaterialTheme.colorScheme.error else content.copy(alpha = 0.75f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (isConnected) {
                Button(
                    onClick = onDisconnect,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_disconnect))
                }
            } else {
                FilledTonalButton(onClick = onScan, enabled = !isScanning, modifier = Modifier.fillMaxWidth()) {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(if (isScanning) R.string.lan_scanning else R.string.lan_action_scan))
                }
            }
        }
    }
}

@Composable
private fun LanHostRow(host: LanHidLink.Host, shape: androidx.compose.ui.graphics.Shape, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Computer, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(host.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    host.address,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(onClick = onClick) { Text(stringResource(R.string.action_connect)) }
        }
    }
}

@Composable
private fun LanEmptyState(isScanning: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                stringResource(if (isScanning) R.string.lan_scanning else R.string.lan_empty_title),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                stringResource(R.string.lan_empty_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "python tools/bluke_lan_receiver.py",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
internal fun LanPinDialog(
    host: LanHidLink.Host,
    initialPin: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var pin by remember(host) { mutableStateOf(initialPin) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Computer, contentDescription = null) },
        title = { Text(stringResource(R.string.lan_pin_title, host.name)) },
        text = {
            Column {
                Text(stringResource(R.string.lan_pin_body), style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { value -> pin = value.filter(Char::isDigit).take(6) },
                    label = { Text(stringResource(R.string.lan_pin_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(pin) }, enabled = pin.length == 6) { Text(stringResource(R.string.action_connect)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
