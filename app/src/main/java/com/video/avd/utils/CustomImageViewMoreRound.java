package com.video.avd.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;

/**
 * Created by nitinagarwal on 3/24/17.
 */

public class CustomImageViewMoreRound extends androidx.appcompat.widget.AppCompatImageView {

    public static float radius = 30.0f;

    public CustomImageViewMoreRound(Context context) {
        super(context);
    }

    public CustomImageViewMoreRound(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomImageViewMoreRound(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Path clipPath = new Path();
        RectF rect = new RectF(0, 0, this.getWidth(), this.getHeight());
        clipPath.addRoundRect(rect, radius, radius, Path.Direction.CW);
        canvas.clipPath(clipPath);
        super.onDraw(canvas);
    }
}
