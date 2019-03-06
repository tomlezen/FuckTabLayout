package com.tlz.fucktablayout.example

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.view.PagerAdapter
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.tlz.fucktablayout.Badge
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        vp_1.adapter = ViewPagerAdapter()
        ftl_1.setupWithViewPager(vp_1)

        vp_2.adapter = ViewPagerAdapter()
        ftl_2.setupWithViewPager(vp_2)

        vp_3.adapter = ViewPagerAdapter(12)
        ftl_3.setupWithViewPager(vp_3)

        // 小红点
        ftl_2.addDotBadge(0)
        // 数字角标.
        ftl_2.addNumberBadge(1, 1)
        ftl_2.addNumberBadge(2, 100)
        ftl_2.addNumberBadge(3, 1000)
        // 自定义角标
        ftl_2.addBadge(4, object : Badge(Color.RED) {

            override fun getMeasureWidth(): Int = 20

            override fun getMeasureHeight(): Int = 20

            override fun draw(cvs: Canvas, drawnRectF: RectF) {
                paint.color = color
                cvs.drawRect(drawnRectF, paint)
            }
        })

        ftl_3.addDotBadge(0)
        // 数字角标.
        val numberBadge = ftl_3.addNumberBadge(1, 1)
        ftl_3.addNumberBadge(2, 10)

        btn_add_badge.setOnClickListener {
            ftl_3.addDotBadge(0, Color.BLUE)
        }

        btn_remove_badge.setOnClickListener {
            // 移除角标.
            ftl_3.removeBadge(0)
        }

        btn_change_number.setOnClickListener {
            // 修改数字.
            numberBadge.number = (numberBadge.number ?: 0) + 1
        }

        ftl_4.addTab(ftl_4.newTab().apply {
            text = "默认排序"
        })
        ftl_4.addTab(ftl_4.newTab().apply {
            text = "距离排序"
            ftl_4.requestLayout()
        }, false)
    }

    class ViewPagerAdapter(private val count: Int = 5) : PagerAdapter() {

        override fun isViewFromObject(view: View, `object`: Any): Boolean = view == `object`

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val imageView = ImageView(container.context)
            imageView.setBackgroundColor(Color.rgb(Random().nextInt(255), Random().nextInt(255), Random().nextInt(255)))
            container.addView(imageView)
            return imageView
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }

        override fun getCount(): Int = count

        override fun getPageTitle(position: Int): CharSequence? = "Item$position"
    }
}
