package autoexclue.layout

import android.view.ViewGroup
import autoexclue.AutoPagerView
import autoexclue.AutoPagerView.Companion.logOf
import autoexclue.Segment

/**
 * Created by lei.jialin on 2021/5/19
 */
class LinearLayoutMaster : AutoPagerView.LayoutMaster() {
    var callbackPageIndex: ((String) -> Unit)? = null

    protected fun layoutChunk(layoutState: AutoPagerView.LayoutState, segment: Segment, pre: Segment?) {
        val pagerView = mAutoPagerView ?: return

        val startTime = System.currentTimeMillis()
        mChildrenHelper?.removeAllViews()
        val isOnlyMeasure = layoutState.isOnlyMeasure()

        var columnTop = getPaddingTop()
        val maxParentAvaliableHeight = mHeight - getPaddingBottom() - getPaddingTop() + columnTop

        var visibleCount = 0
        var start = pre?.end ?: 0
        start = Math.max(start, 0)
        val end = Math.min(start + AutoPagerView.MAX_PER_MEASURE_CNT, getDataSize())
        var dataIndex = start
        var rows = 0
        var layoutFinish = false
        while (dataIndex < end) {
            val numColumn = 1
            var childMaxHeight = 0
            var columnleft = getPaddingLeft()
            var column = 0
            while (column < numColumn) {

                if(rows > 0 && !layoutFinish) columnTop += getVerticalSpacing()
                if(column > 0 && !layoutFinish) columnleft += getHorizontalSpacing()
                var childHeight = 0
                if (isOnlyMeasure) {
                    childHeight = mChildrenHelper?.preMeasureChild(pagerView.context, dataIndex)
                            ?: 0
                    val curRowHeight = childHeight
                    if (columnTop + curRowHeight <= maxParentAvaliableHeight) {
                        columnTop += curRowHeight
                        rows++
                        visibleCount++
                        continue
                    } else {
                        val measureHeight = columnTop - getPaddingTop()
                        fitSegment(segment, height = measureHeight, start = start, size = visibleCount)
                        return
                    }
                }

                val childView = mChildrenHelper?.getViewOf(dataIndex) ?: continue
                if (mChildrenHelper?.isViewGone(childView) == true) continue

                mChildrenHelper?.addView(childView)
                measureChildWithMargins(childView, 0, 0)

                val childWidth = childView.measuredWidth
                if (layoutState.isStepLayout()) {
                    childHeight = childView.measuredHeight
                }
                val lp = childView.layoutParams as AutoPagerView.LayoutParams
                if (columnTop + childHeight > maxParentAvaliableHeight) {
                    layoutFinish = true
                    lp.isInvisibleOutValidLayout = true
                    childView.visibility = ViewGroup.INVISIBLE
                    mChildrenHelper?.removeView(childView)
                } else if (!layoutFinish) {
                    visibleCount++
                    segment.layoutRows = visibleCount
                    childView.layout(columnleft, columnTop, columnleft + childWidth, columnTop + childHeight)
                    childMaxHeight = childHeight
                    if (childView.visibility == ViewGroup.INVISIBLE) {
                        lp.isInvisibleOutValidLayout = false
                        childView.visibility = ViewGroup.VISIBLE
                    }
                }
                column++
                dataIndex++
            }
            rows++
            columnTop += childMaxHeight
            logOf("columnTop: ${columnTop}, dataIndex: ${dataIndex}, layoutHeight: ${columnTop + getPaddingBottom()}")
        }
        val measureHeight = columnTop - getPaddingTop()
        fitSegment(segment, height = measureHeight, start = start, size = visibleCount)
        logOf("onLayout: ${System.currentTimeMillis() - startTime}, measureHeight: ${pagerView.measuredHeight}, layoutHeight: ${columnTop + getPaddingBottom()}")
    }

    private fun fitSegment(segment: Segment, height: Int, start: Int, size: Int) {
        segment.height = height
        segment.width = mWidth
        segment.start = start
        segment.size = size
        segment.end = segment.start + segment.size
        segment.layoutRows = size
        segment.measureRows = size
    }

    override fun dispatchLayout(layoutState: AutoPagerView.LayoutState) {
        getCurSegment()?.let { cur: Segment ->
            val pre = segmentAt(curSegIndex - 1)
            layoutChunk(layoutState, cur, pre)
            onLayoutFinish()
        }
    }

    private fun onLayoutFinish() {
        val c = getCurSegment()
        logOf("onLayoutFinish: page index: ${curSegIndex + 1}/${segments.size}， " +
                "cur page: range [${c?.start} - ${c?.end}) = curItemNum: ${c?.size} / totalItemNum: ${getDataSize()}")
        callbackPageIndex?.invoke("${curSegIndex + 1}/${segments.size}")
    }

    private fun preMeasurePage(layoutState: AutoPagerView.LayoutState, pageWantToMeasure: Int,
                               measureSize: Int = AutoPagerView.MAX_PER_MEASURE_CNT) {
        val cur = segmentAt(pageWantToMeasure)
        val pre = segmentAt(pageWantToMeasure - 1)
        val dataIndex = if (layoutState.isMeasureAll) 0 else pre?.end ?: 0
        val rangeEnd = if (layoutState.isMeasureAll) getDataSize() else Math.min(dataIndex + measureSize, getDataSize())
        generateSegments(layoutState = layoutState, pageIndex = pageWantToMeasure, rangeStart = dataIndex, rangeEnd = rangeEnd)
    }

    override fun onMeasure(mLayoutState: AutoPagerView.LayoutState) {
        preMeasurePage(mLayoutState, pageWantToMeasure = curSegIndex)
    }

    private fun generateSegments(layoutState: AutoPagerView.LayoutState, pageIndex: Int, rangeStart: Int, rangeEnd: Int) {
        var page = pageIndex
        var dataIndex = rangeStart
        val defaultMeasureSize = AutoPagerView.MAX_PER_MEASURE_CNT
        while (dataIndex < rangeEnd) {
            val reuseSegment = tempMeasureSegments.get(page, Segment(0, 0, 0))
            reuseSegment.start = dataIndex
            reuseSegment.size = defaultMeasureSize
            reuseSegment.end = dataIndex + defaultMeasureSize
            tempMeasureSegments.put(page, reuseSegment)
            if (page < segments.size) {
                segments[page] = reuseSegment
            } else segments.add(reuseSegment)
            layoutChunk(layoutState, reuseSegment, segmentAt(page - 1))
            dataIndex += reuseSegment.size
            page++
        }
    }

    private fun measureOnePage(measureAll: Boolean, segment: Segment) {
        val pagerView = mAutoPagerView ?: return
        val paddingTop = getPaddingTop()
        val paddingBottom = getPaddingBottom()
        var collectHeight = 0
        val collectWidth = 0
        var rows = 0
        val childCount = segment.measureSize
        if (childCount > 0) {
            val maxItemsHeight = mHeight - paddingTop - paddingBottom
            val end = Math.min(segment.measureEnd, getDataSize())
            for (index in segment.start until end) {
                val childHeight: Int = if (measureAll) {
                    mChildrenHelper?.preMeasureChild(pagerView.context, index) ?: 0
                } else {
                    val child = mChildrenHelper?.getViewOf(index) ?: continue
                    if (mChildrenHelper?.isViewGone(child) == true) continue
                    measureChildWithMargins(child, 0, 0)
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
}