package com.tlz.fucktablayout


/**
 * Created by Tomlezen.
 * Data: 2018/7/23.
 * Time: 14:35.
 */
interface OnTabSelectedListener {

    fun onTabSelected(tab: FuckTab)

    fun onTabUnselected(tab: FuckTab)

    fun onTabReselected(tab: FuckTab)

}