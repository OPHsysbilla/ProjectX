package am.widget.wraplayout

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.core.view.*
import kotlin.math.abs

/**
 * Created by longkongjun 2021/05/07
 *
 * 墨水屏下的滚动控件，支持垂直，水平
 * 滚动方式：滑动时以控件的宽度（高度）为单位进行翻页
 * 子View布局方式参考LinearLayout
 */
class MegezScrollView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val INVALID_VALUE = -1

    private var mIsBeingDragged = false
    private var mDownMotionX = INVALID_VALUE
    private var mDownMotionY = INVALID_VALUE
    private var mLastMotionX = INVALID_VALUE
    private var mLastMotionY = INVALID_VALUE
    private val mTouchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (super.onInterceptTouchEvent(ev)) {
            return true
        }
        if (orientation == VERTICAL && scrollY == 0 && !canScrollVertically(1)) {
            return false
        }
        if (orientation == HORIZONTAL && scrollX == 0 && !canScrollHorizontally(1)) {
            return false
        }
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mIsBeingDragged = true
                mDownMotionX = ev.x.toInt()
                mDownMotionY = ev.y.toInt()
                mLastMotionX = mDownMotionX
                mLastMotionY = mDownMotionY
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                resetScrollState()
                mIsBeingDragged = false
            }
        }
        return mIsBeingDragged
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mIsBeingDragged = true
                mDownMotionX = event.x.toInt()
                mDownMotionY = event.y.toInt()
                mLastMotionX = mDownMotionX
                mLastMotionY = mDownMotionY
            }

            MotionEvent.ACTION_MOVE -> {
                val curX = event.x.toInt()
                val curY = event.y.toInt()
                if (mIsBeingDragged && orientation == VERTICAL && (abs(curY - mLastMotionY) > mTouchSlop)) {
                    scrollVertically(mDownMotionY - curY)
                    mIsBeingDragged = false
                } else if (mIsBeingDragged && orientation == HORIZONTAL && abs(curX - mLastMotionX) > mTouchSlop) {
                    scrollHorizontally(mDownMotionX - curX)
                    mIsBeingDragged = false
                }
                mLastMotionX = curX
                mLastMotionY = curY
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                resetScrollState()
                mIsBeingDragged = false
            }
        }
        return mIsBeingDragged || super.onTouchEvent(event)
    }

    private fun resetScrollState() {
        mDownMotionX = INVALID_VALUE
        mDownMotionY = INVALID_VALUE
        mLastMotionX = INVALID_VALUE
        mLastMotionY = INVALID_VALUE
    }

    /**
     * @param direction Negative to scroll left, positive to scroll right.
     */
    private fun scrollHorizontally(direction: Int) {
        val range = computeHorizontalScrollRange()
        if (range == 0) return
        scrollX = if (direction < 0) {
            0.coerceAtLeast(scrollX - width)
        } else {
            (range - width).coerceAtMost(scrollX + width)
        }
    }

    /**
     * @param direction Negative to scroll up, positive to scroll down.
     */
    private fun scrollVertically(direction: Int) {
        val range = computeVerticalScrollRange()
        if (range == 0) return
        scrollY = if (direction < 0) {
            0.coerceAtLeast(scrollY - height)
        } else {
            (range - height).coerceAtMost(scrollY + height)
        }
    }

    override fun computeHorizontalScrollRange(): Int {
        if (orientation == VERTICAL) {
            return super.computeHorizontalScrollRange()
        }
        return children.fold(0) { acc, child ->
            acc + child.width + child.marginStart + child.marginRight
        }.coerceAtLeast(width + paddingLeft + paddingRight)
    }

    override fun computeVerticalScrollRange(): Int {
        if (orientation == HORIZONTAL) {
            return super.computeVerticalScrollRange()
        }
        return children.fold(0) { acc, child ->
            acc + child.height + child.marginBottom + child.marginTop
        }.coerceAtLeast(height + paddingTop + paddingBottom)
    }

}