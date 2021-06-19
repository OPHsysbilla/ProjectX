package autoexclue.item

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import autoexclue.AutoPagerView

interface IPagerViewHolderCreator<VH : AutoPagerView.ViewHolder> {
    fun create(view: View): VH
}

interface IViewBindingVHCreator<VH : AutoPagerView.ViewHolder> : IPagerViewHolderCreator<VH> {
    fun inflateBy(inflater: LayoutInflater, parent: ViewGroup): VH
}
