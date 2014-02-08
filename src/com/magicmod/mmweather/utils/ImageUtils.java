package com.magicmod.mmweather.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

public class ImageUtils {

    public static Drawable resizeDrawable(Context context, Drawable image, int targetSize) {
        if (image == null || context == null) {
            return null;
        }

        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, targetSize, context
                .getResources().getDisplayMetrics());
        
        Bitmap from;
        if (image instanceof BitmapDrawable) {
            from = ((BitmapDrawable) image).getBitmap();
        } else {
            from = Bitmap.createBitmap(image.getIntrinsicWidth(), image.getIntrinsicHeight(),
                    Bitmap.Config.ARGB_8888);
        }
        Bitmap to = Bitmap.createScaledBitmap(from, px, px, true);
        return new BitmapDrawable(context.getResources(), to);
    }
    
    public static Bitmap resizeBitmap(Context context, Drawable image, int targetSize) {
        if (image == null || context == null) {
            return null;
        }
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, targetSize, context
                .getResources().getDisplayMetrics());
        
        Bitmap from;
        if (image instanceof BitmapDrawable) {
            from = ((BitmapDrawable) image).getBitmap();
        } else {
            from = Bitmap.createBitmap(image.getIntrinsicWidth(), image.getIntrinsicHeight(),
                    Bitmap.Config.ARGB_8888);
        }
        return Bitmap.createScaledBitmap(from, px, px, true);
    }
}
