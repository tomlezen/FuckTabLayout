package com.tlz.fucktablayout

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.database.DataSetObserver
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.text.Layout
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.util.Pools
import androidx.core.view.GravityCompat
import androidx.core.view.PointerIconCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.TextViewCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.*
import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.math.min

/**
 * Created by Tomlezen.
 * Data: 2018/7/20.
 * Time: 15:12.
 */
@ViewPager.DecorView
class FuckTabLayout(ctx: Context, attrs: AttributeSet) : HorizontalScrollView(ctx, attrs) {

  private val tabs = mutableListOf<FuckTab>()
  private var selectedTab: FuckTab? = null
  private val tabViewContentBounds = RectF()

  private val slidingTabIndicator = SlidingTabIndicator(ctx)

  private var tabPaddingStart: Int
  private var tabPaddingTop: Int
  private var tabPaddingEnd: Int
  private var tabPaddingBottom: Int

  private var tabTextAppearance: Int
  var tabTextColors: ColorStateList? = null
    set(value) {
      if (value != field) {
        field = value
        updateAllTabs()
      }
    }
  var tabIconTint: ColorStateList? = null
    set(value) {
      if (value != field) {
        field = value
        updateAllTabs()
      }
    }
  private var tabRippleColorStateList: ColorStateList? = null
  var tabSelectedIndicator: Drawable? = null
    set(value) {
      if (value != field) {
        field = value
        ViewCompat.postInvalidateOnAnimation(slidingTabIndicator)
      }
    }

  private var tabIconTintMode: PorterDuff.Mode?
  private var tabTextSize: Float
  private var tabTextMultiLineSize: Float
  private var tabSelectedTextBold: Boolean
  private var tabTextIconGap: Int

  private val tabBackgroundResId: Int

  private var tabMaxWidth = Integer.MAX_VALUE
  private val requestedTabMinWidth: Int
  private val requestedTabMaxWidth: Int
  private val scrollableTabMinWidth: Int

  private var contentInsetStart: Int

  var tabGravity: Int = GRAVITY_CENTER
    set(value) {
      if (value != field) {
        field = value
        applyModeAndGravity()
      }
    }
  private var tabIndicatorAnimationDuration: Int
  @TabIndicatorGravity
  var tabIndicatorGravity: Int = INDICATOR_GRAVITY_BOTTOM
    set(value) {
      if (value != field) {
        field = value
        ViewCompat.postInvalidateOnAnimation(slidingTabIndicator)
      }
    }
  @Mode
  var mode: Int = MODE_FIXED
    set(value) {
      if (value != field) {
        field = value
        applyModeAndGravity()
      }
    }
  var inlineLabel: Boolean = false
    set(value) {
      if (value != field) {
        field = value
        for (i in 0 until slidingTabIndicator.childCount) {
          val child = slidingTabIndicator.getChildAt(i)
          if (child is FuckTabView) {
            child.updateOrientation()
          }
        }
        applyModeAndGravity()
      }
    }

  var tabIndicatorFullWidth: Boolean = true
    set(value) {
      field = value
      ViewCompat.postInvalidateOnAnimation(slidingTabIndicator)
    }
  var tabIndicatorFixedWidth: Int = 0
    set(value) {
      field = value
      ViewCompat.postInvalidateOnAnimation(slidingTabIndicator)
    }
  var unboundedRipple: Boolean = false
    set(value) {
      if (value != field) {
        field = value
        for (i in 0 until slidingTabIndicator.childCount) {
          val child = slidingTabIndicator.getChildAt(i)
          if (child is FuckTabView) {
            child.updateBackgroundDrawable(context)
          }
        }
      }
    }

  private val selectedListeners = mutableListOf<OnTabSelectedListener>()
  private var currentVpSelectedListener: OnTabSelectedListener? = null

  private var scrollAnimator: ValueAnimator? = null

  var viewPager: ViewPager? = null
  private var pagerAdapter: PagerAdapter? = null
  private val pagerAdapterObserver by lazy { PagerAdapterObserver() }
  private val pageChangeListener by lazy { TabLayoutOnPageChangeListener(this) }
  private val adapterChangeListener by lazy { AdapterChangeListener() }
  private var setupViewPagerImplicitly = false

  private val tabViewPool = Pools.SimplePool<FuckTabView>(12)

  init {
    isHorizontalScrollBarEnabled = false
    super.addView(
        slidingTabIndicator,
        0,
        LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
    )

    val a = ctx.obtainStyledAttributes(attrs, R.styleable.FuckTabLayout, R.attr.fuckTabStyle, R.style.Widget_Design_FuckTabLayout)

//     if (background is ColorDrawable) {
//       val materialShapeDrawable = MaterialShapeDrawable()
//       materialShapeDrawable.setFillColor(ColorStateList.valueOf(background.getColor()))
//       materialShapeDrawable.initializeElevationOverlay(context)
//       materialShapeDrawable.setElevation(ViewCompat.getElevation(this))
//       ViewCompat.setBackground(this, materialShapeDrawable)
//     }

    slidingTabIndicator.setSelectedIndicatorHeight(a.getDimensionPixelSize(R.styleable.FuckTabLayout_fTabIndicatorHeight, -1))
    slidingTabIndicator.setSelectedIndicatorColor(a.getColor(R.styleable.FuckTabLayout_fTabIndicatorColor, 0))
    tabSelectedIndicator = a.getDrawable(R.styleable.FuckTabLayout_fTabIndicator)
    tabIndicatorGravity = a.getInt(R.styleable.FuckTabLayout_fTabIndicatorGravity, INDICATOR_GRAVITY_BOTTOM)
    tabIndicatorFullWidth = a.getBoolean(R.styleable.FuckTabLayout_fTabIndicatorFullWidth, true)
    tabIndicatorFixedWidth = a.getDimensionPixelSize(R.styleable.FuckTabLayout_fTabIndicatorFixedWidth, 0)

    tabPaddingStart = a.getDimensionPixelSize(R.styleable.FuckTabLayout_fTabPadding, 0)
    tabPaddingTop = tabPaddingStart
    tabPaddingEnd = tabPaddingStart
    tabPaddingBottom = tabPaddingStart
    tabPaddingStart = a.getDimensionPixelSize(R.styleable.FuckTabLayout_fTabPaddingStart, tabPaddingStart)
    tabPaddingTop = a.getDimensionPixelSize(R.styleable.FuckTabLayout_fTabPaddingTop, tabPaddingTop)
    tabPaddingEnd = a.getDimensionPixelSize(R.styleable.FuckTabLayout_fTabPaddingEnd, tabPaddingEnd)
    tabPaddingBottom = a.getDimensionPixelSize(R.styleable.FuckTabLayout_fTabPaddingBottom, tabPaddingBottom)

    tabTextAppearance = a.getResourceId(R.styleable.FuckTabLayout_fTabTextAppearance, R.style.TextAppearance_Design_FuckTab)

    val ta = context.obtainStyledAttributes(tabTextAppearance, androidx.appcompat.R.styleable.TextAppearance)
    try {
      tabTextSize = ta.getDimensionPixelSize(androidx.appcompat.R.styleable.TextAppearance_android_textSize, 0).toFloat()
      tabTextColors = getColorStateList(context, ta, androidx.appcompat.R.styleable.TextAppearance_android_textColor)
    } finally {
      ta.recycle()
    }

    if (a.hasValue(R.styleable.FuckTabLayout_fTabTextSize)) {
      tabTextSize = a.getDimensionPixelSize(R.styleable.FuckTabLayout_fTabTextSize, 0).toFloat()
    }

    tabSelectedTextBold = a.getBoolean(R.styleable.FuckTabLayout_fTabSelectedTextBold, false)
    tabTextIconGap = a.getDimensionPixelSize(R.styleable.FuckTabLayout_fTabTextIconGap, dpToPx(DEFAULT_GAP_TEXT_ICON))

//    tabSelectTextSize = a.getDimensionPixelSize(R.styleable.FuckTabLayout_fTabSelectedTextSize, tabTextSize.toInt()).toFloat()

    if (a.hasValue(R.styleable.FuckTabLayout_fTabTextColor)) {
      tabTextColors = getColorStateList(context, a, R.styleable.FuckTabLayout_fTabTextColor)
    }

    if (a.hasValue(R.styleable.FuckTabLayout_fTabSelectedTextColor)) {
      val selected = a.getColor(R.styleable.FuckTabLayout_fTabSelectedTextColor, 0)
      tabTextColors = createColorStateList(tabTextColors!!.defaultColor, selected)
    }

    tabIconTint = getColorStateList(context, a, R.styleable.FuckTabLayout_fTabIconTint)
    tabIconTintMode = parseTintMode(a.getInt(R.styleable.FuckTabLayout_fTabIconTintMode, -1), null)

    tabRippleColorStateList = getColorStateList(context, a, R.styleable.FuckTabLayout_fTabRippleColor)

    tabIndicatorAnimationDuration = a.getInt(R.styleable.FuckTabLayout_fTabIndicatorAnimationDuration, ANIMATION_DURATION)

    requestedTabMinWidth = a.getDimensionPixelSize(R.styleable.FuckTabLayout_fTabMinWidth, INVALID_WIDTH)
    requestedTabMaxWidth = a.getDimensionPixelSize(R.styleable.FuckTabLayout_fTabMaxWidth, INVALID_WIDTH)
    tabBackgroundResId = a.getResourceId(R.styleable.FuckTabLayout_fTabBackground, 0)
    contentInsetStart = a.getDimensionPixelSize(R.styleable.FuckTabLayout_fTabContentStart, 0)

    mode = a.getInt(R.styleable.FuckTabLayout_fTabMode, MODE_FIXED)
    tabGravity = a.getInt(R.styleable.FuckTabLayout_fTabGravity, GRAVITY_FILL)
    inlineLabel = a.getBoolean(R.styleable.FuckTabLayout_fTabInlineLabel, false)
    unboundedRipple = a.getBoolean(R.styleable.FuckTabLayout_fTabUnboundedRipple, false)
    a.recycle()


    tabTextMultiLineSize = resources.getDimensionPixelSize(R.dimen.fuck_tab_text_size_2line).toFloat()
    scrollableTabMinWidth = resources.getDimensionPixelSize(R.dimen.fuck_tab_scrollable_min_width)

    applyModeAndGravity()
  }

  fun setSelectedTabIndicatorColor(@ColorInt color: Int) {
    slidingTabIndicator.setSelectedIndicatorColor(color)
  }

  fun setScrollPosition(position: Int, positionOffset: Float, updateSelectedText: Boolean, updateIndicatorPosition: Boolean = true) {
    val roundedPosition = Math.round(position + positionOffset)
    if (roundedPosition < 0 || roundedPosition >= slidingTabIndicator.childCount) {
      return
    }

    if (updateIndicatorPosition) {
      slidingTabIndicator.setIndicatorPositionFromTabPosition(position, positionOffset)
    }

    if (scrollAnimator?.isRunning == true) {
      scrollAnimator?.cancel()
    }
    scrollTo(calculateScrollXForTab(position, positionOffset), 0)

    if (updateSelectedText) {
      setSelectedTabView(roundedPosition)
    }
  }

  fun addTab(tab: FuckTab) {
    addTab(tab, tabs.isEmpty())
  }

  fun addTab(tab: FuckTab, position: Int) {
    addTab(tab, position, tabs.isEmpty())
  }

  fun addTab(tab: FuckTab, setSelected: Boolean) {
    addTab(tab, tabs.size, setSelected)
  }

  fun addTab(tab: FuckTab, position: Int, setSelected: Boolean) {
    if (tab.parent != this) {
      throw  IllegalArgumentException("Tab belongs to a different TabLayout.")
    }
    configureTab(tab, position)
    addTabView(tab)

    if (setSelected) {
      tab.select()
    }
  }

  private fun addTabFromItemView(item: FuckTabItem) {
    val tab = newTab()
    if (item.text != null) {
      tab.text = item.text
    }
    if (item.icon != null) {
      tab.icon = item.icon!!
    }
    addTab(tab)
  }

  fun addOnTabSelectedListener(listener: OnTabSelectedListener) {
    if (!selectedListeners.contains(listener)) {
      selectedListeners.add(listener)
    }
  }

  fun removeOnTabSelectedListener(listener: OnTabSelectedListener) {
    selectedListeners.remove(listener)
  }

  fun clearOnTabSelectedListeners() {
    selectedListeners.clear()
  }

  fun newTab(): FuckTab {
    val tab = createTabFromPool()
    tab.parent = this
    tab.view = createTabView(tab)
    return tab
  }

  protected fun createTabFromPool(): FuckTab {
    var tab = tabPool.acquire()
    if (tab == null) {
      tab = FuckTab()
    }
    return tab
  }

  protected fun releaseFromTabPool(tab: FuckTab): Boolean {
    return tabPool.release(tab)
  }

  fun getTabCount(): Int = tabs.size

  fun getTabAt(index: Int): FuckTab? = if (index < 0 || index >= getTabCount()) null else tabs[index]

  fun getSelectedTabPosition(): Int = selectedTab?.position ?: -1

  fun removeTab(tab: FuckTab) {
    if (tab.parent != this) {
      throw IllegalArgumentException("Tab does not belong to this TabLayout.")
    }

    removeTabAt(tab.position)
  }

  fun removeTabAt(position: Int) {
    val selectedTabPosition = selectedTab?.position ?: 0
    removeTabViewAt(position)

    val removedTab = tabs.removeAt(position)
    removedTab.reset()
    releaseFromTabPool(removedTab)

    val newTabCount = tabs.size
    for (i in position until newTabCount) {
      tabs[i].position = i
    }

    if (selectedTabPosition == position) {
      selectTab(if (tabs.isEmpty()) null else tabs[Math.max(0, position - 1)])
    }
  }

  fun removeAllTabs() {
    for (i in (slidingTabIndicator.childCount - 1) downTo 0) {
      removeTabViewAt(i)
    }

    var i = tabs.iterator()
    while (i.hasNext()) {
      val tab = i.next()
      i.remove()
      tab.reset()
      releaseFromTabPool(tab)
      i = tabs.iterator()
    }

    selectedTab = null
  }

  fun setInlineLabelResource(@BoolRes inlineResourceId: Int) {
    inlineLabel = resources.getBoolean(inlineResourceId)
  }

  fun setUnboundedRippleResource(@BoolRes unboundedRippleResourceId: Int) {
    unboundedRipple = (resources.getBoolean(unboundedRippleResourceId))
  }

  fun setTabTextColors(normalColor: Int, selectedColor: Int) {
    tabTextColors = createColorStateList(normalColor, selectedColor)
  }

  fun setTabIconTintResource(@ColorRes iconTintResourceId: Int) {
    tabIconTint = (AppCompatResources.getColorStateList(context, iconTintResourceId))
  }

  fun getTabRippleColor() = tabRippleColorStateList

  fun setTabRippleColor(color: ColorStateList) {
    if (tabRippleColorStateList != color) {
      tabRippleColorStateList = color
      for (i in 0 until slidingTabIndicator.childCount) {
        val child = slidingTabIndicator.getChildAt(i)
        if (child is FuckTabView) {
          child.updateBackgroundDrawable(context)
        }
      }
    }
  }

  fun setTabRippleColorResource(@ColorRes tabRippleColorResourceId: Int) {
    setTabRippleColor(AppCompatResources.getColorStateList(context, tabRippleColorResourceId))
  }

  fun setSelectedTabIndicator(@DrawableRes tabSelectedIndicatorResourceId: Int) {
    tabSelectedIndicator = if (tabSelectedIndicatorResourceId != 0) {
      (AppCompatResources.getDrawable(context, tabSelectedIndicatorResourceId))
    } else {
      null
    }
  }

  fun addDotBadge(index: Int, color: Int = Color.RED, radius: Int = dpToPx(DEFAULT_DOT_BADGE_RADIUS)): DotBadge =
      DotBadge(color, radius).apply {
        addBadge(index, this)
      }

  fun addNumberBadge(index: Int, number: Int, color: Int = Color.RED, textColor: Int = Color.WHITE, textSize: Int = dpToPx(DEFAULT_NUMBER_BADGE_TEXT_SIZE)) =
      NumberBadge(color, textColor, textSize).apply {
        this.number = number
        addBadge(index, this)
      }

  fun addBadge(index: Int, badge: Badge) {
    (slidingTabIndicator.getChildAt(index) as? FuckTabView)?.badge = badge
  }

  fun removeBadge(index: Int) {
    (slidingTabIndicator.getChildAt(index) as? FuckTabView)?.badge = null
  }

  fun getBadge(index: Int): Badge? = (slidingTabIndicator.getChildAt(index) as? FuckTabView)?.badge

  fun setupWithViewPager(
      viewPager: ViewPager?,
      autoRefresh: Boolean = true,
      implicitSetup: Boolean = false
  ) {
    this.viewPager?.removeOnPageChangeListener(pageChangeListener)
    this.viewPager?.removeOnAdapterChangeListener(adapterChangeListener)

    if (currentVpSelectedListener != null) {
      removeOnTabSelectedListener(currentVpSelectedListener!!)
      currentVpSelectedListener = null
    }

    if (viewPager != null) {
      this.viewPager = viewPager

      pageChangeListener.reset()
      viewPager.addOnPageChangeListener(pageChangeListener)

      currentVpSelectedListener = ViewPagerOnTabSelectedListener(viewPager)
      addOnTabSelectedListener(currentVpSelectedListener!!)

      val adapter = viewPager.adapter
      if (adapter != null) {
        setPagerAdapter(adapter, autoRefresh)
      }

      adapterChangeListener.autoRefresh = autoRefresh
      viewPager.addOnAdapterChangeListener(adapterChangeListener)

      setScrollPosition(viewPager.currentItem, 0f, true)
    } else {
      this.viewPager = null
      setPagerAdapter(null, false)
    }

    setupViewPagerImplicitly = implicitSetup
  }

  override fun shouldDelayChildPressedState(): Boolean = getTabScrollRange() > 0

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    if (viewPager == null) {
      val vp = parent
      if (vp is ViewPager) {
        setupWithViewPager(vp, true, true)
      }
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    if (setupViewPagerImplicitly) {
      setupWithViewPager(null)
      setupViewPagerImplicitly = false
    }
  }

  private fun getTabScrollRange(): Int =
      Math.max(0, slidingTabIndicator.width - width - paddingLeft - paddingRight)

  private fun setPagerAdapter(adapter: PagerAdapter?, addObserver: Boolean) {
    pagerAdapter?.unregisterDataSetObserver(pagerAdapterObserver)

    pagerAdapter = adapter

    if (addObserver) {
      pagerAdapter?.registerDataSetObserver(pagerAdapterObserver)
    }

    populateFromPagerAdapter()
  }

  private fun populateFromPagerAdapter() {
    removeAllTabs()

    pagerAdapter?.let {
      for (i in 0 until it.count) {
        addTab(newTab().apply {
          text = it.getPageTitle(i)
        }, false)
      }

      if (viewPager != null && it.count > 0) {
        val curItem = viewPager!!.currentItem
        if (curItem != getSelectedTabPosition() && curItem < getTabCount()) {
          selectTab(getTabAt(curItem))
        }
      }
    }
  }

  private fun updateAllTabs() {
    tabs.forEach { it.updateView() }
  }

  private fun createTabView(tab: FuckTab): FuckTabView {
    var tabView = tabViewPool.acquire()
    if (tabView == null) {
      tabView = FuckTabView(context)
    }
    tabView.tab = tab
    tabView.isFocusable = true
    tabView.minimumWidth = getTabMinWidth()
    return tabView
  }

  private fun configureTab(tab: FuckTab, position: Int) {
    tab.position = position
    tabs.add(position, tab)

    for (i in (position + 1) until tabs.size) {
      tabs[i].position = i
    }
  }

  private fun addTabView(tab: FuckTab) {
    slidingTabIndicator.addView(tab.view, tab.position, createLayoutParamsForTabs())
  }

  override fun addView(child: View?) {
    addViewInternal(child)
  }

  override fun addView(child: View?, index: Int) {
    addViewInternal(child)
  }

  override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
    addViewInternal(child)
  }

  override fun addView(child: View?, params: ViewGroup.LayoutParams?) {
    addViewInternal(child)
  }

  override fun addView(child: View?, width: Int, height: Int) {
    addViewInternal(child)
  }

  private fun addViewInternal(child: View?) {
    if (child is FuckTabItem) {
      addTabFromItemView(child)
    } else {
      throw IllegalArgumentException("Only TabItem instances can be added to TabLayout")
    }
  }

  private fun createLayoutParamsForTabs(): LinearLayout.LayoutParams {
    val lp = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
    updateTabViewLayoutParams(lp)
    return lp
  }

  private fun updateTabViewLayoutParams(lp: LinearLayout.LayoutParams) {
    if (mode == MODE_FIXED && tabGravity == GRAVITY_FILL) {
      lp.width = 0
      lp.weight = 1f
    } else {
      lp.width = LinearLayout.LayoutParams.WRAP_CONTENT
      lp.weight = 0f
    }
  }

  override fun onDraw(canvas: Canvas?) {
    canvas?.let {
      for (i in 0 until slidingTabIndicator.childCount) {
        val tabView = slidingTabIndicator.getChildAt(i)
        if (tabView is FuckTabView) {
          tabView.drawBackground(it)
        }
      }
    }

    super.onDraw(canvas)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val idealHeight = dpToPx(getDefaultHeight()) + paddingTop + paddingBottom
    val newHeightMeasureSpec = when (MeasureSpec.getMode(heightMeasureSpec)) {
      MeasureSpec.AT_MOST -> MeasureSpec.makeMeasureSpec(Math.min(idealHeight, MeasureSpec.getSize(heightMeasureSpec)), MeasureSpec.EXACTLY)
      MeasureSpec.UNSPECIFIED -> MeasureSpec.makeMeasureSpec(idealHeight, MeasureSpec.EXACTLY)
      MeasureSpec.EXACTLY -> heightMeasureSpec
      else -> heightMeasureSpec
    }

    val specWidth = MeasureSpec.getSize(widthMeasureSpec)
    if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
      tabMaxWidth = if (requestedTabMaxWidth > 0) requestedTabMaxWidth else specWidth - dpToPx(TAB_MIN_WIDTH_MARGIN)
    }

    super.onMeasure(widthMeasureSpec, newHeightMeasureSpec)

    if (childCount == 1) {
      val child = getChildAt(0)
      val remeasure = when (mode) {
        MODE_SCROLLABLE, MODE_AUTO -> child.measuredWidth < measuredWidth
        MODE_FIXED -> child.measuredWidth != measuredWidth
        else -> false
      }

      if (remeasure) {
        val childHeightMeasureSpec = getChildMeasureSpec(newHeightMeasureSpec, paddingTop + paddingBottom, child.layoutParams.height)
        val childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY)
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
      }
    }
  }

  private fun removeTabViewAt(position: Int) {
    val view = slidingTabIndicator.getChildAt(position) as? FuckTabView
    slidingTabIndicator.removeViewAt(position)
    if (view != null) {
      view.reset()
      tabViewPool.release(view)
    }
    requestLayout()
  }

  private fun animateToTab(newPosition: Int) {
    if (newPosition == FuckTab.INVALID_POSITION) {
      return
    }

    if (windowToken == null || !ViewCompat.isLaidOut(this) || slidingTabIndicator.childrenNeedLayout()) {
      setScrollPosition(newPosition, 0f, true)
      return
    }

    val startScrollX = scrollX
    val targetScrollX = calculateScrollXForTab(newPosition, 0f)

    if (startScrollX != targetScrollX) {
      ensureScrollAnimator()

      scrollAnimator?.setIntValues(startScrollX, targetScrollX)
      scrollAnimator?.start()
    }

    slidingTabIndicator.animateIndicatorToPosition(newPosition, tabIndicatorAnimationDuration)
  }

  private fun ensureScrollAnimator() {
    if (scrollAnimator == null) {
      scrollAnimator = ValueAnimator().apply {
        interpolator = FastOutSlowInInterpolator()
        duration = tabIndicatorAnimationDuration.toLong()
        addUpdateListener { scrollTo(it.animatedValue as Int, 0) }
      }
    }
  }

  protected fun setScrollAnimatorListener(listener: Animator.AnimatorListener) {
    ensureScrollAnimator()
    scrollAnimator?.addListener(listener)
  }

  private fun setSelectedTabView(position: Int) {
    val tabCount = slidingTabIndicator.childCount
    if (position < tabCount) {
      for (i in 0 until tabCount) {
        val child = slidingTabIndicator.getChildAt(i)
        child.isSelected = i == position
        child.isActivated = i == position
      }
    }
  }

  internal fun selectTab(tab: FuckTab?, updateIndicator: Boolean = true) {
    val currentTab = selectedTab

    if (currentTab == tab) {
      if (tab != null) {
        dispatchTabReselected(tab)
        animateToTab(tab.position)
      }
    } else {
      val newPosition = tab?.position ?: FuckTab.INVALID_POSITION
      if (updateIndicator) {
        if ((currentTab == null || currentTab.position == android.app.ActionBar.Tab.INVALID_POSITION) && newPosition != FuckTab.INVALID_POSITION) {
          setScrollPosition(newPosition, 0f, true)
        } else {
          animateToTab(newPosition)
        }
        if (newPosition != FuckTab.INVALID_POSITION) {
          setSelectedTabView(newPosition)
        }
      }

      selectedTab = tab
      if (currentTab != null) {
        dispatchTabUnselected(currentTab)
      }
      if (tab != null) {
        dispatchTabSelected(tab)
      }
    }
  }

  private fun dispatchTabSelected(tab: FuckTab) {
    selectedListeners.forEach { it.onTabSelected(tab) }
  }

  private fun dispatchTabUnselected(tab: FuckTab) {
    selectedListeners.forEach { it.onTabUnselected(tab) }
  }

  private fun dispatchTabReselected(tab: FuckTab) {
    selectedListeners.forEach { it.onTabReselected(tab) }
  }

  private fun calculateScrollXForTab(position: Int, positionOffset: Float): Int {
    if (mode == MODE_SCROLLABLE || mode == MODE_AUTO) {
      val selectedChild = slidingTabIndicator.getChildAt(position)
      val nextChild = if (position + 1 < slidingTabIndicator.childCount) slidingTabIndicator.getChildAt(position + 1) else null
      val selectedWidth = selectedChild.width
      val nextWidth = nextChild?.width ?: 0


      val scrollBase = selectedChild.left + (selectedWidth / 2) - (width / 2)
      val scrollOffset = ((selectedWidth + nextWidth) * 0.5f * positionOffset).toInt()
      return if (ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_LTR) scrollBase + scrollOffset else scrollBase - scrollOffset
    }
    return 0
  }

  private fun applyModeAndGravity() {
    val paddingStart = if (mode == MODE_SCROLLABLE || mode == MODE_AUTO) Math.max(0, contentInsetStart - tabPaddingStart) else 0

    ViewCompat.setPaddingRelative(slidingTabIndicator, paddingStart, 0, 0, 0)

    when (mode) {
      MODE_AUTO, MODE_FIXED
      -> slidingTabIndicator.gravity = Gravity.CENTER_HORIZONTAL
      MODE_SCROLLABLE -> slidingTabIndicator.gravity = GravityCompat.START
    }

    updateTabViews(true)
  }

  private fun updateTabViews(requestLayout: Boolean) {
    for (i in 0 until slidingTabIndicator.childCount) {
      val child = slidingTabIndicator.getChildAt(i)
      child.minimumWidth = tabMaxWidth
      updateTabViewLayoutParams(child.layoutParams as LinearLayout.LayoutParams)
      if (requestLayout) {
        child.requestLayout()
      }
    }
  }

  private fun dpToPx(dps: Int): Int {
    return Math.round(resources.displayMetrics.density * dps)
  }

  private fun lerp(startValue: Int, endValue: Int, fraction: Float): Int {
    return startValue + Math.round(fraction * (endValue - startValue))
  }

  private fun createColorStateList(defaultColor: Int, selectedColor: Int): ColorStateList {
    val states = arrayOfNulls<IntArray>(2)
    val colors = IntArray(2)
    var i = 0

    states[i] = View.SELECTED_STATE_SET
    colors[i] = selectedColor
    i++

    states[i] = View.EMPTY_STATE_SET
    colors[i] = defaultColor

    return ColorStateList(states, colors)
  }

  @Dimension(unit = Dimension.DP)
  private fun getDefaultHeight(): Int {
    var hasIconAndText = false
    for (i in 0 until tabs.size) {
      val tab = tabs[i]
      if (tab.icon != null && !TextUtils.isEmpty(tab.text)) {
        hasIconAndText = true
        break
      }
    }
    return if (hasIconAndText && !inlineLabel) DEFAULT_HEIGHT_WITH_TEXT_ICON else DEFAULT_HEIGHT
  }

  private fun getTabMinWidth(): Int {
    if (requestedTabMinWidth != INVALID_WIDTH) {
      return requestedTabMinWidth
    }
    return if (mode == MODE_SCROLLABLE || mode == MODE_AUTO) scrollableTabMinWidth else 0
  }

  private fun parseTintMode(value: Int, defaultMode: PorterDuff.Mode?): PorterDuff.Mode? =
      when (value) {
        3 -> PorterDuff.Mode.SRC_OVER
        5 -> PorterDuff.Mode.SRC_IN
        9 -> PorterDuff.Mode.SRC_ATOP
        14 -> PorterDuff.Mode.MULTIPLY
        15 -> PorterDuff.Mode.SCREEN
        16 -> PorterDuff.Mode.ADD
        else -> defaultMode
      }

  private fun getColorStateList(context: Context, attributes: TypedArray, @StyleableRes index: Int): ColorStateList? {
    if (attributes.hasValue(index)) {
      val resourceId = attributes.getResourceId(index, 0)
      if (resourceId != 0) {
        val value = AppCompatResources.getColorStateList(context, resourceId)
        if (value != null) {
          return value
        }
      }
    }
    return attributes.getColorStateList(index)
  }

  inner class FuckTabView(ctx: Context) : LinearLayout(ctx) {

    var tab: FuckTab? = null
      set(value) {
        field = value
        update()
      }

    private var tv: TextView? = null
    private var iv: ImageView? = null
    private var baseBackgroundDrawable: Drawable? = null

    private var defaultMaxLines = 2

    private val badgeDrawnRectF = RectF()
    var badge: Badge? = null
      set(value) {
        value?.target = this@FuckTabView
        field = value
        postInvalidate()
      }

    init {
      updateBackgroundDrawable(context)
      ViewCompat.setPaddingRelative(this, tabPaddingStart, tabPaddingTop, tabPaddingEnd, tabPaddingBottom)

      gravity = Gravity.CENTER
      orientation = if (inlineLabel) HORIZONTAL else VERTICAL
      isClickable = true
      ViewCompat.setPointerIcon(this, PointerIconCompat.getSystemIcon(context, PointerIconCompat.TYPE_HAND))
    }

    fun updateBackgroundDrawable(context: Context) {
      if (tabBackgroundResId != 0) {
        baseBackgroundDrawable = AppCompatResources.getDrawable(context, tabBackgroundResId)
        if (baseBackgroundDrawable?.isStateful == true) {
          baseBackgroundDrawable!!.state = drawableState
        }
      } else {
        baseBackgroundDrawable = null
      }

      val background: Drawable
      val contentDrawable = GradientDrawable()
      contentDrawable.setColor(Color.TRANSPARENT)

      if (tabRippleColorStateList != null) {
        val maskDrawable = GradientDrawable()
        maskDrawable.cornerRadius = 0.00001f
        maskDrawable.setColor(Color.WHITE)

        val rippleColor = RippleUtils.convertToRippleDrawableColor(tabRippleColorStateList!!)
        background = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          RippleDrawable(rippleColor, if (unboundedRipple) null else contentDrawable, if (unboundedRipple) null else maskDrawable)
        } else {
          val rippleDrawable = DrawableCompat.wrap(maskDrawable)
          DrawableCompat.setTintList(rippleDrawable, rippleColor)
          LayerDrawable(arrayOf(contentDrawable, rippleDrawable))
        }
      } else {
        background = contentDrawable
      }
      ViewCompat.setBackground(this, background)
      this@FuckTabLayout.invalidate()
    }

    fun drawBackground(canvas: Canvas) {
      baseBackgroundDrawable?.let {
        it.setBounds(left, top, right, bottom)
        it.draw(canvas)
      }
    }

    override fun drawableStateChanged() {
      super.drawableStateChanged()
      var changed = false
      if (baseBackgroundDrawable?.isStateful == true) {
        changed = changed or baseBackgroundDrawable!!.setState(drawableState)
      }

      if (changed) {
        invalidate()
        this@FuckTabLayout.invalidate()
      }
    }

    override fun performClick(): Boolean {
      val handled = super.performClick()

      return tab?.run {
        if (!handled) {
          playSoundEffect(SoundEffectConstants.CLICK)
        }
        tab?.select()
        true
      } ?: handled
    }

    override fun setSelected(selected: Boolean) {
      val changed = isSelected != selected

      super.setSelected(selected)

      if (changed && selected && Build.VERSION.SDK_INT < 16) {
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED)
      }

      tv?.isSelected = selected
      iv?.isSelected = selected
    }

    override fun onInitializeAccessibilityEvent(event: AccessibilityEvent?) {
      super.onInitializeAccessibilityEvent(event)
      event?.className = ActionBar.Tab::javaClass.name
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo?) {
      super.onInitializeAccessibilityNodeInfo(info)
      info?.className = ActionBar.Tab::javaClass.name
    }

    override fun onMeasure(origWidthMeasureSpec: Int, origHeightMeasureSpec: Int) {
      val specWidthSize = MeasureSpec.getSize(origWidthMeasureSpec)
      val specWidthMode = MeasureSpec.getMode(origHeightMeasureSpec)
      val maxWidth = tabMaxWidth

      val widthMeasureSpec = if (maxWidth > 0 && (specWidthMode == MeasureSpec.UNSPECIFIED || specWidthSize > maxWidth)) {
        MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST)
      } else {
        origWidthMeasureSpec
      }

      super.onMeasure(widthMeasureSpec, origHeightMeasureSpec)

      tv?.let {
        var maxLines = 2

        if (iv?.visibility == View.VISIBLE) {
          maxLines = defaultMaxLines
        } else if (it.lineCount > 1) {
          tabTextSize = tabTextMultiLineSize
        }

        val curTextSize = it.textSize
        val curLineCount = it.lineCount
        val curMaxLines = TextViewCompat.getMaxLines(it)

        if (tabTextSize != curTextSize || (curMaxLines >= 0 && maxLines != curMaxLines)) {
          var updateTextView = true

          if (mode == MODE_FIXED && tabTextSize > curTextSize && curLineCount == 1) {
            val layout = it.layout
            if (layout == null || approximateLineWidth(layout, 0, tabTextSize) > measuredWidth - paddingLeft - paddingRight) {
              updateTextView = false
            }
          }

          if (updateTextView) {
            it.setTextSize(TypedValue.COMPLEX_UNIT_PX, tabTextSize)
            it.maxLines = maxLines
            super.onMeasure(widthMeasureSpec, origHeightMeasureSpec)
          }
        }
      }
    }

    override fun draw(canvas: Canvas?) {
      super.draw(canvas)
      canvas?.let { cvs ->
        badge?.let {
          val badgeHeight = it.getMeasureHeight()
          val badgeWidth = it.getMeasureWidth()
          val contentWidth = getContentWidth()
          val contentHeight = getContentHeight()
          // 避免出界
          val right = min(width.toFloat(), width / 2 + contentWidth / 2 + badgeWidth + 2f)
          val top = max(0f, height / 2 - contentHeight / 2f - badgeHeight / 2f)
          badgeDrawnRectF.set(
              right - badgeWidth,
              top,
              right,
              top + badgeHeight
          )
          it.draw(cvs, badgeDrawnRectF)
        }
      }
    }

    fun reset() {
      tab = null
      isSelected = false
      badge = null
    }

    fun update() {
      if (iv == null) {
        iv = LayoutInflater.from(context).inflate(R.layout.layout_fuck_tab_icon, this, false) as ImageView
        addView(iv, 0)
      }
      if (tv == null) {
        tv = LayoutInflater.from(context).inflate(R.layout.layout_fuck_tab_text, this, false) as TextView
        addView(tv)
        defaultMaxLines = TextViewCompat.getMaxLines(tv!!)
      }
      TextViewCompat.setTextAppearance(tv!!, tabTextAppearance)
      tv?.setTextColor(tabTextColors!!)
      updateTextAndIcon(tv, iv)

      isSelected = tab?.isSelected ?: false
    }

    internal fun updateOrientation() {
      orientation = if (inlineLabel) HORIZONTAL else VERTICAL
      updateTextAndIcon(tv, iv)
    }

    private fun updateTextAndIcon(textView: TextView?, iconView: ImageView?) {
      val icon = tab?.icon
      val text = tab?.text

      if (iconView != null) {
        if (icon != null) {
          iconView.setImageDrawable(icon)
          iconView.visibility = View.VISIBLE
          visibility = View.VISIBLE
        } else {
          iconView.visibility = View.GONE
          iconView.setImageDrawable(null)
        }
      }

      val hasText = !TextUtils.isEmpty(text)
      if (textView != null) {
        if (hasText) {
          textView.text = text
          textView.visibility = View.VISIBLE
          visibility = View.VISIBLE
        } else {
          textView.visibility = View.GONE
          textView.text = null
        }
      }

      if (iconView != null) {
        val lp = iconView.layoutParams as MarginLayoutParams
        var bottomMargin = 0
        if (hasText && iconView.visibility == View.VISIBLE) {
          bottomMargin = tabTextIconGap
        }
        if (bottomMargin != lp.bottomMargin) {
          lp.bottomMargin = bottomMargin
          iconView.requestLayout()
        }
      }
    }

    private fun getContentWidth(): Int {
      var initialized = false
      var left = 0
      var right = 0

      if (tv?.visibility == View.VISIBLE) {
        left = tv!!.left
        right = tv!!.right
        initialized = true
      }
      if (iv?.visibility == View.VISIBLE) {
        left = if (initialized) Math.min(left, iv!!.left) else iv!!.left
        right = if (initialized) Math.max(right, iv!!.right) else iv!!.right
      }

      return right - left
    }

    private fun getContentHeight(): Int {
      var initialized = false
      var top = 0
      var bot = 0

      if (tv?.visibility == View.VISIBLE) {
        top = tv!!.top
        bot = tv!!.bottom
        initialized = true
      }
      if (iv?.visibility == View.VISIBLE) {
        top = if (initialized) Math.min(top, iv!!.top) else iv!!.top
        bot = if (initialized) Math.max(bottom, iv!!.bottom) else iv!!.bottom
      }

      return bot - top
    }

    internal fun calculateTabViewContentBounds(contentBounds: RectF) {
      var tabViewContentWidth = getContentWidth()
      val tabViewContentHeight = getContentHeight()

      if (tabViewContentWidth < dpToPx(MIN_INDICATOR_WIDTH)) {
        tabViewContentWidth = dpToPx(MIN_INDICATOR_WIDTH)
      }

      val tabViewCenterX = (left + right) / 2
      val contentLeftBounds = tabViewCenterX - tabViewContentWidth / 2
      val contentRightBounds = tabViewCenterX + tabViewContentWidth / 2

      val tabViewCenterY = (top + bottom) / 2
      val contentTopBounds = tabViewCenterY - tabViewContentHeight / 2
      val contentBotBounds = tabViewCenterY + tabViewContentHeight / 2

      contentBounds.set(contentLeftBounds.toFloat(), contentTopBounds.toFloat(), contentRightBounds.toFloat(), contentBotBounds.toFloat())
    }

    private fun approximateLineWidth(layout: Layout, line: Int, textSize: Float): Float {
      return layout.getLineWidth(line) * (textSize / layout.paint.textSize)
    }

    fun updateTextColor(color: Int) {
      tv?.setTextColor(color)
    }

    fun updateTextSize(size: Float) {
      tv?.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
      if (tabSelectedTextBold && tab?.isSelected == true) {
        tv?.typeface = Typeface.DEFAULT_BOLD
      } else {
        tv?.typeface = Typeface.DEFAULT
      }
    }
  }

  private inner class SlidingTabIndicator internal constructor(context: Context) : LinearLayout(context) {
    private var selectedIndicatorHeight: Int = 0
    private val selectedIndicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val defaultSelectionIndicator = GradientDrawable()

    internal var selectedPosition = -1
    internal var selectionOffset: Float = 0.toFloat()

    private var _layoutDirection = -1

    private var indicatorLeft = -1
    private var indicatorRight = -1

    private var indicatorAnimator: ValueAnimator? = null

    private val argbEvaluator by lazy { ArgbEvaluator() }

    init {
      setWillNotDraw(false)
    }

    internal fun setSelectedIndicatorColor(color: Int) {
      if (selectedIndicatorPaint.color != color) {
        selectedIndicatorPaint.color = color
        ViewCompat.postInvalidateOnAnimation(this)
      }
    }

    internal fun setSelectedIndicatorHeight(height: Int) {
      if (selectedIndicatorHeight != height) {
        selectedIndicatorHeight = height
        ViewCompat.postInvalidateOnAnimation(this)
      }
    }

    internal fun childrenNeedLayout(): Boolean {
      var i = 0
      while (i < childCount) {
        val child = getChildAt(i)
        if (child.width <= 0) {
          return true
        }
        i++
      }
      return false
    }

    internal fun setIndicatorPositionFromTabPosition(position: Int, positionOffset: Float) {
      if (indicatorAnimator?.isRunning == true) {
        indicatorAnimator?.cancel()
      }

      selectedPosition = position
      selectionOffset = positionOffset
      updateIndicatorPosition()
    }

    override fun onRtlPropertiesChanged(layoutDirection: Int) {
      super.onRtlPropertiesChanged(layoutDirection)
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {

        if (_layoutDirection != layoutDirection) {
          requestLayout()
          _layoutDirection = layoutDirection
        }
      }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)

      if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY) return

      if (mode == MODE_AUTO && tabGravity == GRAVITY_CENTER) {
        val count = childCount
        val largestTabWidth = (0 until count)
            .map { getChildAt(it) }
            .maxBy {
              if (it.visibility == View.VISIBLE) {
                it.measuredWidth
              } else {
                0
              }
            }?.measuredWidth ?: 0

        if (largestTabWidth <= 0) return

        val gutter = dpToPx(FIXED_WRAP_GUTTER_MIN)
        var remeasure = false

        if (largestTabWidth * count <= measuredWidth - gutter * 2) {
          for (j in 0 until count) {
            val lp = getChildAt(j).layoutParams as LayoutParams
            if (lp.width != largestTabWidth || lp.weight != 0f) {
              lp.width = largestTabWidth
              lp.weight = 0f
              remeasure = true
            }
          }
        } else {
          tabGravity = GRAVITY_FILL
          updateTabViews(false)
          remeasure = true
        }

        if (remeasure) {
          super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
      }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      super.onLayout(changed, l, t, r, b)
      if (indicatorAnimator?.isRunning == true) {
        // 这里取消动画后 导致position错误 暂时注释代码
//        indicatorAnimator?.cancel()
//        val duration = indicatorAnimator?.duration ?: 0L
//        animateIndicatorToPosition(selectedPosition, Math.round((1f - indicatorAnimator!!.animatedFraction) * duration))
      } else {
        updateIndicatorPosition()
      }
    }

    private fun updateIndicatorPosition() {
      val selectedTitle = getChildAt(selectedPosition)
      var left: Int
      var right: Int

      if (selectedTitle != null && selectedTitle.width > 0) {
        left = selectedTitle.left
        right = selectedTitle.right

        if (selectedTitle is FuckTabView) {
          if (tabIndicatorFixedWidth > 0) {
            val centerX = (selectedTitle.left + selectedTitle.right) / 2
            left = max(left, centerX - tabIndicatorFixedWidth / 2)
            right = min(right, centerX + tabIndicatorFixedWidth / 2)
          } else if (!tabIndicatorFullWidth) {
            selectedTitle.calculateTabViewContentBounds(tabViewContentBounds)
            left = tabViewContentBounds.left.toInt()
            right = tabViewContentBounds.right.toInt()
          }
        }

        if (selectionOffset > 0f && selectedPosition < childCount - 1) {
          val nextTitle = getChildAt(selectedPosition + 1)
          var nextTitleLeft = nextTitle.left
          var nextTitleRight = nextTitle.right

          if (nextTitle is FuckTabView) {
            if (tabIndicatorFixedWidth > 0) {
              val centerX = (nextTitle.left + nextTitle.right) / 2
              nextTitleLeft = max(nextTitleLeft, centerX - tabIndicatorFixedWidth / 2)
              nextTitleRight = min(nextTitleRight, centerX + tabIndicatorFixedWidth / 2)
            } else if (!tabIndicatorFullWidth) {
              nextTitle.calculateTabViewContentBounds(tabViewContentBounds)
              nextTitleLeft = tabViewContentBounds.left.toInt()
              nextTitleRight = tabViewContentBounds.right.toInt()
            }
          }

          left = (selectionOffset * nextTitleLeft + (1.0f - selectionOffset) * left).toInt()
          right = (selectionOffset * nextTitleRight + (1.0f - selectionOffset) * right).toInt()
        }
      } else {
        right = -1
        left = right
      }

      if (selectedPosition < childCount - 1) {
        (selectedTitle as FuckTabView).apply {
          updateTextColor(getTextColorByFraction(1 - selectionOffset))
          updateTextSize(tabTextSize)
        }
        (getChildAt(selectedPosition + 1) as FuckTabView).apply {
          updateTextColor(getTextColorByFraction(selectionOffset))
          updateTextSize(tabTextSize)
        }
      }

      setIndicatorPosition(left, right)
    }

    internal fun setIndicatorPosition(left: Int, right: Int) {
      if (left != indicatorLeft || right != indicatorRight) {
        indicatorLeft = left
        indicatorRight = right
        ViewCompat.postInvalidateOnAnimation(this)
      }
    }

    internal fun animateIndicatorToPosition(position: Int, duration: Int) {
      if (indicatorAnimator?.isRunning == true) {
        indicatorAnimator?.cancel()
      }

      val targetView = getChildAt(position)
      if (targetView == null) {
        updateIndicatorPosition()
        return
      }

      var targetLeft = targetView.left
      var targetRight = targetView.right

      if (targetView is FuckTabView) {
        if (tabIndicatorFixedWidth > 0) {
          val centerX = (targetView.left + targetView.right) / 2
          targetLeft = max(targetLeft, centerX - tabIndicatorFixedWidth / 2)
          targetRight = min(targetRight, centerX + tabIndicatorFixedWidth / 2)
        } else if (!tabIndicatorFullWidth) {
          targetView.calculateTabViewContentBounds(tabViewContentBounds)
          targetLeft = tabViewContentBounds.left.toInt()
          targetRight = tabViewContentBounds.right.toInt()
        }
      }


      val finalTargetLeft = targetLeft
      val finalTargetRight = targetRight

      val startLeft = indicatorLeft
      val startRight = indicatorRight

      if (startLeft != finalTargetLeft || startRight != finalTargetRight) {
        indicatorAnimator = ValueAnimator().apply {
          interpolator = FastOutSlowInInterpolator()
          setDuration(duration.toLong())
          setFloatValues(0f, 1f)
          addUpdateListener {
            val animatedValue = it.animatedFraction
            setIndicatorPosition(lerp(startLeft, finalTargetLeft, animatedValue), lerp(startRight, finalTargetRight, animatedValue))
            (getChildAt(position) as FuckTabView).apply {
              updateTextColor(getTextColorByFraction(animatedValue))
              updateTextSize(tabTextSize)
            }
            (getChildAt(selectedPosition) as FuckTabView).apply {
              updateTextColor(getTextColorByFraction(1 - animatedValue))
              updateTextSize(tabTextSize)
            }
          }
          addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
              selectedPosition = position
              selectionOffset = 0f
            }
          })
          start()
        }
      }
    }

    override fun draw(canvas: Canvas) {
      var indicatorHeight = tabSelectedIndicator?.intrinsicHeight ?: 0
      if (selectedIndicatorHeight >= 0) {
        indicatorHeight = selectedIndicatorHeight
      }

      var indicatorTop = 0
      var indicatorBottom = 0

      when (tabIndicatorGravity) {
        INDICATOR_GRAVITY_BOTTOM -> {
          indicatorTop = height - indicatorHeight
          indicatorBottom = height
        }
        INDICATOR_GRAVITY_CENTER -> {
          indicatorTop = (height - indicatorHeight) / 2
          indicatorBottom = (height + indicatorHeight) / 2
        }
        INDICATOR_GRAVITY_TOP -> {
          indicatorTop = 0
          indicatorBottom = indicatorHeight
        }
        INDICATOR_GRAVITY_STRETCH -> {
          indicatorTop = 0
          indicatorBottom = height
        }
      }

      if (indicatorLeft in 0 until indicatorRight) {
        val selectedIndicator = DrawableCompat.wrap(tabSelectedIndicator
            ?: defaultSelectionIndicator)
        selectedIndicator.setBounds(indicatorLeft, indicatorTop, indicatorRight, indicatorBottom)
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
          selectedIndicator.setColorFilter(selectedIndicatorPaint.color, PorterDuff.Mode.SRC_IN)
        } else {
          DrawableCompat.setTint(selectedIndicator, selectedIndicatorPaint.color)
        }
        selectedIndicator.draw(canvas)
      }
      super.draw(canvas)
    }

    private val selectedState = intArrayOf(android.R.attr.state_selected)

    private fun getTextColorByFraction(fraction: Float) =
        argbEvaluator.evaluate(fraction, tabTextColors?.defaultColor, tabTextColors?.getColorForState(selectedState, tabTextColors?.defaultColor
            ?: Color.WHITE)) as Int
  }

  class TabLayoutOnPageChangeListener(tabLayout: FuckTabLayout) : OnPageChangeListener {
    private val tabLayoutRef = WeakReference<FuckTabLayout>(tabLayout)
    private var previousScrollState = 0
    private var scrollState = 0

    override fun onPageScrollStateChanged(state: Int) {
      previousScrollState = scrollState
      scrollState = state
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
      val tabLayout = tabLayoutRef.get()
      if (tabLayout != null) {
        val updateText = scrollState != SCROLL_STATE_SETTLING || previousScrollState == SCROLL_STATE_DRAGGING
        val updateIndicator = !(scrollState == SCROLL_STATE_SETTLING && previousScrollState == SCROLL_STATE_IDLE)
        tabLayout.setScrollPosition(position, positionOffset, updateText, updateIndicator)
      }
    }

    override fun onPageSelected(position: Int) {
      val tabLayout = tabLayoutRef.get()
      if (tabLayout != null && tabLayout.getSelectedTabPosition() != position && position < tabLayout.getTabCount()) {
        val updateIndicator = scrollState == SCROLL_STATE_IDLE || (scrollState == SCROLL_STATE_SETTLING && previousScrollState == SCROLL_STATE_IDLE)
        tabLayout.selectTab(tabLayout.getTabAt(position), updateIndicator)
      }
    }

    fun reset() {
      previousScrollState = SCROLL_STATE_IDLE
      scrollState = previousScrollState
    }
  }

  class ViewPagerOnTabSelectedListener(private val viewPager: ViewPager) : OnTabSelectedListener {

    override fun onTabReselected(tab: FuckTab) {}

    override fun onTabSelected(tab: FuckTab) {
      viewPager.currentItem = tab.position
    }

    override fun onTabUnselected(tab: FuckTab) {}
  }

  private inner class PagerAdapterObserver : DataSetObserver() {

    override fun onChanged() {
      populateFromPagerAdapter()
    }

    override fun onInvalidated() {
      populateFromPagerAdapter()
    }
  }

  private inner class AdapterChangeListener(var autoRefresh: Boolean = false) : ViewPager.OnAdapterChangeListener {

    override fun onAdapterChanged(viewPager: ViewPager, oldAdapter: PagerAdapter?, newAdapter: PagerAdapter?) {
      if (this@FuckTabLayout.viewPager == viewPager) {
        setPagerAdapter(newAdapter, autoRefresh)
      }
    }
  }

  companion object {
    const val MODE_SCROLLABLE = 0
    const val MODE_FIXED = 1
    const val MODE_AUTO = 2

    @IntDef(value = [MODE_SCROLLABLE, MODE_FIXED, MODE_AUTO])
    @Retention(AnnotationRetention.SOURCE)
    annotation class Mode

    const val GRAVITY_FILL = 0
    const val GRAVITY_CENTER = 1

    @IntDef(flag = true, value = [GRAVITY_FILL, GRAVITY_CENTER])
    @Retention(AnnotationRetention.SOURCE)
    annotation class TabGravity

    const val INDICATOR_GRAVITY_BOTTOM = 0
    const val INDICATOR_GRAVITY_CENTER = 1
    const val INDICATOR_GRAVITY_TOP = 2
    const val INDICATOR_GRAVITY_STRETCH = 3

    @IntDef(
        value = [
          INDICATOR_GRAVITY_BOTTOM,
          INDICATOR_GRAVITY_CENTER,
          INDICATOR_GRAVITY_TOP,
          INDICATOR_GRAVITY_STRETCH]
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class TabIndicatorGravity

    @Dimension(unit = Dimension.DP)
    private val DEFAULT_HEIGHT_WITH_TEXT_ICON = 72

    @Dimension(unit = Dimension.DP)
    const val DEFAULT_GAP_TEXT_ICON = 8

    @Dimension(unit = Dimension.DP)
    private const val DEFAULT_HEIGHT = 48

    @Dimension(unit = Dimension.DP)
    private const val TAB_MIN_WIDTH_MARGIN = 56

    @Dimension(unit = Dimension.DP)
    private const val MIN_INDICATOR_WIDTH = 24

    @Dimension(unit = Dimension.DP)
    const val FIXED_WRAP_GUTTER_MIN = 16

    @Dimension(unit = Dimension.DP)
    private const val DEFAULT_DOT_BADGE_RADIUS = 2

    @Dimension(unit = Dimension.SP)
    private const val DEFAULT_NUMBER_BADGE_TEXT_SIZE = 11

    private const val INVALID_WIDTH = -1

    private const val ANIMATION_DURATION = 300

    private val tabPool = Pools.SynchronizedPool<FuckTab>(16)
  }

}