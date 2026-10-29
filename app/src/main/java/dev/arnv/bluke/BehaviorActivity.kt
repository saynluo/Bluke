package dev.arnv.bluke

import androidx.compose.ui.res.stringResource
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import dev.arnv.bluke.ui.HARDWARE_VOLUME_REMOTE_PREFERENCE
import dev.arnv.bluke.ui.SettingsCardGroup
import dev.arnv.bluke.ui.SettingsItemData
import dev.arnv.bluke.ui.theme.MyApplicationTheme

class BehaviorActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        setContent {
            MyApplicationTheme {
                var hideUnknown by remember { mutableStateOf(preferences.getBoolean("hide_unknown", false)) }
                var hideUnlikely by remember { mutableStateOf(preferences.getBoolean("hide_unsupported", true)) }
                var showMac by remember { mutableStateOf(preferences.getBoolean("show_mac", false)) }
                var reconnect by remember { mutableStateOf(preferences.getBoolean("auto_connect", true)) }
                var keepAudio by remember { mutableStateOf(preferences.getBoolean("disconnect_audio_profiles", false)) }
                var volumeControlsHost by remember { mutableStateOf(preferences.getBoolean(HARDWARE_VOLUME_REMOTE_PREFERENCE, false)) }
                val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        LargeTopAppBar(
                            title = { Text(stringResource(R.string.settings_group_controls)) },
                            navigationIcon = { IconButton(onClick = ::finish) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back)) } },
                            scrollBehavior = scrollBehavior,
                        )
                    },
                ) { padding ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Spacer(Modifier.height(8.dp))
                        SettingsCardGroup(
                            title = stringResource(R.string.behavior_section),
                            items = listOf(
                                SettingsItemData(stringResource(R.string.behavior_reconnect), stringResource(R.string.behavior_reconnect_sub), { Icon(Icons.Default.BluetoothConnected, null, tint = MaterialTheme.colorScheme.primary) }, { Switch(reconnect, { reconnect = it; preferences.edit { putBoolean("auto_connect", it) } }) }),
                                SettingsItemData(stringResource(R.string.behavior_hide_unnamed), stringResource(R.string.behavior_hide_unnamed_sub), { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.primary) }, { Switch(hideUnknown, { hideUnknown = it; preferences.edit { putBoolean("hide_unknown", it) } }) }),
                                SettingsItemData(stringResource(R.string.behavior_hide_unlikely), stringResource(R.string.behavior_hide_unlikely_sub), { Icon(Icons.Default.FilterAltOff, null, tint = MaterialTheme.colorScheme.primary) }, { Switch(hideUnlikely, { hideUnlikely = it; preferences.edit { putBoolean("hide_unsupported", it) } }) }),
                                SettingsItemData(stringResource(R.string.behavior_show_mac), stringResource(R.string.behavior_show_mac_sub), { Icon(Icons.Default.Fingerprint, null, tint = MaterialTheme.colorScheme.primary) }, { Switch(showMac, { showMac = it; preferences.edit { putBoolean("show_mac", it) } }) }),
                                SettingsItemData(stringResource(R.string.behavior_keep_audio), stringResource(R.string.behavior_keep_audio_sub), { Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary) }, { Switch(keepAudio, { keepAudio = it; preferences.edit { putBoolean("disconnect_audio_profiles", it) } }) }),
                                SettingsItemData(stringResource(R.string.behavior_volume_keys), stringResource(R.string.behavior_volume_keys_sub), { Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = MaterialTheme.colorScheme.primary) }, { Switch(volumeControlsHost, { volumeControlsHost = it; preferences.edit { putBoolean(HARDWARE_VOLUME_REMOTE_PREFERENCE, it) } }) }),
                            ),
                        )
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
