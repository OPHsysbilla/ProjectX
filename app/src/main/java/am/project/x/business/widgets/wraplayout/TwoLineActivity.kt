package am.project.x.business.widgets.wraplayout

import am.project.x.R
import am.widget.wraplayout.MegezScrollView
import am.widget.wraplayout.item.HeadTextCellItem
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import autoexclue.item.AbstractCellItem

class TwoLineActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_two_line)
        initView()
        initData()
    }

    private val rangeSelect by lazy {
        findViewById<MegezScrollView>(R.id.range_select)
    }

    private val tvPageIndex by lazy {
        findViewById<TextView>(R.id.pager_index)
    }
    private val btnPrevPage by lazy {
        findViewById<View>(R.id.btn_prev_page)
    }

    private val btnNextPage by lazy {
        findViewById<View>(R.id.btn_next_page)
    }

    private fun initView() {

    }

    private fun initData() {
        val list = mutableListOf<HeadTextCellItem>()
        for (i in 0 until 10) {
            list.add(HeadTextCellItem("$i-$i"))
        }
        val viewHoldList =
                list.map { it.pagerViewHolderCreator.create(LayoutInflater.from(this).inflate(it.layoutRes, rangeSelect, false)) }
        viewHoldList.forEachIndexed { index, item ->
            rangeSelect.addView(item.itemView)
            list[index].onBindViewHolder(item, rangeSelect)
        }
    }


}