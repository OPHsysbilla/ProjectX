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
import androidx.recyclerview.widget.RecyclerView
import autoexclue.adapter.CellAdapterDataObservable
import autoexclue.adapter.CellAdapterDataObserver
import autoexclue.layout.LayoutChildrenHelper
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by lei.jialin on 2021/4/19
 *
 * TODO:
 * 1. 增加 titleDecoration
 * 2. FIX callbackPageIndex没有显示的问题
 */
open class AutoPagerView : ViewGroup {
    companion object {
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

    class LayoutState {
        enum class Step {
            None,
            Measure,
            Layout,
        }

        fun isStepMeasure(): Boolean = mStep == Step.Measure
        fun isStepLayout(): Boolean = mStep == Step.Layout
        fun isOnlyMeasure() : Boolean = isStepMeasure()

        var isMeasureAll = false
        var mStep: Step = Step.None
        var firstMeasureEnd = true
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        mLayoutState.mStep = LayoutState.Step.Measure
        val measureStartTime = System.currentTimeMillis()
        layoutMaster?.setMeasureSpecs(widthMeasureSpec, heightMeasureSpec)
        layoutMaster?.onMeasure(mLayoutState)
        layoutMaster?.decideMeasuredDimensionFromChildren(mLayoutState, widthMeasureSpec, heightMeasureSpec)
        logOf("onMeasure: ${System.currentTimeMillis() - measureStartTime}, measureHeight: ${measuredHeight}")
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

    private fun getViewOf(index: Int): View? {
        var vh = vhCache.get(index)
        if (vh == null) {
            vh = obtainViewHolder(index)
            vhCache.put(index, vh)
        }
        vh.mPosition = index
        adapter?.bindData2ViewHolder(index, vh, this)
        return vh.itemView
    }

    private val vhCache: SparseArray<ViewHolder> = SparseArray()

    private fun obtainViewHolder(index: Int): ViewHolder? {
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
        if (mLayoutState.mStep == LayoutState.Step.Layout) return
        mLayoutState.mStep = LayoutState.Step.Layout
        layoutMaster?.dispatchLayout(mLayoutState)
        mLayoutState.mStep = LayoutState.Step.None
    }

    fun switchToPage(index: Int) = layoutMaster?.switchToPage(index)

    fun getCurrentIndex() = layoutMaster?.getCurrentIndex() ?: 0

    fun getTotalPage() = layoutMaster?.getTotalPage() ?: 0

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
            override fun preMeasureChild(context: Context, index: Int): Int = adapter?.measureHeightAt(context, index)
                    ?: 0

            override fun removeAllViews() = this@AutoPagerView.removeAllViews()

            override fun getViewOf(dataIndex: Int): View? = this@AutoPagerView.getViewOf(dataIndex)

            override fun removeView(childView: View) = this@AutoPagerView.removeView(childView)

            override fun addView(childView: View) = this@AutoPagerView.addView(childView)
            override fun isViewGone(child: View): Boolean = this@AutoPagerView.isViewGone(child)
        }
    }


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

    }

    abstract class LayoutMaster {
        protected var mWidthMode: Int = MeasureSpec.EXACTLY
        protected var mHeightMode: Int = MeasureSpec.EXACTLY
        protected var mWidth: Int = 0
        protected var mHeight: Int = 0
        var mAutoPagerView: AutoPagerView? = null
        var mChildrenHelper: LayoutChildrenHelper? = null

        protected val segments: MutableList<Segment> = ArrayList()
        protected val tempMeasureSegments: SparseArray<Segment> = SparseArray()
        var curSegIndex = 0

        abstract fun dispatchLayout(layoutState: LayoutState)

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
            mWidthMode = MeasureSpec.EXACTLY
            mHeightMode = MeasureSpec.EXACTLY
        }

        fun setMeasureSpecs(wSpec: Int, hSpec: Int) {
            mWidth = MeasureSpec.getSize(wSpec)
            mWidthMode = MeasureSpec.getMode(wSpec)
            mHeight = MeasureSpec.getSize(hSpec)
            mHeightMode = MeasureSpec.getMode(hSpec)
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


        fun getCurrentIndex() = curSegIndex

        fun getTotalPage() = segments.size

        fun switchToPage(index: Int) {
            val pagerView = mAutoPagerView
            pagerView ?: return
            val oldIndex = curSegIndex
            val s = getCurSegment()
            val lastItemSequence = s?.end ?: 0
            val total = getTotalPage()
            val dataSize = getDataSize()
            if (index <= 0) {
                curSegIndex = 0
            } else if (index >= total && lastItemSequence == dataSize) {
                curSegIndex = total - 1
            } else {
                curSegIndex = index
            }
            if (oldIndex != curSegIndex) {
                pagerView.requestLayout()
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


        private fun checkFirstMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int, layoutState: LayoutState) {
            preMeasurePage(layoutState, pageWantToMeasure = curSegIndex,
                    widthMeasureSpec = widthMeasureSpec, heightMeasureSpec = heightMeasureSpec,
                    firstMeasure = segments.isEmpty())
        }

        private fun preMeasurePage(layoutState: LayoutState, pageWantToMeasure: Int, widthMeasureSpec: Int, heightMeasureSpec: Int, firstMeasure: Boolean = false, measureSize: Int = MAX_PER_MEASURE_CNT) {
            val cur = segmentAt(pageWantToMeasure)
            val pre = segmentAt(pageWantToMeasure - 1)
            val dataIndex = if (firstMeasure) 0 else pre?.end ?: 0
            val rangeEnd = if (firstMeasure) getDataSize() else Math.min(dataIndex + measureSize, getDataSize())

            if (firstMeasure) {
                layoutState.firstMeasureEnd = false
            }
            val measureAll = !layoutState.firstMeasureEnd
            preMeasureDataRange(measureAll = measureAll, pageIndex = pageWantToMeasure,
                    widthMeasureSpec = widthMeasureSpec, heightMeasureSpec = heightMeasureSpec,
                    rangeStart = dataIndex, rangeEnd = rangeEnd)
            if (firstMeasure) {
                layoutState.firstMeasureEnd = true
            }
        }

        private fun preMeasureDataRange(measureAll: Boolean = false, pageIndex: Int, widthMeasureSpec: Int, heightMeasureSpec: Int, rangeStart: Int, rangeEnd: Int) {
            var page = pageIndex
            var dataIndex = rangeStart
            val defaultMeasureSize = MAX_PER_MEASURE_CNT
            while (dataIndex < rangeEnd) {
                val reuseSegment = tempMeasureSegments.get(page, Segment(0, 0, 0))
                reuseSegment.measureStart = dataIndex
                reuseSegment.measureEnd = dataIndex + defaultMeasureSize
                reuseSegment.measureSize = defaultMeasureSize
                measureOnePage(measureAll, reuseSegment, widthMeasureSpec, heightMeasureSpec)
                dataIndex += reuseSegment.measureSize
                tempMeasureSegments.put(page, reuseSegment)
                if (page < segments.size) {
                    segments[page] = reuseSegment
                } else segments.add(reuseSegment)
                page++
            }
        }

        private fun measureOnePage(measureAll: Boolean, segment: Segment, widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val pagerView = mAutoPagerView ?: return
            val paddingTop = getPaddingTop()
            val paddingBottom = getPaddingBottom()
            val heightSize = MeasureSpec.getSize(heightMeasureSpec)
            var collectHeight = 0
            val collectWidth = 0
            var rows = 0
            val childCount = segment.measureSize
            if (childCount > 0) {
                val maxItemsHeight = heightSize - paddingTop - paddingBottom
                val end = Math.min(segment.measureEnd, getDataSize())
                for (index in segment.start until end) {
                    val childHeight: Int = if (measureAll) {
                        mChildrenHelper?.preMeasureChild(pagerView.context, index) ?: 0
                    } else {
                        val child = pagerView.getViewOf(index) ?: continue
                        if (pagerView.isViewGone(child)) continue
                        pagerView.measureChild(child, widthMeasureSpec, heightMeasureSpec)
                        child.measuredHeight
                    }
                    val curRowHeight = if (rows == 0) childHeight else getVerticalSpacing() + childHeight
                    if (collectHeight + curRowHeight <= maxItemsHeight) {
                        collectHeight += curRowHeight
                        rows++
                    }
                }
            }
            segment.height = collectHeight
            segment.width = collectWidth
            segment.measureSize = rows
            segment.measureRows = rows
            segment.measureEnd = segment.measureStart + segment.measureSize
        }

        abstract fun onMeasure(mLayoutState: AutoPagerView.LayoutState)

    }

}