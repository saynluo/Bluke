package dev.arnv.bluke

import androidx.compose.ui.res.stringResource
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.unit.dp
import dev.arnv.bluke.ui.SettingsCardGroup
import dev.arnv.bluke.ui.SettingsItemData
import dev.arnv.bluke.ui.theme.MyApplicationTheme

class SettingsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MyApplicationTheme {
                val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                val lifecycleOwner = LocalLifecycleOwner.current
                var isDevMode by remember { mutableStateOf(sharedPrefs.getBoolean("is_developer_mode", false)) }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            isDevMode = sharedPrefs.getBoolean("is_developer_mode", false)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }
                
                val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        LargeTopAppBar(
                            title = { Text(stringResource(R.string.settings_title)) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                                }
                            },
                            colors = TopAppBarDefaults.largeTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            scrollBehavior = scrollBehavior
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Spacer(Modifier.height(8.dp))
                        SettingsCardGroup(
                            title = stringResource(R.string.settings_group_personalization),
                            items = listOf(
                                SettingsItemData(
                                    title = stringResource(R.string.look_and_feel_title),
                                    subtitle = stringResource(R.string.settings_look_subtitle),
                                    icon = { Icon(Icons.Default.ColorLens, null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = { 
                                        startActivity(Intent(this@SettingsActivity, LookAndFeelActivity::class.java))
                                    }
                                ),
                                SettingsItemData(
                                    title = stringResource(R.string.keyboard_settings_title),
                                    subtitle = stringResource(R.string.settings_keyboard_subtitle),
                                    icon = { Icon(Icons.Default.Keyboard, null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        startActivity(Intent(this@SettingsActivity, KeyboardSettingsActivity::class.java))
                                    }
                                ),
                                SettingsItemData(
                                    title = stringResource(R.string.controller_settings_title),
                                    subtitle = stringResource(R.string.settings_controller_subtitle),
                                    icon = { Icon(Icons.Default.Gamepad, null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        startActivity(Intent(this@SettingsActivity, ControllerSettingsActivity::class.java))
                                    }
                                ),
                            )
                        )
                        SettingsCardGroup(
                            title = stringResource(R.string.settings_group_controls),
                            items = listOf(
                                SettingsItemData(
                                    title = stringResource(R.string.behavior_title),
                                    subtitle = stringResource(R.string.settings_behavior_subtitle),
                                    icon = { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = { 
                                        startActivity(Intent(this@SettingsActivity, BehaviorActivity::class.java))
                                    }
                                ),
                            )
                        )
                        SettingsCardGroup(
                            title = stringResource(R.string.settings_group_support),
                            items = listOf(
                                SettingsItemData(
                                    title = stringResource(R.string.help_title),
                                    subtitle = stringResource(R.string.settings_help_subtitle),
                                    icon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        startActivity(Intent(this@SettingsActivity, HelpActivity::class.java))
                                    }
                                ),
                                SettingsItemData(
                                    title = stringResource(R.string.about_title),
                                    subtitle = stringResource(R.string.settings_about_subtitle),
                                    icon = { Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = { 
                                        startActivity(Intent(this@SettingsActivity, AboutActivity::class.java))
                                    }
                                )
                            ).let { baseList ->
                                if (isDevMode) {
                                    baseList + SettingsItemData(
                                        title = stringResource(R.string.developer_options_title),
                                        subtitle = stringResource(R.string.settings_developer_subtitle),
                                        icon = { Icon(Icons.Default.Code, null, tint = MaterialTheme.colorScheme.primary) },
                                        onClick = { 
                                            startActivity(Intent(this@SettingsActivity, DeveloperOptionsActivity::class.java))
                                        }
                                    )
                                } else {
                                    baseList
                                }
                            }
                        )
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
