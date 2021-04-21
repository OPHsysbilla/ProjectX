package autoexclue.item

import android.view.ViewGroup
import androidx.annotation.LayoutRes
import autoexclue.AutoPagerView

/**
 * Created by lei.jialin on 2021/4/19
 */
abstract class AbstractCellItem<T: AutoPagerView.ViewHolder> {

    abstract val viewHolderCreator: IViewHolderCreator<T>

    @get:LayoutRes
    abstract val layoutRes: Int
    open val viewType: Int
        get() = hashInt(layoutRes)

    private fun hashInt(v: Int): Int {
        var value = v
        value = value xor (value shl 13)
        value = value xor (value ushr 17)
        value = value xor (value shl 5)
        return value
    }
    abstract fun onBindViewHolder(holder: T, parent: ViewGroup)

    abstract fun firstAssumeMeasureHeight(): Int
}