package com.fenbi.megrez.app.exercisescope.cellitem

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.fenbi.megrez.app.devkit.dp2px
import com.fenbi.megrez.app.exercisescope.flowlayout.ChoiceGroup
import com.fenbi.megrez.app.exercisescope.flowlayout.ChoiceTag
import com.fenbi.megrez.app.megrezView.autopager.item.IPagerViewHolderCreator
import com.fenbi.megrez.app.megrezView.autopager.item.pagerViewHolderCreatorEx
import com.yuanfudao.android.megrez.exercisescope.R

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

    override fun firstAssumeMeasureHeight(context: Context): Int = context.dp2px(60)

    override val pagerViewHolderCreator: IPagerViewHolderCreator<ViewHolder> =
        pagerViewHolderCreatorEx { view: View -> ViewHolder(view) }
    override val layoutRes: Int
        get() = R.layout.exercisescope_choicegroup_item_flowlayout

    class ViewHolder internal constructor(itemView: View) : BaseGroupCellItem.ViewHolder(itemView)
}