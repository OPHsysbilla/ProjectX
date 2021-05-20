package am.widget.wraplayout.item

import am.widget.wraplayout.R
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import autoexclue.AutoPagerView
import autoexclue.item.IPagerViewHolderCreator
import autoexclue.item.AbstractCellItem
import autoexclue.item.pagerViewHolderCreatorEx

/**
 * Created by lei.jialin on 2021/4/21
 */
class GroupTitleCellItem(val index: Int, val str: String?) : AbstractCellItem<GroupTitleCellItem.ViewHolder>() {
    override fun onBindViewHolder(holder: ViewHolder, parent: ViewGroup) {
        holder.tv.text = str
        holder.tvLastExercise.text = "第${index + 1}个"

    }

    override val pagerViewHolderCreator: IPagerViewHolderCreator<ViewHolder> = pagerViewHolderCreatorEx { view: View -> ViewHolder(view) }
    override val layoutRes: Int
        get() = R.layout.layout_group_title_cell

    class ViewHolder internal constructor(itemView: View) : AutoPagerView.ViewHolder(itemView) {
        val tv: TextView = itemView.findViewById(R.id.tv_item_simple_text)
        val tvLastExercise: TextView = itemView.findViewById(R.id.tv_last_exercise)
    }

    override fun firstPresetHeight(context: Context): Int  = 87
}