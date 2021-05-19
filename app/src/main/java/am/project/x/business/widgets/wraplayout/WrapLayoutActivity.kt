/*
 * Copyright (C) 2018 AlexMofer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package am.project.x.business.widgets.wraplayout

import am.appcompat.app.BaseActivity
import am.project.x.R
import am.widget.wraplayout.RandomUtils
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import autoexclue.adapter.AutoPageAdapter
import autoexclue.item.AbstractCellItem
import am.widget.wraplayout.item.TestCellItem
import am.widget.wraplayout.item.GroupTitleCellItem
import android.util.Log
import android.widget.Button
import autoexclue.AutoPagerView
import autoexclue.layout.LinearLayoutMaster
import com.fenbi.megrez.app.exercisescope.flowlayout.ChoiceDialog
import kotlinx.android.synthetic.main.activity_wraplayout.*
import java.util.*

/**
 * 自动换行布局
 */
class WrapLayoutActivity : BaseActivity(R.layout.activity_wraplayout), RadioGroup.OnCheckedChangeListener, OnSeekBarChangeListener {
    private var mVContent: AutoPagerView? = null
    private var other: View? = null
    private lateinit var choiceBtn: Button
    private val dialog by lazy {
        ChoiceDialog(this)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(R.id.wl_toolbar)
        mVContent = findViewById<AutoPagerView>(R.id.wl_wl_content)
        other = findViewById(R.id.ohter_layout)
        val gravity = findViewById<RadioGroup>(R.id.wl_rg_gravity)
        val horizontal = findViewById<SeekBar>(R.id.wl_sb_horizontal)
        val vertical = findViewById<SeekBar>(R.id.wl_sb_vertical)
        val height = findViewById<SeekBar>(R.id.wl_sb_height)
        choiceBtn = findViewById<Button>(R.id.choice_button)
        choiceBtn.setOnClickListener {
            dialog.show()
        }
        gravity.setOnCheckedChangeListener(this)
        gravity.check(R.id.wl_rb_top)
        horizontal.setOnSeekBarChangeListener(this)
        horizontal.progress = 15
        vertical.setOnSeekBarChangeListener(this)
        vertical.progress = 15
        height.setOnSeekBarChangeListener(this)
        height.progress = 15
        initAutoLayout()
    }

    private fun initAutoLayout() {
        pager_index.callback = { nextPage ->
            val a = mVContent?.getCurrentIndex() ?: 0
            if (nextPage) {
                mVContent?.switchToPage(a + 1)
            } else {
                mVContent?.switchToPage(a - 1)
            }
        }
        val layoutMaster =  LinearLayoutMaster()
        layoutMaster.callbackPageIndex = {
            pager_index.setPageDecorateText(it)
        }
        mVContent?.layoutMaster = layoutMaster
        adapter.setOnItemClickListener(object : AutoPageAdapter.OnItemClickListener {
            override fun onClick(position: Int, itemView: View, viewHolder: AutoPagerView.ViewHolder, model: AbstractCellItem<*>?) {
                val title = (model as? TestCellItem)?.str ?:""
                Log.d("autopagers", " onClick post: $position, $title")
            }
        })
        mVContent!!.adapter = adapter
        generateData()
    }

    private fun generateData() {
        adapter.clearData()
        val a: MutableList<AbstractCellItem<*>> = ArrayList()
        val groupSize = RandomUtils.nextInt(1, 10)
        for (i in 0 until groupSize) {
            val size = RandomUtils.nextInt(1, 10)
            a.add(GroupTitleCellItem(i, "Group Title ${i}: " + RandomUtils.nextInt(0, 99999)))
            for (j in 0 until size) {
                a.add(TestCellItem(j, "belong Group${i} : " + RandomUtils.nextDouble()))
            }
        }
        adapter.addDataList(a)
        mVContent?.switchToPage(0)
    }

    var adapter = AutoPageAdapter()

    // Listener
    override fun onCheckedChanged(group: RadioGroup, checkedId: Int) {
//        when (checkedId) {
//            R.id.wl_rb_top -> mVContent?.gravity = WrapLayout.GRAVITY_TOP
//            R.id.wl_rb_center -> mVContent?.gravity = WrapLayout.GRAVITY_CENTER
//            R.id.wl_rb_bottom -> mVContent?.gravity = WrapLayout.GRAVITY_BOTTOM
//        }
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        when (seekBar.id) {
            R.id.wl_sb_horizontal -> mVContent?.horizontalSpacing = (progress * resources.displayMetrics.density).toInt()
            R.id.wl_sb_vertical -> mVContent?.verticalSpacing = (progress * resources.displayMetrics.density).toInt()
            R.id.wl_sb_height -> {
                val lp = mVContent!!.layoutParams
                val p = progress * 1.0f / 100f
                lp.height = (p * resources.displayMetrics.density * 1000 + 600).toInt()
                mVContent!!.layoutParams = lp
                mVContent!!.requestLayout()
            }
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {}
    override fun onStopTrackingTouch(seekBar: SeekBar) {}

    companion object {
        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, WrapLayoutActivity::class.java))
        }
    }
}