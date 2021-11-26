package am.project.x.business.widgets.wraplayout

import am.project.x.R
import am.project.x.databinding.LayoutSwitchChoiceBlackAndWhiteBinding
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.ColorInt

class SwitchChoiceView @JvmOverloads constructor(
        context: Context,attrs: AttributeSet? = null,defStyleAttr: Int = 0
) : FrameLayout(context,attrs,defStyleAttr) {

    companion object {
        private const val DEFAULT_CHOICE_COUNT = 2
    }

    private var textSizePixel: Float = 40f
    private var roundRadius: Int = 10

    @ColorInt
    private var highlightColor: Int = Color.BLACK

    @ColorInt
    private var normalColor: Int = Color.WHITE

    private var choiceList = ArrayList<ChoiceItem>()


    private val viewBinding: LayoutSwitchChoiceBlackAndWhiteBinding by lazy {
        LayoutSwitchChoiceBlackAndWhiteBinding.inflate(LayoutInflater.from(context),this,true)
    }

    private val tvChoiceView by lazy {
        listOf(viewBinding.tvChoiceSmallNumber,viewBinding.tvChoiceBigNumber)
    }
    private val bgChoiceView by lazy {
        listOf(viewBinding.bgChoiceSmallNumber,viewBinding.bgChoiceBigNumber)
    }

    private val choiceViewList by lazy {
        listOf(viewBinding.choiceFirst,viewBinding.choiceSecond)
    }

    init {
        initAttr(context,attrs)
    }

    private fun initAttr(context: Context,attrs: AttributeSet?) {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(
                    attrs,
                    R.styleable.SwitchChoiceView
            )
            try {
                highlightColor =
                        a.getColor(
                                R.styleable.SwitchChoiceView_SCV_highlightColor,
                                highlightColor
                        )
                normalColor =
                        a.getColor(
                                R.styleable.SwitchChoiceView_SCV_normalColor,
                                normalColor
                        )
                roundRadius =
                        a.getDimensionPixelSize(
                                R.styleable.SwitchChoiceView_SCV_roundRadius,
                                roundRadius
                        )
                textSizePixel = a.getDimensionPixelSize(
                        R.styleable.SwitchChoiceView_SCV_titleSize,20
                ).toFloat()
                val first = a.getString(R.styleable.SwitchChoiceView_SCV_firstChoiceTitle) ?: ""
                val second = a.getString(R.styleable.SwitchChoiceView_SCV_secondChoiceTitle) ?: ""
                setChoiceItemList(ChoiceItem(first),ChoiceItem(second))

            } finally {
                a.recycle()
            }
        }
    }

    private fun setChoiceItemList(
            firstChoice: ChoiceItem,
            secondChoice: ChoiceItem,
    ) {
        choiceList.clear()
        choiceList.add(firstChoice)
        choiceList.add(secondChoice)
        choiceViewList.forEach { choice ->
            choice.setOnClickListener {
                (choice.parent as? ViewGroup)?.indexOfChild(choice)?.let { index ->
                    selectAt(index)
                }
            }
        }
        tvChoiceView.forEachIndexed { index,tv ->
            tv.setTextColor(getTextHighlightColorList())
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX,textSizePixel)
            tv.text = choiceList.getOrNull(index)?.title ?: ""
        }
        refreshView()
    }

    fun ViewGroup.childrenList(): List<View> = (0 until this.childCount).map { i ->
        getChildAt(i)
    }

    private fun refreshView() {
        val last = Math.max(0,bgChoiceView.size - 1)
        bgChoiceView.forEachIndexed { index,view ->
            val place = when (index) {
                0 -> Place.First
                else -> Place.Last
            }
            view.background = getHighlightDrawableOf(place)
        }
        selectAt(0)
    }

    private fun getTextHighlightColorList(): ColorStateList {
        val normalTextColor = highlightColor
        val pressedTextColor = normalColor
        val selectedState = intArrayOf(android.R.attr.state_selected)
        val checkedState = intArrayOf(android.R.attr.state_checked)
        val pressedState = intArrayOf(android.R.attr.state_pressed)
        val normalState =
                intArrayOf(android.R.attr.state_enabled,-android.R.attr.state_pressed)
        val colors =
                intArrayOf(pressedTextColor,pressedTextColor,pressedTextColor,normalTextColor)
        val states = arrayOf(selectedState,checkedState,pressedState,normalState)
        return ColorStateList(states,colors)
    }

    enum class Place(index: Int) {
        First(0),
        Middle(1),
        Last(2),
    }

    private fun getHighlightDrawableOf(place: Place): StateListDrawable {
        val selectedState = intArrayOf(android.R.attr.state_selected)
        val checkedState = intArrayOf(android.R.attr.state_checked)
        val pressedState = intArrayOf(android.R.attr.state_pressed)
//        val disableState = intArrayOf(-android.R.attr.state_enabled)
        val normalState = intArrayOf(android.R.attr.state_enabled,-android.R.attr.state_pressed)
        val r = roundRadius.toFloat()
        val radiusArray = when (place) {
            Place.First -> floatArrayOf(r,r,0f,0f,0f,0f,r,r)
            Place.Middle -> floatArrayOf(0f,0f,0f,0f,0f,0f,0f,0f)
            Place.Last -> floatArrayOf(0f,0f,r,r,r,r,0f,0f)
        }
        //设置背景颜色
        val drawableList = StateListDrawable()
        val highlight = GradientDrawable().apply {
            cornerRadii = radiusArray
            setColor(highlightColor)
        }
        val normal = GradientDrawable().apply {
            cornerRadii = radiusArray
            setColor(normalColor)
            setStroke(2,highlightColor)
        }
        drawableList.addState(pressedState,highlight)
        drawableList.addState(checkedState,highlight)
        drawableList.addState(selectedState,highlight)
        drawableList.addState(normalState,normal)
        return drawableList
    }

    fun selectAt(index: Int) {
        val selectIndex = Math.max(0,Math.min(index,choiceList.size - 1))
        tvChoiceView.forEachIndexed { i,tv ->
            val select = selectIndex == i
            tv.isSelected = select
            bgChoiceView[i].isSelected = select
            bgChoiceView[i].tag = select
        }
    }


    data class ChoiceItem(val title: String,val onCLick: () -> Unit = {})
}