package com.fenbi.megrez.app.megrezView

import am.widget.wraplayout.R
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup


/**
 * A FlowLayout that all children have the same fixed width which is decided by the given width.
 * The horizontal gap between children will be re-calculate to be equally divided.
 *
 * Notice that ones the horizontal space is not enough for holding the next child, the next child will be move to second line.
 * It will looks like a square, as all elements have same width.
 *
 * 按照水平宽度等分子view的方阵排列
 * */
class FixedFlowLayout @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
) : ViewGroup(
        context,
        attrs,
        defStyleAttr,
) {
    private var mHorizontalSpacing = 0
    private var mVerticalSpacing = 0
    private var item_gravity = 0
    private var itemAlign = 0
    private val lineItemCountList = mutableListOf<Int>()
    private val lineWidthList = mutableListOf<Int>()

    companion object {
        const val TOP = 0
        const val BOTTOM = 1
        const val CENTER = 2
        const val ALIGN_LEFT = 3
        const val ALIGN_CENTER = 4
        const val ALIGN_RIGHT = 5
        const val ALIGN_CENTER_LEFT =
                6 //居中左对齐，先将最长的一行居中，然后将其他行的左边与最长行的左边对齐。在wrap_content下与ALIGN_LEFT效果相同
        const val ALIGN_CENTER_RIGHT =
                7 //居中右对齐，先将最长的一行居中，然后将其他行的右边与最长行的右边对齐。在wrap_content下与ALIGN_RIGHT效果相同
    }

    init {
        init(context, attrs)
    }


    private fun init(context: Context, attrs: AttributeSet?) {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(
                    attrs,
                    R.styleable.FixedFlowLayout
            )
            try {
                mHorizontalSpacing = a.getDimensionPixelSize(
                        R.styleable.FixedFlowLayout_FFL_horizontal_spacing, 0
                )
                mVerticalSpacing = a.getDimensionPixelSize(
                        R.styleable.FixedFlowLayout_FFL_vertical_spacing, 0
                )
                item_gravity = a.getInteger(
                        R.styleable.FixedFlowLayout_FFL_item_gravity, TOP
                )
                itemAlign = a.getInteger(R.styleable.FixedFlowLayout_FFL_item_align, ALIGN_LEFT)
            } finally {
                a.recycle()
            }
        }
    }

    fun setHorizontalSpacing(spacing: Int) {
        mHorizontalSpacing = spacing
    }

    fun setVerticalSpacing(spacing: Int) {
        mVerticalSpacing = spacing
    }

    fun setItemGravity(itemGravity: Int) {
        item_gravity = itemGravity
    }

    fun setItemAlign(alignType: Int) {
        itemAlign = alignType
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = (MeasureSpec.getSize(widthMeasureSpec)
                - paddingRight)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val growHeight = widthMode != MeasureSpec.UNSPECIFIED

        // total width and height
        var width = 0
        var height = paddingTop

        // width and height relative to current line
        var currentWidth = paddingLeft
        var currentHeight = 0
        var breakLine = false
        var spacing = 0
        val count = childCount
        lineItemCountList.clear()
        lineWidthList.clear()
        for (i in 0 until count) {
            val child = getChildAt(i)
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
        }
        var lineStartNumber = 0
        for (i in 0 until count) {
            val child = getChildAt(i)
            val lp = child.layoutParams as LayoutParams
            spacing = mHorizontalSpacing
            if (lp.horizontalSpacing >= 0) {
                spacing = lp.horizontalSpacing
            }
            lp.x = currentWidth
            lp.y = height
            if (growHeight && (breakLine || currentWidth + child.measuredWidth > widthSize)) {
                lineItemCountList.add(i - lineStartNumber)
                lineWidthList.add(currentWidth - spacing - paddingLeft)
                aline(height, currentHeight, lineStartNumber, i)
                lineStartNumber = i
                height += currentHeight + mVerticalSpacing
                currentHeight = 0
                width = Math.max(width, currentWidth - spacing)
                currentWidth = paddingLeft
                lp.x = currentWidth
                lp.y = height
            }
            currentHeight = Math.max(currentHeight, child.measuredHeight)
            currentWidth += child.measuredWidth + spacing
            breakLine = lp.breakLine
        }
        aline(height, currentHeight, lineStartNumber, count)
        if (count != 0) {
            lineItemCountList.add(count - lineStartNumber)
            lineWidthList.add(currentWidth - spacing - paddingLeft)
            height += currentHeight
            width = Math.max(width, currentWidth - spacing)
        }
        width += paddingRight
        height += paddingBottom
        makeUpLinesHorizontalSpace(resolveSize(width, widthMeasureSpec))
        setMeasuredDimension(
                resolveSize(width, widthMeasureSpec),
                resolveSize(height, heightMeasureSpec)
        )
    }

    /**
     * @param height 当前行的y基准坐标
     * @param currentHeight 当前行的最大view高
     * @param lineStartNumber 当前行的开始位置
     * @param lineEndNumber 当前行的结束位置
     */
    private fun aline(height: Int, currentHeight: Int, lineStartNumber: Int, lineEndNumber: Int) {
        for (i in lineStartNumber until lineEndNumber) {
            val lineChild = getChildAt(i)
            val lineLp = lineChild.layoutParams as LayoutParams
            lineLp.y = height + getOffsetHeight(currentHeight, lineChild.measuredHeight)
        }
    }

    /**
     * 获取view的y坐标偏移量
     * @param lineHeight 行高
     * @param viewHeight view的高
     * @return
     */
    private fun getOffsetHeight(lineHeight: Int, viewHeight: Int): Int =
            when (item_gravity) {
                BOTTOM -> lineHeight - viewHeight
                CENTER -> (lineHeight - viewHeight) / 2
                TOP -> 0
                else -> 0
            }

    /**
     * 根据对齐方式，补偿每行的水平位置
     *
     * @param w FixedFlowLayout的宽度
     */
    private fun makeUpLinesHorizontalSpace(w: Int) {
        var viewWidth = w
        val count = childCount
        if (count == 0) return
        viewWidth = viewWidth - paddingLeft - paddingRight
        var lineNumber = 0
        var breakLineThreshold = lineItemCountList[lineNumber]
        for (i in 0 until count) {
            val child = getChildAt(i)
            val lp = child.layoutParams as LayoutParams
            if (i >= breakLineThreshold) {
                lineNumber++
                breakLineThreshold += lineItemCountList[lineNumber]
            }
            var maxChildWidth = 0
            for (childWidth in lineWidthList) {
                if (childWidth > maxChildWidth) {
                    maxChildWidth = childWidth
                }
            }
            val leftHorizonMakeUp = when (itemAlign) {
                ALIGN_CENTER -> (viewWidth - lineWidthList[lineNumber]) / 2
                ALIGN_RIGHT -> viewWidth - lineWidthList[lineNumber]
                ALIGN_CENTER_LEFT -> (viewWidth - maxChildWidth) / 2
                ALIGN_CENTER_RIGHT -> viewWidth - lineWidthList[lineNumber] - (viewWidth - maxChildWidth) / 2
                ALIGN_LEFT -> 0
                else -> 0
            }
            lp.x += leftHorizonMakeUp
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val count = childCount
        for (i in 0 until count) {
            val child = getChildAt(i)
            val lp = child.layoutParams as LayoutParams
            child.layout(
                    lp.x, lp.y, lp.x + child.measuredWidth, lp.y
                    + child.measuredHeight
            )
        }
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams): Boolean {
        return p is LayoutParams
    }

    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams {
        return LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun generateLayoutParams(
            p: ViewGroup.LayoutParams
    ): ViewGroup.LayoutParams {
        return LayoutParams(p.width, p.height)
    }

    override fun generateLayoutParams(
            attrs: AttributeSet
    ): ViewGroup.LayoutParams {
        return LayoutParams(context, attrs)
    }

    class LayoutParams : ViewGroup.LayoutParams {
        var x = 0
        var y = 0
        var horizontalSpacing = -1
        var breakLine = false

        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
            val a = context.obtainStyledAttributes(
                    attrs,
                    R.styleable.FixedFlowLayout_LayoutParams
            )
            try {
                horizontalSpacing = a.getDimensionPixelSize(
                        R.styleable.FixedFlowLayout_LayoutParams_FFL_layout_horizontal_spacing, -1
                )
                breakLine = a.getBoolean(
                        R.styleable.FixedFlowLayout_LayoutParams_FFL_layout_break_line, false
                )
            } finally {
                a.recycle()
            }
        }

        constructor(w: Int, h: Int) : super(w, h) {}
    }
}