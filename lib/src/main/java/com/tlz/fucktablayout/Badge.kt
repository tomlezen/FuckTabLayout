package com.tlz.fucktablayout

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Created by Tomlezen.
 * Data: 2018/7/25.
 * Time: 9:27.
 */

/**
 * @property color Int 基础颜色.
 * @property paint Paint
 * @constructor
 */
abstract class Badge(protected val color: Int) {

    internal var target: View? = null

    protected val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    abstract fun getMeasureWidth(): Int
    abstract fun getMeasureHeight(): Int

    abstract fun draw(cvs: Canvas, drawnRectF: RectF)

}

/**
 * 点.
 * @property radius Float 半径.
 * @constructor
 */
class DotBadge(color: Int, private val radius: Int) : Badge(color) {

    override fun getMeasureWidth(): Int = radius * 2

    override fun getMeasureHeight(): Int = getMeasureWidth()

    override fun draw(cvs: Canvas, drawnRectF: RectF) {
        paint.color = color
        cvs.drawCircle(drawnRectF.centerX(), drawnRectF.centerY(), min(drawnRectF.width() / 2f, drawnRectF.height() / 2f), paint)
    }

}

/**
 * 数字.
 * @property textColor Int
 * @property textSize Float
 * @property number Int?
 * @constructor
 */
class NumberBadge(color: Int, private val textColor: Int, private val textSize: Int) : Badge(color) {

    var number: Int? = null
        set(value) {
            field = value
            target?.postInvalidate()
        }

    init {
        paint.textSize = textSize.toFloat()
    }

    override fun getMeasureWidth(): Int {
        val measureHeight = getMeasureHeight()
        var measureWidth = paint.measureText(getDrawnStr()).roundToInt()
        if (measureHeight > measureWidth + 5) {
            measureWidth = measureHeight
        } else {
            measureWidth += measureHeight / 2
        }
        return measureWidth
    }

    override fun getMeasureHeight(): Int = (paint.descent() - paint.ascent()).roundToInt()

    override fun draw(cvs: Canvas, drawnRectF: RectF) {
        val drawnText = getDrawnStr()
        if (drawnText.isNotEmpty()) {
            paint.color = color
            cvs.drawRoundRect(drawnRectF, drawnRectF.height() / 2, drawnRectF.height() / 2, paint)
            paint.color = textColor
            cvs.drawText(
                    drawnText,
                    drawnRectF.centerX() - paint.measureText(drawnText) / 2,
                    drawnRectF.top + (drawnRectF.height() - paint.descent() + paint.ascent()) / 2 - paint.ascent(),
                    paint)
        }
    }

    private fun getDrawnStr(): String =
            when {
                number == null -> ""
                number!! in (0 until 10) -> number.toString()
                number!! in (10 until 100) -> "9+"
                number!! in (100 until 1000) -> "99+"
                number!! > 999 -> "999+"
                else -> ""
            }

}