package am.widget.wraplayout.item

import am.widget.wraplayout.R
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import autoexclue.AutoPagerView
import autoexclue.item.IViewHolderCreator
import autoexclue.item.AbstractCellItem
import autoexclue.item.pagerViewHolderCreatorEx

/**
 * Created by lei.jialin on 2021/4/19
 */
class ExerciseCellItem(private val index: Int, s: String?) : AbstractCellItem<ExerciseCellItem.ViewHolder>() {
    private val str: String
    override fun onBindViewHolder(holder: ViewHolder, parent: ViewGroup) {
        holder.tv.text = str
        holder.tvLastExercise.text = "第${index + 1}个"

    }

    override fun firstAssumeMeasureHeight(): Int = 100

    override val viewHolderCreator: IViewHolderCreator<ViewHolder> = pagerViewHolderCreatorEx { view: View -> ViewHolder(view) }
    override val layoutRes: Int
        get() = R.layout.layout_cement_test

    class ViewHolder internal constructor(itemView: View) : AutoPagerView.ViewHolder(itemView) {
        val tv: TextView = itemView.findViewById(R.id.tv_item_simple_text)
        val tvLastExercise: TextView = itemView.findViewById(R.id.tv_last_exercise)
    }

    init {
        val ss = StringBuilder()
        for (i in 0..9) {
            ss.append(s)
        }
        str = ss.toString()
    }
}