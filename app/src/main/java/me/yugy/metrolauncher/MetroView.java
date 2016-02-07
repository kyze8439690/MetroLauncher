package me.yugy.metrolauncher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.widget.ScrollerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import me.yugy.app.common.compat.ViewCompat;
import me.yugy.app.common.utils.ViewGroupUtils;

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
    private int mColumnNum = 4; //// TODO: 1/31/16
    @LayoutState
    private int mLayoutState;

    private int mFirstRowTop;

    private HashMap<Integer, Integer[]> mLocationMap;

    private int mActivePointerId = INVALID_POINTER;
    private int mMotionX, mMotionY;

    private boolean mDisallowIntercept = false;
    private int mTouchSlop;

    private VelocityTracker mVelocityTracker;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private float mVelocityScale = 1.0f;
    private FlingRunnable mFlingRunnable;

    private RecycleBin mRecycleBin;

    public MetroView(Context context) {
        this(context, null);
    }

    public MetroView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MetroView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mInflater = LayoutInflater.from(context);
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MetroView, defStyleAttr, 0);
        mDividerSize = a.getDimensionPixelSize(R.styleable.MetroView_mv_dividerSize, -1);
        a.recycle();

        if (mDividerSize == -1) {
            mDividerSize = context.getResources().getDimensionPixelSize(R.dimen.default_divider_size);
        }

        setClipChildren(false);
        setClipToPadding(false);
        mRecycleBin = new RecycleBin();
    }

    public void animateOpen(final View view) {
        final int count = getChildCount();
        List<View> views = new ArrayList<>(count - 1);
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (!child.equals(view)) {
                views.add(child);
            }
        }
        int startOffset = 0;
        while (views.size() > 0) {
            final int index = new Random().nextInt(views.size());
            final View animateView = views.remove(index);
            animateView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    animateFlip(animateView);
                }
            }, startOffset);
            startOffset += 25;
        }
        startOffset += 300;
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                animateFlip(view);
            }
        }, startOffset);
    }

    private void animateFlip(final View animateView) {
        animateView.setPivotX(0);
        animateView.animate()
                .rotationY(-45)
                .translationX(-animateView.getLeft() - animateView.getWidth() - getWidth() / 2)
//                .scaleX(1.1f).scaleY(1.1f)
                .setDuration(300)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        animateView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        animateView.setLayerType(View.LAYER_TYPE_NONE, null);
                        animateView.setVisibility(View.INVISIBLE);
                        animateView.setRotationY(0);
                        animateView.setTranslationX(0f);
                        animateView.setScaleX(1f);
                        animateView.setScaleY(1f);
                    }
                })
                .start();
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
                initOrResetVelocityTracker();
                mVelocityTracker.addMovement(event);
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
                    initVelocityTrackerIfNotExists();
                    mVelocityTracker.addMovement(event);
                    if (startScrollIfNeeded(y, event)) {
                        return true;
                    }
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                mActivePointerId = INVALID_POINTER;
                recycleVelocityTracker();
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
        initVelocityTrackerIfNotExists();
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
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                final float yVelocity =
                        VelocityTrackerCompat.getYVelocity(velocityTracker, mActivePointerId);
                final int initialVelocity = (int) (yVelocity * mVelocityScale);
                boolean flingVelocity = Math.abs(initialVelocity) > mMinimumVelocity;
                if (flingVelocity) {
                    if (mFlingRunnable == null) {
                        mFlingRunnable = new FlingRunnable();
                    }
                    mFlingRunnable.start(-initialVelocity);
                } else {
                    if (mFlingRunnable != null) {
                        mFlingRunnable.endFling();
                    }
                }
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
        if (mVelocityTracker != null) {
            mVelocityTracker.addMovement(event);
        }
        return true;
    }

    private boolean startScrollIfNeeded(int y, MotionEvent event) {
        final int deltaY = y - mMotionY;
        final int distance = Math.abs(deltaY);
        if (distance > mTouchSlop) {
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

    /**
     * @param deltaY scroll offset, scroll down if offset > 0, scroll up if offset < 0
     * @return return true if have scroll to edge
     */
    private boolean trackMotionScroll(int deltaY) {
        final int childCount = getChildCount();
        if (childCount == 0 || mAdapter == null) {
            return true;
        }

        boolean result = false;
        final int oldTop = mFirstRowTop;
        if (mFirstRowTop + deltaY > 0) {  //detect scroll to top
            deltaY = -oldTop;
            result = true;
        }

        if (!result) {
            final View lastChild = getChildAt(childCount - 1);
            final LayoutParams lastLp = (LayoutParams) lastChild.getLayoutParams();
            if (lastLp.index == mAdapter.getCount() - 1) {
                Integer[] location = getLocationInMap(lastLp.index);
                final int row = location[1];
                int maxBottom = lastChild.getBottom();
                for (int i = childCount - 1; i >= 0 ; i--) {
                    final View child = getChildAt(i);
                    final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    final int index = lp.index;
                    location = getLocationInMap(index);
                    if (location[1] == row) {
                        maxBottom = Math.max(maxBottom, child.getBottom());
                    } else {
                        break;
                    }
                }
                if (maxBottom + deltaY < getBottom() - getPaddingBottom()) {    //detect scroll to bottom
                    deltaY = getBottom() - getPaddingBottom() - maxBottom;
                    result = true;
                }
            }
        }

        mFirstRowTop += deltaY;

        ViewGroupUtils.offsetChildrenTopAndBottom(this, deltaY);

        final boolean down = deltaY < 0;
        List<View> indexOfChildrenToRemove = new ArrayList<>();

        if (down) {
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                if (child.getBottom() < getTop()) {
                    indexOfChildrenToRemove.add(child);
                    LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    mRecycleBin.saveView(lp.size, child);
                }
            }
        } else {
            for (int i = childCount - 1; i >= 0; i--) {
                final View child = getChildAt(i);
                if (child.getTop() > getBottom()) {
                    indexOfChildrenToRemove.add(child);
                    LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    mRecycleBin.saveView(lp.size, child);
                }
            }
        }

        if (indexOfChildrenToRemove.size() > 0) {
            for (View child : indexOfChildrenToRemove) {
                detachViewFromParent(child);
            }
        }

        fillGap(down);

        log("result: " + result + ", deltaY: " + deltaY + ", mFirstRowTop: " + mFirstRowTop + ", viewCount: " + getChildCount());

        return result;
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

    private void fillGap(boolean down) {
        if (mAdapter == null || getChildCount() == 0) return;
        if (down) {
            final View lastChild = getChildAt(getChildCount() - 1);
            LayoutParams lastLp = (LayoutParams) lastChild.getLayoutParams();
            int startIndex = lastLp.index + 1;
            int startTop = lastChild.getTop() + mUnitSize + mDividerSize;
            fillDown(startIndex, startTop);
        } else {
            fillUp();
        }
    }

    private void fillChildren() {
        log("fillChildren()");
        if (mAdapter == null) return;
        mFirstRowTop = 0;
        int startIndex = 0;
        int startTop = getPaddingTop();
        fillDown(startIndex, startTop);
        mLayoutState = LAYOUT_STATE_IDLE;
        requestLayout();
    }

    private void fillDown(int startIndex, int startTop) {
        if (mAdapter == null) return;
        HashSet<Integer> index = getIndexInLayout();
        while (startTop < getBottom() && startIndex <= mAdapter.getCount() - 1) {
            if (index.contains(startIndex)) {
                startIndex++;
                continue;
            }
            int size = mAdapter.getSize(startIndex);
            View view = setupAndAddView(startIndex, size);
            if (view == null) {
                startIndex++;
                continue;
            }
            index.add(startIndex);
            Integer[] location = getLocationInMap(startIndex);
            final int left = getPaddingLeft() + location[0] * (mUnitSize + mDividerSize);
            final int top = getPaddingTop() + location[1] * (mUnitSize + mDividerSize) + mFirstRowTop;
            view.layout(left, top, left + view.getMeasuredWidth(), top + view.getMeasuredHeight());
            startIndex++;
            if (startIndex < mAdapter.getCount()) {
                Integer[] nextLocation = getLocationInMap(startIndex);
                if (!nextLocation[1].equals(location[1])) { //next line
                    startTop += mUnitSize + mDividerSize;
                }
            }
        }
    }

    private void fillUp() {
        if (mAdapter == null) return;
        HashSet<Integer> index = getIndexInLayout();
        final int count = mAdapter.getCount();
        for (int i = 0; i < count; i++) {
            if (index.contains(i)) continue;
            Integer[] location = getLocationInMap(i);
            final int size = mAdapter.getSize(i);
            final int left = getPaddingLeft() + location[0] * (mUnitSize + mDividerSize);
            final int top = getPaddingTop() + location[1] * (mUnitSize + mDividerSize) + mFirstRowTop;
            final int bottom = top + getWidthAndHeightBaseOnSize(size)[1];
            if (top > getBottom()) return;
            if (bottom > getTop()) {
                View view = setupAndAddView(i, size);
                if (view == null) continue;
                index.add(i);
                view.layout(left, top, left + view.getMeasuredWidth(), top + view.getMeasuredHeight());
            }
        }
    }

    private HashSet<Integer> getIndexInLayout() {
        HashSet<Integer> index = new HashSet<>();
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            index.add(lp.index);
        }
        return index;
    }

    @Nullable
    private View setupAndAddView(int index, @Size int size) {
        if (mAdapter == null) return null;
        boolean reuse = true;
        View recycledView = mRecycleBin.getRecycledView(size);
        View view = mAdapter.getView(mInflater, index, recycledView, this);
        if (view != recycledView) {
            mRecycleBin.saveView(size, recycledView);
            reuse = false;
        }
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        if (lp == null || lp.width < 0 || lp.height < 0 || lp.size != size) {
            int[] widthAndHeight = getWidthAndHeightBaseOnSize(size);
            lp = new LayoutParams(widthAndHeight[0], widthAndHeight[1], size, index);
            view.measure(
                    MeasureSpec.makeMeasureSpec(widthAndHeight[0], MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(widthAndHeight[1], MeasureSpec.EXACTLY));
        } else {
            if (view.isLayoutRequested()) {
                int[] widthAndHeight = getWidthAndHeightBaseOnSize(size);
                lp = new LayoutParams(widthAndHeight[0], widthAndHeight[1], size, index);
                view.measure(
                        MeasureSpec.makeMeasureSpec(widthAndHeight[0], MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(widthAndHeight[1], MeasureSpec.EXACTLY));
            }
            lp.index = index;
        }
        view.setLayoutParams(lp);
        if (reuse) {
            attachViewToParent(view, -1, view.getLayoutParams());
        } else {
            addViewInLayout(view, -1, view.getLayoutParams(), true);
        }
        return view;
    }

    private int[] getWidthAndHeightBaseOnSize(int size) {
        int itemWidth = 0, itemHeight = 0;
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
        return new int[] {itemWidth, itemHeight};
    }

    private Integer[] getLocationInMap(int index) {
        return mLocationMap.get(index);
    }

    private Integer[] getLocationInGrid(List<Integer[]> grid, int index, boolean init) {
        try {
            if (init) {
                mTempColumn = mTempRow = 0;
            }

            if (mTempColumn >= mColumnNum) { //move down
                mTempRow++;
                mTempColumn = 0;
                return getLocationInGrid(grid, index, false);
            }

            if (grid.get(mTempRow)[mTempColumn] == index) {
                return new Integer[]{mTempColumn, mTempRow};
            } else {    //move right
                mTempColumn++;
                return getLocationInGrid(grid, index, false);
            }
        } catch (IndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException("Out of bounds when get location for " + index);
        }
    }

    public void setAdapter(@Nullable MetroAdapter adapter) {
        mAdapter = adapter;
        resetLayout();
    }

    private void resetLayout() {
        mRecycleBin.reset();
        mLocationMap = new HashMap<>();
        initLocationMap();
        removeAllViewsInLayout();
        mLayoutState = LAYOUT_STATE_INITIAL;
        requestLayout();
    }

    private int mTempColumn, mTempRow;

    private void initLocationMap() {
        log("initLocationMap()");
        if (mAdapter == null) return;
        final int itemCount = mAdapter.getCount();
        mTempColumn = mTempRow = 0;
        List<Integer[]> grid = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            //check whether grid row is enough.
            if (mTempRow + 1 >= grid.size() - 1) {
                appendGridRows(grid, mColumnNum);
            }
            //check space
            checkAndFillSpace(grid, i);
        }
        for (int i = 0; i < itemCount; i++) {
            Integer[] location = getLocationInGrid(grid, i, true);
            mLocationMap.put(i, location);
        }
    }

    private void checkAndFillSpace(List<Integer[]> grid, int index) {
        if (mAdapter == null) return;
        int size;
        size = mAdapter.getSize(index);
        switch (size) {
            case SIZE_SMALL:
                if (mTempColumn >= mColumnNum) { //row is not enough, move to next row
                    mTempColumn = 0;
                    mTempRow++;
                    checkAndFillSpace(grid, index);
                } else {
                    if (grid.get(mTempRow)[mTempColumn] == -1) {
                        grid.get(mTempRow)[mTempColumn] = index;
                    } else {
                        //space has been taken, move right;
                        mTempColumn++;
                        checkAndFillSpace(grid, index);
                    }
                }
                break;
            case SIZE_MIDDLE:
                if (mTempColumn + 1 >= mColumnNum) { //row is not enough, move to next row
                    mTempColumn = 0;
                    mTempRow++;
                    checkAndFillSpace(grid, index);
                } else {
                    if (grid.get(mTempRow)[mTempColumn] == -1
                            && grid.get(mTempRow)[mTempColumn + 1] == -1
                            && grid.get(mTempRow + 1)[mTempColumn] == -1
                            && grid.get(mTempRow + 1)[mTempColumn + 1] == -1) {
                        grid.get(mTempRow)[mTempColumn] = index;
                        grid.get(mTempRow)[mTempColumn + 1] = index;
                        grid.get(mTempRow + 1)[mTempColumn] = index;
                        grid.get(mTempRow + 1)[mTempColumn + 1] = index;
                    } else {    //space has been taken, move right
                        mTempColumn++;
                        checkAndFillSpace(grid, index);
                    }
                }
                break;
            case SIZE_BIG:
                if (mTempColumn + 3 >= mColumnNum) { //row is not enough, move to next row
                    mTempColumn = 0;
                    mTempRow++;
                    checkAndFillSpace(grid, index);
                } else {
                    if (grid.get(mTempRow)[mTempColumn] == -1
                            && grid.get(mTempRow)[mTempColumn + 1] == -1
                            && grid.get(mTempRow)[mTempColumn + 2] == -1
                            && grid.get(mTempRow)[mTempColumn + 3] == -1
                            && grid.get(mTempRow + 1)[mTempColumn] == -1
                            && grid.get(mTempRow + 1)[mTempColumn + 1] == -1
                            && grid.get(mTempRow + 1)[mTempColumn + 2] == -1
                            && grid.get(mTempRow + 1)[mTempColumn + 3] == -1) {
                        grid.get(mTempRow)[mTempColumn] = index;
                        grid.get(mTempRow)[mTempColumn + 1] = index;
                        grid.get(mTempRow)[mTempColumn + 2] = index;
                        grid.get(mTempRow)[mTempColumn + 3] = index;
                        grid.get(mTempRow + 1)[mTempColumn] = index;
                        grid.get(mTempRow + 1)[mTempColumn + 1] = index;
                        grid.get(mTempRow + 1)[mTempColumn + 2] = index;
                        grid.get(mTempRow + 1)[mTempColumn + 3] = index;
                    } else {    //space has been taken, move right;
                        mTempColumn++;
                        checkAndFillSpace(grid, index);
                    }
                }
                break;
        }
    }

    private static void appendGridRows(List<Integer[]> grid, int columnSize) {
        for (int i = 0; i < 50; i++) {
            Integer[] row = new Integer[columnSize];
            for (int j = 0; j < row.length; j++) {
                row[j] = -1;
            }
            grid.add(row);
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
        if (disallowIntercept) {
            recycleVelocityTracker();
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private static void log(String msg) {
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, msg);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mFlingRunnable != null) {
            removeCallbacks(mFlingRunnable);
        }
    }

    private class FlingRunnable implements Runnable {

        private static final int FLYWHEEL_TIMEOUT = 40;

        private ScrollerCompat mScroller;
        private int mLastFlingY;

        public FlingRunnable() {
            mScroller = ScrollerCompat.create(getContext());
        }

        void start(int initialVelocity) {
            int initialY = initialVelocity < 0 ? Integer.MAX_VALUE : 0;
            mLastFlingY = initialY;
            mScroller.fling(0, initialY, 0, initialVelocity,
                    0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
            ViewCompat.postOnAnimation(MetroView.this, this);
        }

        void endFling() {
            removeCallbacks(this);
            mScroller.abortAnimation();
        }

        @Override
        public void run() {
            if ((mAdapter != null && mAdapter.getCount() == 0) || getChildCount() == 0) {
                endFling();
                return;
            }

            final ScrollerCompat scroller = mScroller;
            boolean more = scroller.computeScrollOffset();
            final int y = scroller.getCurrY();

            int delta = mLastFlingY - y;

            if (delta > 0) {
                delta = Math.min(getHeight() - getPaddingBottom() - getPaddingTop() - 1, delta);
            } else {
                delta = Math.max(-getHeight() - getPaddingBottom() - getPaddingTop() - 1, delta);
            }

            final boolean atEdge = trackMotionScroll(delta);
            final boolean atEnd = atEdge && (delta != 0);

            if (more && !atEnd) {
                mLastFlingY = y;
                ViewCompat.postOnAnimation(MetroView.this, this);
            } else {
                endFling();
            }
        }
    }

    class RecycleBin {
        private LinkedList<View> mSmallSizeViews = new LinkedList<>();
        private LinkedList<View> mMiddleSizeViews = new LinkedList<>();
        private LinkedList<View> mBigSizeViews = new LinkedList<>();

        public void reset() {
            mSmallSizeViews.clear();
            mMiddleSizeViews.clear();
            mBigSizeViews.clear();
        }

        public void saveView(@Size int size, View view) {
            if (view != null) {
                LinkedList<View> container = getContainer(size);
                if (container != null) {
                    container.push(view);
                }
            }
        }

        @Nullable
        public View getRecycledView(@Size int size) {
            LinkedList<View> container = getContainer(size);
            if (container != null) {
                View view = container.poll();
                if (view == null) return null;
                LayoutParams lp = (LayoutParams) view.getLayoutParams();
                if (lp.size != size) {
                    saveView(lp.size, view);
                    return null;
                } else {
                    return view;
                }
            }
            return null;
        }

        private LinkedList<View> getContainer(@Size int size) {
            switch (size) {
                case SIZE_SMALL: return mSmallSizeViews;
                case SIZE_MIDDLE: return mMiddleSizeViews;
                case SIZE_BIG: return mBigSizeViews;
                default: return null;
            }
        }
    }

}
