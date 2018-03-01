package com.woxthebox.draglistview.utils;

import android.view.View;

/**
 * Default implementation of the {@link RecyclerItemTouchListener.ItemClickListener}.
 * @author thesurix
 */
public class DefaultItemClickListener implements RecyclerItemTouchListener.ItemClickListener {

    @Override
    public boolean onItemClick(final View view, final int position) {
        return false;
    }

    @Override
    public void onItemLongPress(final View view, final int position) {
    }

    @Override
    public boolean onDoubleTap(final View view, final int position) {
        return false;
    }
}
