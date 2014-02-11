/*************************************************************************

Copyright 2014 MagicMod Project

This file is part of MagicMod Weather.

MagicMod Weather is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

MagicMod Weather is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with MagicMod Weather. If not, see <http://www.gnu.org/licenses/>.

*************************************************************************/

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
