package autoexclue;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import am.widget.wraplayout.R;
import androidx.annotation.NonNull;

/**
 * Created by lei.jialin on 2021/4/19
 */
public class AutoExcludeLayout extends ViewGroup {
    private static final int MAX_PER_MEASURE_CNT = 10;
    Adapter adapter;

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
    private int mGravity = GRAVITY_TOP;
    private boolean mIsAttachToWindow;
    private boolean mAdapterUpdateDuringMeasure;
    private MtObserver mObserver = new MtObserver();
    private boolean isLayouting;

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
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mIsAttachToWindow = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mIsAttachToWindow = false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int itemsWidth = 0;
        int itemsHeight = 0;

        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();
        final int paddingTop = getPaddingTop();
        final int paddingBottom = getPaddingBottom();
        final int suggestedMinimumWidth = getSuggestedMinimumWidth();
        final int suggestedMinimumHeight = getSuggestedMinimumHeight();

        int oldNumRows = mNumRows;

        checkFirstMeasure(widthMeasureSpec, heightMeasureSpec);

        Segment s = getCurrentSegment();
        if (s != null) {
            mNumRows = s.rows;
            itemsHeight = s.height;
            itemsWidth = s.width;
        }

        itemsWidth = Math.max(paddingLeft + itemsWidth + paddingRight, suggestedMinimumWidth);
        itemsHeight = Math.max(paddingTop + itemsHeight + paddingBottom, suggestedMinimumHeight);
        setMeasuredDimension(resolveSize(itemsWidth, widthMeasureSpec),
                resolveSize(itemsHeight, heightMeasureSpec));
        if (mNumRows != oldNumRows) {
            segments.clear();
            forceLayout();
        }
    }

    private void checkFirstMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Segment s = getCurrentSegment();
        if (s == null) {// || segments.isEmpty() && adapter.getTotalDataSize() != 0) {
            preMeasureAllItem(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private Segment getCurrentSegment() {
        Segment s = curSegIndex < segments.size() && curSegIndex >= 0 ? segments.get(curSegIndex) : null;
        curSegment = s;
        return s;
    }

    private void preMeasureAllItem(int widthMeasureSpec, int heightMeasureSpec) {
        int dataIndex = 0;
        int totalDataSize = adapter.getTotalDataSize();
        findPageMap.clear();
        segments.clear();
        curSegIndex = 0;
        while (dataIndex < totalDataSize) {
            reuseSegment = new Segment(0, 0, 0);
            reuseSegment.start = dataIndex;
            reuseSegment.end = dataIndex + MAX_PER_MEASURE_CNT;
            reuseSegment.size = MAX_PER_MEASURE_CNT;
            findPageMap.put(dataIndex, reuseSegment.start);
            calcOnePage(widthMeasureSpec, heightMeasureSpec, reuseSegment);
            dataIndex += reuseSegment.size;
            segments.add(reuseSegment);
        }
    }

    private SparseArray<Integer> findPageMap = new SparseArray<>();
    private List<Segment> segments = new ArrayList<>();
    private int curSegIndex = 0;
    private Segment curSegment = null;
    Segment reuseSegment = new Segment(0, 0, 0);

    private void calcOnePage(int widthMeasureSpec, int heightMeasureSpec, Segment segment) {
        final int paddingTop = getPaddingTop();
        final int paddingBottom = getPaddingBottom();
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightMode = MeasureSpec.getSize(heightMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int collectHeight = 0;
        int collectWidth = 0;
        mNumRows = 0;
        mNumColumns.clear();
        ArrayList<Integer> mChildMaxWidth = new ArrayList<>();
        mChildMaxWidth.clear();

        int childCount = segment.size;
        if (childCount > 0) {
            int numColumns = 0;
//                final int maxItemsWidth = widthSize - paddingStart - paddingEnd;
            final int maxItemsHeight = heightSize - paddingTop - paddingBottom;
            int rowWidth = 0;
            int rowHeight = 0;
            int end = Math.min(segment.end, adapter.getTotalDataSize());
            for (int index = segment.start; index < end; index++) {
                View child = getViewOf(index);
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
                if (rowHeight + childHeight + mVerticalSpacing <= maxItemsHeight) {
                    rowHeight += childHeight + mVerticalSpacing;
                    mNumRows++;
                    mChildMaxWidth.add(childHeight + mVerticalSpacing);
                    lp.isWithinValidLayout = true;
                    lp.isInvisibleOutValidLayout = false;
                    // TODO: refect
                    collectHeight += mNumRows == 1 ? rowHeight : mVerticalSpacing + rowHeight;
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
        segment.height = collectHeight;
        segment.width = collectWidth;
        segment.childMaxWidth = mChildMaxWidth;
        segment.size = mNumRows;
        segment.rows = mNumRows;
        segment.end = segment.start + segment.size;

    }

    private View getViewOf(int index) {
        ViewHolder vh = vhCache.get(index);
        if (index >= vhCache.size() || index < 0 || vhCache.get(index) == null) {
            vh = obtainViewHolder(index);
            vhCache.put(index, vh);
        }
        return vh.itemView;
    }

    private SparseArray<ViewHolder> vhCache = new SparseArray<>();

    private ViewHolder obtainViewHolder(int index) {
        ViewHolder holder = adapter.onCreateViewHolderAt(index, this);
        final ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
        final LayoutParams aeLayoutParams;
        if (lp == null) {
            aeLayoutParams = (LayoutParams) generateDefaultLayoutParams();
            holder.itemView.setLayoutParams(aeLayoutParams);
        } else if (!checkLayoutParams(lp)) {
            aeLayoutParams = (LayoutParams) generateLayoutParams(lp);
            holder.itemView.setLayoutParams(aeLayoutParams);
        } else {
            aeLayoutParams = (LayoutParams) lp;
        }
        aeLayoutParams.mViewHolder = holder;
        return holder;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Segment s = curSegment == null && !segments.isEmpty() ? segments.get(curSegIndex) : curSegment;
        if (s == null) {
            return;
        }
        curSegment = s;
        mNumRows = s.rows;
        layoutChunk(s);
    }

    protected void layoutChunk(Segment segment) {
        if (isLayouting) return;
        isLayouting = true;
        removeAllViews();
        final int paddingStart = getPaddingLeft();
        final int paddingTop = getPaddingTop();
        final int gravity = mGravity;
        int numChild = 0;
        int columnTop = paddingTop - mVerticalSpacing;
        for (int row = segment.start; row < segment.end; ) { // row++
            int numColumn = 1; //mNumColumns.get(row);
            int childMaxHeight = segment.childMaxWidth.get(row - segment.start);
            int startX = paddingStart - mHorizontalSpacing;
            for (int index = 0; index < numColumn; ) {
                View childView = getViewOf(row);
                LayoutParams lp = (LayoutParams) childView.getLayoutParams();

                numChild++;
                if (childView == null || childView.getVisibility() == View.GONE) {
                    continue;
                }
                final int childWidth = childView.getMeasuredWidth();
                final int childHeight = childView.getMeasuredHeight();
                final int childGravity = lp.getGravity();

                startX += mHorizontalSpacing;
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
//                if(!lp.isWithinValidLayout) continue;
                if (startY + childHeight > getMeasuredHeight()) {
                    segment.layoutRows = row + 1;
                    lp.isInvisibleOutValidLayout = true;
                    childView.setVisibility(View.INVISIBLE);
                    removeView(childView);
                } else {
                    addView(childView);
                    childView.layout(startX, startY, startX + childWidth, startY + childHeight);
                    if (childView.getVisibility() == View.INVISIBLE) {
                        lp.isInvisibleOutValidLayout = false;
                        childView.setVisibility(View.VISIBLE);
                    }
                }
                row++;
                index++;
            }
            columnTop += mVerticalSpacing + childMaxHeight;
        }
        isLayouting = false;
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

    public void setAdapter(Adapter adapter) {
        if (this.adapter != null) {
            this.adapter.unregisterAdapterDataObserver(mObserver);
        }
        this.adapter = adapter;
        if (adapter != null) {
            adapter.registerAdapterDataObserver(mObserver);
        }
    }

    /**
     * Per-child layout information associated with AutoExcludeLayout.
     */
    @SuppressWarnings("WeakerAccess")
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        public ViewHolder mViewHolder;
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

    private class MtObserver extends AdapterDataObserver {
        MtObserver() {
        }

        @Override
        public void onChanged() {
            requestLayout();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            triggerUpdateProcessor();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            triggerUpdateProcessor();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            triggerUpdateProcessor();
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            triggerUpdateProcessor();
        }

        void triggerUpdateProcessor() {
            mAdapterUpdateDuringMeasure = true;
            requestLayout();
        }
    }

    public static class Adapter<T> {
        private AdapterDataObservable mObservable = new AdapterDataObservable();
        private SparseArray<ViewHolder> viewHolderSparseArray = new SparseArray<>();
        public List<CementItem<?>> cementItems = new ArrayList<>();

        public ViewHolder onCreateViewHolderAt(int index, ViewGroup parent) {
            CementItem<?> item = cementItems.get(index);
            ViewHolder vh = findViewHolder(item, parent);
            return vh;
        }

        //        <editor-fold desc="AdapterDataObserver">
        public void registerAdapterDataObserver(@NonNull AdapterDataObserver observer) {
            mObservable.registerObserver(observer);
        }

        public void unregisterAdapterDataObserver(@NonNull AdapterDataObserver observer) {
            mObservable.unregisterObserver(observer);
        }

        public final boolean hasObservers() {
            return mObservable.hasObservers();
        }

        public final void notifyDataSetChanged() {
            mObservable.notifyChanged();
        }

        public final void notifyItemChanged(int position) {
            mObservable.notifyItemRangeChanged(position, 1);
        }

        public final void notifyItemRangeChanged(int positionStart, int itemCount) {
            mObservable.notifyItemRangeChanged(positionStart, itemCount);
        }

        public final void notifyItemInserted(int position) {
            mObservable.notifyItemRangeInserted(position, 1);
        }

        public final void notifyItemMoved(int fromPosition, int toPosition) {
            mObservable.notifyItemMoved(fromPosition, toPosition);
        }

        public final void notifyItemRangeInserted(int positionStart, int itemCount) {
            mObservable.notifyItemRangeInserted(positionStart, itemCount);
        }

        public final void notifyItemRemoved(int position) {
            mObservable.notifyItemRangeRemoved(position, 1);
        }

        public final void notifyItemRangeRemoved(int positionStart, int itemCount) {
            mObservable.notifyItemRangeRemoved(positionStart, itemCount);
        }
//        </editor-fold>

        private ViewHolder findViewHolder(CementItem<?> item, ViewGroup parent) {
            int layoutId = item.getLayoutId();
            View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
            ViewHolder vh = new ViewHolder(view);
            item.onBindViewHolder(vh, parent);
            viewHolderSparseArray.put(layoutId, vh);
            return vh;
        }

        public void clearData() {
            int intialSize = cementItems.size();
            cementItems.clear();
            notifyItemRangeRemoved(intialSize, cementItems.size());
        }

        public void addDataList(List<CementItem<?>> cements) {
            int intialSize = cementItems.size();
            this.cementItems.addAll(cements);
            notifyItemRangeInserted(intialSize, cementItems.size());
        }

        public int getTotalDataSize() {
            return cementItems.size();
        }
    }

    public static class ViewHolder {
        private final View itemView;

        ViewHolder(View itemView) {
            this.itemView = itemView;
        }
    }
}
