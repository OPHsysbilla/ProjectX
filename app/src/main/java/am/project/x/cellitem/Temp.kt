//package com.fenbi.megrez.app.exercisescope.cellitem
//
//import android.content.Context
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import com.fenbi.megrez.app.devkit.dp2px
//import com.fenbi.megrez.app.exercisescope.flowlayout.ChoiceGroup
//import com.fenbi.megrez.app.exercisescope.flowlayout.ChoiceTag
//import com.fenbi.megrez.app.megrezView.FixedFlowLayout
//import com.fenbi.megrez.app.megrezView.autopager.AutoPagerView
//import com.fenbi.megrez.app.megrezView.autopager.item.AbstractCellItem
//import com.fenbi.megrez.app.megrezView.autopager.item.IPagerViewHolderCreator
//import com.fenbi.megrez.app.megrezView.autopager.item.pagerViewHolderCreatorEx
//import com.yuanfudao.android.megrez.exercisescope.R
//
///**
// * Created by lei.jialin on 2021/4/22
// */
//
//class ChoiceGroupCellItem(
//    val groupNum: Int,
//    var choiceGroup: ChoiceGroup<*>,
//    private val onSelect: (groupNum: Int, indexInList: Int, choice: ChoiceTag<*>?) -> Unit
//) :
//    AbstractCellItem<ChoiceGroupCellItem.ViewHolder>() {
//
//    override fun onBindViewHolder(holder: ViewHolder, parent: ViewGroup) {
//        holder.tvGroupTitle.text = choiceGroup.groupTitle
//        holder.tvGroupTitle.visibility =
//            if (choiceGroup.groupTitle.isNullOrEmpty()) View.GONE else View.GONE
//        refreshFlowLayout(holder.flowLayout, choiceGroup.choices)
//    }
//
//    private fun refreshView(view: View?, index: Int, tag: ChoiceTag<*>): View? {
//        val tvTag = view?.findViewById<TextView>(R.id.tv_text)
//        tvTag?.text = tag.name
//        view?.apply {
//            this.tag = tag
//            this.isSelected = tag.isSelect
//            this.setOnClickListener { v ->
//                val tagSelect = v.tag as? ChoiceTag<*>
//                onSelect.invoke(
//                    groupNum,
//                    choiceGroup.choices.indexOf(tagSelect), tagSelect
//                )
//            }
//        }
//        return view
//    }
//
//    private fun refreshFlowLayout(flowLayout: FixedFlowLayout, choices: List<ChoiceTag<*>>?) {
//        if (choices.isNullOrEmpty()) return
//        choices.forEachIndexed { index, tag ->
//            val view = flowLayout.getChildAt(index)
//                ?: createView(flowLayout).also { flowLayout.addView(it) }
//            refreshView(view, index, tag)
//        }
//    }
//
//    private fun createView(viewGroup: ViewGroup): View {
//        return LayoutInflater.from(viewGroup.context)
//            .inflate(R.layout.exercisescope_single_choice_select, viewGroup, false)
//    }
//
//
//    override fun firstAssumeMeasureHeight(context: Context): Int = context.dp2px(60)
//
//    override val pagerViewHolderCreator: IPagerViewHolderCreator<ViewHolder> =
//        pagerViewHolderCreatorEx { view: View -> ViewHolder(view) }
//    override val layoutRes: Int
//        get() = R.layout.exercisescope_choicegroup_item_flowlayout
//
//    class ViewHolder internal constructor(itemView: View) : AutoPagerView.ViewHolder(itemView) {
//        val tvGroupTitle: TextView = itemView.findViewById(R.id.title)
//        val flowLayout: FixedFlowLayout = itemView.findViewById(R.id.flow_layout)
//    }
//}