package dev.arnv.bluke

import androidx.compose.ui.res.stringResource
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.arnv.bluke.ui.theme.MyApplicationTheme
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlin.time.Duration.Companion.milliseconds

class HelpActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isFirstRun = intent.getBooleanExtra("first_run", false)
        
        setContent {
            MyApplicationTheme {
                var timer by remember { mutableIntStateOf(if (isFirstRun) 5 else 0) }
                LaunchedEffect(timer) {
                    if (timer > 0) {
                        delay(1000L.milliseconds)
                        timer -= 1
                    }
                }
                androidx.activity.compose.BackHandler(enabled = isFirstRun && timer > 0) {
                    // Do nothing until timer completes
                }
                
                val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        LargeTopAppBar(
                            title = { Text(stringResource(R.string.help_guide_title)) },
                            navigationIcon = {
                                if (!isFirstRun || timer == 0) {
                                    IconButton(onClick = { finish() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.largeTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            scrollBehavior = scrollBehavior
                        )
                    },
                    bottomBar = {
                        if (isFirstRun) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 8.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { finish() },
                                    enabled = timer == 0,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(if (timer > 0) stringResource(R.string.help_understand_timer, timer) else stringResource(R.string.help_understand))
                                }
                            }
                        }
                    }
                ) { innerPadding ->

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        
                        StepSection(
                            icon = Icons.Default.Wifi,
                            title = stringResource(R.string.help_lan),
                            steps = listOf(
                                stringResource(R.string.help_lan_1),
                                stringResource(R.string.help_lan_2),
                                stringResource(R.string.help_lan_3),
                                stringResource(R.string.help_lan_4),
                            )
                        )

                        StepSection(
                            icon = Icons.Default.Bluetooth,  
                            title = stringResource(R.string.help_new_host),
                            steps = listOf(
                                stringResource(R.string.help_new_host_1),
                                stringResource(R.string.help_new_host_2),
                                stringResource(R.string.help_new_host_3),
                                stringResource(R.string.help_new_host_4)
                            )
                        )
                        
                        StepSection(
                            icon = Icons.Default.BluetoothConnected, 
                            title = stringResource(R.string.help_repair),
                            steps = listOf(
                                stringResource(R.string.help_repair_1),
                                stringResource(R.string.help_repair_2),
                                stringResource(R.string.help_repair_3),
                                stringResource(R.string.help_repair_4)
                            )
                        )
                        
                        StepSection(
                            icon = Icons.Default.Warning, 
                            title = stringResource(R.string.help_refused),
                            isWarning = true,
                            steps = listOf(
                                stringResource(R.string.help_refused_1),
                                stringResource(R.string.help_refused_2),
                                stringResource(R.string.help_refused_3)
                            )
                        )

                        StepSection(
                            icon = Icons.Default.ErrorOutline, 
                            title = stringResource(R.string.help_controller),
                            isWarning = true,
                            steps = listOf(
                                stringResource(R.string.help_controller_1),
                                stringResource(R.string.help_controller_2),
                                stringResource(R.string.help_controller_3),
                                stringResource(R.string.help_controller_4)
                            )
                        )

                        StepSection(
                            icon = Icons.Default.Warning,
                            title = stringResource(R.string.help_limits),
                            steps = listOf(
                                stringResource(R.string.help_limits_1),
                                stringResource(R.string.help_limits_2),
                                stringResource(R.string.help_limits_3),
                                stringResource(R.string.help_limits_4)
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StepSection(icon: ImageVector, title: String, steps: List<String>, isWarning: Boolean = false) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (isWarning) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isWarning) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isWarning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                steps.forEachIndexed { index, step ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(if (isWarning) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }
            }
        }
    }
}
