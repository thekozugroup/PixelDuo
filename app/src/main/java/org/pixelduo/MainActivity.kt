package org.pixelduo

import android.content.Intent
import android.hardware.SensorManager
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private var enabled by mutableStateOf(false)
    private var allowed by mutableStateOf(false)
    private var powerSave by mutableStateOf(false)
    private var motion by mutableStateOf(true)
    private var setup by mutableStateOf(false)
    private val prefs by lazy { getSharedPreferences("pixelduo", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val supported = getSystemService(SensorManager::class.java).hingeSensor() != null
        setContent {
            val colors = if (isSystemInDarkTheme()) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)
            @OptIn(ExperimentalMaterial3ExpressiveApi::class)
            MaterialExpressiveTheme(colorScheme = colors, motionScheme = MotionScheme.expressive()) {
                Surface(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
                        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth()
                            .verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(40.dp))
                            FoldIllustration(Modifier.size(176.dp))
                            Spacer(Modifier.height(32.dp))
                            Text(stringResource(R.string.headline), style = MaterialTheme.typography.headlineLarge,
                                modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.intro), style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(32.dp))
                            Surface(shape = MaterialTheme.shapes.extraLarge,
                                color = if (enabled && allowed) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHigh) {
                                Row(Modifier.fillMaxWidth().heightIn(min = 88.dp)
                                    .toggleable(enabled && allowed, supported, Role.Switch) { toggle(it) }
                                    .padding(horizontal = 24.dp, vertical = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(stringResource(R.string.toggle_label), style = MaterialTheme.typography.titleMedium)
                                        Text(stringResource(if (enabled && allowed) R.string.on else R.string.off),
                                            style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Switch(checked = enabled && allowed, onCheckedChange = null, enabled = supported)
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                            val status = when {
                                !supported -> R.string.unsupported
                                enabled && !allowed -> R.string.setup_needed
                                enabled && powerSave -> R.string.battery_paused
                                enabled && !motion -> R.string.motion_paused
                                enabled -> R.string.active_hint
                                prefs.getBoolean("pending", false) -> R.string.setup_needed
                                else -> R.string.inactive_hint
                            }
                            Text(stringResource(status), style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.battery_hint), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    if (setup) AlertDialog(onDismissRequest = { setup = false },
                        title = { Text(stringResource(R.string.setup_title)) },
                        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                            Text(stringResource(R.string.setup_body))
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(R.string.restricted_hint), style = MaterialTheme.typography.bodySmall)
                        } },
                        confirmButton = { TextButton(onClick = {
                            setup = false
                            prefs.edit().putBoolean("pending", true).apply()
                            try {
                                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            } catch (_: android.content.ActivityNotFoundException) {
                                Toast.makeText(this, R.string.permission_missing, Toast.LENGTH_LONG).show()
                            }
                        }) { Text(stringResource(R.string.open_settings)) } },
                        dismissButton = { TextButton(onClick = { setup = false }) { Text(stringResource(R.string.not_now)) } })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        allowed = getSystemService(AccessibilityManager::class.java)
            .getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.packageName == packageName }
        if (allowed && prefs.getBoolean("pending", false)) {
            prefs.edit().putBoolean("enabled", true).putBoolean("pending", false).apply()
        }
        enabled = prefs.getBoolean("enabled", false)
        powerSave = getSystemService(PowerManager::class.java).isPowerSaveMode
        motion = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }

    private fun toggle(value: Boolean) {
        if (value && !allowed) setup = true else {
            enabled = value
            prefs.edit().putBoolean("enabled", value).putBoolean("pending", false).apply()
        }
    }
}

@Composable private fun FoldIllustration(modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier, shape = MaterialTheme.shapes.extraLarge, color = colors.surfaceContainerHigh) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.lucide_fold_horizontal), contentDescription = null,
                tint = colors.primary, modifier = Modifier.size(80.dp))
        }
    }
}
