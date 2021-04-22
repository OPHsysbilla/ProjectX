package autoexclue

import am.widget.wraplayout.R
import android.annotation.TargetApi
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import autoexclue.adapter.CellAdapterDataObservable
import autoexclue.adapter.CellAdapterDataObserver
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
        private const val MAX_PER_MEASURE_CNT = 10
        private val ATTRS = intArrayOf(android.R.attr.horizontalSpacing,
                android.R.attr.verticalSpacing)
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

    private var lastWidthMeasureSpec: Int = 0
    private var lastHeightMeasureSpec: Int = 0

    private var mIsAttachToWindow = false
    private var mAdapterUpdateDuringMeasure = false
    private val mObserver = MtObserver()
    private var isDuringLayout = false
    private var firstMeasureEnd = true

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

    private fun logOf(s: String) {
        Log.d("AutoPagerView", s)
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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measureStartTime = System.currentTimeMillis()
        var itemsWidth = 0
        var itemsHeight = 0
        val paddingLeft = paddingLeft
        val paddingRight = paddingRight
        val paddingTop = paddingTop
        val paddingBottom = paddingBottom
        val suggestedMinimumWidth = suggestedMinimumWidth
        val suggestedMinimumHeight = suggestedMinimumHeight

        lastWidthMeasureSpec = widthMeasureSpec
        lastHeightMeasureSpec = heightMeasureSpec
        checkFirstMeasure(widthMeasureSpec, heightMeasureSpec)
        val s = getCurSegment()
        if (s != null) {
            itemsHeight = s.height
            itemsWidth = s.width
        }
        itemsWidth = Math.max(paddingLeft + itemsWidth + paddingRight, suggestedMinimumWidth)
        itemsHeight = Math.max(paddingTop + itemsHeight + paddingBottom, suggestedMinimumHeight)
        setMeasuredDimension(resolveSize(itemsWidth, widthMeasureSpec),
                resolveSize(itemsHeight, heightMeasureSpec))
        logOf("onMeasure: ${System.currentTimeMillis() - measureStartTime}")
    }

    private fun checkFirstMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        preMeasurePage(pageWantToMeasure = curSegIndex,
                widthMeasureSpec = widthMeasureSpec, heightMeasureSpec = heightMeasureSpec,
                firstMeasure = segments.isEmpty())
    }

    private fun preMeasurePage(pageWantToMeasure: Int, widthMeasureSpec: Int, heightMeasureSpec: Int, firstMeasure: Boolean = false, measureSize: Int = MAX_PER_MEASURE_CNT) {
        val cur = segmentAt(pageWantToMeasure)
        val pre = segmentAt(pageWantToMeasure - 1)
        val dataIndex = if (firstMeasure) 0 else pre?.end ?: 0
        val rangeEnd = if (firstMeasure) getDataSize() else Math.min(dataIndex + measureSize, getDataSize())


        if (firstMeasure) {
            firstMeasureEnd = false
        }
        val measureAll = !firstMeasureEnd
        preMeasureDataRange(measureAll = measureAll, pageIndex = pageWantToMeasure,
                widthMeasureSpec = widthMeasureSpec, heightMeasureSpec = heightMeasureSpec,
                rangeStart = dataIndex, rangeEnd = rangeEnd)
        if (firstMeasure) {
            firstMeasureEnd = true
        }
    }

    private fun getDataSize(): Int = adapter?.totalDataSize() ?: 0

    private fun getCurSegment(): Segment? = segmentAt(curSegIndex)

    private fun segmentAt(i: Int): Segment? = segments.getOrNull(i)

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

    private val segments: MutableList<Segment> = ArrayList()
    private val tempMeasureSegments: SparseArray<Segment> = SparseArray()
    private var curSegIndex = 0

    private fun measureOnePage(measureAll: Boolean, segment: Segment, widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val paddingTop = paddingTop
        val paddingBottom = paddingBottom
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
                    adapter?.measureHeightAt(context, index) ?: 0
                } else {
                     val child = getViewOf(index) ?: continue
                    if (isViewGone(child)) continue
                    measureChild(child, widthMeasureSpec, heightMeasureSpec)
                    child.measuredHeight
                }
                val curRowHeight = if (rows == 0) childHeight else mVerticalSpacing + childHeight
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

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (isDuringLayout) return
        isDuringLayout = true
        getCurSegment()?.let { cur: Segment ->
            val pre = segmentAt(curSegIndex - 1)
            layoutChunk(cur, pre)
            onLayoutFinish()
        }
        isDuringLayout = false
    }

    private fun onLayoutFinish() {
        val c = getCurSegment()
        logOf("onLayoutFinish: page index: ${curSegIndex + 1}/${segments.size}， " +
                "cur page: range [${c?.start} - ${c?.end}) = curItemNum: ${c?.size} / totalItemNum: ${getDataSize()}")
        callbackPageIndex?.invoke("${curSegIndex + 1}/${segments.size}")
    }

    var callbackPageIndex: ((String) -> Unit)? = null


    fun switchToPage(index: Int) {
        val oldIndex = curSegIndex
        val s = getCurSegment()
        val lastItemSequence = s?.end ?: 0
        val total = getTotalPage()
        if (index <= 0) {
            curSegIndex = 0
        } else if (index >= total && lastItemSequence == getDataSize()) {
            curSegIndex = total - 1
        } else {
            curSegIndex = index
        }
        if (oldIndex != curSegIndex) {
            requestLayout()
        }
    }

    fun getCurrentIndex() = curSegIndex

    fun getTotalPage() = segments.size

    private fun layoutChunk(segment: Segment, pre: Segment?) {
        val startTime = System.currentTimeMillis()
        removeAllViews()
        val paddingStart = paddingLeft
        val paddingTop = paddingTop
        var visibleCount = 0
        var columnTop = paddingTop - mVerticalSpacing
        var start = pre?.end ?: 0
        start = Math.max(start, 0)
        val end = Math.min(start + MAX_PER_MEASURE_CNT, getDataSize())
        var dataIndex = start
        while (dataIndex < end) {
            val numColumn = 1
            var childMaxHeight = 0
            var startX = paddingStart - mHorizontalSpacing
            var column = 0
            while (column < numColumn) {
                val childView = getViewOf(dataIndex)
                if (childView == null || childView.visibility == GONE) {
                    continue
                }
                val childWidth = childView.measuredWidth
                val childHeight = childView.measuredHeight
                val lp = childView.layoutParams as LayoutParams
                startX += mHorizontalSpacing
                val startY = columnTop + mVerticalSpacing
                if (startY + childHeight > measuredHeight) {
                    lp.isInvisibleOutValidLayout = true
                    childView.visibility = INVISIBLE
                    removeView(childView)
                } else {
                    visibleCount++
                    segment.layoutRows = visibleCount
                    addView(childView)
                    childView.layout(startX, startY, startX + childWidth, startY + childHeight)
                    childMaxHeight = Math.max(childMaxHeight, childHeight)
                    if (childView.visibility == INVISIBLE) {
                        lp.isInvisibleOutValidLayout = false
                        childView.visibility = VISIBLE
                    }
                }
                column++
                dataIndex++
            }
            columnTop += mVerticalSpacing + childMaxHeight
        }
        segment.start = start
        segment.size = segment.layoutRows
        segment.measureRows = segment.layoutRows
        segment.end = segment.start + segment.size
        logOf("onLayout: ${System.currentTimeMillis() - startTime}")
    }

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


    /**
     * Per-child layout information associated with AutoExcludeLayout.
     */
    class LayoutParams : MarginLayoutParams {
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
}