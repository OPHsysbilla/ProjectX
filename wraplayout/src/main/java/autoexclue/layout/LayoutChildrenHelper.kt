package autoexclue.layout

import android.view.View

/**
 * Created by lei.jialin on 2021/5/19
 */
interface LayoutChildrenHelper {

    fun getChildAt(i: Int): View?
    fun getChildrenCount(): Int
    fun preMeasureChild(index: Int): Int
    fun removeAllChildOnScreen()
    fun getViewOf(dataIndex: Int): View?
    fun removeChild(childView: Int, childView1: View)
    fun addView(childView: View)
    fun isViewGone(child: View): Boolean
    fun onChildAdded(dataIndex: Int, childWidth: Int, childHeight: Int)
}