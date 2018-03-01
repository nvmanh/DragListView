package com.woxthebox.draglistview.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Class that is responsible for handling item touch events.
 * @author thesurix
 */
public class RecyclerItemTouchListener extends RecyclerView.SimpleOnItemTouchListener {

    /**
     * The listener that is used to notify when a tap, long press or double tap occur.
     */
    public interface ItemClickListener {

        /**
         * Called when a tap occurs on a specified item.
         * @param view pressed view
         * @param position item's position
         * @return true if the event is consumed, else false
         */
        boolean onItemClick(View view, int position);

        /**
         * Called when a long press occurs on a specified item.
         * @param view pressed view
         * @param position item's position
         */
        void onItemLongPress(View view, int position);

        /**
         * Called when a double tap occurs on a specified item.
         * @param view tapped view
         * @param position item's position
         * @return true if the event is consumed, else false
         */
        boolean onDoubleTap(View view, int position);
    }

    private final GestureDetector mGestureDetector;
    private final GestureClickListener mGestureClickListener;

    /**
     * Constructs {@link RecyclerView} touch listener.
     * @param ctx context
     * @param listener listener for item's click events
     */
    public RecyclerItemTouchListener(@NonNull final Context ctx, @NonNull final ItemClickListener listener) {
        mGestureClickListener  = new GestureClickListener(listener);
        mGestureDetector = new GestureDetector(ctx, mGestureClickListener);
    }

    @Override
    public boolean onInterceptTouchEvent(final RecyclerView view, final MotionEvent e) {
        final View childView = view.findChildViewUnder(e.getX(), e.getY());
        if (childView != null) {
            final int childPosition = view.getChildPosition(childView);
            mGestureClickListener.setTouchedView(childView, childPosition);
            return mGestureDetector.onTouchEvent(e);
        }
        return false;
    }

    private static class GestureClickListener extends GestureDetector.SimpleOnGestureListener {

        private ItemClickListener listener;

        private View view;
        private int viewPosition;

        public GestureClickListener(final ItemClickListener listener) {
            this.listener = listener;
        }

        @Override
        public boolean onSingleTapConfirmed(final MotionEvent e) {
            return listener.onItemClick(view, viewPosition);
        }

        @Override
        public void onLongPress(final MotionEvent e) {
            listener.onItemLongPress(view, viewPosition);
        }

        @Override
        public boolean onDoubleTap(final MotionEvent e) {
            return listener.onDoubleTap(view, viewPosition);
        }

        void setTouchedView(final View view, final int viewPosition) {
            this.view = view;
            this.viewPosition = viewPosition;
        }
    }
}
