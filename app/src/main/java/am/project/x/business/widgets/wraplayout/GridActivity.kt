package am.project.x.business.widgets.wraplayout

import am.project.x.R
import am.project.x.widget.FixedGridLayout
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
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
                Toast.makeText(this,"long click",Toast.LENGTH_LONG)
                        .apply { this.setGravity(Gravity.CENTER,0,0) }.show()
            }
            )
        }
        fixGridLayout.setData(column = 3,row = 3, viewHolders = res,defaultPage = 0)
    }


    private val fixGridLayout by lazy {
        findViewById<FixedGridLayout>(R.id.fix_grid_layout)
    }

    private fun initView() {

    }


    fun Float.dp2px(context: Context): Int {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,this,
                context.resources.displayMetrics
        ).toInt()
    }

}