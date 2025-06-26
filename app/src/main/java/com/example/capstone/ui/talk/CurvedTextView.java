package com.example.capstone.ui.talk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class CurvedTextView extends View {
    private Paint paint;
    private Path path;
    private String text = "Press and hold to speak!";

    public CurvedTextView(Context context) {
        super(context);
        init();
    }

    public CurvedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CurvedTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(getResources().getColor(android.R.color.darker_gray));
        paint.setTextSize(48);
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setLetterSpacing(0.1f);

        path = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        path.reset();
        float radius = Math.min(w, h) / 2f;
        path.addArc(w / 4f, h / 2f - radius / 2, 3 * w / 4f, h / 2f + radius / 2, 180, 180);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawTextOnPath(text, path, 0, 0, paint);
    }

    public void setText(String text) {
        this.text = text;
        invalidate();
    }
}