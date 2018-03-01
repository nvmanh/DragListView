package com.woxthebox.draglistview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by nus on 10/26/16.
 */

public class ScaleUtil {

    private static final float SCALE_SMALL = 0.5f;
    private static final float SCALE_NORMAL = 2.0f;

    public static float pixelsToSp(Context context, float px) {
        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        return px / scaledDensity;
    }

    public static void scaleViewAndChildren(View root, float scale, int start, int end) {
        if (start > end) return;
        // Retrieve the view's layout information
        ViewGroup.LayoutParams layoutParams = root.getLayoutParams();

        // Scale the View itself
        if (layoutParams.width != ViewGroup.LayoutParams.MATCH_PARENT
                && layoutParams.width != ViewGroup.LayoutParams.WRAP_CONTENT) {
            layoutParams.width *= scale;
        }
        if (layoutParams.height != ViewGroup.LayoutParams.MATCH_PARENT
                && layoutParams.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
            layoutParams.height *= scale;
        }
        // If the View has margins, scale those too
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) layoutParams;
            marginParams.leftMargin *= scale;
            marginParams.topMargin *= scale;
            marginParams.rightMargin *= scale;
            marginParams.bottomMargin *= scale;
        }
        root.setLayoutParams(layoutParams);
        // Same treatment for padding
        root.setPadding((int) (root.getPaddingLeft() * scale), (int) (root.getPaddingTop() * scale),
                (int) (root.getPaddingRight() * scale), (int) (root.getPaddingBottom() * scale));

        if (root instanceof TextView) {
            //TextVtTextSize(tv.getTextSize() * scale);
            scaleTextView((TextView) root, scale);
        }

        if (root instanceof ImageView) {
            scaleImageView((ImageView) root, scale);
        }

        // If it's a ViewGroup, recurse!
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                scaleViewAndChildren(vg.getChildAt(i), scale, start + 1, end);
            }
        }
    }

    public static void scaleTextView(TextView textView, float scale) {
        float newSize = pixelsToSp(textView.getContext(), textView.getTextSize()) * scale;
        textView.setTextSize(newSize); //< We do NOT want to do this.
    }

    private static void scaleImageView(ImageView imageView, float scale) {
        Drawable drawing = imageView.getDrawable();
        Bitmap bitmap = ((BitmapDrawable) drawing).getBitmap();
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        BitmapDrawable result = new BitmapDrawable(scaledBitmap);

        imageView.setImageDrawable(result);
    }

    // Scale the given view, its contents, and all of its children by the given factor.
    public static void scaleViewAndChildren(View root, float scale, int canary) {
        // Retrieve the view's layout information
        ViewGroup.LayoutParams layoutParams = root.getLayoutParams();

        // Scale the View itself
        if (layoutParams.width != ViewGroup.LayoutParams.MATCH_PARENT
                && layoutParams.width != ViewGroup.LayoutParams.WRAP_CONTENT) {
            layoutParams.width *= scale;
        }
        if (layoutParams.height != ViewGroup.LayoutParams.MATCH_PARENT
                && layoutParams.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
            layoutParams.height *= scale;
        }
        // If the View has margins, scale those too
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) layoutParams;
            marginParams.leftMargin *= scale;
            marginParams.topMargin *= scale;
            marginParams.rightMargin *= scale;
            marginParams.bottomMargin *= scale;
        }
        root.setLayoutParams(layoutParams);
        // Same treatment for padding
        root.setPadding((int) (root.getPaddingLeft() * scale), (int) (root.getPaddingTop() * scale),
                (int) (root.getPaddingRight() * scale), (int) (root.getPaddingBottom() * scale));

        if (root instanceof TextView) {
            scaleTextView((TextView) root, scale);
        }

        if (root instanceof ImageView) {
            scaleImageView((ImageView) root, scale);
        }

        // If it's a ViewGroup, recurse!
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                scaleViewAndChildren(vg.getChildAt(i), scale, canary + 1);
            }
        }
    }

    public static float getScale(boolean isScaled) {
        return isScaled ? SCALE_SMALL : SCALE_NORMAL;
    }
}
