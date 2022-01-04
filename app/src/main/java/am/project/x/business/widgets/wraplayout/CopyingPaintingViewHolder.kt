package am.project.x.business.widgets.wraplayout

import am.project.x.R
import am.project.x.databinding.ItemLayoutPaintingBinding
import am.project.x.widget.FixedGridLayout
import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import java.util.*
import kotlin.random.Random

/**
 * Created by lei.jialin on 11/26/21
 */
class CopyingPaintingViewHolder(onClick: (Boolean) -> Unit,onLongClick: (Boolean) -> Unit = {}) : FixedGridLayout.GridViewHolder(onClick,onLongClick) {

    @SuppressLint("SetTextI18n")
    override fun onBindingData(view: View,selectMode: Boolean) {
        val binding = ItemLayoutPaintingBinding.bind(view)
        binding.name.text = "ns: ${Random(100).nextInt()}"
    }

    override fun generateLayoutParam(context: Context): ViewGroup.MarginLayoutParams {
        return ViewGroup.MarginLayoutParams(293,473)
                .apply {
                    setMargins(52,0,52,0)
                }
    }

    override fun inflateRootView(context: Context): View {
        return ItemLayoutPaintingBinding.inflate(LayoutInflater.from(context)).root
    }
}