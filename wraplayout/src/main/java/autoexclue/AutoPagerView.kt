package autoexclue

import am.widget.wraplayout.R
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.children
import autoexclue.adapter.CellAdapterDataObservable
import autoexclue.adapter.CellAdapterDataObserver
import autoexclue.layout.LayoutChildrenHelper
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Created by lei.jialin on 2021/4/19
 * 自动分页的控件
 * 【注意⚠️】：1.需要预设ViewHolder高度来初始化总页数 {@link autoexclue.AutoPagerView.ViewHolder#firstPresetHeight()}
 *             不预设高度会导致一直往后翻时总页数在增加的情况
 *           2.为了需要显示总页数增加了很多额外计算，第一次测量时会预估所有项的预设高度值。
 *
 * TODO:
 * 1. 增加 titleDecoration
 * 2. FIX callbackPageIndex没有显示的问题
 */
open class AutoPagerView : ViewGroup {
    companion object {
        const val FILL_PREV = -1
        const val FILL_NEXT = 1
        const val MAX_PER_MEASURE_CNT = 10
        private val ATTRS = intArrayOf(android.R.attr.horizontalSpacing,
                android.R.attr.verticalSpacing)

        fun logOf(s: String) {
            Log.d("AutoPagerView", s)
        }
    }

    private val mTempRect: Rect = Rect()

    var layoutMaster: LayoutMaster? = null
        set(value) {
            field?.setRecyclerView(null)
            value?.setRecyclerView(this)
            field = value
        }

    var adapter: Adapter<*>? = null
        set(value) {
            field?.unregisterAdapterDataObserver(mObserver)
            field?.onDetachedFromRecyclerView(this)
            field = value
            value?.registerAdapterDataObserver(mObserver)
            value?.onAttachedToRecyclerView(this)
        }
    private var mVerticalSpacing = 0
    private var mHorizontalSpacing = 0

    private var mIsAttachToWindow = false
    private var mAdapterUpdateDuringMeasure = false
    private val mObserver = MtObserver()

    protected var mLayoutState: LayoutState = LayoutState()

    open class ViewHolder constructor(val itemView: View) {
        var viewType: Int = -1
        var mPosition = -1
    }

    constructor(context: Context) : super(context) {
        initView(context, null, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView(context, attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initView(context, attrs, defStyleAttr)
    }

    @TargetApi(21)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        initView(context, attrs, defStyleAttr)
    }

    private fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        val a = context.obtainStyledAttributes(attrs, ATTRS, defStyleAttr, 0)
        val n = a.indexCount
        var horizontalSpacing = 0
        var verticalSpacing = 0
        for (i in 0 until n) {
            when (val attr = a.getIndex(i)) {
                0 -> horizontalSpacing = a.getDimensionPixelSize(attr, horizontalSpacing)
                1 -> verticalSpacing = a.getDimensionPixelSize(attr, verticalSpacing)
            }
        }
        a.recycle()
        val custom = context.obtainStyledAttributes(attrs, R.styleable.AutoPagerView)
        horizontalSpacing = custom.getDimensionPixelSize(
                R.styleable.AutoPagerView_APV_HorizontalSpacing, horizontalSpacing)
        verticalSpacing = custom.getDimensionPixelSize(
                R.styleable.AutoPagerView_APV_VerticalSpacing, verticalSpacing)
        custom.recycle()
        mHorizontalSpacing = horizontalSpacing
        mVerticalSpacing = verticalSpacing
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return LayoutParams(context, attrs)
    }

    /**
     * Returns a set of layout parameters with a width of
     * [ViewGroup.LayoutParams.WRAP_CONTENT],
     * a height of [ViewGroup.LayoutParams.WRAP_CONTENT] and no spanning.
     */
    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams {
        return LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    // Override to allow type-checking of LayoutParams.
    override fun checkLayoutParams(p: ViewGroup.LayoutParams): Boolean {
        return p is LayoutParams
    }

    override fun generateLayoutParams(lp: ViewGroup.LayoutParams): ViewGroup.LayoutParams {
        return if (lp is LayoutParams) {
            LayoutParams(lp)
        } else {
            LayoutParams(lp)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mIsAttachToWindow = true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mIsAttachToWindow = false
    }

    class LayoutState() {

        enum class Step {
            None,
            MeasureAll,
            Measure,
            Layout,
        }

        fun isNormalMeasure(): Boolean = mStep == Step.Measure
        fun isStepLayout(): Boolean = mStep == Step.Layout
        fun isInitMeasureAll(): Boolean = mStep == Step.MeasureAll
        fun defaultMeasureSize(): Int = if (maxLayoutRow > 0) maxLayoutRow else AutoPagerView.MAX_PER_MEASURE_CNT

        var beenLayoutAtLeastOnce: Boolean = true
        var maxLayoutRow: Int = -1
        var mPendingSwitchPage: Int = -1
        var mPendingSwitchToDirection: Int = FILL_NEXT
        var mPendingChangeSegmentStart: Int = -1
        var nextTimeMeasureAll = false
        var mStep: Step = Step.None
        var needComputeTotalPage = false
    }

    class LayoutResult {

    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measureStartTime = System.currentTimeMillis()
        layoutMaster?.setMeasureSpecs(mLayoutState, widthMeasureSpec, heightMeasureSpec)
        layoutMaster?.measureChildrens(mLayoutState)
        layoutMaster?.decideMeasuredDimensionFromChildren(mLayoutState, widthMeasureSpec, heightMeasureSpec)
        logOf("onMeasure: time: ${System.currentTimeMillis() - measureStartTime},state:${mLayoutState.mStep},  measureHeight: ${measuredHeight}")
    }

    private fun getDataSize(): Int = adapter?.totalDataSize() ?: 0

    private fun isViewGone(child: View): Boolean {
        if (child.visibility == GONE) {
            (child.layoutParams as? LayoutParams)?.apply {
                this.isWithinValidLayout = false
                this.isInvisibleOutValidLayout = false
            }
            return true
        }
        return false
    }

    private fun getViewHolderCache(viewType: Int): ViewHolder? = vhCache.get(viewType)?.firstOrNull()

    private fun viewTypeAt(dataIndex: Int): Int = adapter?.itemViewType(dataIndex) ?: -1

    private fun takeAwayViewHolderCache(viewType: Int): ViewHolder? = vhCache.get(viewType)?.pollLast()

    private fun appendViewHolderCache(viewType: Int, child: ViewHolder) = vhCache.get(viewType)?.add(child)

    private fun getViewOf(index: Int): View? {
        val vh = tryObtainViewHolder(index)
        vh ?: return null
        vh.mPosition = index
        adapter?.bindData2ViewHolder(index, vh, this)
        return vh.itemView
    }

    private val vhCache: HashMap<Int, LinkedList<ViewHolder>> = HashMap()

    private fun tryObtainViewHolder(index: Int): ViewHolder? {
        val cache = takeAwayViewHolderCache(viewTypeAt(index))
        if (cache == null) {
            val viewType = adapter?.itemViewType(index) ?: -1
            vhCache.getOrElse(viewType) {
                vhCache.put(viewType, LinkedList<ViewHolder>())
            }
            val time = System.currentTimeMillis()
            val vh = createNewViewHolder(index)
            logOf("createNew time: ${System.currentTimeMillis() - time}")
            return vh
        }
        return cache
    }

    private fun createNewViewHolder(index: Int): ViewHolder? {
        val holder = adapter?.onCreateViewHolderAt(index, this)
        val lp = holder?.itemView?.layoutParams
        val aeLayoutParams: LayoutParams
        if (lp == null) {
            aeLayoutParams = generateDefaultLayoutParams() as LayoutParams
            holder?.itemView?.layoutParams = aeLayoutParams
        } else if (!checkLayoutParams(lp)) {
            aeLayoutParams = generateLayoutParams(lp) as LayoutParams
            holder.itemView.layoutParams = aeLayoutParams
        } else {
            aeLayoutParams = lp as LayoutParams
        }
        aeLayoutParams.mViewHolder = holder
        return holder
    }


    private fun defaultOnMeasure(widthSpec: Int, heightSpec: Int) {
        // calling LayoutManager here is not pretty but that API is already public and it is better
        // than creating another method since this is internal.
        val s = layoutMaster?.getCurSegment()
        val preMeasureWidth = s?.width ?: 0
        val preMeasureHeight = s?.height ?: 0

        val width = LayoutMaster.chooseDesiredSize(widthSpec,
                paddingLeft + paddingRight + preMeasureWidth,
                ViewCompat.getMinimumWidth(this))
        val height = LayoutMaster.chooseDesiredSize(heightSpec,
                paddingTop + paddingBottom + preMeasureHeight,
                ViewCompat.getMinimumHeight(this))
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        layoutMaster?.onLayoutWithInBounds(mLayoutState, changed, l, t, r, b)
    }

    fun switchToPage(index: Int) = layoutMaster?.switchToPage(index)

    fun scrollToPosition(dataIndex: Int) = layoutMaster?.scrollToPosition(dataIndex)

    fun getCurrentIndex() = layoutMaster?.getCurrentIndex() ?: 0

    fun getTotalPageCount() = layoutMaster?.getTotalPageCount() ?: 0

    /**
     * 设置水平间距
     *
     * @param pixelSize 水平间距
     */
    var horizontalSpacing: Int
        get() = mHorizontalSpacing
        set(pixelSize) {
            mHorizontalSpacing = pixelSize
            requestLayout()
        }

    /**
     * 设置垂直间距
     *
     * @param pixelSize 垂直间距
     */
    var verticalSpacing: Int
        get() = mVerticalSpacing
        set(pixelSize) {
            mVerticalSpacing = pixelSize
            requestLayout()
        }


    private val layoutChildrenHelper: LayoutChildrenHelper by lazy {
        object : LayoutChildrenHelper {
            override fun getChildAt(i: Int): View? = this@AutoPagerView.getChildAt(i)
            override fun getChildrenCount(): Int = this@AutoPagerView.childCount
            override fun preMeasureChild(index: Int): Int = this@AutoPagerView.preMeasureChild(index)

            override fun removeAllChildOnScreen() = this@AutoPagerView.removeAllChildOnScreen()

            override fun getViewOf(dataIndex: Int): View? = this@AutoPagerView.getViewOf(dataIndex)

            override fun removeChild(dataIndex: Int, childView: View) = this@AutoPagerView.removeChild(childView)

            override fun addView(childView: View) = this@AutoPagerView.addView(childView)
            override fun isViewGone(child: View): Boolean = this@AutoPagerView.isViewGone(child)
            override fun onChildAdded(dataIndex: Int, childWidth: Int, childHeight: Int) = this@AutoPagerView.onChildAdded(dataIndex, childWidth, childHeight)
        }
    }

    private fun removeAllChildOnScreen() {
        children.forEach {
            val lp = it.layoutParams as? LayoutParams
            lp?.mViewHolder?.let { vh ->
                appendViewHolderCache(vh.viewType, vh)
            }
        }
        removeAllViews()
    }

    private fun removeChild(childView: View) {
        val lp = childView.layoutParams as? LayoutParams ?: return
        lp.mViewHolder?.let {
            appendViewHolderCache(it.viewType, it)
        }
        removeView(childView)
    }

    private val mHeightRecorded: SparseArray<Pair<Int, Int>> = SparseArray()

    private fun onChildAdded(dataIndex: Int, childWidth: Int, childHeight: Int) {
        val viewType = viewTypeAt(dataIndex)
        val vh = getViewHolderCache(viewType)
        val lp = vh?.itemView?.layoutParams as? LayoutParams
        lp?.setLayoutHeight(childWidth, childHeight)
        mHeightRecorded.put(viewType, Pair(childWidth, childHeight))
    }

    private fun preMeasureChild(index: Int): Int {
        val pair = adapter?.itemViewType(index)?.let { viewType -> mHeightRecorded.get(viewType) }
        val recorded = pair?.second
        if (recorded.isPositive()) return recorded.validInt()

        val vh = getViewHolderCache(index)
        val itemView = vh?.itemView
        val lp = itemView?.layoutParams as? LayoutParams
        val previousLayoutHeight = lp?.layoutHeight
        if (previousLayoutHeight.isPositive()) return previousLayoutHeight.validInt()

        val measureHeight = itemView?.measuredHeight
        if (measureHeight.isPositive()) return measureHeight.validInt()

        val presetHeight = adapter?.measureHeightAt(context, index)
        if (presetHeight.isPositive()) return presetHeight.validInt()
        return 0
    }

    fun Int?.isPositive() = this != null && this > 0
    fun Int?.validInt() = this ?: 0


    //<editor-fold desc="measure child decorate">
    private fun getDecoratedBoundsWithMarginsInt(view: View?, outBounds: Rect) {
        val lp = view?.layoutParams as? AutoPagerView.LayoutParams ?: return
        val insets = lp.mDecorInsets
        outBounds.set(view.left - insets.left - lp.leftMargin,
                view.top - insets.top - lp.topMargin,
                view.right + insets.right + lp.rightMargin,
                view.bottom + insets.bottom + lp.bottomMargin)
    }

    // TODO: 完善decor
    private fun getItemDecorInsetsForChild(child: View): Rect? = Rect()
    //</editor-fold>

    /**
     * Per-child layout information associated with AutoExcludeLayout.
     */
    class LayoutParams : MarginLayoutParams {
        var layoutHeight: Int = 0
        var layoutWidth: Int = 0
        fun setLayoutHeight(childWidth: Int, childHeight: Int) {
            layoutHeight = childHeight
            layoutWidth = childWidth
        }

        val mDecorInsets = Rect()
        var mViewHolder: ViewHolder? = null
        var isWithinValidLayout = false
        var isInvisibleOutValidLayout = false

        constructor(c: Context, attrs: AttributeSet?) : super(c, attrs)
        constructor(width: Int, height: Int) : super(width, height)
        constructor(source: ViewGroup.LayoutParams?) : super(source)
        constructor(source: LayoutParams) : super(source)
    }

    private inner class MtObserver : CellAdapterDataObserver() {
        override fun onChanged() {
            requestLayout()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            triggerUpdateProcessor()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            triggerUpdateProcessor()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            triggerUpdateProcessor()
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            triggerUpdateProcessor()
        }

        fun triggerUpdateProcessor() {
            mAdapterUpdateDuringMeasure = true
            requestLayout()
        }
    }

    abstract class Adapter<VH : ViewHolder> {
        abstract fun totalDataSize(): Int

        private val mObservable = CellAdapterDataObservable()
        private val viewHolderSparseArray = SparseArray<VH>()

        abstract fun onCreateViewHolderAt(index: Int, parent: ViewGroup): VH


        fun onAttachedToRecyclerView(autoPagerView: AutoPagerView) {}

        fun onDetachedFromRecyclerView(autoPagerView: AutoPagerView) {}

        //        <editor-fold desc="AdapterDataObserver">
        fun registerAdapterDataObserver(observer: CellAdapterDataObserver) {
            mObservable.registerObserver(observer)
        }

        fun unregisterAdapterDataObserver(observer: CellAdapterDataObserver) {
            mObservable.unregisterObserver(observer)
        }

        fun hasObservers(): Boolean {
            return mObservable.hasObservers()
        }

        fun notifyDataSetChanged() {
            mObservable.notifyChanged()
        }

        fun notifyItemChanged(position: Int) {
            mObservable.notifyItemRangeChanged(position, 1)
        }

        fun notifyItemRangeChanged(positionStart: Int, itemCount: Int) {
            mObservable.notifyItemRangeChanged(positionStart, itemCount)
        }

        fun notifyItemInserted(position: Int) {
            mObservable.notifyItemRangeInserted(position, 1)
        }

        fun notifyItemMoved(fromPosition: Int, toPosition: Int) {
            mObservable.notifyItemMoved(fromPosition, toPosition)
        }

        fun notifyItemRangeInserted(positionStart: Int, itemCount: Int) {
            mObservable.notifyItemRangeInserted(positionStart, itemCount)
        }

        fun notifyItemRemoved(position: Int) {
            mObservable.notifyItemRangeRemoved(position, 1)
        }

        fun notifyItemRangeRemoved(positionStart: Int, itemCount: Int) {
            mObservable.notifyItemRangeRemoved(positionStart, itemCount)
        }
        //        </editor-fold desc="AdapterDataObserver">

        abstract fun bindData2ViewHolder(index: Int, vh: ViewHolder, parent: ViewGroup)

        abstract fun measureHeightAt(context: Context, index: Int): Int

        abstract fun itemViewType(index: Int): Int
    }

    abstract class LayoutMaster {
        var mPendingPosition: Int = -1
        protected var mWidthMode: Int = MeasureSpec.EXACTLY
        protected var mHeightMode: Int = MeasureSpec.EXACTLY
        protected var mWidth: Int = 0
        protected var mHeight: Int = 0
        var mAutoPagerView: AutoPagerView? = null
        var mChildrenHelper: LayoutChildrenHelper? = null

        protected val segments: MutableList<Segment> = ArrayList()
        protected val tempMeasureSegments: SparseArray<Segment> = SparseArray()
        var curSegIndex = 0
        var mTotalPageCount = 0

        abstract fun dispatchLayout(layoutState: LayoutState)

        open fun canScrollHorizontally(): Boolean = false

        open fun canScrollVertically(): Boolean = false

        private fun validDataSizeInSegment(toIndex: Int): Int {

            var valid: Int
            val s = getCurSegment()
            val lastItemSequenceOfThisPage = s?.end ?: 0
            val total = getTotalPageCount()
            val dataSize = getDataSize()
            if (toIndex <= 0) {
                valid = 0
            } else if (toIndex >= total - 1 && lastItemSequenceOfThisPage == dataSize) {
                valid = total - 1
            } else {
                valid = toIndex
            }
            logOf("validDataSize:  switchToIndex$toIndex, valid: $valid, lastItem: $lastItemSequenceOfThisPage, dataSize: $dataSize")
            return valid
        }

        fun setRecyclerView(autoPagerView: AutoPagerView?) {
            if (autoPagerView == null) {
                mAutoPagerView = null
                mWidth = 0
                mHeight = 0
                mChildrenHelper = null
            } else {
                mAutoPagerView = autoPagerView
                mChildrenHelper = autoPagerView.layoutChildrenHelper
                mWidth = autoPagerView.width
                mHeight = autoPagerView.height
            }
            mWidthMode = View.MeasureSpec.EXACTLY
            mHeightMode = MeasureSpec.EXACTLY
        }

        fun setMeasureSpecs(layoutState: LayoutState, wSpec: Int, hSpec: Int) {
            mWidthMode = MeasureSpec.getMode(wSpec)
            mHeightMode = MeasureSpec.getMode(hSpec)
            val widthSize = MeasureSpec.getSize(wSpec)
            val heightSize = MeasureSpec.getSize(hSpec)
            if (widthSize != mWidth || heightSize != mHeight) {
                setNextTimeMeasureAll(layoutState)
            }
            mWidth = widthSize
            mHeight = heightSize
        }

        open fun decideMeasuredDimensionFromChildren(layoutState: LayoutState, widthSpec: Int, heightSpec: Int) {
            val pagerView = mAutoPagerView ?: return
            val count: Int = getChildCount()
            if (count == 0) {
                pagerView.defaultOnMeasure(widthSpec, heightSpec)
                return
            }
            var minX = Int.MAX_VALUE
            var minY = Int.MAX_VALUE
            var maxX = Int.MIN_VALUE
            var maxY = Int.MIN_VALUE
            for (i in 0 until count) {
                val child: View = getChildAt(i) ?: continue
                val bounds: Rect = pagerView.mTempRect
                getDecoratedBoundsWithMargins(child, bounds)
                minX = Math.min(minX, bounds.left)
                maxX = Math.max(maxX, bounds.right)
                minY = Math.min(minY, bounds.top)
                maxY = Math.max(maxY, bounds.bottom)
            }
            pagerView.mTempRect.set(minX, minY, maxX, maxY)
            setMeasuredDimension(pagerView.mTempRect, widthSpec, heightSpec)
        }

        companion object {
            @JvmStatic
            fun chooseDesiredSize(spec: Int, desired: Int, min: Int): Int {
                val mode = MeasureSpec.getMode(spec)
                val size = MeasureSpec.getSize(spec)
                return when (mode) {
                    MeasureSpec.EXACTLY -> size
                    MeasureSpec.AT_MOST -> Math.min(size, Math.max(desired, min))
                    MeasureSpec.UNSPECIFIED -> Math.max(desired, min)
                    else -> Math.max(desired, min)
                }
            }

            @JvmStatic
            fun getChildMeasureSpecInner(parentSize: Int, parentMode: Int, padding: Int,
                                         childDimension: Int): Int {
                val size = Math.max(0, parentSize - padding)
                var resultSize = 0
                var resultMode = 0
                if (childDimension >= 0) {
                    resultSize = childDimension
                    resultMode = MeasureSpec.EXACTLY
                } else if (childDimension == ViewGroup.LayoutParams.MATCH_PARENT) {
                    resultSize = size
                    resultMode = parentMode
                } else if (childDimension == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    resultSize = size
                    resultMode = if (parentMode == MeasureSpec.AT_MOST || parentMode == MeasureSpec.EXACTLY) {
                        MeasureSpec.AT_MOST
                    } else {
                        MeasureSpec.UNSPECIFIED
                    }
                }
                return MeasureSpec.makeMeasureSpec(resultSize, resultMode)
            }

            @JvmStatic
            fun isMeasurementUpToDate(childSize: Int, spec: Int, dimension: Int): Boolean {
                val specMode = MeasureSpec.getMode(spec)
                val specSize = MeasureSpec.getSize(spec)
                if (dimension > 0 && childSize != dimension) {
                    return false
                }
                return when (specMode) {
                    MeasureSpec.UNSPECIFIED -> true
                    MeasureSpec.AT_MOST -> specSize >= childSize
                    MeasureSpec.EXACTLY -> specSize == childSize
                    else -> false
                }
            }
        }

        open fun setMeasuredDimension(childrenBounds: Rect, wSpec: Int, hSpec: Int) {
            val usedWidth: Int = childrenBounds.width() + getPaddingLeft() + getPaddingRight()
            val usedHeight: Int = childrenBounds.height() + getPaddingTop() + getPaddingBottom()
            val width = chooseDesiredSize(wSpec, usedWidth, getMinimumWidth())
            val height = chooseDesiredSize(hSpec, usedHeight, getMinimumHeight())
            setFinalMeasuredDimension(resolveSize(width, wSpec), resolveSize(height, hSpec))
        }

        protected fun getMinimumWidth(): Int = mAutoPagerView?.let { ViewCompat.getMinimumWidth(it) }
                ?: 0

        protected fun getMinimumHeight(): Int = mAutoPagerView?.let { ViewCompat.getMinimumHeight(it) }
                ?: 0

        protected fun getPaddingLeft(): Int = mAutoPagerView?.paddingLeft ?: 0
        protected fun getPaddingTop(): Int = mAutoPagerView?.paddingTop ?: 0
        protected fun getPaddingRight(): Int = mAutoPagerView?.paddingRight ?: 0
        protected fun getPaddingBottom(): Int = mAutoPagerView?.paddingBottom ?: 0
        protected fun getVerticalSpacing(): Int = mAutoPagerView?.verticalSpacing ?: 0
        protected fun getHorizontalSpacing(): Int = mAutoPagerView?.horizontalSpacing ?: 0
        private fun getItemDecorInsetsForChild(child: View): Rect = mAutoPagerView?.getItemDecorInsetsForChild(child)
                ?: Rect()

        fun setFinalMeasuredDimension(widthSize: Int, heightSize: Int) {
            mAutoPagerView?.setMeasuredDimension(widthSize, heightSize)
        }

        fun getChildAt(i: Int): View? = mChildrenHelper?.getChildAt(i)

        fun getChildCount(): Int = mChildrenHelper?.getChildrenCount() ?: 0

        protected fun getDecoratedBoundsWithMargins(child: View?, bounds: Rect) = mAutoPagerView?.getDecoratedBoundsWithMarginsInt(child, bounds)

        fun getTotalPageCount() = mTotalPageCount

        fun getCurrentIndex() = curSegIndex

        fun switchToPage(index: Int) {
            val oldIndex = curSegIndex
            val newIndex = validDataSizeInSegment(index)
            if (oldIndex != newIndex) {
                mPendingPosition = segmentAt(newIndex)?.start ?: segmentAt(newIndex - 1)?.end ?: -1
                mAutoPagerView?.post { mAutoPagerView?.requestLayout() }
            }
        }

        fun getDataSize() = mAutoPagerView?.getDataSize() ?: 0

        fun getCurSegment(): Segment? = segmentAt(curSegIndex)

        fun segmentAt(i: Int): Segment? = segments.getOrNull(i)

        fun measureChildWithMargins(child: View, widthUsed: Int, heightUsed: Int) {
            var w = widthUsed
            var h = heightUsed
            val lp = child.layoutParams as LayoutParams
            val insets: Rect = getItemDecorInsetsForChild(child)
            w += insets.left + insets.right
            h += insets.top + insets.bottom
            val widthSpec = LayoutMaster.getChildMeasureSpecInner(mWidth, mWidthMode,
                    getPaddingLeft() + getPaddingRight()
                            + lp.leftMargin + lp.rightMargin + w, lp.width)
            val heightSpec = LayoutMaster.getChildMeasureSpecInner(mHeight, mHeightMode,
                    (getPaddingTop() + getPaddingBottom()
                            + lp.topMargin + lp.bottomMargin + h), lp.height)
            if (shouldMeasureChild(child, widthSpec, heightSpec, lp)) {
                child.measure(widthSpec, heightSpec)
            }
        }

        open fun shouldMeasureChild(child: View, widthSpec: Int, heightSpec: Int, lp: LayoutParams): Boolean {
            return (child.isLayoutRequested
                    || !isMeasurementUpToDate(child.width, widthSpec, lp.width)
                    || !isMeasurementUpToDate(child.height, heightSpec, lp.height))
        }

        fun measureChildrens(layoutState: LayoutState) {
//            if (layoutState.isNormalMeasure()) return
            layoutState.mStep = LayoutState.Step.Measure

            if (layoutState.needComputeTotalPage) {
                layoutState.needComputeTotalPage = false
                markPageNotConsistency(layoutState)
            }
            val isMeasureAll = layoutState.nextTimeMeasureAll && layoutState.beenLayoutAtLeastOnce
            if (isMeasureAll) {
                if (layoutState.isInitMeasureAll()) {
                    // previous measureAll process not yet finish, not interrupt it
                    return
                }
                layoutState.mStep = AutoPagerView.LayoutState.Step.MeasureAll
            }
            onMeasureChildrens(layoutState, isMeasureAll)
            mTotalPageCount = segments.size
            layoutState.nextTimeMeasureAll = false

            layoutState.mStep = LayoutState.Step.None
        }

        abstract fun onMeasureChildrens(layoutState: LayoutState, isInitMeasureAll: Boolean)

        fun toPrevPage() {
            switchToPage(curSegIndex - 1)
        }

        fun toNextPage() {
            switchToPage(curSegIndex + 1)
        }

        fun checkPageCountConsistency(): Boolean {
            val lastSegment = segments.lastOrNull() ?: return segments.isEmpty()
            if (lastSegment.end != getDataSize()) return false

            val start = curSegIndex
            if (!checkSegmentCountConsistency(start)) return false
            for (i in start until segments.size) if (!checkSegmentCountConsistency(i)) return false
            for (i in start downTo 0) if (!checkSegmentCountConsistency(i)) return false
            return true
        }

        private fun checkSegmentCountConsistency(segmentIndex: Int): Boolean {
            val s = segmentAt(segmentIndex)
//            val temp: Segment = tempMeasureSegments.get(segmentIndex)
            return s?.layoutRows == s?.size
        }

        fun setNextTimeMeasureAll(layoutState: LayoutState) {
            layoutState.nextTimeMeasureAll = true
            layoutState.mPendingChangeSegmentStart = getCurSegment()?.start ?: -1
            curSegIndex = 0
            segments.clear()
        }

        fun markPageNotConsistency(layoutState: LayoutState) {
            setNextTimeMeasureAll(layoutState)
        }

        abstract fun onLayoutWithInBounds(layoutState: LayoutState, changed: Boolean, l: Int, t: Int, r: Int, b: Int)
        abstract fun layoutChildrens(layoutState: LayoutState)
        fun scrollToPosition(dataIndex: Int) {
            mPendingPosition = dataIndex
            mAutoPagerView?.post { mAutoPagerView?.requestLayout() }
        }
    }

}