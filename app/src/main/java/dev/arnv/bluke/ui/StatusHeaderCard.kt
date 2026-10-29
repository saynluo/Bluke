package dev.arnv.bluke.ui

import androidx.annotation.StringRes
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.arnv.bluke.R
import dev.arnv.bluke.bluetooth.BluetoothState
import dev.arnv.bluke.bluetooth.HidFailure
import dev.arnv.bluke.bluetooth.HidLifecycleState

internal val ConnectedGreen = Color(0xFF34A853)

internal fun shouldOfferHidRestart(state: HidLifecycleState): Boolean =
    state is HidLifecycleState.Error && when (state.failure) {
        HidFailure.BINDING_REJECTED,
        HidFailure.BINDING_TIMEOUT,
        HidFailure.REGISTRATION_REJECTED,
        HidFailure.REGISTRATION_TIMEOUT -> true
        HidFailure.CONNECTION_REJECTED -> false
    }

@StringRes
internal fun hidStatusLabel(state: BluetoothState, lifecycle: HidLifecycleState): Int = when {
    state is BluetoothState.PermissionRequired -> R.string.hid_status_permission_denied
    state is BluetoothState.BluetoothOff -> R.string.hid_status_bluetooth_off
    lifecycle is HidLifecycleState.BindingProxy -> R.string.hid_status_starting
    lifecycle is HidLifecycleState.Registering -> R.string.hid_status_starting
    lifecycle is HidLifecycleState.Connecting -> R.string.hid_status_connecting
    shouldOfferHidRestart(lifecycle) -> R.string.hid_status_offline
    state is BluetoothState.Connected -> R.string.hid_status_connected
    state is BluetoothState.PairingMode -> R.string.hid_status_ready
    else -> R.string.hid_status_offline
}

/** A small coloured dot plus label, shared by the Bluetooth and LAN headers. */
@Composable
internal fun StatusPill(label: String, dotColor: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
internal fun StatusHeaderCard(
    bluetoothState: BluetoothState,
    hidLifecycleState: HidLifecycleState,
    connectedHostName: String?,
    isScanning: Boolean,
    onToggleScan: () -> Unit,
    onRestartHid: () -> Unit
) {
    val isConnected = bluetoothState is BluetoothState.Connected
    val container = if (isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (isConnected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content)
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
                    Icon(
                        imageVector = when {
                            isConnected -> Icons.Default.BluetoothConnected
                            isScanning -> Icons.AutoMirrored.Filled.BluetoothSearching
                            else -> Icons.Default.Bluetooth
                        },
                        contentDescription = null,
                        tint = if (isConnected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    StatusPill(
                        label = stringResource(hidStatusLabel(bluetoothState, hidLifecycleState)),
                        dotColor = if (isConnected) ConnectedGreen else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when {
                            isConnected -> connectedHostName ?: stringResource(R.string.unknown_host)
                            isScanning -> stringResource(R.string.bt_header_scanning)
                            else -> stringResource(R.string.bt_header_ready)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(if (isConnected) R.string.bt_header_connected_hint else R.string.bt_header_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = content.copy(alpha = 0.75f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onToggleScan,
                    modifier = Modifier.weight(1f),
                    colors = if (isScanning) ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ) else ButtonDefaults.filledTonalButtonColors()
                ) {
                    Icon(
                        imageVector = if (isScanning) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(if (isScanning) R.string.action_stop_scan else R.string.action_scan_devices))
                }
            }
            if (shouldOfferHidRestart(hidLifecycleState)) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onRestartHid,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_restart_hid))
                }
            }
        }
    }
}
