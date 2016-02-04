package me.yugy.metrolauncher;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

public class MetroView extends ViewGroup {

    private static final String LOG_TAG = "MetroView";

    public static final int SIZE_SMALL = 0;
    public static final int SIZE_MIDDLE = 1;
    public static final int SIZE_BIG = 2;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SIZE_SMALL, SIZE_MIDDLE, SIZE_BIG})
    public @interface Size {}

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({LAYOUT_STATE_INITIAL, LAYOUT_STATE_IDLE})
    private @interface LayoutState {}

    private static final int LAYOUT_STATE_INITIAL = 0;
    private static final int LAYOUT_STATE_IDLE = 1;

    private static final int INVALID_POINTER = -1;

    @Nullable private MetroAdapter mAdapter;
    private LayoutInflater mInflater;
    private int mDividerSize;
    private int mUnitSize;
    private int mColumnNum = 6; //// TODO: 1/31/16
    @LayoutState
    private int mLayoutState;

    private int mFirstRowIndex, mFirstRowTop;

    private List<Integer[]> mGrid;

    private int mActivePointerId = INVALID_POINTER;
    private int mMotionX, mMotionY;

    private boolean mDisallowIntercept = false;
    private int mTouchSlop;

    boolean mScrollingCacheEnabled;
    boolean mCachingStarted;
    private Runnable mClearScrollingCache;

    public MetroView(Context context) {
        this(context, null);
    }

    public MetroView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MetroView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mInflater = LayoutInflater.from(context);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MetroView, defStyleAttr, 0);
        mDividerSize = a.getDimensionPixelSize(R.styleable.MetroView_mv_dividerSize, -1);
        a.recycle();

        if (mDividerSize == -1) {
            mDividerSize = context.getResources().getDimensionPixelSize(R.dimen.default_divider_size);
        }

        setAlwaysDrawnWithCacheEnabled(false);
        setScrollingCacheEnabled(true);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (MotionEventCompat.getActionMasked(event)) {
            case MotionEvent.ACTION_DOWN: {
                final int x = (int) event.getX();
                final int y = (int) event.getY();
                mActivePointerId = MotionEventCompat.getPointerId(event, 0);
                mMotionX = x;
                mMotionY = y;
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (mActivePointerId != INVALID_POINTER) {
                    int pointerIndex = MotionEventCompat.findPointerIndex(event, mActivePointerId);
                    if (pointerIndex == -1) {
                        pointerIndex = 0;
                        mActivePointerId = MotionEventCompat.getPointerId(event, pointerIndex);
                    }
                    final int y = (int) MotionEventCompat.getY(event, pointerIndex);
                    if (startScrollIfNeeded(y, event)) {
                        return true;
                    }
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                mActivePointerId = INVALID_POINTER;
                break;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                onSecondaryPointerUp(event);
                break;
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            // A disabled view that is clickable still consumes the touch
            // events, it just doesn't respond to them.
            return isClickable() || isLongClickable();
        }
        switch (MotionEventCompat.getActionMasked(event)) {
            case MotionEvent.ACTION_DOWN: {
                mActivePointerId = MotionEventCompat.getPointerId(event, 0);
                final int x = (int) event.getX();
                final int y = (int) event.getY();
                mMotionX = x;
                mMotionY = y;
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                int pointerIndex = MotionEventCompat.findPointerIndex(event, mActivePointerId);
                if (pointerIndex == -1) {
                    pointerIndex = 0;
                    mActivePointerId = MotionEventCompat.getPointerId(event, pointerIndex);
                }

                final int x = (int) MotionEventCompat.getX(event, pointerIndex);
                final int y = (int) MotionEventCompat.getY(event, pointerIndex);
                scrollIfNeeded(y, event);
                break;
            }
            case MotionEvent.ACTION_UP: {
                mActivePointerId = INVALID_POINTER;
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = INVALID_POINTER;
                break;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                onSecondaryPointerUp(event);
                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(event);
                final int id = MotionEventCompat.getPointerId(event, index);
                final int x = (int) MotionEventCompat.getX(event, index);
                final int y = (int) MotionEventCompat.getY(event, index);
                mActivePointerId = id;
                mMotionX = x;
                mMotionY = y;
                break;
            }
        }
        return true;
    }

    private boolean startScrollIfNeeded(int y, MotionEvent event) {
        final int deltaY = y - mMotionY;
        final int distance = Math.abs(deltaY);
        if (distance > mTouchSlop) {
            createScrollingCache();
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            scrollIfNeeded(y, event);
            return true;
        }
        return false;
    }

    private void scrollIfNeeded(int y, MotionEvent event) {
        int deltaY = y - mMotionY;
        if (!mDisallowIntercept) {
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
        }
        boolean atEdge = false;
        if (deltaY != 0) {
            atEdge = trackMotionScroll(deltaY);
        }

        if (!atEdge) {

        }
        mMotionY = y;
    }

    private boolean trackMotionScroll(int deltaY) {
        mFirstRowTop += deltaY;
        ViewGroupUtils.offsetChildrenTopAndBottom(this, deltaY);
        return false;
    }

    private void onSecondaryPointerUp(MotionEvent event) {
        final int pointerIndex = MotionEventCompat.getActionIndex(event);
        final int pointerId = MotionEventCompat.getPointerId(event, pointerIndex);
        if (pointerId != mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mMotionX = (int) MotionEventCompat.getX(event, newPointerIndex);
            mMotionY = (int) MotionEventCompat.getY(event, newPointerIndex);
            mActivePointerId = MotionEventCompat.getPointerId(event, newPointerIndex);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        log("onMeasure()");
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int availableWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        mUnitSize = (availableWidth - (mColumnNum - 1) * mDividerSize) / mColumnNum;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        log("onLayout()");
        switch (mLayoutState) {
            case LAYOUT_STATE_INITIAL:
                fillChildren();
                break;
            case LAYOUT_STATE_IDLE:
                break;
        }
    }

    private void fillChildren() {
        log("fillChildren()");
        if (mAdapter == null) return;
        mFirstRowIndex = mFirstRowTop = 0;
        int currentIndex = 0;
        int reachedBottom = getPaddingTop();
        while (reachedBottom < getBottom() - getPaddingBottom()
                && currentIndex <= mAdapter.getCount() - 1) {
            View view = mAdapter.getView(mInflater, currentIndex, null, this);
            if (mCachingStarted && !view.isDrawingCacheEnabled()) {
                view.setDrawingCacheEnabled(true);
            }
            int size = mAdapter.getSize(currentIndex);
            int itemWidth = 0, itemHeight = 0;
            LayoutParams lp = (LayoutParams) view.getLayoutParams();
            if (lp == null || lp.width < 0 || lp.height < 0 || lp.size != size) {
                switch (size) {
                    case SIZE_SMALL:
                        itemWidth = itemHeight = mUnitSize;
                        break;
                    case SIZE_MIDDLE:
                        itemWidth = itemHeight = mUnitSize * 2 + mDividerSize;
                        break;
                    case SIZE_BIG:
                        itemWidth = mUnitSize * 4 + mDividerSize * 3;
                        itemHeight = mUnitSize * 2 + mDividerSize;
                        break;
                }
                lp = new LayoutParams(itemWidth, itemHeight, size, currentIndex);
                view.measure(
                        MeasureSpec.makeMeasureSpec(itemWidth, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(itemHeight, MeasureSpec.EXACTLY));
            } else {
                lp.index = currentIndex;
            }
            addViewInLayout(view, -1, lp, true);
            int[] location = getLocationInGrid(currentIndex);
            final int left = getPaddingLeft() + location[0] * (mUnitSize + mDividerSize);
            final int top = getPaddingTop() + location[1] * (mUnitSize + mDividerSize) + mFirstRowTop;
            view.layout(left, top, left + view.getMeasuredWidth(), top + view.getMeasuredHeight());
            reachedBottom = Math.max(view.getBottom(), reachedBottom);
            currentIndex++;
        }
        mLayoutState = LAYOUT_STATE_IDLE;
        requestLayout();
    }

    private int[] getLocationInGrid(int index) {
        if (index == 0) {   //init
            mCurrentGridColumn = mCurrentGridRow = 0;
        }

        if (mCurrentGridColumn >= mColumnNum) { //move down
            mCurrentGridRow++;
            mCurrentGridColumn = 0;
            return getLocationInGrid(index);
        }

        if (mGrid.get(mCurrentGridRow)[mCurrentGridColumn] == index) {
            return new int[] { mCurrentGridColumn, mCurrentGridRow };
        } else {    //move right
            mCurrentGridColumn++;
            return getLocationInGrid(index);
        }
    }

    public void setAdapter(@Nullable MetroAdapter adapter) {
        mAdapter = adapter;
        resetLayout();
    }

    private void resetLayout() {
        newEmptyGrid();
        fillGrid();
        removeAllViewsInLayout();
        mLayoutState = LAYOUT_STATE_INITIAL;
        requestLayout();
    }

    private int mCurrentGridColumn, mCurrentGridRow;

    private void fillGrid() {
        log("fillGrid");
        if (mAdapter == null) return;
        final int itemCount = mAdapter.getCount();
        mCurrentGridColumn = mCurrentGridRow = 0;
        for (int i = 0; i < itemCount; i++) {
            //check whether grid row is enough.
            if (mCurrentGridRow + 1 >= mGrid.size() - 1) {
                appendGridRows();
            }
            //check space
            checkAndFillSpace(i);
        }
        logGrid();
    }

    private void logGrid() {
        for(Integer[] row : mGrid) {
            StringBuilder builder = new StringBuilder("[");
            for (Integer index : row) {
                builder.append(index).append(", ");
            }
            builder.delete(builder.length() - 2, builder.length() - 1);
            builder.append("]");
            log(builder.toString());
        }
    }

    private void checkAndFillSpace(int index) {
        if (mAdapter == null) return;
        int size;
        size = mAdapter.getSize(index);
        switch (size) {
            case SIZE_SMALL:
                if (mCurrentGridColumn >= mColumnNum) { //row is not enough, move to next row
                    mCurrentGridColumn = 0;
                    mCurrentGridRow++;
                    checkAndFillSpace(index);
                } else {
                    if (mGrid.get(mCurrentGridRow)[mCurrentGridColumn] == -1) {
                        mGrid.get(mCurrentGridRow)[mCurrentGridColumn] = index;
                    } else {
                        //space has been taken, move right;
                        mCurrentGridColumn++;
                        checkAndFillSpace(index);
                    }
                }
                break;
            case SIZE_MIDDLE:
                if (mCurrentGridColumn + 1 >= mColumnNum) { //row is not enough, move to next row
                    mCurrentGridColumn = 0;
                    mCurrentGridRow++;
                    checkAndFillSpace(index);
                } else {
                    if (mGrid.get(mCurrentGridRow)[mCurrentGridColumn] == -1
                            && mGrid.get(mCurrentGridRow)[mCurrentGridColumn + 1] == -1
                            && mGrid.get(mCurrentGridRow + 1)[mCurrentGridColumn] == -1
                            && mGrid.get(mCurrentGridRow + 1)[mCurrentGridColumn + 1] == -1) {
                        mGrid.get(mCurrentGridRow)[mCurrentGridColumn] = index;
                        mGrid.get(mCurrentGridRow)[mCurrentGridColumn + 1] = index;
                        mGrid.get(mCurrentGridRow + 1)[mCurrentGridColumn] = index;
                        mGrid.get(mCurrentGridRow + 1)[mCurrentGridColumn + 1] = index;
                    } else {    //space has been taken, move right
                        mCurrentGridColumn++;
                        checkAndFillSpace(index);
                    }
                }
                break;
            case SIZE_BIG:
                if (mCurrentGridColumn + 3 >= mColumnNum) { //row is not enough, move to next row
                    mCurrentGridColumn = 0;
                    mCurrentGridRow++;
                    checkAndFillSpace(index);
                } else {
                    if (mGrid.get(mCurrentGridRow)[mCurrentGridColumn] == -1
                            && mGrid.get(mCurrentGridRow)[mCurrentGridColumn + 1] == -1
                            && mGrid.get(mCurrentGridRow)[mCurrentGridColumn + 2] == -1
                            && mGrid.get(mCurrentGridRow)[mCurrentGridColumn + 3] == -1
                            && mGrid.get(mCurrentGridRow + 1)[mCurrentGridColumn] == -1
                            && mGrid.get(mCurrentGridRow + 1)[mCurrentGridColumn + 1] == -1
                            && mGrid.get(mCurrentGridRow + 1)[mCurrentGridColumn + 2] == -1
                            && mGrid.get(mCurrentGridRow + 1)[mCurrentGridColumn + 3] == -1) {
                        mGrid.get(mCurrentGridRow)[mCurrentGridColumn] = index;
                        mGrid.get(mCurrentGridRow)[mCurrentGridColumn + 1] = index;
                        mGrid.get(mCurrentGridRow)[mCurrentGridColumn + 2] = index;
                        mGrid.get(mCurrentGridRow)[mCurrentGridColumn + 3] = index;
                        mGrid.get(mCurrentGridRow + 1)[mCurrentGridColumn] = index;
                        mGrid.get(mCurrentGridRow + 1)[mCurrentGridColumn + 1] = index;
                        mGrid.get(mCurrentGridRow + 1)[mCurrentGridColumn + 2] = index;
                        mGrid.get(mCurrentGridRow + 1)[mCurrentGridColumn + 3] = index;
                    } else {    //space has been taken, move right;
                        mCurrentGridColumn++;
                        checkAndFillSpace(index);
                    }
                }
                break;
        }
    }

    private void newEmptyGrid() {
        mGrid = new ArrayList<>();
        appendGridRows();
    }

    private void appendGridRows() {
        for (int i = 0; i < 50; i++) {
            Integer[] row = new Integer[mColumnNum];
            for (int j = 0; j < row.length; j++) {
                row[j] = -1;
            }
            mGrid.add(row);
        }
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {

        @Size int size = SIZE_SMALL;
        int index = -1;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(int width, int height, @Size int size, int index) {
            super(width, height);
            this.size = size;
            this.index = index;
        }
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                SIZE_SMALL, -1);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        mDisallowIntercept = disallowIntercept;
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    private static void log(String msg) {
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, msg);
    }

    private void createScrollingCache() {
        if (mScrollingCacheEnabled && !mCachingStarted && !isHardwareAccelerated()) {
            setChildrenDrawnWithCacheEnabled(true);
            setChildrenDrawingCacheEnabled(true);
            mCachingStarted = true;
        }
    }

    public void setScrollingCacheEnabled(boolean enabled) {
        if (mScrollingCacheEnabled && !enabled) {
            clearScrollingCache();
        }
        mScrollingCacheEnabled = enabled;
    }

    private void clearScrollingCache() {
        log("clearScrollingCache");
        if (!isHardwareAccelerated()) {
            if (mClearScrollingCache == null) {
                mClearScrollingCache = new Runnable() {
                    @Override
                    public void run() {
                        if (mCachingStarted) {
                            mCachingStarted = false;
                            setChildrenDrawnWithCacheEnabled(false);
                            if ((getPersistentDrawingCache() & PERSISTENT_SCROLLING_CACHE) == 0) {
                                setChildrenDrawingCacheEnabled(false);
                            }
                            if (!isAlwaysDrawnWithCacheEnabled()) {
                                invalidate();
                            }
                        }
                    }
                };
            }
            post(mClearScrollingCache);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mClearScrollingCache != null) {
            removeCallbacks(mClearScrollingCache);
        }
    }
}
