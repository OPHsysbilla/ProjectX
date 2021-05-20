package com.fenbi.megrez.app.exercisescope.cellitem

import am.project.x.R
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import autoexclue.item.IPagerViewHolderCreator
import autoexclue.item.pagerViewHolderCreatorEx
import com.fenbi.megrez.app.exercisescope.flowlayout.ChoiceGroup
import com.fenbi.megrez.app.exercisescope.flowlayout.ChoiceTag

/**
 * Created by lei.jialin on 2021/4/22
 */
class ChoiceGroupCellItem(
    groupNum: Int,
    choiceGroup: ChoiceGroup<String>,
    onSelect: (groupNum: Int, indexInList: Int, choice: ChoiceTag<String>?) -> Unit
) :
    BaseGroupCellItem<String, ChoiceGroupCellItem.ViewHolder>(groupNum, choiceGroup, onSelect) {

    override fun createView(viewGroup: ViewGroup): View {
        return LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.exercisescope_single_choice_select, viewGroup, false)
    }

    override fun firstPresetHeight(context: Context): Int = 174

    override val pagerViewHolderCreator: IPagerViewHolderCreator<ChoiceGroupCellItem.ViewHolder> =
        pagerViewHolderCreatorEx { view: View -> ViewHolder(view) }
    override val layoutRes: Int
        get() = R.layout.exercisescope_choicegroup_item_flowlayout

    class ViewHolder constructor(itemView: View) : BaseGroupCellItem.ViewHolder(itemView)
}