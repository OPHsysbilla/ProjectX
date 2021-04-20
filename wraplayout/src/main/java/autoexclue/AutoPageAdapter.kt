package autoexclue

import android.util.Pair
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes

class AutoPageAdapter : AutoPagerView.Adapter<AutoPagerView.ViewHolder>() {
    //        </editor-fold>
    var displyItems = CellList()

    override fun onCreateViewHolderAt(index: Int, parent: ViewGroup): AutoPagerView.ViewHolder {
        val item = displyItems.getOrNull(index) ?: throw IllegalAccessException(" out of box~ ")
        return displyItems.viewHolderFactory.create(item.viewType, parent)
    }
    fun clearData() {
        displyItems.clear()
    }

    fun addDataList(cements: List<AbstractCellItem<*>>?) {
        cements ?: return
        displyItems.addAll(cements)
    }

    override fun totalDataSize(): Int =  displyItems.size

    class CellList : ArrayList<AbstractCellItem<*>>() {
        val viewHolderFactory = ViewHolderFactory()
        override fun add(element: AbstractCellItem<*>): Boolean {
            viewHolderFactory.register(element)
            return super.add(element)
        }

        override fun add(index: Int, element: AbstractCellItem<*>) {
            viewHolderFactory.register(element)
            super.add(index, element)
        }

        override fun addAll(elements: Collection<AbstractCellItem<*>>): Boolean {
            viewHolderFactory.register(elements)
            return super.addAll(elements)
        }

        override fun addAll(index: Int, elements: Collection<AbstractCellItem<*>>): Boolean {
            viewHolderFactory.register(elements)
            return super.addAll(index, elements)
        }
    }

    //<editor-fold desc="ViewHolderFactory">
    class ViewHolderFactory {
        private val creatorSparseArray = SparseArray<Pair<Int, IViewHolderCreator<*>>?>()
        fun register(cement: AbstractCellItem<*>) {
            val viewType = cement.viewType
            if (viewType == View.NO_ID) {
                throw RuntimeException("illegal viewType=$viewType")
            }
            if (creatorSparseArray[viewType] == null) {
                creatorSparseArray.put(
                        viewType,
                        android.util.Pair.create(cement.layoutRes, cement.viewHolderCreator)
                )
            }
        }

        fun register(cements: Collection<AbstractCellItem<*>?>?) {
            cements ?: return
            for (cement in cements) {
                cement?.let { register(cement) }
            }
        }

        fun create(@LayoutRes viewType: Int, parent: ViewGroup): AutoPagerView.ViewHolder {
            val info = creatorSparseArray[viewType]
                    ?: throw RuntimeException("cannot find viewHolderCreator for viewType=$viewType")
            return try {
                info.second.create(
                        LayoutInflater.from(parent.context).inflate(info.first, parent, false))
            } catch (e: Exception) {
                throw RuntimeException("cannot inflate view=${parent.context.resources.getResourceName(info.first)} reason:${e.message}".trimIndent(), e)
            }
        }
    }

    override fun bindData2ViewHolder(index: Int, vh: AutoPagerView.ViewHolder, parent: ViewGroup) {
       val item = displyItems.getOrNull(index) as? AbstractCellItem<AutoPagerView.ViewHolder> ?: return
        item.onBindViewHolder(vh, parent)
    }

    //</editor-fold>
}