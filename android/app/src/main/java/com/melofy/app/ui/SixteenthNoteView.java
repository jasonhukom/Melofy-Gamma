package com.melofy.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class SixteenthNoteView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int tintColor = Color.WHITE;

    public SixteenthNoteView(Context context) {
        super(context);
        init();
    }

    public SixteenthNoteView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SixteenthNoteView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setStyle(Paint.Style.FILL);
    }

    public void setTintColor(int color) {
        this.tintColor = color;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        paint.setColor(tintColor);

        // Note 1 (Left)
        float note1CenterX = w * 0.28f;
        float note1CenterY = h * 0.78f;
        float note1Radius = w * 0.12f;

        // Note 2 (Right)
        float note2CenterX = w * 0.72f;
        float note2CenterY = h * 0.68f;
        float note2Radius = w * 0.12f;

        // Draw note heads
        canvas.drawCircle(note1CenterX, note1CenterY, note1Radius, paint);
        canvas.drawCircle(note2CenterX, note2CenterY, note2Radius, paint);

        // Stems
        float strokeWidth = w * 0.08f;
        float stem1X = note1CenterX + note1Radius * 0.7f;
        float stem2X = note2CenterX + note2Radius * 0.7f;

        float stem1TopY = h * 0.22f;
        float stem2TopY = h * 0.12f;

        // Left Stem
        canvas.drawRect(stem1X - strokeWidth / 2f, stem1TopY, stem1X + strokeWidth / 2f, note1CenterY, paint);

        // Right Stem
        canvas.drawRect(stem2X - strokeWidth / 2f, stem2TopY, stem2X + strokeWidth / 2f, note2CenterY, paint);

        // Top Slanted Beam 1
        Path beam1Path = new Path();
        beam1Path.moveTo(stem1X - strokeWidth / 2f, stem1TopY);
        beam1Path.lineTo(stem2X + strokeWidth / 2f, stem2TopY);
        beam1Path.lineTo(stem2X + strokeWidth / 2f, stem2TopY + h * 0.09f);
        beam1Path.lineTo(stem1X - strokeWidth / 2f, stem1TopY + h * 0.09f);
        beam1Path.close();
        canvas.drawPath(beam1Path, paint);

        // Second Slanted Beam 2
        float beam2Offset = h * 0.13f;
        Path beam2Path = new Path();
        beam2Path.moveTo(stem1X - strokeWidth / 2f, stem1TopY + beam2Offset);
        beam2Path.lineTo(stem2X + strokeWidth / 2f, stem2TopY + beam2Offset);
        beam2Path.lineTo(stem2X + strokeWidth / 2f, stem2TopY + beam2Offset + h * 0.08f);
        beam2Path.lineTo(stem1X - strokeWidth / 2f, stem1TopY + beam2Offset + h * 0.08f);
        beam2Path.close();
        canvas.drawPath(beam2Path, paint);
    }
}
