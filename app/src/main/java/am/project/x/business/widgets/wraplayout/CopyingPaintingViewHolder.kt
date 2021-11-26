package am.project.x.business.widgets.wraplayout

import am.project.x.widget.FixedGridLayout
import android.view.View

/**
 * Created by lei.jialin on 11/26/21
 */
class CopyingPaintingViewHolder(onClick: (Boolean) -> Unit,onLongClick: (Boolean) -> Unit = {}) : FixedGridLayout.ViewHolder(onClick,onLongClick) {
    override fun onBindingData(view: View,selectMode: Boolean) {
    }
}