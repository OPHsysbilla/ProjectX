package autoexclue

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration

/**
 * Created by lei.jialin on 2021/5/20
 */
class FlingPagerView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AutoPagerView(context, attrs, defStyleAttr) {

    enum class ScrollState {
        IDLE,
        DRAGGING,
        SCROLLING,
        SETTLE,
    }

    private var mTouchSuppressed: Boolean = false

    // Touch/scrolling handling
    private var mScrollState: ScrollState = ScrollState.IDLE
    private var mInitialTouchX: Int = 0
    private var mInitialTouchY: Int = 0
    private var mLastTouchX: Int = 0
    private var mLastTouchY: Int = 0
    private var mScrollPointerId: Int = 0
    private var mIgnoreMotionEventTillDown: Boolean = false
    private var mVelocityTracker: VelocityTracker? = null
    private val INVALID_VALUE = -1
    private val mTouchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
//        Log.d("AutoPagerView", "onInterceptTouchEvent $ev")
        if (super.onInterceptTouchEvent(ev)) {
            return true
        }
        val lm = layoutMaster ?: return false
        if (mTouchSuppressed) {
            // When layout is suppressed,  RV does not intercept the motion event.
            // A child view e.g. a button may still get the click.
            return false
        }
        val canScrollHorizontally: Boolean = lm.canScrollHorizontally()
        val canScrollVertically: Boolean = lm.canScrollVertically()

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        }
        mVelocityTracker?.addMovement(ev)

        val action: Int = ev.getActionMasked()
        val actionIndex: Int = ev.getActionIndex()

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (mIgnoreMotionEventTillDown) {
                    mIgnoreMotionEventTillDown = false
                }
                mScrollPointerId = ev.getPointerId(0)
                markInitTouchLocation(0, ev)

                if (mScrollState == ScrollState.SETTLE) {
                    parent.requestDisallowInterceptTouchEvent(true)
                    mScrollState = ScrollState.DRAGGING
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                mScrollPointerId = ev.getPointerId(actionIndex)
                markInitTouchLocation(actionIndex, ev)
            }
            MotionEvent.ACTION_MOVE -> {
                val index: Int = ev.findPointerIndex(mScrollPointerId)
                if (index < 0) {
                    Log.d("AutoPagerView", "pointer index for id "
                            + mScrollPointerId + " not found. Did any MotionEvents get skipped?")
                    return false
                }
                val x = (ev.getX(index) + 0.5f).toInt()
                val y = (ev.getY(index) + 0.5f).toInt()
                if (mScrollState != ScrollState.DRAGGING) {
                    val dx: Int = x - mInitialTouchX
                    val dy: Int = y - mInitialTouchY
                    var startScroll = false
                    if (canScrollHorizontally && Math.abs(dx) > mTouchSlop) {
                        mLastTouchX = x
                        startScroll = true
                    }
                    if (canScrollVertically && Math.abs(dy) > mTouchSlop) {
                        mLastTouchY = y
                        startScroll = true
                    }
                    if (startScroll) {
                        mScrollState = ScrollState.DRAGGING
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                pickNewPointer(ev)
            }
            MotionEvent.ACTION_UP -> {
                mVelocityTracker?.clear()
            }
            MotionEvent.ACTION_CANCEL -> {
                mVelocityTracker?.clear()
                mScrollState = ScrollState.IDLE
            }
        }
        return mScrollState == ScrollState.DRAGGING
    }

    private fun pickNewPointer(e: MotionEvent) {
        val actionIndex: Int = e.actionIndex
        if (e.getPointerId(actionIndex) == mScrollPointerId) {
            // Pick a new pointer to pick up the slack.
            val newIndex = if (actionIndex == 0) 1 else 0
            mScrollPointerId = e.getPointerId(newIndex)
            markInitTouchLocation(newIndex, e)
        }
    }

    private fun markInitTouchLocation(newIndex: Int, e: MotionEvent) {
        mLastTouchX = (e.getX(newIndex) + 0.5f).toInt()
        mLastTouchY = (e.getY(newIndex) + 0.5f).toInt()
        mInitialTouchX = mLastTouchX
        mInitialTouchY = mLastTouchY
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
//        Log.d("AutoPagerView", "onTouchEvent $e")
        if (mTouchSuppressed || mIgnoreMotionEventTillDown) {
            return false
        }
        val lm = layoutMaster ?: return false
        val canScrollHorizontally: Boolean = lm.canScrollHorizontally()
        val canScrollVertically: Boolean = lm.canScrollVertically()
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        }
        var eventAddedToVelocityTracker = false
        val action = e.actionMasked
        val actionIndex = e.actionIndex
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mScrollPointerId = e.getPointerId(0)
                markInitTouchLocation(0, e)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                mScrollPointerId = e.getPointerId(actionIndex)
                markInitTouchLocation(actionIndex, e)
            }
            MotionEvent.ACTION_MOVE -> {
                val index = e.findPointerIndex(mScrollPointerId)
                if (index < 0) {
                    Log.e("AutoPagerView", "Error processing scroll; pointer index for id "
                            + mScrollPointerId + " not found. Did any MotionEvents get skipped?")
                    return false
                }
                val x = (e.getX(index) + 0.5f).toInt()
                val y = (e.getY(index) + 0.5f).toInt()
                var dx = mLastTouchX - x
                var dy = mLastTouchY - y
                if (mScrollState != ScrollState.SCROLLING) {
                    var startScroll = false
                    if (canScrollHorizontally) {
                        dx = if (dx > 0) {
                            Math.max(0, dx - mTouchSlop)
                        } else {
                            Math.min(0, dx + mTouchSlop)
                        }
                        if (dx != 0) {
                            startScroll = true
                        }
                    }
                    if (canScrollVertically) {
                        dy = if (dy > 0) {
                            Math.max(0, dy - mTouchSlop)
                        } else {
                            Math.min(0, dy + mTouchSlop)
                        }
                        if (dy != 0) {
                            startScroll = true
                        }
                    }
                    if (startScroll) {
                        mScrollState = ScrollState.SCROLLING
                    }
                    if (mScrollState == ScrollState.SCROLLING) {
                        mLastTouchX = x
                        mLastTouchY = y
                        if (scrollByInternal(
                                        if (canScrollHorizontally) dx else 0,
                                        if (canScrollVertically) dy else 0,
                                        e)) {
                            parent.requestDisallowInterceptTouchEvent(true)
                        }
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                pickNewPointer(e)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mVelocityTracker?.clear()
                mScrollState = ScrollState.IDLE
            }
        }
        return true
    }


    private fun scrollByInternal(deltaX: Int, deltaY: Int, e: MotionEvent): Boolean {
        if (deltaX != 0) {
            return horizontalScrollBy(deltaX, e)
        }
        if (deltaY != 0) {
            return verticalScrollBy(deltaY, e)
        }
        return false
    }

    private fun verticalScrollBy(distance: Int, e: MotionEvent): Boolean {
        val lm = layoutMaster ?: return false
        val range = computeVerticalScrollRange()
        if (range == 0 || distance == 0) return false
        if (distance < 0) {
            lm.toPrevPage(mLayoutState)
        } else {
            lm.toNextPage(mLayoutState)
        }
        return true
    }

    private fun horizontalScrollBy(distance: Int, e: MotionEvent): Boolean {
        val lm = layoutMaster ?: return false
        val range = computeHorizontalScrollRange()
        if (range == 0 || distance == 0) return false
        if (distance < 0) {
            lm.toPrevPage(mLayoutState)
        } else {
            lm.toNextPage(mLayoutState)
        }
        return true
    }

    /**
     * @param direction Negative to scroll up, positive to scroll down.
     */
    private fun scrollOnePage(direction: Int) {
        val lm = layoutMaster
                ?: throw IllegalArgumentException("scrollOnePage: no layoutMaster been attached. ")
        if (direction < 0) {
            lm.toPrevPage(mLayoutState)
        } else {
            lm.toNextPage(mLayoutState)
        }
    }
}
