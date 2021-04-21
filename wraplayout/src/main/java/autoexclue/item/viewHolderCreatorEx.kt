package autoexclue.item

import android.view.View
import autoexclue.AutoPagerView

inline fun <VH : AutoPagerView.ViewHolder> pagerViewHolderCreatorEx(crossinline f: (view: View) -> VH) =
        object : IViewHolderCreator<VH> {
            override fun create(view: View): VH = f(view)
        }