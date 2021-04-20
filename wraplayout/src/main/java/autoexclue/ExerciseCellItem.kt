package autoexclue

import am.widget.wraplayout.R
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/**
 * Created by lei.jialin on 2021/4/19
 */
class ExerciseCellItem(s: String?) : AbstractCellItem<ExerciseCellItem.ViewHolder>() {
    private val str: String
    override fun onBindViewHolder(holder: ExerciseCellItem.ViewHolder, parent: ViewGroup) {
        holder.tv.text = str
    }

    override val viewHolderCreator: IViewHolderCreator<ViewHolder> = viewHolderCreatorEx { view: View -> ViewHolder(view) }
    override val layoutRes: Int
        get() = R.layout.layout_cement_test

    class ViewHolder internal constructor(itemView: View) : AutoPagerView.ViewHolder(itemView) {
        val tv: TextView = itemView.findViewById(R.id.tv_item_simple_title)
    }

    init {
        val ss = StringBuilder()
        for (i in 0..9) {
            ss.append(s)
        }
        str = ss.toString()
    }
}