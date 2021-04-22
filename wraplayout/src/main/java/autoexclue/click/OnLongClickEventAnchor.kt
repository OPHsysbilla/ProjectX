package autoexclue.click

import android.view.View
import autoexclue.AutoPagerView
import autoexclue.adapter.AutoPageAdapter
import autoexclue.item.AbstractCellItem

/**
 * Created by lei.jialin on 2021/4/22
 */

abstract class OnLongClickEventAnchor<VH : AutoPagerView.ViewHolder>(clazz: Class<VH>) : EventAnchor<VH>(clazz) {
    abstract fun onLongClick(
            position: Int, view: View, viewHolder: VH,
            rawData: AbstractCellItem<*>?
    ) : Boolean

    override fun onEvent(view: View, viewHolder: VH, adapter: AutoPageAdapter) {
        view.setOnLongClickListener { v ->
            onLongClick(position = viewHolder.mPosition, view = v, viewHolder = viewHolder, rawData = rawDataAt(viewHolder, adapter))
        }

    }
}