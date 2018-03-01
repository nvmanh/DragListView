package com.woxthebox.draglistview.utils;

import android.view.View;

/**
 * Created by root on 3/24/17.
 */

public interface SimpleItemGestureListener {
    boolean onSingleTapItem(View view, int position, Object object);

    boolean onDoubleTapItem(View view, int position, Object object);
}
