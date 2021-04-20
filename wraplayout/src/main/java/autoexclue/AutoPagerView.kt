package autoexclue

import am.widget.wraplayout.R
import android.annotation.TargetApi
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.util.SparseArray
import android.util.SparseIntArray
import android.view.View
import android.view.ViewGroup
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by lei.jialin on 2021/4/19
 */
class AutoPagerView : ViewGroup {
    var adapter: Adapter<*>? = null
        set(value) {
            field?.unregisterAdapterDataObserver(mObserver)
            field = value
            value?.registerAdapterDataObserver(mObserver)
        }
    private var mVerticalSpacing = 0
    private var mHorizontalSpacing = 0

    /**
     * 获取行数目
     *
     * @return 行数目
     */
    var numRows = 0
        private set
    private val mNumLayoutRows = 0
    private val mNumColumns = ArrayList<Int>()
    private val mChildMaxWidth = ArrayList<Int>()
    private var mGravity = GRAVITY_TOP
    private var mIsAttachToWindow = false
    private var mAdapterUpdateDuringMeasure = false
    private val mObserver = MtObserver()
    private var isLayouting = false

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
            val attr = a.getIndex(i)
            when (attr) {
                0 -> horizontalSpacing = a.getDimensionPixelSize(attr, horizontalSpacing)
                1 -> verticalSpacing = a.getDimensionPixelSize(attr, verticalSpacing)
            }
        }
        a.recycle()
        val custom = context.obtainStyledAttributes(attrs, R.styleable.AutoExcludeLayout)
        horizontalSpacing = custom.getDimensionPixelSize(
                R.styleable.AutoExcludeLayout_AEL_HorizontalSpacing, horizontalSpacing)
        verticalSpacing = custom.getDimensionPixelSize(
                R.styleable.AutoExcludeLayout_AEL_VerticalSpacing, verticalSpacing)
        val gravity = custom.getInt(R.styleable.AutoExcludeLayout_AEL_Gravity, GRAVITY_TOP)
        custom.recycle()
        mHorizontalSpacing = horizontalSpacing
        mVerticalSpacing = verticalSpacing
        mGravity = gravity
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

    private var measureStartTime = 0L
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureStartTime = System.currentTimeMillis()
        var itemsWidth = 0
        var itemsHeight = 0
        val paddingLeft = paddingLeft
        val paddingRight = paddingRight
        val paddingTop = paddingTop
        val paddingBottom = paddingBottom
        val suggestedMinimumWidth = suggestedMinimumWidth
        val suggestedMinimumHeight = suggestedMinimumHeight

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
        Log.d("autopagers", "onMeasure: ${System.currentTimeMillis() - measureStartTime}")
    }

    var lastWidthSpec = 0
    var lastHeightSpec = 0
    private fun checkFirstMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
//        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
//        val heightMode = MeasureSpec.getSize(heightMeasureSpec)
//        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        lastWidthSpec = widthMeasureSpec
        lastHeightSpec = heightMeasureSpec
        preMeasureAllItem(widthMeasureSpec, heightMeasureSpec)
    }

    private fun getCurSegment(): Segment? = segmentAt(curSegIndex)

    private fun segmentAt(i: Int): Segment? = segments.getOrNull(i)

    private fun preMeasureAllItem(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val mAdapter = adapter
        mAdapter ?: throw IllegalArgumentException(" no Adapter for what? ")
        var dataIndex = 0
        val totalDataSize = mAdapter.totalDataSize()
        findPageMap.clear()
        segments.clear()
        var page = 0
        while (dataIndex < totalDataSize) {
            reuseSegment = tempMeasureSegments.get(page, Segment(0, 0, 0))
            reuseSegment.start = dataIndex
            reuseSegment.measureEnd = dataIndex + MAX_PER_MEASURE_CNT
            reuseSegment.measureSize = MAX_PER_MEASURE_CNT
            findPageMap.put(dataIndex, dataIndex)
            calcOnePage(reuseSegment, widthMeasureSpec, heightMeasureSpec)
            dataIndex += reuseSegment.measureSize
            tempMeasureSegments.put(page, reuseSegment)
            segments.add(reuseSegment)
            page++
        }
    }

    private val findPageMap = SparseIntArray()
    private val segments: MutableList<Segment> = ArrayList()
    private val tempMeasureSegments: SparseArray<Segment> = SparseArray()
    private var curSegIndex = 0
    var reuseSegment = Segment(0, 0, 0)
    private fun calcOnePage(segment: Segment, widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val mAdapter = adapter
        mAdapter ?: throw IllegalArgumentException(" no Adapter for what? ")

        val paddingTop = paddingTop
        val paddingBottom = paddingBottom
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getSize(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        var collectHeight = 0
        val collectWidth = 0
        var rows = 0
        mNumColumns.clear()
        val mChildMaxWidth = ArrayList<Int>()
        mChildMaxWidth.clear()
        val childCount = segment.measureSize
        if (childCount > 0) {
            val numColumns = 0
            //                final int maxItemsWidth = widthSize - paddingStart - paddingEnd;
            val maxItemsHeight = heightSize - paddingTop - paddingBottom
            val rowWidth = 0
            var rowHeight = 0
            val end = Math.min(segment.measureEnd, mAdapter.totalDataSize())
            for (index in segment.start until end) {
                val child = getViewOf(index)
                val lp = child?.layoutParams as LayoutParams
                if (child.visibility == GONE) {
                    lp.isWithinValidLayout = false
                    lp.isInvisibleOutValidLayout = false
                    continue
                }
                //                    if (mNumRows == 0) mNumRows = 1;
                measureChild(child, widthMeasureSpec, heightMeasureSpec)
                val childWidth = child.measuredWidth
                val childHeight = child.measuredHeight
                val curRowHeight = if (rows == 0) childHeight else mVerticalSpacing + childHeight
                if (collectHeight + curRowHeight <= maxItemsHeight) {
                    collectHeight += curRowHeight
                    rows++
                    mChildMaxWidth.add(childHeight + mVerticalSpacing)
                    lp.isWithinValidLayout = true
                    lp.isInvisibleOutValidLayout = false
                } else {
                    mChildMaxWidth.add(childHeight + mVerticalSpacing)
                    lp.isWithinValidLayout = false
                    lp.isInvisibleOutValidLayout = false
                }
                //                    if (numColumns == 0) {
//                        rowWidth = -mHorizontalSpacing;
//                    }
//                    if (rowWidth + childWidth + mHorizontalSpacing <= maxItemsWidth) {
//                        rowWidth += childWidth + mHorizontalSpacing;
//                        rowHeight = Math.max(childHeight, rowHeight);
//                        numColumns++;
//                    } else {
//                        itemsWidth = Math.max(rowWidth, itemsWidth);
//                        itemsHeight += mNumRows == 1 ? rowHeight : mVerticalSpacing + rowHeight;
//                        mNumColumns.add(numColumns);
//                        mChildMaxWidth.add(rowHeight);
//                        mNumRows++;
//                        rowWidth = 0;
//                        rowHeight = 0;
//                        numColumns = 0;
//                        rowWidth += childWidth;
//                        rowHeight = Math.max(childHeight, rowHeight);
//                        numColumns++;
//                    }
            }
            //                if (numColumns != 0) {
//                    itemsHeight += mNumRows == 1 ? rowHeight : mVerticalSpacing + rowHeight;
//                    mNumColumns.add(numColumns);
//                    mChildMaxWidth.add(rowHeight);
//                }
        }
        segment.height = collectHeight
        segment.width = collectWidth
        segment.childMaxWidth = mChildMaxWidth
        segment.measureSize = rows
        segment.measureRows = rows
        segment.measureEnd = segment.measureStart + segment.measureSize
    }

    private fun getViewOf(index: Int): View {
        var vh = vhCache.get(index)
        if (vh == null) {
            vh = obtainViewHolder(index)
            vhCache.put(index, vh)
        }
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
        if (isLayouting) return
        isLayouting = true
        getCurSegment()?.let { cur: Segment ->
            val pre = segmentAt(curSegIndex - 1)
            layoutChunk(cur, pre)
            onLayoutFinish()
        }
        isLayouting = false
    }

    private fun onLayoutFinish() {
        val c = getCurSegment()
        Log.d("autopagers", "onLayoutFinish: page index: ${curSegIndex + 1}/${segments.size}， " +
                "cur page: range [${c?.start} - ${c?.end}) = curItemNum: ${c?.size} / totalItemNum: ${adapter?.totalDataSize()}")
        callback?.invoke("${curSegIndex + 1}/${segments.size}")
    }

    var callback: ((String) -> Unit)? = null

    fun switchToPage(index: Int) {
        val oldIndex = curSegIndex
        val total = totalPage()
        if (index <= 0) {
            curSegIndex = 0
        } else if (index >= total) {
            curSegIndex = total - 1
        } else {
            curSegIndex = index
        }
        if (oldIndex != curSegIndex) {
            requestLayout()
        }
    }

    fun getCurrentIndex() = curSegIndex

    fun totalPage() = segments.size

    protected fun layoutChunk(segment: Segment, pre: Segment?) {
        val startTime = System.currentTimeMillis()
        removeAllViews()
        Log.d("autopagers", "removeAllViews: ${System.currentTimeMillis() - startTime}")
        val paddingStart = paddingLeft
        val paddingTop = paddingTop
        var visibleCount = 0
        var columnTop = paddingTop - mVerticalSpacing
        var start = pre?.end ?: 0
        start = Math.max(start, 0)

        val end = Math.min(start + MAX_PER_MEASURE_CNT, adapter?.totalDataSize() ?: 0)
        var dataIndex = start
        while (dataIndex < end) {
            // row++
            val numColumn = 1 //mNumColumns.get(row);
//            val childMaxWidth = segment.childMaxWidth[dataIndex - start]
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
                //                childView.layout(startX, startY, startX + childWidth, startY + childHeight);

//                startX += childWidth;
//                if(!lp.isWithinValidLayout) continue;
                if (startY + childHeight > measuredHeight) {
                    lp.isInvisibleOutValidLayout = true
                    childView.visibility = INVISIBLE
                    removeView(childView)
                } else {
                    visibleCount++
                    segment.layoutRows = visibleCount
                    val addTime = System.currentTimeMillis()
                    addView(childView)
                    Log.d("autopagers", "addView: ${System.currentTimeMillis() - addTime}")
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
            columnTop += mVerticalSpacing + childMaxHeight//childMaxWidth
        }
        val lastEnd = pre?.end ?: 0
        segment.start = start
        segment.size = segment.layoutRows
        segment.measureRows = segment.layoutRows
        segment.end = segment.start + segment.size
        Log.d("autopagers", "onLayout: ${System.currentTimeMillis() - startTime}")
    }
    /**
     * 获取水平间距
     *
     * @return 水平间距
     */
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
     * 获取垂直间距
     *
     * @return 垂直间距
     */
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
     * 获取某一行列数目
     *
     * @param index 行号
     * @return 列数目
     */
    fun getNumColumns(index: Int): Int {
        val numColumns = -1
        return if (index < 0 || index >= mNumColumns.size) {
            numColumns
        } else mNumColumns[index]
    }
    /**
     * 获取子项对齐模式
     *
     * @return 对齐模式
     */
    /**
     * 设置子项对齐模式
     *
     * @param gravity 对齐模式
     */
    var gravity: Int
        get() = mGravity
        set(gravity) {
            if (gravity != GRAVITY_TOP && gravity != GRAVITY_CENTER && gravity != GRAVITY_BOTTOM) return
            mGravity = gravity
            requestLayout()
        }

    /**
     * Per-child layout information associated with AutoExcludeLayout.
     */
    class LayoutParams : MarginLayoutParams {
        var mViewHolder: ViewHolder? = null
        /**
         * 获取布局
         *
         * @return 布局
         */
        /**
         * 设置布局
         *
         * @param gravity 布局
         */
        var gravity = GRAVITY_PARENT
        var isWithinValidLayout = false
        var isInvisibleOutValidLayout = false

        constructor(c: Context, attrs: AttributeSet?) : super(c, attrs) {
            var gravity = GRAVITY_PARENT
            val custom = c.obtainStyledAttributes(attrs, R.styleable.AutoExcludeLayout_Layout)
            gravity = custom.getInt(R.styleable.AutoExcludeLayout_Layout_AEL_Layout_gravity, gravity)
            custom.recycle()
            this.gravity = gravity
        }

        constructor(width: Int, height: Int) : super(width, height) {}
        constructor(source: ViewGroup.LayoutParams?) : super(source) {}
        constructor(source: LayoutParams) : super(source) {
            gravity = source.gravity
        }
    }

    private inner class MtObserver internal constructor() : AdapterDataObserver() {
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

        private val mObservable = AdapterDataObservable()
        private val viewHolderSparseArray = SparseArray<VH>()

        abstract fun onCreateViewHolderAt(index: Int, parent: ViewGroup): VH

        //        <editor-fold desc="AdapterDataObserver">
        fun registerAdapterDataObserver(observer: AdapterDataObserver) {
            mObservable.registerObserver(observer)
        }

        fun unregisterAdapterDataObserver(observer: AdapterDataObserver) {
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

        abstract fun bindData2ViewHolder(index: Int, vh: ViewHolder, parent: ViewGroup)

    }

    open class ViewHolder internal constructor(val itemView: View)
    companion object {
        private const val MAX_PER_MEASURE_CNT = 10
        const val GRAVITY_PARENT = -1 // 使用全局对齐方案
        const val GRAVITY_TOP = 0 // 子项顶部对齐
        const val GRAVITY_CENTER = 1 // 子项居中对齐
        const val GRAVITY_BOTTOM = 2 // 子项底部对齐
        private val ATTRS = intArrayOf(android.R.attr.horizontalSpacing,
                android.R.attr.verticalSpacing)
    }
}