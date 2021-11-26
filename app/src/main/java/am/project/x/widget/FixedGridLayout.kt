package am.project.x.widget

import am.project.x.R
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.LayoutRes
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
    private var mLayoutConfig: IFixLayoutConfig = DefaultLayoutConfig(3,6)
    private val viewList = mutableListOf<View>()
    private val pageList = mutableListOf<List<ViewHolder>>()

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

    fun initLayoutConfig(layoutConfig: IFixLayoutConfig) {
        mLayoutConfig = layoutConfig
    }

    fun setData(
            viewHolders: List<ViewHolder>,
            defaultPage: Int,
    ) {
        pageList.clear()
        if (mLayoutConfig.column == 0 || mLayoutConfig.row == 0) return
        val maxCountOnePage = Math.max(1,mLayoutConfig.column * mLayoutConfig.row)
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

    private fun renderSelectView(selectView: ImageView,holder: ViewHolder) {
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

    private fun prepareView(count: Int,page: List<ViewHolder>) {
        val oldSize = viewList.size
        val newViews = mutableListOf<View>()
        val regulation = mLayoutConfig
        when {
            count > 0 -> {
                val inflater = LayoutInflater.from(context)
                val layout = regulation.getLayoutId()
                repeat(count) {
                    newViews.add(
                            inflater.inflate(layout,null,false)
                    )
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
            val col = (oldSize + index).rem(mLayoutConfig.column)
            val row = (oldSize + index).div(mLayoutConfig.column)
            val lp = regulation.generateLayoutParam(context).apply {
                if (this.width > 0 || this.height > 0) {
                    this.setMargins(
                            this.leftMargin + col * (this.leftMargin + this.width + this.rightMargin),
                            this.topMargin + row * (this.topMargin + this.height + this.bottomMargin),
                            0,
                            0,
                    )
                }
            }
            addView(view,lp)
        }
    }

    fun getPageList(): List<List<ViewHolder>> = pageList

    fun getSelectedFile() = pageList.flatten().filter { it.isSelected }

    abstract class ViewHolder constructor(
            val onClick: (Boolean) -> Unit,
            val onLongClick: (Boolean) -> Unit = {}
    ) {
        abstract fun onBindingData(view: View,selectMode: Boolean)
        var isSelected = false
    }

    abstract class IFixLayoutConfig(val column: Int,val row: Int) {
        @LayoutRes
        abstract fun getLayoutId(): Int
        abstract fun generateLayoutParam(context: Context): MarginLayoutParams
    }

    private class DefaultLayoutConfig(column: Int,row: Int) : IFixLayoutConfig(column,row) {
        override fun getLayoutId(): Int = R.layout.layout_cement_head_text

        override fun generateLayoutParam(context: Context): MarginLayoutParams {
            return LayoutParams(
                    400,
                    184
            ).apply {
                setMargins(
                        30,
                        30,
                        0,
                        0,
                )
            }
        }

    }
}