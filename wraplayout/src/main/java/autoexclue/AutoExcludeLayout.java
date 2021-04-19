package autoexclue;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import am.widget.wraplayout.R;

/**
 * Created by lei.jialin on 2021/4/19
 */
public class AutoExcludeLayout extends ViewGroup {
    AutoDataAdapter adapter;

    public static final int GRAVITY_PARENT = -1;// 使用全局对齐方案
    public static final int GRAVITY_TOP = 0;// 子项顶部对齐
    public static final int GRAVITY_CENTER = 1;// 子项居中对齐
    public static final int GRAVITY_BOTTOM = 2; // 子项底部对齐
    private static final int[] ATTRS = new int[]{android.R.attr.horizontalSpacing,
            android.R.attr.verticalSpacing};
    private int mVerticalSpacing = 0;
    private int mHorizontalSpacing = 0;
    private int mNumRows = 0;
    private int mNumLayoutRows = 0;
    private ArrayList<Integer> mNumColumns = new ArrayList<>();
    private ArrayList<Integer> mChildMaxWidth = new ArrayList<>();
    private ArrayList<Integer> mChildTop = new ArrayList<>();
    private int mGravity = GRAVITY_TOP;

    public AutoExcludeLayout(Context context) {
        super(context);
        initView(context, null, 0);
    }

    public AutoExcludeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs, 0);
    }

    public AutoExcludeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public AutoExcludeLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context, attrs, defStyleAttr);
    }

    private void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        final TypedArray a = context.obtainStyledAttributes(attrs, ATTRS, defStyleAttr, 0);
        int n = a.getIndexCount();
        int horizontalSpacing = 0;
        int verticalSpacing = 0;
        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case 0:
                    horizontalSpacing = a.getDimensionPixelSize(attr, horizontalSpacing);
                    break;
                case 1:
                    verticalSpacing = a.getDimensionPixelSize(attr, verticalSpacing);
                    break;
            }
        }
        a.recycle();
        TypedArray custom = context.obtainStyledAttributes(attrs, R.styleable.AutoExcludeLayout);
        horizontalSpacing = custom.getDimensionPixelSize(
                R.styleable.AutoExcludeLayout_AEL_HorizontalSpacing, horizontalSpacing);
        verticalSpacing = custom.getDimensionPixelSize(
                R.styleable.AutoExcludeLayout_AEL_VerticalSpacing, verticalSpacing);
        int gravity = custom.getInt(R.styleable.AutoExcludeLayout_AEL_Gravity, GRAVITY_TOP);
        custom.recycle();
        mHorizontalSpacing = horizontalSpacing;
        mVerticalSpacing = verticalSpacing;
        mGravity = gravity;
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    /**
     * Returns a set of layout parameters with a width of
     * {@link ViewGroup.LayoutParams#WRAP_CONTENT},
     * a height of {@link ViewGroup.LayoutParams#WRAP_CONTENT} and no spanning.
     */
    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    // Override to allow type-checking of LayoutParams.
    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp instanceof LayoutParams) {
            return new LayoutParams((LayoutParams) lp);
        } else {
            return new LayoutParams(lp);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int paddingStart = getPaddingLeft();
        final int paddingEnd = getPaddingRight();
        final int paddingTop = getPaddingTop();
        final int paddingBottom = getPaddingBottom();
        final int suggestedMinimumWidth = getSuggestedMinimumWidth();
        final int suggestedMinimumHeight = getSuggestedMinimumHeight();
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightMode = MeasureSpec.getSize(heightMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int itemsWidth = 0;
        int itemsHeight = 0;
        int oldNumRows = mNumRows;
        mNumRows = 0;
        mNumColumns.clear();
        mChildMaxWidth.clear();
        if (getChildCount() > 0) {
            if (heightMode == MeasureSpec.UNSPECIFIED) {
                // TODO: empty implement
            } else {
                int numColumns = 0;
//                final int maxItemsWidth = widthSize - paddingStart - paddingEnd;
                final int maxItemsHeight = heightSize - paddingTop - paddingBottom;
                int rowWidth = 0;
                int rowHeight = 0;
                for (int index = 0; index < getChildCount(); index++) {
                    View child = getChildAt(index);
                    LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    if (child.getVisibility() == View.GONE) {
                        lp.isWithinValidLayout = false;
                        lp.isInvisibleOutValidLayout = false;
                        continue;
                    }
//                    if (mNumRows == 0) mNumRows = 1;
                    measureChild(child, widthMeasureSpec, heightMeasureSpec);
                    final int childWidth = child.getMeasuredWidth();
                    final int childHeight = child.getMeasuredHeight();
                    if(rowHeight + childHeight + mVerticalSpacing <= maxItemsHeight) {
                        rowHeight += childHeight + mVerticalSpacing;
                        mChildTop.add(rowHeight);
                        mNumRows ++;
                        mChildMaxWidth.add(childHeight + mVerticalSpacing);
                        lp.isWithinValidLayout = true;
                        lp.isInvisibleOutValidLayout = false;
                        // TODO: refect
                        itemsHeight  += mNumRows == 1 ? rowHeight : mVerticalSpacing + rowHeight;
                    } else {
                        mChildMaxWidth.add(childHeight + mVerticalSpacing);
                        lp.isWithinValidLayout = false;
                        lp.isInvisibleOutValidLayout = false;
                    }
//                    if (numColumns == 0) {
//                        rowWidth = -mHorizontalSpacing;
//                    }
//                    if (rowWidth + childWidth + mHorizontalSpacing <= maxItemsWidth) {
//                        rowWidth += childWidth + mHorizontalSpacing;
//                        rowHeight = Math.max(childHeight, rowHeight);
//                        numColumns++;
//                    } else {
//                        itemsWidth = Math.max(rowWidth, itemsWidth);
//                        itemsHeight += mNumRows == 1 ? rowHeight : mVerticalSpacing + rowHeight;
//                        mNumColumns.add(numColumns);
//                        mChildMaxWidth.add(rowHeight);
//                        mNumRows++;
//                        rowWidth = 0;
//                        rowHeight = 0;
//                        numColumns = 0;
//                        rowWidth += childWidth;
//                        rowHeight = Math.max(childHeight, rowHeight);
//                        numColumns++;
//                    }
                }
//                if (numColumns != 0) {
//                    itemsHeight += mNumRows == 1 ? rowHeight : mVerticalSpacing + rowHeight;
//                    mNumColumns.add(numColumns);
//                    mChildMaxWidth.add(rowHeight);
//                }
            }
        }
        itemsWidth = Math.max(paddingStart + itemsWidth + paddingEnd, suggestedMinimumWidth);

        itemsHeight = Math.max(paddingTop + itemsHeight + paddingBottom, suggestedMinimumHeight);
        setMeasuredDimension(resolveSize(itemsWidth, widthMeasureSpec),
                resolveSize(itemsHeight, heightMeasureSpec));
        if(mNumRows!=oldNumRows) forceLayout();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int paddingStart = getPaddingLeft();
        final int paddingTop = getPaddingTop();
        final int gravity = mGravity;
        int numChild = 0;
        int columnTop = paddingTop - mVerticalSpacing;
        int size = getChildCount();
        for (int row = 0; row < size; row++) {
            int numColumn = 1; //mNumColumns.get(row);
            int childMaxHeight = mChildMaxWidth.get(row);
            int startX = 0;//paddingStart - mHorizontalSpacing;
            for (int index = 0; index < numColumn; ) {
                View childView = getChildAt(numChild);
                LayoutParams lp = (LayoutParams) childView.getLayoutParams();

                numChild++;
                if (childView == null || childView.getVisibility() == View.GONE) {
                    continue;
                }
                final int childWidth = childView.getMeasuredWidth();
                final int childHeight = childView.getMeasuredHeight();
                final LayoutParams layoutParams = (LayoutParams) childView.getLayoutParams();
                final int childGravity = layoutParams.getGravity();

//                startX += mHorizontalSpacing;
                int topOffset = 0;
                switch (childGravity) {
                    default:
                    case GRAVITY_PARENT: {
                        switch (gravity) {
                            case GRAVITY_CENTER:
                                topOffset = Math.round((childMaxHeight - childHeight) * 0.5f);
                                break;
                            case GRAVITY_BOTTOM:
                                topOffset = childMaxHeight - childHeight;
                                break;
                            default:
                            case GRAVITY_TOP:
                                topOffset = 0;
                                break;
                        }
                    }
                    break;
                    case GRAVITY_CENTER:
                        topOffset = Math.round((childMaxHeight - childHeight) * 0.5f);
                        break;
                    case GRAVITY_BOTTOM:
                        topOffset = childMaxHeight - childHeight;
                        break;
                    case GRAVITY_TOP:
                        topOffset = 0;
                        break;
                }
                int startY = columnTop + mVerticalSpacing + topOffset;
//                childView.layout(startX, startY, startX + childWidth, startY + childHeight);

//                startX += childWidth;
                index++;
//                if(!lp.isWithinValidLayout) continue;
                if(startY + childHeight > getMeasuredHeight()) {
                    mNumLayoutRows = index;
                    lp.isInvisibleOutValidLayout = true;
                    childView.setVisibility(View.INVISIBLE);
                } else {
                    childView.layout(startX, startY, startX + childWidth, startY + childHeight);
                    if (childView.getVisibility() == View.INVISIBLE) {
                        lp.isInvisibleOutValidLayout = false;
                        childView.setVisibility(View.VISIBLE);
                    }
                }
            }
            columnTop += mVerticalSpacing + childMaxHeight;
        }
    }

    /**
     * 获取水平间距
     *
     * @return 水平间距
     */
    public int getHorizontalSpacing() {
        return mHorizontalSpacing;
    }

    /**
     * 设置水平间距
     *
     * @param pixelSize 水平间距
     */
    public void setHorizontalSpacing(int pixelSize) {
        mHorizontalSpacing = pixelSize;
        requestLayout();
    }

    /**
     * 获取垂直间距
     *
     * @return 垂直间距
     */
    public int getVerticalSpacing() {
        return mVerticalSpacing;
    }

    /**
     * 设置垂直间距
     *
     * @param pixelSize 垂直间距
     */
    public void setVerticalSpacing(int pixelSize) {
        mVerticalSpacing = pixelSize;
        requestLayout();
    }

    /**
     * 获取行数目
     *
     * @return 行数目
     */
    public int getNumRows() {
        return mNumRows;
    }

    /**
     * 获取某一行列数目
     *
     * @param index 行号
     * @return 列数目
     */
    public int getNumColumns(int index) {
        int numColumns = -1;
        if (index < 0 || index >= mNumColumns.size()) {
            return numColumns;
        }
        return mNumColumns.get(index);
    }

    /**
     * 获取子项对齐模式
     *
     * @return 对齐模式
     */
    public int getGravity() {
        return mGravity;
    }

    /**
     * 设置子项对齐模式
     *
     * @param gravity 对齐模式
     */
    public void setGravity(int gravity) {
        if (gravity != GRAVITY_TOP && gravity != GRAVITY_CENTER && gravity != GRAVITY_BOTTOM)
            return;
        mGravity = gravity;
        requestLayout();
    }

    /**
     * Per-child layout information associated with AutoExcludeLayout.
     */
    @SuppressWarnings("WeakerAccess")
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {

        private int mGravity = AutoExcludeLayout.GRAVITY_PARENT;
        private boolean isWithinValidLayout;
        private boolean isInvisibleOutValidLayout;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            int gravity = AutoExcludeLayout.GRAVITY_PARENT;
            TypedArray custom = c.obtainStyledAttributes(attrs, R.styleable.AutoExcludeLayout_Layout);
            gravity = custom.getInt(R.styleable.AutoExcludeLayout_Layout_AEL_Layout_gravity, gravity);
            custom.recycle();
            mGravity = gravity;
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(LayoutParams source) {
            super(source);
            mGravity = source.mGravity;
        }

        /**
         * 获取布局
         *
         * @return 布局
         */
        public int getGravity() {
            return mGravity;
        }

        /**
         * 设置布局
         *
         * @param gravity 布局
         */
        public void setGravity(int gravity) {
            mGravity = gravity;
        }
    }
}
