package am.project.x.business.widgets.wraplayout

import am.project.x.R
import am.project.x.widget.FixedGridLayout
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Created by lei.jialin on 11/26/21
 */
class GridActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grid)
        initView()
        initData()
    }

    private fun initData() {
        val res = (0..100).map {
            CopyingPaintingViewHolder(onClick = {
                Toast.makeText(this,"click",Toast.LENGTH_LONG)
                        .apply { this.setGravity(Gravity.CENTER,0,0) }.show()
            },onLongClick = {
            }
            )
        }
        fixGridLayout.setData(res,0)
    }


    private val fixGridLayout by lazy {
        findViewById<FixedGridLayout>(R.id.fix_grid_layout)
    }

    private fun initView() {
        fixGridLayout.initLayoutConfig(object : FixedGridLayout.IFixLayoutConfig(3,3) {
            override fun getLayoutId(): Int = R.layout.item_layout_painting
            override fun generateLayoutParam(context: Context): ViewGroup.MarginLayoutParams {
                return FrameLayout.LayoutParams(293,473)
                        .apply {
                            setMargins(52,0,52,0)
                        }
            }

        })
    }


    fun Float.dp2px(context: Context): Int {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, this,
                context.resources.displayMetrics
        ).toInt()
    }

}