package am.widget.wraplayout

import android.content.ClipData
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.RequiresApi

/**
 * Created by lei.jialin on 2021/7/5
 */
class TextChoiceSwitchButton : FrameLayout {
    var dataList= mutableListOf<TextChoiceSwitchButton.Item> ()
    private var mRoot: View? = null

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context) {
        mRoot = LayoutInflater.from(context).inflate(R.layout.layout_number_choice_group, this, true)
        refreshUI()
    }

    private fun setItemList(list: List<TextChoiceSwitchButton.Item>) {
        dataList.clear()
        dataList.addAll(list)
        refreshUI()
    }

    private fun refreshUI() {
        mRoot ?: return

    }

    data class Item(
            var title: String,
            var isSelected: Boolean = false,
            val onClick: (() -> Unit)? = null,
    )
}
