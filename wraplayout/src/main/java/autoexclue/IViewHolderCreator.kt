package autoexclue

import android.view.View

interface IViewHolderCreator<VH : AutoPagerView.ViewHolder> {
    fun create(view: View): VH
}