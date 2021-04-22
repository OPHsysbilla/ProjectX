package com.fenbi.megrez.app.exercisescope.flowlayout

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.StyleRes
import com.fenbi.megrez.app.exercisescope.cellitem.ChoiceGroupCellItem
import com.fenbi.megrez.app.megrezView.autopager.AutoPagerView
import com.fenbi.megrez.app.megrezView.autopager.adapter.AutoPageAdapter
import com.yuanfudao.android.megrez.exercisescope.R

/**
 * Created by lei.jialin on 2021/4/22
 */
class ChoiceDialog @JvmOverloads constructor(
    context: Context,
    @StyleRes themeResId: Int = R.style.PlainDialogStyle
) : Dialog(context, themeResId) {

    companion object {
        fun create(context: Context): ChoiceDialog {

            // instantiate the dialog with the custom Theme
            val dialog =
                ChoiceDialog(context)
            //dialog.setCanceledOnTouchOutside(false);

            return dialog
        }
    }

    private lateinit var autoPager: AutoPagerView
    private lateinit var root: ViewGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val layout: View =
            inflater.inflate(R.layout.exercisescope_choice_dialog, null)
        root = layout as ViewGroup
        addContentView(
            layout, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        setContentView(layout)
        initView(root)
    }

    private val adapter = AutoPageAdapter()

    private val groups: List<ChoiceGroup<String>> = listOf(
        ChoiceGroup("听写顺序", DictationSequence.values().map { ChoiceTag(it.title, it.storeKey) }),
        ChoiceGroup("读音设置", DeconSubjectChinese.values().map { ChoiceTag(it.title, it.storeKey) }),
        ChoiceGroup("读音设置", DeaconSubjectEnglish.values().map { ChoiceTag(it.title, it.storeKey) }),
        ChoiceGroup("读音间隔", PauseGapTime.values().map { ChoiceTag(it.title, it.storeKey) })
    )

    private fun initView(root: ViewGroup) {
        autoPager = root.findViewById(R.id.auto_pager_layout)
        refreshSetting()
    }

    private fun refreshSetting() {
        val cells = groups.mapIndexed { index, data ->
            ChoiceGroupCellItem(index, data,
                onSelect = { groupNum: Int, indexInList: Int, choice: ChoiceTag<String>? ->
                    onCellSelect(groupNum, indexInList, choice)
                }
            )
        }
        adapter.clearData()
        adapter.addDataList(cells)
        adapter.notifyDataSetChanged()
    }

    private fun onCellSelect(groupNum: Int, indexInList: Int, choice: ChoiceTag<String>?) {
        val group = groups.getOrNull(groupNum) ?: return
        val curSelect = (choice ?: group.choices.getOrNull(indexInList)) ?: return
        curSelect.isSelect = true
        group.lastSelect?.isSelect = false
        group.lastSelect = curSelect
        refreshSetting()
    }
}