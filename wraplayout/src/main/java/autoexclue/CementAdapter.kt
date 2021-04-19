package autoexclue

import android.util.Pair
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes

class CementAdapter : AutoExcludeLayout.Adapter<AutoExcludeLayout.ViewHolder>() {
    //        </editor-fold>
    var cementItems = ModelList()

    override fun onCreateViewHolderAt(index: Int, parent: ViewGroup): AutoExcludeLayout.ViewHolder {
        val item = cementItems.getOrNull(index) ?: throw IllegalAccessException(" out of box~ ")
        return cementItems.viewHolderFactory.create(item.viewType, parent)
    }
    fun clearData() {
        val intialSize = cementItems.size
        cementItems.clear()
        notifyItemRangeRemoved(intialSize, cementItems.size)
    }

    fun addDataList(cements: List<CementItem<*>>?) {
        cements ?: return
        val intialSize = cementItems.size
        cementItems.addAll(cements)
        notifyItemRangeInserted(intialSize, cementItems.size)
    }

    override val totalDataSize: Int
        get() = cementItems.size


    class ModelList : ArrayList<CementItem<*>>() {
        val viewHolderFactory = ViewHolderFactory()
        override fun add(element: CementItem<*>): Boolean {
            viewHolderFactory.register(element)
            return super.add(element)
        }

        override fun add(index: Int, element: CementItem<*>) {
            viewHolderFactory.register(element)
            super.add(index, element)
        }

        override fun addAll(elements: Collection<CementItem<*>>): Boolean {
            viewHolderFactory.register(elements)
            return super.addAll(elements)
        }

        override fun addAll(index: Int, elements: Collection<CementItem<*>>): Boolean {
            viewHolderFactory.register(elements)
            return super.addAll(index, elements)
        }
    }

    //<editor-fold desc="ViewHolderFactory">
    class ViewHolderFactory {
        private val creatorSparseArray = SparseArray<Pair<Int, IViewHolderCreator<*>>?>()
        fun register(cement: CementItem<*>) {
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

        fun register(cements: Collection<CementItem<*>?>?) {
            cements ?: return
            for (cement in cements) {
                cement?.let { register(cement) }
            }
        }

        fun create(@LayoutRes viewType: Int, parent: ViewGroup): AutoExcludeLayout.ViewHolder {
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

    override fun bindData2ViewHolder(index: Int, vh: AutoExcludeLayout.ViewHolder, parent: ViewGroup) {
       val item = cementItems.getOrNull(index) as? CementItem<AutoExcludeLayout.ViewHolder> ?: return
        item.onBindViewHolder(vh, parent)
    }

    //</editor-fold>
}