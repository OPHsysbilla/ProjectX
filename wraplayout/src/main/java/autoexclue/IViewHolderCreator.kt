package autoexclue

import android.view.View

interface IViewHolderCreator<VH : AutoExcludeLayout.ViewHolder> {
    fun create(view: View): VH
}