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
import java.util.*

/**
 * Created by lei.jialin on 2021/4/19
 */
class TestCellItem(private val index: Int, val str: String?, val acc: Int) : AbstractCellItem<TestCellItem.ViewHolder>() {
    override fun onBindViewHolder(holder: ViewHolder, parent: ViewGroup) {
        holder.tv.text = "[$acc]" + str
        holder.tvLastExercise.text = "第%d个".format(Locale.CHINA, index)
    }

    override fun firstPresetHeight(context: Context): Int = 174


    override val pagerViewHolderCreator: IPagerViewHolderCreator<ViewHolder> = pagerViewHolderCreatorEx { view: View -> ViewHolder(view) }
    override val layoutRes: Int
        get() = R.layout.layout_cement_test

    class ViewHolder internal constructor(itemView: View) : AutoPagerView.ViewHolder(itemView) {
        val tv: TextView = itemView.findViewById(R.id.tv_item_simple_text)
        val tvLastExercise: TextView = itemView.findViewById(R.id.tv_last_exercise)
    }
}