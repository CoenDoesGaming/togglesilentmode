package com.example.myapplication // Updated to match your project's exact package name

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon as DrawableIcon
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Register the App Shortcut when the app starts
        createAppShortcut()

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Helper function to play mode-specific haptics
        fun triggerHapticFeedback(context: Context, mode: Int) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }

                if (vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val effect = when (mode) {
                            AudioManager.RINGER_MODE_NORMAL -> VibrationEffect.createOneShot(25, 80) // Quieter, shorter tap for Normal
                            AudioManager.RINGER_MODE_VIBRATE -> VibrationEffect.createWaveform(longArrayOf(0, 80, 60, 80), -1) // Double pulse for Vibrate
                            AudioManager.RINGER_MODE_SILENT -> VibrationEffect.createWaveform(longArrayOf(0, 30, 50, 30, 50, 30), -1) // Multiple little vibrations for Silent
                            else -> VibrationEffect.createOneShot(25, 80)
                        }
                        vibrator.vibrate(effect)
                    } else {
                        @Suppress("DEPRECATION")
                        when (mode) {
                            AudioManager.RINGER_MODE_NORMAL -> vibrator.vibrate(25)
                            AudioManager.RINGER_MODE_VIBRATE -> vibrator.vibrate(longArrayOf(0, 80, 60, 80), -1)
                            AudioManager.RINGER_MODE_SILENT -> vibrator.vibrate(longArrayOf(0, 30, 50, 30, 50, 30), -1)
                            else -> vibrator.vibrate(25)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Intercept app launch to see if it came from our shortcut
        if (intent?.action == "$packageName.TOGGLE_MODE") {
            if (notificationManager.isNotificationPolicyAccessGranted) {
                val currentMode = audioManager.ringerMode
                val ringVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)

                try {
                    if (currentMode == AudioManager.RINGER_MODE_NORMAL && ringVolume > 0) {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                        triggerHapticFeedback(this, AudioManager.RINGER_MODE_VIBRATE)
                    } else if (currentMode == AudioManager.RINGER_MODE_VIBRATE) {
                        // Standard Silent Mode
                        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                        triggerHapticFeedback(this, AudioManager.RINGER_MODE_SILENT)
                    } else {
                        // Restore Normal mode with volume
                        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                        val maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
                        audioManager.setStreamVolume(AudioManager.STREAM_RING, maxRing / 2, 0) // Restore to 50%
                        triggerHapticFeedback(this, AudioManager.RINGER_MODE_NORMAL)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Exit immediately so the app doesn't stay open on the screen
                finish()

                return
            } else {
                Toast.makeText(this, "Permission required to toggle mode", Toast.LENGTH_LONG).show()
            }
        }

        setContent {
            val darkTheme = isSystemInDarkTheme()

            // Adjust status bar icon colors based on light/dark theme
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                }
            }

            // Automatically adapts to system dark/light theme
            MaterialTheme(
                colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
            ) {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SilentModeController()
                }
            }
        }
    }

    private fun createAppShortcut() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = getSystemService(ShortcutManager::class.java)

            val toggleIntent = Intent(this, MainActivity::class.java).apply {
                action = "$packageName.TOGGLE_MODE"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val shortcut = ShortcutInfo.Builder(this, "toggle_mode")
                .setShortLabel("Toggle Mode")
                .setLongLabel("Toggle Ringer Mode")
                // Uses the new custom vector icon instead of the app launcher icon!
                .setIcon(DrawableIcon.createWithResource(applicationContext, R.drawable.ic_custom_shortcut))
                .setIntent(toggleIntent)
                .build()

            shortcutManager?.dynamicShortcuts = listOf(shortcut)
        }
    }
}

@Composable
fun SilentModeController() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Services required to check/change ringer mode and permissions
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val notificationManager = remember { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    // Helper to trigger haptics from UI buttons
    val playHaptic: (Int) -> Unit = { mode ->
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = when (mode) {
                        AudioManager.RINGER_MODE_NORMAL -> VibrationEffect.createOneShot(25, 80) // Quieter, shorter tap
                        AudioManager.RINGER_MODE_VIBRATE -> VibrationEffect.createWaveform(longArrayOf(0, 80, 60, 80), -1)
                        AudioManager.RINGER_MODE_SILENT -> VibrationEffect.createWaveform(longArrayOf(0, 30, 50, 30, 50, 30), -1) // Multiple little vibrations
                        else -> VibrationEffect.createOneShot(25, 80)
                    }
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    when (mode) {
                        AudioManager.RINGER_MODE_NORMAL -> vibrator.vibrate(25)
                        AudioManager.RINGER_MODE_VIBRATE -> vibrator.vibrate(longArrayOf(0, 80, 60, 80), -1)
                        AudioManager.RINGER_MODE_SILENT -> vibrator.vibrate(longArrayOf(0, 30, 50, 30, 50, 30), -1)
                        else -> vibrator.vibrate(25)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // State variables to track the current mode, volume, and permission status
    var currentMode by remember { mutableIntStateOf(audioManager.ringerMode) }
    var ringVol by remember { mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_RING)) }
    var hasPermission by remember { mutableStateOf(notificationManager.isNotificationPolicyAccessGranted) }

    // Observe the app lifecycle. When the user returns to the app (e.g., from the Settings screen),
    // we want to refresh the permission status and the current ringer mode.
    DisposableEffect(lifecycleOwner) {
        // Receiver to auto-update UI if mode changes in the background (or takes a split second to apply)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AudioManager.RINGER_MODE_CHANGED_ACTION) {
                    currentMode = audioManager.ringerMode
                    ringVol = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION))

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = notificationManager.isNotificationPolicyAccessGranted
                currentMode = audioManager.ringerMode
                ringVol = audioManager.getStreamVolume(AudioManager.STREAM_RING)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            context.unregisterReceiver(receiver)
        }
    }

    // UI Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Current Mode",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Display the current active mode
        Text(
            text = when (currentMode) {
                AudioManager.RINGER_MODE_SILENT -> "Silent Mode"
                AudioManager.RINGER_MODE_VIBRATE -> "Vibrate Mode"
                AudioManager.RINGER_MODE_NORMAL -> "Normal Mode"
                else -> "Unknown"
            },
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Android requires explicit user permission to toggle "Do Not Disturb" / Silent Mode
        if (!hasPermission) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Permission Required",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To change the device to Silent mode, you must grant 'Do Not Disturb' access.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            // Directs the user to the exact settings page needed
                            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                            context.startActivity(intent)
                        }
                    ) {
                        Text("Open Settings")
                    }
                }
            }
        } else {
            // Once permission is granted, display the control buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {

                Button(
                    onClick = {
                        try {
                            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                            val maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
                            audioManager.setStreamVolume(AudioManager.STREAM_RING, maxRing / 2, 0)
                            currentMode = audioManager.ringerMode
                            ringVol = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                            playHaptic(AudioManager.RINGER_MODE_NORMAL)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text("Set to Normal")
                }

                Button(
                    onClick = {
                        try {
                            audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                            currentMode = audioManager.ringerMode
                            ringVol = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                            playHaptic(AudioManager.RINGER_MODE_VIBRATE)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.7f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Set to Vibrate")
                }

                Button(
                    onClick = {
                        try {
                            // Standard Silent Mode
                            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                            currentMode = audioManager.ringerMode
                            ringVol = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                            playHaptic(AudioManager.RINGER_MODE_SILENT)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.7f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("Set to Silent")
                }
            }
        }
    }
}