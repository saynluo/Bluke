package dev.arnv.bluke.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.arnv.bluke.R
import dev.arnv.bluke.bluetooth.BluetoothState

@Composable
internal fun ProfileNotSupportedScreen(
    bluetoothState: BluetoothState,
    onEnableBluetooth: () -> Unit,
    onRetry: () -> Unit,
    onUseLan: () -> Unit,
) {
    val checking = bluetoothState is BluetoothState.CheckingCapabilities ||
        bluetoothState is BluetoothState.InitializingCapabilities
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (checking) Icons.AutoMirrored.Filled.BluetoothSearching else Icons.Default.BluetoothDisabled,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(
                    when (bluetoothState) {
                        is BluetoothState.CheckingCapabilities, is BluetoothState.InitializingCapabilities -> R.string.unsupported_title_checking
                        is BluetoothState.Unsupported -> R.string.unsupported_title_no_bluetooth
                        is BluetoothState.ProfileNotSupported -> R.string.unsupported_title_profile
                        else -> R.string.unsupported_title_off
                    }
                ),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(
                    when (bluetoothState) {
                        is BluetoothState.CheckingCapabilities, is BluetoothState.InitializingCapabilities -> R.string.unsupported_body_checking
                        is BluetoothState.ProfileNotSupported -> R.string.unsupported_body_profile
                        is BluetoothState.Unsupported -> R.string.unsupported_body_no_bluetooth
                        else -> R.string.unsupported_body_off
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (bluetoothState is BluetoothState.ProfileNotSupported) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.unsupported_body_profile_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(24.dp))
            when (bluetoothState) {
                is BluetoothState.CheckingCapabilities, is BluetoothState.InitializingCapabilities ->
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                is BluetoothState.BluetoothOff -> Button(onClick = onEnableBluetooth, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_turn_on_bluetooth))
                }
                is BluetoothState.ProfileNotSupported -> OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_retry))
                }
                else -> Unit
            }
            if (!checking) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onUseLan, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_use_lan_instead))
                }
            }
        }
    }
}
