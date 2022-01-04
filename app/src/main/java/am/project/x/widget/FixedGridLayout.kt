package am.project.x.widget

import am.project.x.R
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.isVisible
import java.util.*

/**
 * created by tangjing on 2021/8/2
 */
class FixedGridLayout @JvmOverloads constructor(
        context: Context,
        attributeSet: AttributeSet? = null,
        typeStyle: Int = 0
) : FrameLayout(context,attributeSet,typeStyle) {
    private var mColumn: Int = 3
    private var mRow: Int = 3
    private var maxCountOnePage: Int = 9
    private val viewList = mutableListOf<View>()
    private val pageList = mutableListOf<List<GridViewHolder>>()

    // TODO： @leijialin 改成数据驱动的形式
    var disableSelect = false

    private var pageHash = UUID.randomUUID().toString()

    var currentPage = -1
        set(value) {
            if (value >= 0 && value < pageList.size) {
                field = value
                render(value)
            }
        }
    var isSelectMode = false

    private fun updateStatus(isSelectMode: Boolean) {
        this.isSelectMode = isSelectMode
        if (!isSelectMode) {
            pageList.flatten().forEach {
                it.isSelected = false
            }
        }
        render(currentPage)
    }

    fun selectAll(isSelected: Boolean) {
        pageList.flatten().forEach {
            it.isSelected = isSelected
        }
        render(currentPage)
    }

    fun setData(
            column: Int = mColumn,
            row: Int = mRow,
            viewHolders: List<GridViewHolder>,
            defaultPage: Int,
    ) {
        pageList.clear()
        mColumn = column
        mColumn = row
        maxCountOnePage = Math.max(1,column * row)
        pageList.addAll(viewHolders.chunked(maxCountOnePage))
        currentPage = (defaultPage.coerceAtLeast(0)) / maxCountOnePage
    }

    @SuppressLint("SetTextI18n")
    private fun render(pageIndex: Int) {
        renderSpecific(pageIndex)
    }

    private fun renderSpecific(pageIndex: Int) {
        val page = pageList.getOrNull(pageIndex) ?: return
        val count = page.size - viewList.size
        prepareView(count,page)
        val pairs = page.zip(viewList)
        pairs.forEach { (holder,view) ->
            view.visibility = VISIBLE
            holder.onBindingData(view,isSelectMode)
            view.setOnClickListener {
                if (isSelectMode) {
                    holder.isSelected = !holder.isSelected
                }
                holder.onBindingData(view,isSelectMode)
                holder.onClick(isSelectMode)
            }
            view.setOnLongClickListener {
                if (disableSelect) return@setOnLongClickListener false
                if (!isSelectMode) {
                    holder.isSelected = true
                    holder.onLongClick(holder.isSelected)
                    true
                } else {
                    false
                }
            }

        }
    }

    private fun renderSelectView(selectView: ImageView,holder: GridViewHolder) {
        if (isSelectMode) {
            selectView.visibility = VISIBLE
            val selectIcon = if (holder.isSelected) {
                R.mipmap.megrez_view_ic_circle_select
            } else {
                R.mipmap.megrez_view_ic_circle_unselect
            }
            selectView.setImageResource(selectIcon)
        } else {
            selectView.visibility = INVISIBLE
        }
    }

    private fun prepareView(count: Int,page: List<GridViewHolder>) {
        val oldSize = viewList.size
        val newViews = mutableListOf<View>()
        when {
            count > 0 -> {
                repeat(count) {
                    page.getOrNull(it)?.inflateRootView(context)?.let { root ->
                        newViews.add(root)
                        Log.d("whuys","root: $root, parent: ${root.parent}, ")
                    }
                }
                viewList.addAll(newViews)
            }
            count < 0 -> {
                viewList.takeLast(-count).forEach {
                    it.isVisible = false
                }
            }
            else -> {
            }
        }
        newViews.forEachIndexed { index,view ->
            val col = (oldSize + index).rem(mColumn)
            val row = (oldSize + index).div(mColumn)
            page.getOrNull(index)?.generateLayoutParam(context)?.apply {
                if (this.width > 0 || this.height > 0) {
                    this.setMargins(
                            this.leftMargin + col * (this.leftMargin + this.width + this.rightMargin),
                            this.topMargin + row * (this.topMargin + this.height + this.bottomMargin),
                            0,
                            0,
                    )
                }
                addView(view,this)
            }
        }
    }

    fun getPageList(): List<List<GridViewHolder>> = pageList

    fun getSelectedFile() = pageList.flatten().filter { it.isSelected }

    abstract class GridViewHolder constructor(
            val onClick: (Boolean) -> Unit,
            val onLongClick: (Boolean) -> Unit = {}
    ) {
        abstract fun onBindingData(view: View,selectMode: Boolean)
        abstract fun inflateRootView(context: Context): View
        abstract fun generateLayoutParam(context: Context): MarginLayoutParams
        var isSelected = false
    }
}