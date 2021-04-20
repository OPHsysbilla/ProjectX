package autoexclue

import android.util.SparseArray
import java.util.*

class PageManager {
    var layout: AutoPagerView? = null
    var data: List<AbstractCellItem<*>>? = null
    var segments: List<Segment>? = null
    var findPageMap: SparseArray<Int>? = null
    var curSegment: Segment? = null
    var adapter = AutoPageAdapter()
    var pageCount = 0
    private fun switchToItem(item: AbstractCellItem<*>) {
        val find = data!!.indexOf(item)
        if (find == -1) return
        if (find >= 0 && find < data!!.size) {
            switchToPage(find)
        }
    }

    private fun switchToPage(dataIndex: Int) {
        val page = findPageByDataIndex(dataIndex)
        curSegment = segments!![page]
        adapter.clearData()
        val items: MutableList<AbstractCellItem<*>> = ArrayList()
        for (i in curSegment!!.start until curSegment!!.end) {
            items.add(data!![i])
        }
        adapter.addDataList(items)
        adapter.notifyDataSetChanged()
    }

    private fun findPageByDataIndex(dataIndex: Int): Int {
        var page = 0
        var cur = segments!![page]
        while (page < segments!!.size && cur != null && cur.end < dataIndex) {
            page++
            cur = segments!![page]
        }
        return page
    }
}