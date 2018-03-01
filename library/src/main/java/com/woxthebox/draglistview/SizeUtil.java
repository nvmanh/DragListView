package com.woxthebox.draglistview;

import android.content.Context;
import android.util.TypedValue;

/**
 * Created by FRAMGIA\nguyen.viet.manh on 2/28/18.
 */

public class SizeUtil {
    public static int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }

    public static int pxToDp(Context context, int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, px,
                context.getResources().getDisplayMetrics());
    }

    public static int dpResToPx(Context context, int resId) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                context.getResources().getDimension(resId),
                context.getResources().getDisplayMetrics());
    }

    public static int pxToSp(Context context, float px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, px,
                context.getResources().getDisplayMetrics());
    }
}
