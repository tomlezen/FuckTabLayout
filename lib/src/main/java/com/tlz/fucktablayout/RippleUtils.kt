package com.tlz.fucktablayout

import android.annotation.TargetApi
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.util.StateSet
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils


/**
 * Created by Tomlezen.
 * Data: 2018/7/23.
 * Time: 17:20.
 */
object RippleUtils {

    private val USE_FRAMEWORK_RIPPLE = VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP

    private val PRESSED_STATE_SET = intArrayOf(android.R.attr.state_pressed)
    private val HOVERED_FOCUSED_STATE_SET = intArrayOf(android.R.attr.state_hovered, android.R.attr.state_focused)
    private val FOCUSED_STATE_SET = intArrayOf(android.R.attr.state_focused)
    private val HOVERED_STATE_SET = intArrayOf(android.R.attr.state_hovered)

    private val SELECTED_PRESSED_STATE_SET = intArrayOf(android.R.attr.state_selected, android.R.attr.state_pressed)
    private val SELECTED_HOVERED_FOCUSED_STATE_SET = intArrayOf(android.R.attr.state_selected, android.R.attr.state_hovered, android.R.attr.state_focused)
    private val SELECTED_FOCUSED_STATE_SET = intArrayOf(android.R.attr.state_selected, android.R.attr.state_focused)
    private val SELECTED_HOVERED_STATE_SET = intArrayOf(android.R.attr.state_selected, android.R.attr.state_hovered)
    private val SELECTED_STATE_SET = intArrayOf(android.R.attr.state_selected)

    fun convertToRippleDrawableColor(rippleColor: ColorStateList): ColorStateList {
        if (USE_FRAMEWORK_RIPPLE) {
            val size = 2

            val states = arrayOfNulls<IntArray>(size)
            val colors = IntArray(size)
            var i = 0

            states[i] = SELECTED_STATE_SET
            colors[i] = getColorForState(rippleColor, SELECTED_PRESSED_STATE_SET)
            i++

            states[i] = StateSet.NOTHING
            colors[i] = getColorForState(rippleColor, PRESSED_STATE_SET)

            return ColorStateList(states, colors)
        } else {
            val size = 10

            val states = arrayOfNulls<IntArray>(size)
            val colors = IntArray(size)
            var i = 0

            states[i] = SELECTED_PRESSED_STATE_SET
            colors[i] = getColorForState(rippleColor, SELECTED_PRESSED_STATE_SET)
            i++

            states[i] = SELECTED_HOVERED_FOCUSED_STATE_SET
            colors[i] = getColorForState(rippleColor, SELECTED_HOVERED_FOCUSED_STATE_SET)
            i++

            states[i] = SELECTED_FOCUSED_STATE_SET
            colors[i] = getColorForState(rippleColor, SELECTED_FOCUSED_STATE_SET)
            i++

            states[i] = SELECTED_HOVERED_STATE_SET
            colors[i] = getColorForState(rippleColor, SELECTED_HOVERED_STATE_SET)
            i++

            states[i] = SELECTED_STATE_SET
            colors[i] = Color.TRANSPARENT
            i++

            states[i] = PRESSED_STATE_SET
            colors[i] = getColorForState(rippleColor, PRESSED_STATE_SET)
            i++

            states[i] = HOVERED_FOCUSED_STATE_SET
            colors[i] = getColorForState(rippleColor, HOVERED_FOCUSED_STATE_SET)
            i++

            states[i] = FOCUSED_STATE_SET
            colors[i] = getColorForState(rippleColor, FOCUSED_STATE_SET)
            i++

            states[i] = HOVERED_STATE_SET
            colors[i] = getColorForState(rippleColor, HOVERED_STATE_SET)
            i++

            states[i] = StateSet.NOTHING
            colors[i] = Color.TRANSPARENT

            return ColorStateList(states, colors)
        }
    }

    @ColorInt
    private fun getColorForState(rippleColor: ColorStateList?, state: IntArray): Int {
        val color: Int = rippleColor?.getColorForState(state, rippleColor.defaultColor) ?: Color.TRANSPARENT
        return if (USE_FRAMEWORK_RIPPLE) doubleAlpha(color) else color
    }

    @ColorInt
    @TargetApi(VERSION_CODES.LOLLIPOP)
    private fun doubleAlpha(@ColorInt color: Int): Int {
        val alpha = Math.min(2 * Color.alpha(color), 255)
        return ColorUtils.setAlphaComponent(color, alpha)
    }

}