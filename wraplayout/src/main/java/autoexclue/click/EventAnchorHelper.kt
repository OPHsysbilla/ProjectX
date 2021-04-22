package autoexclue.click

import android.view.View
import autoexclue.AutoPagerView
import autoexclue.adapter.AutoPageAdapter
import java.util.ArrayList

/**
 * Created by lei.jialin on 2021/4/22
 */
class EventAnchorHelper<VH : AutoPagerView.ViewHolder> {
    private val isAfterBind = false
    private val eventList: MutableList<EventAnchor<VH>> = ArrayList()
    fun add(eventAnchor: EventAnchor<VH>) {
        check(!isAfterBind) { " ViewHolder is already created,   should add EventAnchor before view been attached. " }
        eventList.add(eventAnchor)
    }


    fun bind(viewHolder: AutoPagerView.ViewHolder, adapter: AutoPageAdapter) {
        for (eventAnchor in eventList) {
            if (!eventAnchor.clazz.isInstance(viewHolder)) continue
            val vh = eventAnchor.clazz.cast(viewHolder) ?: continue
            eventAnchor.onBindMany(vh)?.map { view ->
                attachToView(eventAnchor, adapter, vh, view)
            }
        }
    }

    private fun attachToView(eventHook: EventAnchor<VH>, adapter: AutoPageAdapter, viewHolder: VH, view: View?) {
        // TODO： mPosition会有不准的可能？
        //  因为在极短时间内频繁挪动/插入删除的时候，layout可能并没有结束，获得的是上一次layout的position
        view ?: return
        eventHook.onEvent(view, viewHolder, adapter)
    }

    fun remove(eventHook: EventAnchor<VH>) {
        check(!isAfterBind) { " EventAnchor is already setup, should remove EventAnchor before view been attached. " }
        eventList.remove(eventHook)
    }


}