package autoexclue

import android.view.View

interface IViewHolderCreator<VH : AutoPageListView.ViewHolder> {
    fun create(view: View): VH
}