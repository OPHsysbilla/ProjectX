package autoexclue

import android.util.SparseArray
import java.util.*

class PageManager {
    var layout: AutoExcludeLayout? = null
    var data: List<CementItem<*>>? = null
    var segments: List<Segment>? = null
    var findPageMap: SparseArray<Int>? = null
    var curSegment: Segment? = null
    var adapter = CementAdapter()
    var pageCount = 0
    private fun switchToItem(item: CementItem<*>) {
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
        val items: MutableList<CementItem<*>> = ArrayList()
        for (i in curSegment!!.start until curSegment!!.end) {
            items.add(data!![i])
        }
        adapter.addDataList(items)
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