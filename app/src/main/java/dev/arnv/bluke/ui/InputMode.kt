package dev.arnv.bluke.ui

import dev.arnv.bluke.R
import androidx.annotation.StringRes
import android.content.SharedPreferences
import androidx.core.content.edit

private const val INPUT_MODE_CYCLE_PREFERENCE = "cycle_connection_modes"

enum class InputMode(
    val id: Int,
    val preferenceKey: String,
    @StringRes val labelRes: Int,
) {
    KEYBOARD(0, "keyboard", R.string.input_mode_keyboard),
    TOUCHPAD(1, "touchpad", R.string.input_mode_touchpad),
    GAMEPAD(2, "gamepad", R.string.input_mode_gamepad),
    MEDIA_PRESENTATION(4, "media_presentation", R.string.input_mode_media),
    ;

    companion object {
        fun fromId(id: Int): InputMode = entries.firstOrNull { it.id == id } ?: KEYBOARD
    }
}

private val legacyDefaultModeKeys = setOf("keyboard", "touchpad", "gamepad")
private const val REMOVED_MOUSE_MODE_KEY = "mouse"
private const val REMOVED_KEYBOARD_TOUCHPAD_MODE_KEY = "keyboard_touchpad"
val defaultInputModeKeys: Set<String> = InputMode.entries.mapTo(linkedSetOf()) { it.preferenceKey }

fun normalizeInputModeKeys(savedKeys: Set<String>?): Set<String> {
    val normalized = when {
        savedKeys == null || savedKeys == legacyDefaultModeKeys -> defaultInputModeKeys
        else -> savedKeys.mapNotNullTo(linkedSetOf()) { key ->
            when (key) {
                REMOVED_MOUSE_MODE_KEY -> InputMode.MEDIA_PRESENTATION.preferenceKey
                REMOVED_KEYBOARD_TOUCHPAD_MODE_KEY -> null
                else -> key
            }
        }.filterTo(linkedSetOf()) { key ->
            InputMode.entries.any { it.preferenceKey == key }
        }
    }
    return normalized.ifEmpty { setOf(InputMode.KEYBOARD.preferenceKey) }
}

fun SharedPreferences.enabledInputModes(): List<InputMode> {
    val saved = getStringSet(INPUT_MODE_CYCLE_PREFERENCE, null)?.toSet()
    val normalized = normalizeInputModeKeys(saved)
    if (saved != normalized) {
        edit { putStringSet(INPUT_MODE_CYCLE_PREFERENCE, normalized) }
    }
    return InputMode.entries.filter { normalized.contains(it.preferenceKey) }
}
