package autoexclue.item

import android.view.View
import autoexclue.AutoPagerView

interface IPagerViewHolderCreator<VH : AutoPagerView.ViewHolder> {
    fun create(view: View): VH
}