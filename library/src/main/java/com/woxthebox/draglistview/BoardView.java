/*
 * Copyright 2014 Magnus Woxblom
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.woxthebox.draglistview;

import android.content.ClipData;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Scroller;
import com.woxthebox.draglistview.utils.DefaultItemClickListener;
import com.woxthebox.draglistview.utils.RecyclerItemTouchListener;
import java.util.ArrayList;

import static android.support.v7.widget.RecyclerView.NO_POSITION;

public class BoardView extends HorizontalScrollView implements AutoScroller.AutoScrollListener {

    public interface BoardListener {
        void onItemDragStarted(int column, int row);

        void onItemChangedPosition(int oldColumn, int oldRow, int newColumn, int newRow);

        void onItemChangedColumn(int oldColumn, int newColumn);

        void onItemDragEnded(int fromColumn, int fromRow, int toColumn, int toRow);

        void onColumnPositionChanged(int from, int to);

        void onItemClick(View view, int position, Object data);
    }

    public interface BoardCallback {
        boolean canDragItemAtPosition(int column, int row);

        boolean canDropItemAtPosition(int oldColumn, int oldRow, int newColumn, int newRow);
    }

    public enum ColumnSnapPosition {
        LEFT, CENTER, RIGHT
    }

    private static final int SCROLL_ANIMATION_DURATION_DRAG = 1000;
    private final int SCROLL_ANIMATION_DURATION_DEFAULT = 325;
    private int SCROLL_ANIMATION_DURATION = SCROLL_ANIMATION_DURATION_DEFAULT;
    private static final int PAUSE_COLUMN_SCROLL_DURATION = 1000;
    private static final int MARGIN_SMALL = 16;
    private static final int MARGIN_LARGE = 64;

    //private static final int SCROLL_ANIMATION_DURATION = 325;
    private Scroller mScroller;
    private AutoScroller mAutoScroller;
    private GestureDetector mGestureDetector;
    private GestureDetector mFooterGestureDetector;
    private GestureDetector mColumnGestureDetector;
    private ColumnGestureListener mColumnGestureListener;
    private View mDraggingColumnView;
    private int mDraggingColumn;
    private FrameLayout mRootLayout;
    LinearLayout mColumnLayout;
    ArrayList<DragItemRecyclerView> mLists = new ArrayList<>();
    SparseArray<View> mHeaders = new SparseArray<>();
    SparseArray<View> mFooters = new SparseArray<>();
    SparseArray<View> mBorders = new SparseArray<>();
    private DragItemRecyclerView mCurrentRecyclerView;
    private DragItem mDragItem;
    private BoardListener mBoardListener;
    private BoardCallback mBoardCallback;
    private boolean mSnapToColumnWhenScrolling = true;
    private boolean mSnapToColumnWhenDragging = true;
    private boolean mSnapToColumnInLandscape = false;
    private ColumnSnapPosition mSnapPosition = ColumnSnapPosition.CENTER;
    private int mCurrentColumn;
    private float mTouchX;
    private float mTouchY;
    int mColumnWidth;
    private int mDragStartColumn;
    private int mDragStartRow;
    private boolean mHasLaidOut;
    private boolean mDragEnabled = true;
    private int mLastDragColumn = NO_POSITION;
    private int mLastDragRow = NO_POSITION;
    private SavedState mSavedState;
    private boolean isScaled;
    private int mActiveFeature = 0;
    private long lastTouchTime = -1;

    public BoardView(Context context) {
        super(context);
    }

    public BoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BoardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Resources res = getResources();
        boolean isPortrait =
                res.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (isPortrait) {
            mColumnWidth = (int) (res.getDisplayMetrics().widthPixels * 0.87);
        } else {
            mColumnWidth = (int) (res.getDisplayMetrics().density * 320);
        }

        mGestureDetector = new GestureDetector(getContext(), new GestureListener());

        mFooterGestureDetector = new GestureDetector(getContext(), new FooterGestureListener());
        mColumnGestureListener = new ColumnGestureListener();
        mColumnGestureDetector = new GestureDetector(getContext(), mColumnGestureListener);

        mScroller = new Scroller(getContext(), new DecelerateInterpolator(1.1f));
        mAutoScroller = new AutoScroller(getContext(), this);
        mAutoScroller.setAutoScrollMode(
                snapToColumnWhenDragging() ? AutoScroller.AutoScrollMode.COLUMN
                        : AutoScroller.AutoScrollMode.POSITION);
        mDragItem = new DragItem(getContext());

        mRootLayout = new FrameLayout(getContext());
        mRootLayout.setLayoutParams(
                new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));

        mColumnLayout = new LinearLayout(getContext());
        mColumnLayout.setOrientation(LinearLayout.HORIZONTAL);
        mColumnLayout.setLayoutParams(
                new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        mColumnLayout.setMotionEventSplittingEnabled(false);

        mRootLayout.addView(mColumnLayout);
        mRootLayout.addView(mDragItem.getDragItemView());
        addView(mRootLayout);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        // Snap to closes column after first layout.
        // This is needed so correct column is scrolled to after a rotation.
        if (!mHasLaidOut && mSavedState != null) {
            mCurrentColumn = mSavedState.currentColumn;
            scrollToColumn(mCurrentColumn, false);
            mSavedState = null;
        }
        mHasLaidOut = true;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mSavedState = ss;
        requestLayout();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, getClosestSnapColumn());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean retValue = handleTouchEvent(event, false);
        return retValue || super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean retValue = handleTouchEvent(event);
        return retValue || super.onTouchEvent(event);
    }

    private boolean handleTouchEvent(MotionEvent event) {
        return handleTouchEvent(event, true);
    }

    private boolean handleTouchEvent(MotionEvent event, boolean touch) {
        if (mLists.size() == 0) {
            return false;
        }
        mTouchX = event.getX();
        mTouchY = event.getY();
        if (isDragging()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    if (!mAutoScroller.isAutoScrolling()) {
                        updateScrollPosition();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mAutoScroller.stopAutoScroll();
                    mCurrentRecyclerView.onDragEnded();
                    if (snapToColumnWhenScrolling()) {
                        scrollToColumn(getColumnOfList(mCurrentRecyclerView), true);
                    }
                    invalidate();
                    break;
            }
            return true;
        } else {
            if (event.getAction() == MotionEvent.ACTION_DOWN && touch) {

                long thisTime = System.currentTimeMillis();
                if (thisTime - lastTouchTime < 200) {
                    //scale();
                    lastTouchTime = -1;
                } else {
                    lastTouchTime = thisTime;
                }
            }
            if (!isScaled && isPortrait()) {
                if (mGestureDetector.onTouchEvent(event)) {
                    return touch;
                } else if (event.getAction() == MotionEvent.ACTION_UP
                        || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    int scrollX = getScrollX();
                    int featureWidth = mColumnWidth;
                    mActiveFeature = ((scrollX + (featureWidth / 2)) / featureWidth);
                    int scrollTo =
                            (int) (mActiveFeature * featureWidth - (getMeasuredWidth() * 0.07));
                    smoothScrollTo(scrollTo, 0);
                    return touch;
                }
            }
        }
        return false;
    }

    @Override
    public void computeScroll() {
        if (!mScroller.isFinished() && mScroller.computeScrollOffset()) {
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();
            if (getScrollX() != x || getScrollY() != y) {
                scrollTo(x, y);
            }

            // If auto scrolling at the same time as the scroller is running,
            // then update the drag item position to prevent stuttering item
            if (mAutoScroller.isAutoScrolling()) {
                mDragItem.setPosition(getListTouchX(mCurrentRecyclerView),
                        getListTouchY(mCurrentRecyclerView));
            }

            ViewCompat.postInvalidateOnAnimation(this);
        } else {
            super.computeScroll();
        }
    }

    @Override
    public void onAutoScrollPositionBy(int dx, int dy) {
        if (isDragging()) {
            scrollBy(dx, dy);
            updateScrollPosition();
        } else {
            mAutoScroller.stopAutoScroll();
        }
    }

    @Override
    public void onAutoScrollColumnBy(int columns) {
        if (isDragging()) {
            int newColumn = mCurrentColumn + columns;
            if (columns != 0 && newColumn >= 0 && newColumn < mLists.size()) {
                scrollToColumn(newColumn, true);
            }
            try {
                updateScrollPosition();
            } catch (Exception e) {

            }
        } else {
            mAutoScroller.stopAutoScroll();
        }
    }

    private void updateScrollPosition() {
        // Updated event to scrollview coordinates
        DragItemRecyclerView currentList = getCurrentRecyclerView(mTouchX + getScrollX());
        if (mCurrentRecyclerView != currentList) {
            int oldColumn = getColumnOfList(mCurrentRecyclerView);
            int newColumn = getColumnOfList(currentList);
            long itemId = mCurrentRecyclerView.getDragItemId();

            // Check if it is ok to drop the item in the new column first
            int newPosition = currentList.getDragPositionForY(getListTouchY(currentList));
            if (mBoardCallback == null || mBoardCallback.canDropItemAtPosition(mDragStartColumn,
                    mDragStartRow, newColumn, newPosition)) {
                Object item = mCurrentRecyclerView.removeDragItemAndEnd();
                if (item != null) {
                    mCurrentRecyclerView = currentList;
                    mCurrentRecyclerView.addDragItemAndStart(getListTouchY(mCurrentRecyclerView),
                            item, itemId);
                    mDragItem.setOffset(((View) mCurrentRecyclerView.getParent()).getLeft(),
                            mCurrentRecyclerView.getTop());

                    if (mBoardListener != null) {
                        mBoardListener.onItemChangedColumn(oldColumn, newColumn);
                    }
                }
            }
        }

        // Updated event to list coordinates
        mCurrentRecyclerView.onDragging(getListTouchX(mCurrentRecyclerView),
                getListTouchY(mCurrentRecyclerView));

        float scrollEdge = getResources().getDisplayMetrics().widthPixels * 0.14f;
        if (mTouchX > getWidth() - scrollEdge && getScrollX() < mColumnLayout.getWidth()) {
            mAutoScroller.startAutoScroll(AutoScroller.ScrollDirection.LEFT);
        } else if (mTouchX < scrollEdge && getScrollX() > 0) {
            mAutoScroller.startAutoScroll(AutoScroller.ScrollDirection.RIGHT);
        } else {
            mAutoScroller.stopAutoScroll();
        }
        invalidate();
    }

    private float getListTouchX(DragItemRecyclerView list) {
        return mTouchX + getScrollX() - ((View) list.getParent()).getLeft();
    }

    private float getListTouchY(DragItemRecyclerView list) {
        return mTouchY - list.getTop();
    }

    private DragItemRecyclerView getCurrentRecyclerView(float x) {
        for (DragItemRecyclerView list : mLists) {
            View parent = (View) list.getParent();
            if (parent.getLeft() <= x && parent.getRight() > x) {
                return list;
            }
        }
        return mCurrentRecyclerView;
    }

    private int getColumnOfList(DragItemRecyclerView list) {
        int column = 0;
        for (int i = 0; i < mLists.size(); i++) {
            RecyclerView tmpList = mLists.get(i);
            if (tmpList == list) {
                column = i;
            }
        }
        return column;
    }

    private int getCurrentColumn(float posX) {
        for (int i = 0; i < mLists.size(); i++) {
            RecyclerView list = mLists.get(i);
            View parent = (View) list.getParent();
            if (parent.getLeft() <= posX && parent.getRight() > posX) {
                return i;
            }
        }
        return 0;
    }

    private int getClosestSnapColumn() {
        int column = 0;
        int minDiffX = Integer.MAX_VALUE;
        for (int i = 0; i < mLists.size(); i++) {
            View listParent = (View) mLists.get(i).getParent();

            int diffX = 0;
            switch (mSnapPosition) {
                case LEFT:
                    int leftPosX = getScrollX();
                    diffX = Math.abs(listParent.getLeft() - leftPosX);
                    break;
                case CENTER:
                    int middlePosX = getScrollX() + getMeasuredWidth() / 2;
                    diffX = Math.abs(listParent.getLeft() + mColumnWidth / 2 - middlePosX);
                    break;
                case RIGHT:
                    int rightPosX = getScrollX() + getMeasuredWidth();
                    diffX = Math.abs(listParent.getRight() - rightPosX);
                    break;
            }

            if (diffX < minDiffX) {
                minDiffX = diffX;
                column = i;
            }
        }
        return column;
    }

    private boolean snapToColumnWhenScrolling() {
        boolean isPortrait =
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        return mSnapToColumnWhenScrolling && (isPortrait || mSnapToColumnInLandscape);
    }

    private boolean snapToColumnWhenDragging() {
        boolean isPortrait =
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        return mSnapToColumnWhenDragging && (isPortrait || mSnapToColumnInLandscape);
    }

    private boolean isDragging() {
        return mCurrentRecyclerView != null && mCurrentRecyclerView.isDragging();
    }

    public RecyclerView getRecyclerView(int column) {
        if (column >= 0 && column < mLists.size()) {
            return mLists.get(column);
        }
        return null;
    }

    public DragItemAdapter getAdapter(int column) {
        if (column >= 0 && column < mLists.size()) {
            return (DragItemAdapter) mLists.get(column).getAdapter();
        }
        return null;
    }

    public int getItemCount() {
        int count = 0;
        for (DragItemRecyclerView list : mLists) {
            count += list.getAdapter().getItemCount();
        }
        return count;
    }

    public int getItemCount(int column) {
        if (mLists.size() > column) {
            return mLists.get(column).getAdapter().getItemCount();
        }
        return 0;
    }

    public int getColumnCount() {
        return mLists.size();
    }

    public View getHeaderView(int column) {
        return mHeaders.get(column);
    }

    public void removeItem(int column, int row) {
        if (!isDragging()
                && mLists.size() > column
                && mLists.get(column).getAdapter().getItemCount() > row) {

            updateColumnHeight(column);

            DragItemAdapter adapter = (DragItemAdapter) mLists.get(column).getAdapter();
            adapter.removeItem(row);
        }
    }

    public void addItem(int column, int row, Object item, boolean scrollToItem) {
        if (!isDragging()
                && mLists.size() > column
                && mLists.get(column).getAdapter().getItemCount() >= row) {
            DragItemAdapter adapter = (DragItemAdapter) mLists.get(column).getAdapter();
            adapter.addItem(row, item);
            updateColumnHeight(column);
            if (scrollToItem) {
                scrollToItem(column, row, false);
            }
        }
    }

    public void moveItem(int fromColumn, int fromRow, int toColumn, int toRow,
            boolean scrollToItem) {
        if (!isDragging()
                && mLists.size() > fromColumn
                && mLists.get(fromColumn).getAdapter().getItemCount() > fromRow
                && mLists.size() > toColumn
                && mLists.get(toColumn).getAdapter().getItemCount() >= toRow) {
            if (fromColumn == toColumn) {
                DragItemAdapter adapter = (DragItemAdapter) mLists.get(fromColumn).getAdapter();
                Object item = adapter.removeItem(fromRow);
                adapter = (DragItemAdapter) mLists.get(toColumn).getAdapter();
                adapter.addItem(toRow, item);
            } else {
                updateColumnHeight(fromColumn);

                DragItemAdapter adapter = (DragItemAdapter) mLists.get(fromColumn).getAdapter();
                Object item = adapter.removeItem(fromRow);
                adapter = (DragItemAdapter) mLists.get(toColumn).getAdapter();
                adapter.addItem(toRow, item);

                updateColumnHeight(toColumn);
            }
            if (scrollToItem) {
                scrollToItem(toColumn, toRow, false);
            }
        }
    }

    public void moveItem(long itemId, int toColumn, int toRow, boolean scrollToItem) {
        for (int i = 0; i < mLists.size(); i++) {
            RecyclerView.Adapter adapter = mLists.get(i).getAdapter();
            final int count = adapter.getItemCount();
            for (int j = 0; j < count; j++) {
                long id = adapter.getItemId(j);
                if (id == itemId) {
                    moveItem(i, j, toColumn, toRow, scrollToItem);
                    return;
                }
            }
        }
    }

    public void replaceItem(int column, int row, Object item, boolean scrollToItem) {
        if (!isDragging()
                && mLists.size() > column
                && mLists.get(column).getAdapter().getItemCount() > row) {
            DragItemAdapter adapter = (DragItemAdapter) mLists.get(column).getAdapter();
            adapter.removeItem(row);
            adapter.addItem(row, item);
            if (scrollToItem) {
                scrollToItem(column, row, false);
            }
        }
    }

    public void scrollToItem(int column, int row, boolean animate) {
        if (!isDragging()
                && mLists.size() > column
                && mLists.get(column).getAdapter().getItemCount() > row) {
            mScroller.forceFinished(true);
            scrollToColumn(column, animate);
            if (animate) {
                mLists.get(column).smoothScrollToPosition(row);
            } else {
                mLists.get(column).scrollToPosition(row);
            }
        }
    }

    public void scrollToColumn(int column, boolean animate) {
        if (mLists.size() <= column) {
            return;
        }

        View parent = (View) mLists.get(column).getParent();
        int newX = 0;
        switch (mSnapPosition) {
            case LEFT:
                newX = parent.getLeft();
                break;
            case CENTER:
                newX = parent.getLeft() - (getMeasuredWidth() - parent.getMeasuredWidth()) / 2;
                break;
            case RIGHT:
                newX = parent.getRight() - getMeasuredWidth();
                break;
        }

        int maxScroll = mRootLayout.getMeasuredWidth() - getMeasuredWidth();
        newX = newX < 0 ? 0 : newX;
        newX = newX > maxScroll ? maxScroll : newX;
        if (getScrollX() != newX) {
            mScroller.forceFinished(true);
            if (animate) {
                mScroller.startScroll(getScrollX(), getScrollY(), newX - getScrollX(), 0,
                        SCROLL_ANIMATION_DURATION);
                ViewCompat.postInvalidateOnAnimation(this);
            } else {
                scrollTo(newX, getScrollY());
            }
        }
        mCurrentColumn = column;
    }

    public void clearBoard() {
        int count = mLists.size();
        for (int i = count - 1; i >= 0; i--) {
            mColumnLayout.removeViewAt(i);
            mHeaders.remove(i);
            mLists.remove(i);
            //mBorders.remove(i);
        }
    }

    public void removeColumn(int column) {
        if (column >= 0 && mLists.size() > column) {
            mColumnLayout.removeViewAt(column);
            mHeaders.remove(column);
            mLists.remove(column);
        }
    }

    public boolean isDragEnabled() {
        return mDragEnabled;
    }

    public void setDragEnabled(boolean enabled) {
        mDragEnabled = enabled;
        if (mLists.size() > 0) {
            for (DragItemRecyclerView list : mLists) {
                list.setDragEnabled(mDragEnabled);
            }
        }
    }

    /**
     * @param width the width of columns in both portrait and landscape. This must be called before
     * {@link #addColumnList} is
     * called for the width to take effect.
     */
    public void setColumnWidth(int width) {
        mColumnWidth = width;
    }

    /**
     * @param snapToColumn true if scrolling should snap to columns. Only applies to portrait mode.
     */
    public void setSnapToColumnsWhenScrolling(boolean snapToColumn) {
        mSnapToColumnWhenScrolling = snapToColumn;
    }

    /**
     * @param snapToColumn true if dragging should snap to columns when dragging towards the edge.
     * Only applies to portrait mode.
     */
    public void setSnapToColumnWhenDragging(boolean snapToColumn) {
        mSnapToColumnWhenDragging = snapToColumn;
        mAutoScroller.setAutoScrollMode(
                snapToColumnWhenDragging() ? AutoScroller.AutoScrollMode.COLUMN
                        : AutoScroller.AutoScrollMode.POSITION);
    }

    /**
     * @param snapToColumnInLandscape true if dragging should snap to columns when dragging towards
     * the edge also in landscape mode.
     */
    public void setSnapToColumnInLandscape(boolean snapToColumnInLandscape) {
        mSnapToColumnInLandscape = snapToColumnInLandscape;
        mAutoScroller.setAutoScrollMode(
                snapToColumnWhenDragging() ? AutoScroller.AutoScrollMode.COLUMN
                        : AutoScroller.AutoScrollMode.POSITION);
    }

    /**
     * @param snapPosition determines what position a column will snap to. LEFT, CENTER or RIGHT.
     */
    public void setColumnSnapPosition(ColumnSnapPosition snapPosition) {
        mSnapPosition = snapPosition;
    }

    /**
     * @param snapToTouch true if the drag item should snap to touch position when a drag is
     * started.
     */
    public void setSnapDragItemToTouch(boolean snapToTouch) {
        mDragItem.setSnapToTouch(snapToTouch);
    }

    public void setBoardListener(BoardListener listener) {
        mBoardListener = listener;
    }

    public void setBoardCallback(BoardCallback callback) {
        mBoardCallback = callback;
    }

    public void setCustomDragItem(DragItem dragItem) {
        DragItem newDragItem;
        if (dragItem != null) {
            newDragItem = dragItem;
        } else {
            newDragItem = new DragItem(getContext());
        }

        newDragItem.setSnapToTouch(mDragItem.isSnapToTouch());
        mDragItem = newDragItem;
        mRootLayout.removeViewAt(1);
        mRootLayout.addView(mDragItem.getDragItemView());
    }

    public DragItemRecyclerView addColumnList(final DragItemAdapter adapter, final View header,
            final View footer, boolean hasFixedItemSize) {
        final DragItemRecyclerView recyclerView =
                (DragItemRecyclerView) LayoutInflater.from(getContext())
                        .inflate(R.layout.drag_item_recycler_view, this, false);
        recyclerView.setHorizontalScrollBarEnabled(false);
        recyclerView.setVerticalScrollBarEnabled(false);
        recyclerView.setMotionEventSplittingEnabled(false);
        recyclerView.setDragItem(mDragItem);
        recyclerView.setLayoutParams(
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT));
        recyclerView.setLayoutManager(
                new DragLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setHasFixedSize(hasFixedItemSize);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setDragItemListener(new DragItemRecyclerView.DragItemListener() {
            @Override
            public void onDragStarted(int itemPosition, float x, float y) {
                mDragStartColumn = getColumnOfList(recyclerView);
                mDragStartRow = itemPosition;
                mCurrentRecyclerView = recyclerView;
                mDragItem.setOffset(((View) mCurrentRecyclerView.getParent()).getX(),
                        mCurrentRecyclerView.getY());
                if (mBoardListener != null) {
                    mBoardListener.onItemDragStarted(mDragStartColumn, mDragStartRow);
                }
                invalidate();
            }

            @Override
            public void onDragging(int itemPosition, float x, float y) {
                int column = getColumnOfList(recyclerView);
                boolean positionChanged = column != mLastDragColumn || itemPosition != mLastDragRow;
                if (mBoardListener != null && positionChanged) {
                    mLastDragColumn = column;
                    mLastDragRow = itemPosition;
                    mBoardListener.onItemChangedPosition(mDragStartColumn, mDragStartRow, column,
                            itemPosition);
                }
            }

            @Override
            public void onDragEnded(int newItemPosition) {
                mLastDragColumn = NO_POSITION;
                mLastDragRow = NO_POSITION;
                int mDragEndColumn = getColumnOfList(recyclerView);
                updateColumnHeight(mDragStartColumn);
                updateColumnHeight(mDragEndColumn);

                if (mBoardListener != null) {
                    mBoardListener.onItemDragEnded(mDragStartColumn, mDragStartRow, mDragEndColumn,
                            newItemPosition);
                    mActiveFeature = mDragEndColumn;
                }
            }
        });
        recyclerView.setDragItemCallback(new DragItemRecyclerView.DragItemCallback() {
            @Override
            public boolean canDragItemAtPosition(int dragPosition) {
                int column = getColumnOfList(recyclerView);
                return mBoardCallback == null || mBoardCallback.canDragItemAtPosition(column,
                        dragPosition);
            }

            @Override
            public boolean canDropItemAtPosition(int dropPosition) {
                int column = getColumnOfList(recyclerView);
                return mBoardCallback == null || mBoardCallback.canDropItemAtPosition(
                        mDragStartColumn, mDragStartRow, column, dropPosition);
            }
        });

        recyclerView.setAdapter(adapter);
        recyclerView.setDragEnabled(mDragEnabled);
        adapter.setDragStartedListener(new DragItemAdapter.DragStartCallback() {
            @Override
            public boolean startDrag(View itemView, long itemId) {
                return recyclerView.startDrag(itemView, itemId, getListTouchX(recyclerView),
                        getListTouchY(recyclerView));
            }

            @Override
            public boolean isDragging() {
                return recyclerView.isDragging();
            }
        });

        recyclerView.addOnItemTouchListener(
                new RecyclerItemTouchListener(getContext(), new DefaultItemClickListener() {
                    @Override
                    public boolean onDoubleTap(View view, int position) {
                        scale();
                        return true;
                    }

                    @Override
                    public boolean onItemClick(View view, int position) {
                        if (mBoardListener != null) {
                            mBoardListener.onItemClick(view, position,
                                    adapter.getItemList().get(position));
                        }
                        return true;
                    }
                }));

        final LinearLayout layout = (LinearLayout) createBorder();
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LayoutParams(mColumnWidth, LayoutParams.MATCH_PARENT));

        if (header != null) {
            header.setOnTouchListener(new HeaderTouchListener());
            layout.setTag(mLists.size());
            layout.addView(header);
            mHeaders.put(mLists.size(), header);
        }

        layout.addView(recyclerView,
                new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT,
                        RecyclerView.LayoutParams.WRAP_CONTENT));

        if (footer != null) {
            layout.addView(footer);
            mFooters.put(mLists.size(), footer);
            footer.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    mFooterGestureDetector.onTouchEvent(motionEvent);
                    return true;
                }
            });
        }

        mLists.add(recyclerView);

        recyclerView.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        } else {
                            recyclerView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        }
                        int top = getStatusHeight() + getActionBarHeight() + header.getHeight();
                        int screenHeight = getResources().getDisplayMetrics().heightPixels;
                        final int rvHeight = recyclerView.getHeight();
                        recyclerView.setTag(rvHeight);
                        int footerHeight = getFooterHeight();
                        int maxHeight = screenHeight - top - footerHeight;

                        if (rvHeight > maxHeight) {
                            recyclerView.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, maxHeight));
                        }
                        if (isScaled) scaleChild(layout);
                    }
                });

        mColumnLayout.addView(layout);
        return recyclerView;
    }

    public void updateColumnHeight(final int column) {
        final RecyclerView recyclerView = getRecyclerView(column);
        final View header = getHeaderView(column);

        //recyclerView.getRecycledViewPool().clear();

        recyclerView.setLayoutParams(
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        recyclerView.getAdapter().notifyDataSetChanged();

        recyclerView.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        } else {
                            recyclerView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        }
                        int top = getStatusHeight() + getActionBarHeight() + header.getHeight();
                        int screenHeight = getResources().getDisplayMetrics().heightPixels;
                        int rvHeight = recyclerView.getHeight();
                        recyclerView.setTag(rvHeight);
                        int footerHeight = getFooterHeight();
                        int maxHeight = screenHeight - top - footerHeight;
                        if (rvHeight >= maxHeight) {
                            recyclerView.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, maxHeight));
                        } else {
                            recyclerView.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, rvHeight));
                        }
                    }
                });
    }

    public void scale() {
        isScaled = !isScaled;
        //scale column and child
        scaleChild(mColumnLayout);
        //scale recycler view and child
        for (int i = 0; i < mLists.size(); i++) {
            DragItemAdapter adapter = (DragItemAdapter) mLists.get(i).getAdapter();
            adapter.scale();

            RecyclerView rv = getRecyclerView(i);
            if (rv == null || rv.getTag() == null) continue;
            int oldHeight = (int) rv.getTag();
            rv.setTag((int) Math.floor(oldHeight * ScaleUtil.getScale(isScaled)));
            updateColumnHeight(i);
        }

        //scale drag item
        mDragItem.scale(isScaled);
    }

    private void scaleChild(View view) {
        ScaleUtil.scaleViewAndChildren(view, ScaleUtil.getScale(isScaled), 0, 4);
    }

    private int getFooterHeight() {
        return SizeUtil.dpToPx(getContext(), 48);
    }

    private int getActionBarHeight() {
        int actionBarHeight = 0;
        TypedValue tv = new TypedValue();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (getContext().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,
                        getResources().getDisplayMetrics());
            }
        } else {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,
                    getResources().getDisplayMetrics());
        }
        return actionBarHeight;
    }

    private int getStatusHeight() {
        final Resources resources = getContext().getResources();
        final int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        } else {
            return (int) Math.ceil((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 24 : 25)
                    * resources.getDisplayMetrics().density);
        }
    }

    @SuppressWarnings("WeakerAccess")
    static class SavedState extends BaseSavedState {
        public int currentColumn;

        private SavedState(Parcelable superState, int currentColumn) {
            super(superState);
            this.currentColumn = currentColumn;
        }

        public SavedState(Parcel source) {
            super(source);
            currentColumn = source.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(currentColumn);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    private class FooterGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            scale();
            //return super.onDoubleTap(e);
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return super.onSingleTapConfirmed(e);
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_MIN_DISTANCE = 5;
        private static final int SWIPE_THRESHOLD_VELOCITY = 300;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (!isScaled && isPortrait()) {
                try {
                    int featureWidth = (int) (getMeasuredWidth() * 0.86);
                    //right to left
                    if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
                            && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                        mActiveFeature = (mActiveFeature < (mLists.size() - 1)) ? mActiveFeature + 1
                                : mLists.size() - 1;
                        smoothScrollTo(
                                (int) (mActiveFeature * featureWidth - (getMeasuredWidth() * 0.07)),
                                0);
                        return true;
                    }
                    //left to right
                    else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
                            && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                        mActiveFeature = (mActiveFeature > 0) ? mActiveFeature - 1 : 0;
                        smoothScrollTo(
                                (int) (mActiveFeature * featureWidth - (getMeasuredWidth() * 0.07)),
                                0);
                        return true;
                    }
                } catch (Exception e) {
                    //TODO
                }
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }

    class ColumnGestureListener extends GestureDetector.SimpleOnGestureListener {
        private View pressedView;

        public void setPressedView(View view) {
            this.pressedView = view;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            ViewGroup parentLayout = (ViewGroup) pressedView.getParent();
            mDraggingColumnView = parentLayout;
            //mDraggingColumn = getCurrentColumn(getScrollX() + e.getX());
            mDraggingColumn = (int) parentLayout.getTag();
            ClipData data = ClipData.newPlainText("", "");
            DragShadowBuilder shadowBuilder =
                    new ColumnDragShadowBuilder(parentLayout, (int) e.getX(), (int) e.getY());
            mColumnLayout.setOnDragListener(new ColumnDragListener());
            mDraggingColumnView = parentLayout;
            mDraggingColumnView.setVisibility(View.INVISIBLE);
            mColumnLayout.startDrag(data, shadowBuilder, null, 0);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            scale();
            return super.onDoubleTap(e);
        }
    }

    class ColumnDragShadowBuilder extends DragShadowBuilder {
        int touchPointXCoord, touchPointYCoord;
        View view;
        final int width;
        final int height;
        final float rotationRad = 5;
        final float scale = 0.9f;
        final int extra = 100; // extra space to avoid clip shadow

        public ColumnDragShadowBuilder(View view, int touchPointXCoord, int touchPointYCoord) {
            super(view);
            this.touchPointXCoord = touchPointXCoord;
            this.touchPointYCoord = touchPointYCoord;

            this.view = view;
            int w = (int) (view.getWidth() * view.getScaleX());
            int h = (int) (view.getHeight() * view.getScaleY());
            double s = Math.abs(Math.sin(rotationRad));
            double c = Math.abs(Math.cos(rotationRad));
            width = (int) (w * c + h * s);
            height = (int) (w * s + h * c);
        }

        @Override
        public void onProvideShadowMetrics(Point shadowSize, Point shadowTouchPoint) {
            super.onProvideShadowMetrics(shadowSize, shadowTouchPoint);

            shadowSize.set(view.getWidth() + extra, view.getHeight() + extra);
            shadowTouchPoint.set(touchPointXCoord, touchPointYCoord);
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            canvas.scale(scale, scale, width / 2, height / 2);
            canvas.rotate(rotationRad, width / 2, height / 2);
            super.onDrawShadow(canvas);
        }
    }

    final class HeaderTouchListener implements OnTouchListener {
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mColumnGestureListener.setPressedView(view);
            mColumnGestureDetector.onTouchEvent(motionEvent);
            return true;
        }
    }

    final class ColumnDragListener implements OnDragListener {
        private int mLastColumn = 0;
        private float mNewXOnScreen = 0;
        private boolean mDragEnded = true;
        private boolean mShouldScrollList = true;
        private final int delayBeforeNextScroll =
                Math.max(PAUSE_COLUMN_SCROLL_DURATION, SCROLL_ANIMATION_DURATION);

        public void onDragging() {
            if (mShouldScrollList) {
                int newColumn = this.nextColumnByScreenX(mNewXOnScreen);
                if (newColumn != mLastColumn) {
                    Handler handler = new Handler();
                    scrollToColumn(newColumn, true);
                    if (newColumn < mLists.size() - 1) {
                        swapColumn(mLastColumn, newColumn);
                        mLastColumn = newColumn;
                        mShouldScrollList = false;
                        final ColumnDragListener self = this;

                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!mDragEnded) {
                                    mShouldScrollList = true;

                                    self.onDragging();
                                }
                            }
                        }, delayBeforeNextScroll);
                    }
                }
            }
        }

        protected int nextColumnByScreenX(float newX) {
            int newColumn = mLastColumn;
            if (newX < mColumnWidth * 0.4f) {
                newColumn = mLastColumn - 1;
            } else if (newX > mColumnWidth * 0.6f) {
                newColumn = mLastColumn + 1;
            }
            if (newColumn < 0 || newColumn > mLists.size() - 1) {
                newColumn = mLastColumn;
            }
            return newColumn;
        }

        @Override
        public boolean onDrag(View v, DragEvent event) {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    mLastColumn = mDraggingColumn;
                    mDragEnded = false;
                    // minhtd: update duration
                    SCROLL_ANIMATION_DURATION = SCROLL_ANIMATION_DURATION_DRAG;
                    scrollToColumn(mLastColumn, false);
                    break;
                case DragEvent.ACTION_DRAG_LOCATION:
                    mNewXOnScreen = event.getX() - getScrollX();
                    this.onDragging();
                    break;
                case DragEvent.ACTION_DROP:
                    if (mDraggingColumn != mLastColumn) {
                        if (mBoardListener != null) {
                            mBoardListener.onColumnPositionChanged(mDraggingColumn, mLastColumn);
                        }
                    }
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    mDraggingColumnView.setVisibility(VISIBLE);
                    SCROLL_ANIMATION_DURATION = SCROLL_ANIMATION_DURATION_DEFAULT;
                    for (int i = 0; i < mColumnLayout.getChildCount(); i++) {
                        mColumnLayout.getChildAt(i).setTag(i);
                    }
                    mDragEnded = true;
                    mActiveFeature = mLastColumn;
                default:
                    break;
            }
            return true;
        }
    }

    public void swapColumn(int firstColumn, int secondColumn) {
        if (firstColumn > secondColumn) {
            int tmpColumn = firstColumn;
            firstColumn = secondColumn;
            secondColumn = tmpColumn;
        }

        View firstView = mColumnLayout.getChildAt(firstColumn);
        View secondView = mColumnLayout.getChildAt(secondColumn);
        if (firstView == null || secondView == null) {
            return;
        }

        mColumnLayout.removeView(firstView);
        mColumnLayout.removeView(secondView);
        mColumnLayout.addView(secondView, firstColumn);
        mColumnLayout.addView(firstView, secondColumn);
        DragItemRecyclerView firstItem = mLists.remove(firstColumn);
        DragItemRecyclerView secondItem = mLists.remove(secondColumn - 1);
        mLists.add(firstColumn, secondItem);
        mLists.add(secondColumn, firstItem);

        View firstHeader = mHeaders.get(firstColumn);
        View secondHeader = mHeaders.get(secondColumn);
        mHeaders.put(firstColumn, secondHeader);
        mHeaders.put(secondColumn, firstHeader);

        View firstFooter = mFooters.get(firstColumn);
        View secondFooter = mFooters.get(secondColumn);
        mFooters.put(firstColumn, secondFooter);
        mFooters.put(secondColumn, firstFooter);

        //View firstBorder = mBorders.get(firstColumn);
        //View secondBorder = mBorders.get(secondColumn);
        //mBorders.put(firstColumn, secondBorder);
        //mBorders.put(secondColumn, firstBorder);
    }

    private boolean isPortrait() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    private View createBorder() {
        final LinearLayout border = new LinearLayout(getContext());
        RelativeLayout.LayoutParams params =
                new RelativeLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            border.setBackground(getResources().getDrawable(R.drawable.border_edit));
        } else {
            border.setBackgroundDrawable(getResources().getDrawable(R.drawable.border_edit));
        }

        ((GradientDrawable) border.getBackground()).setColor(Color.parseColor("#FAFAFA"));
        GradientDrawable gd = (GradientDrawable) border.getBackground();
        gd.setStroke(2, Color.parseColor("#FAFAFA"));
        if (mLists.size() == 0) {
            params.setMargins((int) ((getMeasuredWidth() * 0.14) / 2), 0, 0, 0);
        } else {
            params.setMargins(MARGIN_SMALL, MARGIN_LARGE, MARGIN_SMALL, MARGIN_SMALL);
        }
        border.setLayoutParams(params);
        return border;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        for (int i = 0; i < mLists.size(); i++) {
            RecyclerView rv = getRecyclerView(i);
            if (rv == null || rv.getTag() == null) continue;
            int oldHeight = (int) rv.getTag();
            rv.setTag((int) Math.floor(oldHeight * ScaleUtil.getScale(isScaled)));
            updateColumnHeight(i);
        }
    }
}
