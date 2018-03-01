package com.woxthebox.draglistview;

import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by FRAMGIA\nguyen.viet.manh on 2/28/18.
 */

public class GestureView extends ViewGroup {

    private GestureDetector mGestureDetector;
    private boolean isInit;

    private GestureDetector.SimpleOnGestureListener mGestureListener;

    public GestureView(Context context) {
        super(context);
        init();
    }

    public GestureView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GestureView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public GestureView(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    @Override
    protected void onLayout(boolean b, int i, int i1, int i2, int i3) {
    }

    private void init() {
        if (isInit) return;
        isInit = true;
        //mGestureDetector = new GestureDetector(getContext(), new GestureListener());
    }

    //@Override
    //public boolean onInterceptTouchEvent(MotionEvent ev) {
    //    return mGestureDetector.onTouchEvent(ev);
    //    //return super.onInterceptTouchEvent(ev);
    //}

    //@Override
    //public boolean onTouchEvent(MotionEvent e) {
    //    return mGestureDetector.onTouchEvent(e);
    //    //return super.onTouchEvent(e);
    //}

    public GestureDetector.SimpleOnGestureListener getGestureListener() {
        return mGestureListener;
    }

    public void setGestureListener(GestureDetector.SimpleOnGestureListener gestureListener) {
        mGestureListener = gestureListener;
    }

    //public class GestureListener extends GestureDetector.SimpleOnGestureListener {
    //
    //    @Override
    //    public boolean onDoubleTap(MotionEvent e) {
    //        Log.i("GestureListener", "onDoubleTap: -------------->" + 3);
    //        if (getGestureListener() != null) {
    //            return getGestureListener().onDoubleTap(e);
    //        }
    //        return super.onDoubleTap(e);
    //    }
    //
    //    @Override
    //    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
    //        if(getGestureListener()!=null){
    //            return getGestureListener().onFling(e1, e2, velocityX, velocityY);
    //        }
    //        return super.onFling(e1, e2, velocityX, velocityY);
    //    }
    //
    //    @Override
    //    public boolean onSingleTapConfirmed(MotionEvent e) {
    //        Log.i("GestureListener", "onSingleTapConfirmed: -------------->" + 1);
    //        if (getGestureListener() != null) {
    //            return getGestureListener().onSingleTapConfirmed(e);
    //        }
    //        return super.onSingleTapConfirmed(e);
    //    }
    //
    //    @Override
    //    public boolean onDown(MotionEvent e) {
    //        Log.i("GestureListener", "onDown: -------------->" + 2);
    //        if (getGestureListener() != null) {
    //            return getGestureListener().onDown(e);
    //        }
    //        return super.onDown(e);
    //    }
    //}
}
