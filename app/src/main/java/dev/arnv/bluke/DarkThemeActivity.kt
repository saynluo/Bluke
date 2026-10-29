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
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import dev.arnv.bluke.ui.SettingsCardGroup
import dev.arnv.bluke.ui.SettingsItemData
import dev.arnv.bluke.ui.theme.MyApplicationTheme

class DarkThemeActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        
        setContent {
            MyApplicationTheme {
                var themeMode by remember { mutableIntStateOf(sharedPrefs.getInt("theme_mode", 0)) } // 0: System, 1: Off, 2: On
                var highContrastMode by remember { mutableStateOf(sharedPrefs.getBoolean("high_contrast_mode", false)) }
                
                val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
                
                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        LargeTopAppBar(
                            title = { Text(stringResource(R.string.dark_theme_title)) },
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
                            items = listOf(
                                SettingsItemData(
                                    title = stringResource(R.string.theme_system),
                                    action = {
                                        RadioButton(
                                            selected = themeMode == 0,
                                            onClick = {
                                                themeMode = 0
                                                sharedPrefs.edit { putInt("theme_mode", 0) }
                                            }
                                        )
                                    },
                                    onClick = {
                                        themeMode = 0
                                        sharedPrefs.edit { putInt("theme_mode", 0) }
                                    }
                                ),
                                SettingsItemData(
                                    title = stringResource(R.string.theme_off),
                                    action = {
                                        RadioButton(
                                            selected = themeMode == 1,
                                            onClick = {
                                                themeMode = 1
                                                sharedPrefs.edit { putInt("theme_mode", 1) }
                                            }
                                        )
                                    },
                                    onClick = {
                                        themeMode = 1
                                        sharedPrefs.edit { putInt("theme_mode", 1) }
                                    }
                                ),
                                SettingsItemData(
                                    title = stringResource(R.string.theme_on),
                                    action = {
                                        RadioButton(
                                            selected = themeMode == 2,
                                            onClick = {
                                                themeMode = 2
                                                sharedPrefs.edit { putInt("theme_mode", 2) }
                                            }
                                        )
                                    },
                                    onClick = {
                                        themeMode = 2
                                        sharedPrefs.edit { putInt("theme_mode", 2) }
                                    }
                                )
                            )
                        )

                        SettingsCardGroup(
                            title = stringResource(R.string.additional_settings),
                            items = listOf(
                                SettingsItemData(
                                    title = stringResource(R.string.high_contrast_title),
                                    subtitle = stringResource(R.string.high_contrast_sub),
                                    icon = { Icon(Icons.Default.Contrast, null, tint = MaterialTheme.colorScheme.primary) },
                                    action = {
                                        Switch(
                                            checked = highContrastMode,
                                            onCheckedChange = { 
                                                highContrastMode = it
                                                sharedPrefs.edit { putBoolean("high_contrast_mode", it) }
                                            }
                                        )
                                    }
                                )
                            )
                        )
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
