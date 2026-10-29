package dev.arnv.bluke.ui

import dev.arnv.bluke.R
import androidx.annotation.StringRes

const val HARDWARE_VOLUME_REMOTE_PREFERENCE = "media_hardware_volume_remote"
const val TOUCHPAD_MODIFIER_POSITION_PREFERENCE = "touchpad_modifier_position"

enum class TouchpadModifierPosition(val preferenceValue: String, @StringRes val labelRes: Int) {
    OFF("off", R.string.modifiers_off),
    LEFT("left", R.string.modifiers_left),
    RIGHT("right", R.string.modifiers_right),
    ;

    fun next(): TouchpadModifierPosition = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromPreference(value: String?): TouchpadModifierPosition =
            if (value == "both") LEFT
            else entries.firstOrNull { it.preferenceValue == value } ?: OFF
    }
}

private val touchpadSpeedSteps = listOf(0.25f, 0.5f, 1f, 1.5f, 2f, 2.5f)

fun nextTouchpadSpeed(current: Float): Float =
    touchpadSpeedSteps.firstOrNull { it > current + 0.01f } ?: touchpadSpeedSteps.first()
