package autoexclue.layout

import android.view.View
import android.view.ViewGroup

/**
 * Created by lei.jialin on 2021/6/17
 */
object ViewSpecMethod {
    @JvmStatic
    fun chooseDesiredSize(spec: Int, desired: Int, min: Int): Int {
        val mode = View.MeasureSpec.getMode(spec)
        val size = View.MeasureSpec.getSize(spec)
        return when (mode) {
            View.MeasureSpec.EXACTLY -> size
            View.MeasureSpec.AT_MOST -> Math.min(size, Math.max(desired, min))
            View.MeasureSpec.UNSPECIFIED -> Math.max(desired, min)
            else -> Math.max(desired, min)
        }
    }

    @JvmStatic
    fun getChildMeasureSpecOf(parentSize: Int, parentMode: Int, padding: Int,
                              childDimension: Int): Int {
        val size = Math.max(0, parentSize - padding)
        var resultSize = 0
        var resultMode = 0
        if (childDimension >= 0) {
            resultSize = childDimension
            resultMode = View.MeasureSpec.EXACTLY
        } else if (childDimension == ViewGroup.LayoutParams.MATCH_PARENT) {
            resultSize = size
            resultMode = parentMode
        } else if (childDimension == ViewGroup.LayoutParams.WRAP_CONTENT) {
            resultSize = size
            resultMode = if (parentMode == View.MeasureSpec.AT_MOST || parentMode == View.MeasureSpec.EXACTLY) {
                View.MeasureSpec.AT_MOST
            } else {
                View.MeasureSpec.UNSPECIFIED
            }
        }
        return View.MeasureSpec.makeMeasureSpec(resultSize, resultMode)
    }

    @JvmStatic
    fun isMeasurementUpToDate(childSize: Int, spec: Int, dimension: Int): Boolean {
        val specMode = View.MeasureSpec.getMode(spec)
        val specSize = View.MeasureSpec.getSize(spec)
        if (dimension > 0 && childSize != dimension) {
            return false
        }
        return when (specMode) {
            View.MeasureSpec.UNSPECIFIED -> true
            View.MeasureSpec.AT_MOST -> specSize >= childSize
            View.MeasureSpec.EXACTLY -> specSize == childSize
            else -> false
        }
    }
}