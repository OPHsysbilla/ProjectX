package autoexclue.layout

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import autoexclue.AutoPagerView
import autoexclue.AutoPagerView.Companion.logOf
import autoexclue.Segment
import java.lang.StringBuilder

/**
 * Created by lei.jialin on 2021/5/19
 */
class LinearLayoutMaster(var mOrientation: Int = VERTICAL, var allowShowHalfVisibleCell: Boolean = false) : AutoPagerView.LayoutMaster() {

    companion object {
        const val HORIZONTAL = LinearLayout.HORIZONTAL
        const val VERTICAL = LinearLayout.VERTICAL
    }

    var callbackPageIndex: ((curPage: Int, totalPage: Int) -> Unit)? = null
    var debug: ((log: String) -> Unit)? = null
    override fun canScrollHorizontally(): Boolean = mOrientation == HORIZONTAL
    override fun canScrollVertically(): Boolean = mOrientation == VERTICAL


    protected fun layoutChunk(layoutState: AutoPagerView.LayoutState, segment: Segment, pre: Segment?) {
        val pagerView = mAutoPagerView ?: return

        val startTime = System.currentTimeMillis()
        mChildrenHelper?.removeAllChildOnScreen()
        val isOnlyMeasure = layoutState.isInitMeasureAll()

        var columnTop = getPaddingTop()
        val maxParentAvaliableHeight = mHeight - getPaddingBottom() - getPaddingTop() + columnTop
        val recordSegmentSize = segment.size

        var visibleCount = 0
        var start = pre?.end ?: 0
        start = Math.max(start, 0)
        val end = Math.min(start + AutoPagerView.MAX_PER_MEASURE_CNT, getDataSize())
        var dataIndex = start
        var rows = 0
        var layoutFinish = false
        var logger = ""
        var firstExceedIndex = -1
        while (dataIndex < end) {
            val numColumn = 1
            var childMaxHeight = 0
            var columnleft = getPaddingLeft()
            var column = 0
            while (column < numColumn) {
                if (rows > 0 && !layoutFinish) columnTop += getVerticalSpacing()
                if (column > 0 && !layoutFinish) columnleft += getHorizontalSpacing()
                var childHeight = 0
                if (isOnlyMeasure) {
                    childHeight = mChildrenHelper?.preMeasureChild(dataIndex)
                            ?: 0
                    val curRowHeight = childHeight
                    logger += "h[$dataIndex]=$childHeight, "
                    column++
                    dataIndex++
                    if (columnTop + curRowHeight <= maxParentAvaliableHeight) {
                        columnTop += curRowHeight
                        rows++
                        visibleCount++
                        continue
                    } else {
                        val measureHeight = columnTop - getPaddingTop()
                        fitSegment(segment, height = measureHeight, start = start, size = visibleCount)
                    }
                    logOf("isOnlyMeasure: $logger,  step: ${layoutState.mStep}, columnTop:$columnTop}, needComputeTotal:${layoutState.needComputeTotalPage}, nextTimeMeasureAll: ${layoutState.nextTimeMeasureAll}")
                    return
                }

                val childView = mChildrenHelper?.getViewOf(dataIndex) ?: continue
                if (mChildrenHelper?.isViewGone(childView) == true) continue

                mChildrenHelper?.addView(childView)
                measureChildWithMargins(childView, 0, 0)

                val childWidth = childView.measuredWidth
                childHeight = childView.measuredHeight
                mChildrenHelper?.onChildAdded(dataIndex, childWidth, childHeight)
                logger += "h[$dataIndex]=$childHeight, "
                val left = columnleft
                val top = columnTop
                val right = columnleft + childWidth
                val bottom = columnTop + childHeight
                val isExceed = bottom > maxParentAvaliableHeight
                        || layoutFinish && childView.visibility == View.VISIBLE
                if (isExceed) {
                    if (!layoutFinish) {
                        firstExceedIndex = dataIndex
                    }
                    layoutFinish = true
                    layoutState.beenLayoutAtLeastOnce = true
                    if (allowShowHalfVisibleCell && firstExceedIndex == dataIndex) {
                        // could show this half visible item
                        displayChildWithLayout(dataIndex, childView, left, top, right, bottom)
                    } else removeChild(dataIndex, childView)
                } else if (!layoutFinish) {
                    visibleCount++
                    childMaxHeight = childHeight
                    displayChildWithLayout(dataIndex, childView, left, top, right, bottom)
                }
                column++
                dataIndex++
            }
            rows++
            columnTop += childMaxHeight
        }
        layoutState.maxLayoutRow = Math.max(segment.layoutRows, layoutState.maxLayoutRow)
        segment.layoutRows = visibleCount
        if (recordSegmentSize != visibleCount) {
            layoutState.needComputeTotalPage = true
        }
        val measureHeight = columnTop - getPaddingTop()
        fitSegment(segment, height = measureHeight, start = start, size = visibleCount)
        logOf("layoutChunk: time: ${System.currentTimeMillis() - startTime}, $logger, step: ${layoutState.mStep}, layoutFinish: $layoutFinish, columnTop:$columnTop}, measureHeight: ${pagerView.measuredHeight}, layoutHeight: ${columnTop + getPaddingBottom()}")
    }

    private fun displayChildWithLayout(dataIndex: Int, childView: View,
                                       left: Int, top: Int, right: Int, bottom: Int) {
        childView.layout(left, top, right, bottom)
        markChildVisible(dataIndex, childView)
    }

    private fun markChildVisible(dataIndex: Int, childView: View) {
        if (childView.visibility == ViewGroup.INVISIBLE) {
            val lp = childView.layoutParams as AutoPagerView.LayoutParams
            lp.isInvisibleOutValidLayout = false
            childView.visibility = ViewGroup.VISIBLE
        }
    }

    private fun removeChild(dataIndex: Int, childView: View) {
        val lp = childView.layoutParams as AutoPagerView.LayoutParams
        lp.isInvisibleOutValidLayout = true
        childView.visibility = ViewGroup.INVISIBLE
        mChildrenHelper?.removeChild(dataIndex, childView)
    }

    private fun fitSegment(segment: Segment, height: Int, start: Int, size: Int) {
        segment.height = height
        segment.width = mWidth
        segment.start = start
        segment.addEnd(size)
    }

    override fun dispatchLayout(layoutState: AutoPagerView.LayoutState) {
        // TODO: FIX it: mPendingSwitchPage will be able to scroll to the target position
        // Always put mPendingSwitchPosition at the first one
        // when reach to the very end/start (aka. the first or last elemnt in whole data list)
        // that means as pageCount not matched, behave like we are RecyclerView#scrollToPosition(index)

        var resetPendingSegmentStart = false
        var resetPendingPosition = false

        val log = "mPendingPosition:${mPendingPosition},pendingSegmentStart: ${layoutState.mPendingChangeSegmentStart}, page index before:${curSegIndex}"
        val index = if (layoutState.isInitMeasureAll()) {
            -1
        } else if (mPendingPosition >= 0) {
            resetPendingPosition = true
            mPendingPosition
        } else if (layoutState.mPendingChangeSegmentStart >= 0) {
            resetPendingSegmentStart = true
            layoutState.mPendingChangeSegmentStart
        } else -1
        val page = findWhichPage(index)
        if (segmentAt(page) != null) {
            curSegIndex = page
        }

        layoutPageOf(layoutState, page)
        logOf(log + ", pick curSegIndex: ${curSegIndex}")

        if (resetPendingPosition) {
            mPendingPosition = -1
        }
        if (resetPendingSegmentStart) {
            layoutState.mPendingChangeSegmentStart = -1
        }
    }

    private fun findWhichPage(index: Int): Int {
        val which = if (index >= 0) segments.find { it.start <= index && it.end > index } else null
        return which?.let { segments.indexOf(it) } ?: curSegIndex
    }

    private fun layoutPageOf(layoutState: AutoPagerView.LayoutState, pageIndex: Int) {
        segmentAt(pageIndex)?.let { cur: Segment ->
            val pre = segmentAt(pageIndex - 1)
            layoutChunk(layoutState, cur, pre)
            onLayoutFinish(layoutState, cur)
        }
    }

    override fun layoutChildrens(layoutState: AutoPagerView.LayoutState) {
        layoutState.mStep = AutoPagerView.LayoutState.Step.Layout
        dispatchLayout(layoutState)

        if (layoutState.needComputeTotalPage) {
            layoutState.needComputeTotalPage = false
            reInitSegmentFromBegin(layoutState)
            measureChildrens(layoutState)
            layoutChildrens(layoutState)
        }
        layoutState.mStep = AutoPagerView.LayoutState.Step.None
    }

    override fun onLayoutWithInBounds(layoutState: AutoPagerView.LayoutState, changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val startTime = System.currentTimeMillis()
        layoutChildrens(layoutState)
        logOf("onLayout: time ${System.currentTimeMillis() - startTime}, cur:${getCurSegment()}, state:${layoutState.mStep}, needComputeTotalPage: ${layoutState.needComputeTotalPage}, nextTimeMeasureAll:${layoutState.nextTimeMeasureAll}")
        layoutState.beenLayoutAtLeastOnce = true
    }

    private fun onLayoutFinish(layoutState: AutoPagerView.LayoutState, c: Segment) {
        logOf("onLayoutFinish: page: ${curSegIndex + 1}/${getTotalPageCount()}， " +
                "cur[start-end-layoutRows]:[${c.start} - ${c.end} ~ ${c.layoutRows}]， [segSize-dataSize]: [${c.size}-${getDataSize()}]")
        debug?.invoke("[segSize-dataSize]:[${c.size}-${getDataSize()}], page:${curSegIndex + 1}/${segments.size}, cur: ${c} , segs: ${printSegment()}")
        callbackPageIndex?.invoke(curSegIndex + 1, segments.size)
    }

    override fun onMeasureChildrens(layoutState: AutoPagerView.LayoutState, isInitMeasureAll: Boolean) {
        if (isInitMeasureAll) {
            generateSegments(layoutState = layoutState, pageIndex = 0, rangeStart = 0,
                    rangeEnd = getDataSize())
            logOf("generateSegments: pageCount: ${segments.size}, MeasureAll: ${layoutState.isInitMeasureAll()}, segment: ${printSegment()}")
        } else {
            measureOnePage(layoutState, curSegIndex)
            logOf("one-seg-measure: cur:${getCurSegment()?.toString()}, MeasureAll: ${layoutState.isInitMeasureAll()}, segment: ${printSegment()}")
        }
    }

    private fun measureOnePage(layoutState: AutoPagerView.LayoutState, pageIndex: Int) {
        val cur = segmentAt(pageIndex)
        val pre = segmentAt(pageIndex - 1)
        val preEnd = pre?.end
        val rangeStart = preEnd ?: cur?.start ?: 0
//        val measureSize: Int = AutoPagerView.MAX_PER_MEASURE_CNT
//        val rangeEnd = Math.min(rangeStart + measureSize, getDataSize())
        generateOneSegment(page = pageIndex, layoutState = layoutState, dataIndex = rangeStart)
    }

    private fun generateSegments(layoutState: AutoPagerView.LayoutState, pageIndex: Int, rangeStart: Int, rangeEnd: Int) {
        var page = pageIndex
        var dataIndex = rangeStart

        while (dataIndex < rangeEnd) {
            val reuseSegment = generateOneSegment(page, layoutState, dataIndex) ?: break
            val count = reuseSegment.size
            logOf("one-seg-gen: ${reuseSegment.toString()}, whlie: ${dataIndex < rangeEnd},MeasureAll: ${layoutState.isInitMeasureAll()}, segment: ${printSegment()}")
            dataIndex += count
            page++
        }
    }

    private fun validDataIndex(index: Int) = Math.max(0, Math.min(index, getDataSize()))

    fun Segment?.addEnd(index: Int) {
        this ?: return
        this.end = validDataIndex(this.start + index)
        this.size = Math.max(0, this.end - this.start)
    }

    private fun generateOneSegment(page: Int, layoutState: AutoPagerView.LayoutState, dataIndex: Int): Segment? {
        val reuseSegment = tempMeasureSegments.get(page, Segment(0, 0, 0))
        val measureSize = if (reuseSegment.size == 0) {
            layoutState.defaultMeasureSize()
        } else {
            reuseSegment.size
        }
        reuseSegment.start = dataIndex
        reuseSegment.addEnd(measureSize)
        if (page < segments.size) {
            segments[page] = reuseSegment
        } else segments.add(reuseSegment)
        tempMeasureSegments.put(page, reuseSegment)
        layoutChunk(layoutState, reuseSegment, segmentAt(page - 1))
        if (reuseSegment.size <= 0) {
            segments.remove(reuseSegment)
            return null
        }
        return reuseSegment
    }

    private fun printSegment(): String {
        sb.setLength(0)
        segments.forEach { sb.append(it.toString()) }
        return sb.toString()
    }


    protected val sb: StringBuilder = StringBuilder()
}