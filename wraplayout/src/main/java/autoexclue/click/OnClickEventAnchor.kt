package autoexclue.click

import android.view.View
import autoexclue.AutoPagerView
import autoexclue.adapter.AutoPageAdapter
import autoexclue.item.AbstractCellItem

abstract class OnClickEventAnchor<VH : AutoPagerView.ViewHolder>(clazz: Class<VH>) : EventAnchor<VH>(clazz) {
    abstract fun onClick(
            position: Int, view: View, viewHolder: VH,
            rawData: AbstractCellItem<*>?
    )

    override fun onEvent(view: View, viewHolder: VH, adapter: AutoPageAdapter) {
        view.setOnClickListener { v ->
            onClick(position = viewHolder.mPosition, view = v, viewHolder = viewHolder, rawData = rawDataAt(viewHolder, adapter))
        }

    }
}