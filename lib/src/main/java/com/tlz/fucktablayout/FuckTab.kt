package com.tlz.fucktablayout

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources

/**
 * Created by Tomlezen.
 * Data: 2018/7/20.
 * Time: 15:21.
 */
class FuckTab(var parent: FuckTabLayout? = null, var view: FuckTabLayout.FuckTabView? = null) {

    var tag: String? = null
    var icon: Drawable? = null
        set(value) {
            field = value
            updateView()
        }
    var text: CharSequence? = null
        set(value) {
            field = value
            updateView()
        }
    var position: Int = INVALID_POSITION

    val isSelected: Boolean
        get() = parent?.getSelectedTabPosition() == position

    fun setIcon(@DrawableRes resId: Int) {
        parent?.let {
            icon = (AppCompatResources.getDrawable(it.context, resId))
        }
    }

    fun setText(@StringRes resId: Int) {
        text = parent?.context?.getString(resId)
    }

    fun select() {
        parent?.selectTab(this)
    }

    fun updateView() {
        view?.update()
    }

    fun reset() {
        position = INVALID_POSITION
        parent = null
        view = null
    }

    companion object {
        const val INVALID_POSITION = -1
    }

}