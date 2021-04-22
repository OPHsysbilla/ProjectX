package com.fenbi.megrez.app.exercisescope.cellitem

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.fenbi.megrez.app.exercisescope.flowlayout.ChoiceGroup
import com.fenbi.megrez.app.exercisescope.flowlayout.ChoiceTag
import com.fenbi.megrez.app.megrezView.FixedFlowLayout
import com.fenbi.megrez.app.megrezView.autopager.AutoPagerView
import com.fenbi.megrez.app.megrezView.autopager.item.AbstractCellItem
import com.yuanfudao.android.megrez.exercisescope.R

/**
 * Created by lei.jialin on 2021/4/22
 */
abstract class BaseGroupCellItem<T, VH : BaseGroupCellItem.ViewHolder>(
    val groupNum: Int,
    var choiceGroup: ChoiceGroup<T>,
    private val onSelect: (groupNum: Int, indexInList: Int, choice: ChoiceTag<T>?) -> Unit
) :
    AbstractCellItem<VH>() {

    override fun onBindViewHolder(holder: VH, parent: ViewGroup) {
        holder.tvGroupTitle.text = choiceGroup.groupTitle
        holder.tvGroupTitle.visibility = if (choiceGroup.groupTitle.isNullOrEmpty()) View.GONE else View.GONE
        refreshFlowLayout(holder.flowLayout, choiceGroup.choices)
    }

    private fun refreshView(view: View?, index: Int, tag: ChoiceTag<T>): View? {
        val tvTag = view?.findViewById<TextView>(R.id.tv_text)
        tvTag?.text = tag.name
        view?.apply {
            this.tag = tag
            this.isSelected = tag.isSelect
            this.setOnClickListener { v ->
                val tagSelect = v.tag as? ChoiceTag<T>
                onSelect.invoke(
                    groupNum,
                    choiceGroup.choices.indexOf(tagSelect), tagSelect
                )
            }
        }
        return view
    }

    private fun refreshFlowLayout(flowLayout: FixedFlowLayout, choices: List<ChoiceTag<T>>?) {
        if (choices.isNullOrEmpty()) return
        choices.forEachIndexed { index, tag ->
            val view = flowLayout.getChildAt(index)
                ?: createView(flowLayout).also { flowLayout.addView(it) }
            refreshView(view, index, tag)
        }
    }

    abstract fun createView(viewGroup: ViewGroup): View

    override val layoutRes: Int
        get() = R.layout.exercisescope_choicegroup_item_flowlayout

    abstract class ViewHolder internal constructor(itemView: View) : AutoPagerView.ViewHolder(itemView) {
        val tvGroupTitle: TextView = itemView.findViewById(R.id.title)
        val flowLayout: FixedFlowLayout = itemView.findViewById(R.id.flow_layout)
    }
}