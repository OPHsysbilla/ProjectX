package autoexclue.item

import android.view.View
import autoexclue.AutoPagerView

interface IViewHolderCreator<VH : AutoPagerView.ViewHolder> {
    fun create(view: View): VH
}