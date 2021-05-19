package am.widget.wraplayout.item

import am.widget.wraplayout.R
import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import autoexclue.AutoPagerView
import autoexclue.item.AbstractCellItem
import autoexclue.item.IPagerViewHolderCreator
import autoexclue.item.pagerViewHolderCreatorEx
import java.util.*

/**
 * Created by lei.jialin on 2021/5/18
 */
class HeadTextCellItem(val str: String?) : AbstractCellItem<HeadTextCellItem.ViewHolder>() {
    override fun onBindViewHolder(holder: ViewHolder, parent: ViewGroup) {
        holder.tv.text = str
    }

    override fun firstAssumeMeasureHeight(context: Context): Int =
            TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 162f,
            context.resources.displayMetrics).toInt()

    override val pagerViewHolderCreator: IPagerViewHolderCreator<ViewHolder> = pagerViewHolderCreatorEx { view: View -> ViewHolder(view) }
    override val layoutRes: Int
        get() = R.layout.layout_cement_head_text

    class ViewHolder internal constructor(itemView: View) : AutoPagerView.ViewHolder(itemView) {
        val tv: TextView = itemView.findViewById(R.id.head_text)
    }
}