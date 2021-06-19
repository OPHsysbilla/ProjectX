package autoexclue.adapter

import android.content.Context
import android.util.Pair
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import autoexclue.AutoPagerView
import autoexclue.click.EventAnchor
import autoexclue.click.EventAnchorHelper
import autoexclue.click.OnClickEventAnchor
import autoexclue.click.OnLongClickEventAnchor
import autoexclue.item.AbstractCellItem
import autoexclue.item.IPagerViewHolderCreator

class AutoPageAdapter : AutoPagerView.Adapter<AutoPagerView.ViewHolder>() {
    private val eventHookHelper: EventAnchorHelper<AutoPagerView.ViewHolder> = EventAnchorHelper()
    private var isAttached: Boolean = false

    //        </editor-fold>
    var displyItems = CellList()

    override fun onCreateViewHolderAt(index: Int, parent: ViewGroup): AutoPagerView.ViewHolder {
        val item = displyItems.getOrNull(index) ?: throw IllegalAccessException(" out of box~ ")
        val viewHolder =  displyItems.viewHolderFactory.create(item.viewType, parent)
        viewHolder.mPosition = index
        viewHolder.viewType = item.viewType
        eventHookHelper.bind(viewHolder, this)
        return viewHolder
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
        private val creatorSparseArray = SparseArray<Pair<Int, IPagerViewHolderCreator<*>>?>()
        fun register(cement: AbstractCellItem<*>) {
            val viewType = cement.viewType
            if (viewType == View.NO_ID) {
                throw RuntimeException("illegal viewType=$viewType")
            }
            if (creatorSparseArray[viewType] == null) {
                creatorSparseArray.put(
                        viewType,
                        android.util.Pair.create(cement.layoutRes, cement.pagerViewHolderCreator)
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
    var callbackHeight: ((dataIndex:Int) -> Int)? = null
    override fun measureHeightAt(index: Int, context: Context): Int
            =  displyItems.getOrNull(index)?.firstAssumeMeasureHeight(context) ?: 0

    //</editor-fold>

    override fun bindData2ViewHolder(index: Int, vh: AutoPagerView.ViewHolder, parent: ViewGroup) {
        val item = displyItems.getOrNull(index) as? AbstractCellItem<AutoPagerView.ViewHolder> ?: return
        item.onBindViewHolder(vh, parent)
    }

    fun indexOf(cellItem: AbstractCellItem<*>): Int {
        return displyItems.indexOf(cellItem)
    }

    fun dataAt(index: Int) = displyItems.getOrNull(index)

    override fun onAttachedToRecyclerView(autoPagerView: AutoPagerView) {
        super.onAttachedToRecyclerView(autoPagerView)
        isAttached = true
    }

    override fun onDetachedFromRecyclerView(autoPagerView: AutoPagerView) {
        super.onDetachedFromRecyclerView(autoPagerView)
        isAttached = false
    }

    //</editor-fold>

    //<editor-fold desc="Event Hook">
    fun addEventAnchor(
            eventHook: EventAnchor<AutoPagerView.ViewHolder>
    ) {
        if (isAttached) {
            throw IllegalAccessException("addEventAnchor should be called before view is attached, normally before setAdapter")
        }
        eventHookHelper.add(eventHook)
    }

    fun removeEventAnchor(
            eventHook: EventAnchor<AutoPagerView.ViewHolder>) {
        eventHookHelper.remove(eventHook)
    }
    //</editor-fold desc="Event Hook">

    //<editor-fold desc="OnClickListener">
    private var onItemClickListener: OnItemClickListener? = null
    private var clickEventAnchor: EventAnchor<AutoPagerView.ViewHolder>? = null
    private var onItemLongClickListener: OnItemLongClickListener? = null
    private var longClickEventAnchor: OnLongClickEventAnchor<AutoPagerView.ViewHolder>? = null

    private fun addOnItemClickEventHook() {
        clickEventAnchor?.let {
            removeEventAnchor(it)
        }
        val click = object :
                OnClickEventAnchor<AutoPagerView.ViewHolder>(AutoPagerView.ViewHolder::class.java) {

            override fun onBindMany(viewHolder: AutoPagerView.ViewHolder): List<View?>? {
                return listOf(viewHolder.itemView)
            }

            override fun onClick(position: Int, view: View, viewHolder: AutoPagerView.ViewHolder, rawData: AbstractCellItem<*>?) {
                onItemClickListener?.onClick(position, view, viewHolder, rawData)
            }

        }
        addEventAnchor(click)
        clickEventAnchor = click
    }

    /**
     * Register a callback to be invoked when [.models] are clicked.
     * If the view of this model is not clickable, it will not trigger callback.
     *     * @throws IllegalStateException this method must be called before
     * [AutoPageAdapter#setAdapter]
     */
    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        check(!(isAttached && clickEventAnchor == null && onItemClickListener != null)) {
            "setOnItemClickListener must be called before the setAdapter"
        }
        if (!isAttached && clickEventAnchor == null) {
            addOnItemClickEventHook()
        }
        this.onItemClickListener = onItemClickListener
    }

    private fun addOnItemLongClickEventHook() {
        longClickEventAnchor?.let {
            removeEventAnchor(it)
        }
        val longClick = object :
                OnLongClickEventAnchor<AutoPagerView.ViewHolder>(AutoPagerView.ViewHolder::class.java) {

            fun onBind(viewHolder: AutoPagerView.ViewHolder): View? {
                return if (viewHolder.itemView.isClickable) viewHolder.itemView else null
            }

            override fun onBindMany(viewHolder: AutoPagerView.ViewHolder): List<View?>? {
                return listOf(viewHolder.itemView)
            }


            override fun onLongClick(position: Int, view: View,
                                     viewHolder: AutoPagerView.ViewHolder,
                                     rawData: AbstractCellItem<*>?): Boolean {
                return onItemLongClickListener?.onLongClick(position, view, viewHolder, rawData) == true
            }

        }
        addEventAnchor(longClick)
        longClickEventAnchor = longClick
    }

    /**
     * If the view of this model is not long clickable, it will not trigger callback.
     *
     * @throws IllegalStateException
     * this method must be called before setAdapter
     */
    fun setOnItemLongClickListener(onItemLongClickListener: OnItemLongClickListener?) {
        check(!(isAttached && this.onItemLongClickListener == null && onItemLongClickListener != null)) {
            "setOnItemLongClickListener() must be called before setAdapter()"
        }
        if (isAttached && longClickEventAnchor == null) {
            addOnItemLongClickEventHook()
        }
        this.onItemLongClickListener = onItemLongClickListener
    }

    interface OnItemLongClickListener {
        fun onLongClick(position: Int, itemView: View,
                        viewHolder: AutoPagerView.ViewHolder,
                        model: AbstractCellItem<*>?): Boolean
    }

    interface OnItemClickListener {
        fun onClick(position: Int, itemView: View,
                    viewHolder: AutoPagerView.ViewHolder,
                    model: AbstractCellItem<*>?)
    }

    override fun itemViewType(index: Int): Int = displyItems.getOrNull(index)?.viewType ?: -1
    //</editor-fold>
}