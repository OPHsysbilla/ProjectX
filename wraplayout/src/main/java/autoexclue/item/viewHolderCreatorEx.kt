package autoexclue.item

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import autoexclue.AutoPagerView

inline fun <VH : AutoPagerView.ViewHolder> pagerViewHolderCreatorEx(crossinline f: (view: View) -> VH) =
        object : IPagerViewHolderCreator<VH> {
            override fun create(view: View): VH = f(view)
        }

inline fun <VH : AutoPagerView.ViewHolder> viewBindingVHCreatorEx(
        crossinline inflaterF: (inflater: LayoutInflater, parent: ViewGroup) -> VH) =
        object : IViewBindingVHCreator<VH> {
            override fun inflateBy(inflater: LayoutInflater, parent: ViewGroup): VH =
                    inflaterF(inflater, parent)

            override fun create(view: View): VH = throw UnsupportedOperationException()
        }