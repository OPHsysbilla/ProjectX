package am.widget.wraplayout

import am.widget.wraplayout.R
import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView

/**
 * Created by lei.jialin on 2021/3/25
 */
open class PageIndicatorView : FrameLayout {
    private var decorateText: String = ""
    protected var btnPrev: View? = null
    protected var btnNext: View? = null
    protected var tvPageIndex: TextView? = null
    var callback: ((nextPage: Boolean) -> Unit)? = {}

    constructor(context: Context?) : super(context!!) {
        init()
    }


    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    @TargetApi(Build.VERSION_CODES.M)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init()
    }

    private fun init() {
        initView()
        initEvent()
    }


    fun setPrevEnable(enable: Boolean) {
        btnPrev?.isEnabled = enable
    }

    fun setNextEnable(enable: Boolean) {
        btnNext?.isEnabled = enable

    }

    fun setPageDecorateText(text: String) {
        this.decorateText = text
        tvPageIndex?.text = text
    }

    private fun initView() {
        LayoutInflater.from(context)
            .inflate(R.layout.layout_page_indicator, this, true)
        if (isInEditMode) return
        btnPrev = findViewById(R.id.btn_prev_page)
        btnNext = findViewById(R.id.btn_next_page)
        tvPageIndex = findViewById(R.id.tv_page_index)
        tvPageIndex?.text = decorateText
    }

    protected open fun initEvent() {
        btnPrev?.setOnClickListener { v: View? -> callback?.invoke(false) }
        btnNext?.setOnClickListener { v: View? -> callback?.invoke(true) }
    }
}