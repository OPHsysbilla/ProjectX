package autoexclue.layout

import android.content.Context
import android.view.View

/**
 * Created by lei.jialin on 2021/5/19
 */
interface LayoutChildrenHelper {

    fun getChildAt(i: Int): View?
    fun getChildrenCount(): Int
    fun preMeasureChild(index: Int): Int
    fun removeAllViews()
    fun getViewOf(dataIndex: Int): View?
    fun removeView(childView: View)
    fun addView(childView: View)
    fun isViewGone(child: View): Boolean
    fun onChildAdded(dataIndex: Int, childWidth: Int, childHeight: Int)
}