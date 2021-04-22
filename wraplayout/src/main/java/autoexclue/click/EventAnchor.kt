package autoexclue.click

import android.view.View
import autoexclue.AutoPagerView
import autoexclue.adapter.AutoPageAdapter
import autoexclue.item.AbstractCellItem

/**
 * Created by lei.jialin on 2021/4/22
 */
abstract class EventAnchor<VH : AutoPagerView.ViewHolder>(val clazz: Class<VH>) {
    abstract fun onEvent(view: View, viewHolder: VH, adapter: AutoPageAdapter)

    protected fun rawDataAt(viewHolder: VH?, adapter: AutoPageAdapter): AbstractCellItem<*>? {
        val pos = viewHolder?.mPosition ?: return null
        return adapter.dataAt(pos)
    }

    open fun onBindMany(viewHolder: VH): List<View?>? {
        return null
    }
}