package com.example.myapplication

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Shared ringer-mode logic used by the shortcut, ToggleActivity and the in-app buttons. */
object RingerToggle {

    fun hasPermission(context: Context): Boolean =
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .isNotificationPolicyAccessGranted

    /** Rotates Normal -> Vibrate -> Silent -> Normal and plays the matching haptic. */
    fun toggle(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentMode = audioManager.ringerMode
        val ringVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)

        val next = when {
            currentMode == AudioManager.RINGER_MODE_NORMAL && ringVolume > 0 -> AudioManager.RINGER_MODE_VIBRATE
            currentMode == AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
            else -> AudioManager.RINGER_MODE_NORMAL
        }
        setMode(context, next)
    }

    /** Sets the ringer mode (restoring 50% ring volume for Normal) and plays the matching haptic. */
    fun setMode(context: Context, mode: Int) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        try {
            audioManager.ringerMode = mode
            if (mode == AudioManager.RINGER_MODE_NORMAL) {
                val maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
                audioManager.setStreamVolume(AudioManager.STREAM_RING, maxRing / 2, 0)
            }
            playHaptic(context, mode)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playHaptic(context: Context, mode: Int) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (!vibrator.hasVibrator()) return

            val pattern = when (mode) {
                AudioManager.RINGER_MODE_VIBRATE -> longArrayOf(0, 80, 60, 80) // Double pulse
                AudioManager.RINGER_MODE_SILENT -> longArrayOf(0, 30, 50, 30, 50, 30) // Triple quick pulse
                else -> null // Normal: single quiet tap
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    if (pattern != null) VibrationEffect.createWaveform(pattern, -1)
                    else VibrationEffect.createOneShot(25, 80)
                )
            } else {
                @Suppress("DEPRECATION")
                if (pattern != null) vibrator.vibrate(pattern, -1) else vibrator.vibrate(25)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
