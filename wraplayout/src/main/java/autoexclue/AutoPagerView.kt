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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var itemsWidth = 0
        var itemsHeight = 0
        val paddingLeft = paddingLeft
        val paddingRight = paddingRight
        val paddingTop = paddingTop
        val paddingBottom = paddingBottom
        val suggestedMinimumWidth = suggestedMinimumWidth
        val suggestedMinimumHeight = suggestedMinimumHeight
        checkFirstMeasure(widthMeasureSpec, heightMeasureSpec)
        val s = currentSegment
        if (s != null) {
            numRows = s.measureRows
            itemsHeight = s.height
            itemsWidth = s.width
        }
        itemsWidth = Math.max(paddingLeft + itemsWidth + paddingRight, suggestedMinimumWidth)
        itemsHeight = Math.max(paddingTop + itemsHeight + paddingBottom, suggestedMinimumHeight)
        setMeasuredDimension(resolveSize(itemsWidth, widthMeasureSpec),
                resolveSize(itemsHeight, heightMeasureSpec))
    }

    private fun checkFirstMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val s = currentSegment
        if (s == null) { // || segments.isEmpty() && adapter.getTotalDataSize() != 0) {
            preMeasureAllItem(widthMeasureSpec, heightMeasureSpec)
        }
    }

    private var currentSegment: Segment? = null
        get() = if (curSegIndex < segments.size && curSegIndex >= 0) segments[curSegIndex] else null

    private fun preMeasureAllItem(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val mAdapter = adapter
        mAdapter ?: throw IllegalArgumentException(" no Adapter for what? ")
        var dataIndex = 0
        val totalDataSize = mAdapter.totalDataSize
        findPageMap.clear()
        segments.clear()
        curSegIndex = 0
        while (dataIndex < totalDataSize) {
            reuseSegment = Segment(0, 0, 0)
            reuseSegment.start = dataIndex
            reuseSegment.end = dataIndex + MAX_PER_MEASURE_CNT
            reuseSegment.size = MAX_PER_MEASURE_CNT
            findPageMap.put(dataIndex, reuseSegment.start)
            calcOnePage(widthMeasureSpec, heightMeasureSpec, reuseSegment)
            dataIndex += reuseSegment.size
            segments.add(reuseSegment)
        }
    }

    private val findPageMap = SparseIntArray()
    private val segments: MutableList<Segment> = ArrayList()
    private var curSegIndex = 0
    private var curSegment: Segment? = null
    var reuseSegment = Segment(0, 0, 0)
    private fun calcOnePage(widthMeasureSpec: Int, heightMeasureSpec: Int, segment: Segment) {
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
        numRows = 0
        mNumColumns.clear()
        val mChildMaxWidth = ArrayList<Int>()
        mChildMaxWidth.clear()
        val childCount = segment.size
        if (childCount > 0) {
            val numColumns = 0
            //                final int maxItemsWidth = widthSize - paddingStart - paddingEnd;
            val maxItemsHeight = heightSize - paddingTop - paddingBottom
            val rowWidth = 0
            var rowHeight = 0
            val end = Math.min(segment.end, mAdapter.totalDataSize)
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
                if (rowHeight + childHeight + mVerticalSpacing <= maxItemsHeight) {
                    rowHeight += childHeight + mVerticalSpacing
                    numRows++
                    mChildMaxWidth.add(childHeight + mVerticalSpacing)
                    lp.isWithinValidLayout = true
                    lp.isInvisibleOutValidLayout = false
                    // TODO: refect
                    collectHeight += if (numRows == 1) rowHeight else mVerticalSpacing + rowHeight
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
        segment.size = numRows
        segment.measureRows = numRows
        segment.end = segment.start + segment.size
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
        val s = (if (curSegment == null && !segments.isEmpty()) segments[curSegIndex] else curSegment)
                ?: return
        curSegment = s
        numRows = s.measureRows
        layoutChunk(s)
        onLayoutFinish()
    }

    private fun onLayoutFinish() {
        val c = currentSegment
        Log.d("autopagers", "onLayoutFinish: page index: ${curSegIndex + 1}/${segments.size + 1}， " +
                "cur page data: ${c?.layoutRows}/ ${adapter?.totalDataSize}")
    }

    protected fun layoutChunk(segment: Segment) {
        if (isLayouting) return
        segment.layoutRows = 0
        isLayouting = true
        removeAllViews()
        val paddingStart = paddingLeft
        val paddingTop = paddingTop
        val gravity = mGravity
        var numChild = 0
        var columnTop = paddingTop - mVerticalSpacing
        var row = segment.start
        val end = Math.min(segment.start + segment.measureRows, adapter?.totalDataSize ?: 0)
        while (row < end) {
            // row++
            val numColumn = 1 //mNumColumns.get(row);
            val childMaxHeight = segment.childMaxWidth[row - segment.start]
            var startX = paddingStart - mHorizontalSpacing
            var column = 0
            while (column < numColumn) {
                val childView = getViewOf(row)
                numChild++
                if (childView == null || childView.visibility == GONE) {
                    continue
                }
                val childWidth = childView.measuredWidth
                val childHeight = childView.measuredHeight
                val lp = childView.layoutParams as LayoutParams
                val childGravity = lp.gravity
                startX += mHorizontalSpacing
                val topOffset = when (childGravity) {
                    GRAVITY_PARENT -> {
                        when (gravity) {
                            GRAVITY_CENTER -> Math.round((childMaxHeight - childHeight) * 0.5f)
                            GRAVITY_BOTTOM -> childMaxHeight - childHeight
                            GRAVITY_TOP -> 0
                            else -> 0
                        }
                    }
                    GRAVITY_CENTER -> Math.round((childMaxHeight - childHeight) * 0.5f)
                    GRAVITY_BOTTOM -> childMaxHeight - childHeight
                    GRAVITY_TOP -> 0
                    else -> {
                        when (gravity) {
                            GRAVITY_CENTER -> Math.round((childMaxHeight - childHeight) * 0.5f)
                            GRAVITY_BOTTOM -> childMaxHeight - childHeight
                            GRAVITY_TOP -> 0
                            else -> 0
                        }
                    }
                }
                val startY = columnTop + mVerticalSpacing + topOffset
                //                childView.layout(startX, startY, startX + childWidth, startY + childHeight);

//                startX += childWidth;
//                if(!lp.isWithinValidLayout) continue;
                if (startY + childHeight > measuredHeight) {
                    lp.isInvisibleOutValidLayout = true
                    childView.visibility = INVISIBLE
                    removeView(childView)
                } else {
                    segment.layoutRows = row + 1
                    addView(childView)
                    childView.layout(startX, startY, startX + childWidth, startY + childHeight)
                    if (childView.visibility == INVISIBLE) {
                        lp.isInvisibleOutValidLayout = false
                        childView.visibility = VISIBLE
                    }
                }
                column++
            }
            row++
            columnTop += mVerticalSpacing + childMaxHeight
        }
        segment.size = segment.layoutRows
        segment.end = segment.start + segment.size
        isLayouting = false
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
        abstract val totalDataSize: Int

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