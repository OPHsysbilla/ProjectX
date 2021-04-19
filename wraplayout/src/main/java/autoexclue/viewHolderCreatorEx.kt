package autoexclue

import android.view.View

inline fun <VH : AutoExcludeLayout.ViewHolder> viewHolderCreatorEx(crossinline f: (view: View) -> VH) =
        object : IViewHolderCreator<VH> {
            override fun create(view: View): VH = f(view)
        }