package com.shrreya.instagrambanner.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import com.shrreya.instagrambanner.models.Constants;

public class CameraGridView extends View {

    private int mSquareLength, mPanelLength, mPanels;
    private Paint mPaint;

    public CameraGridView(Context context, int squareLength) {
        super(context);
        mSquareLength = squareLength;
        mPanelLength = squareLength / Constants.DEFAULT_PANELS;

        //  Set Paint options.
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(5);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.argb(255, 255, 255, 255));
    }

    public void setPanels(int panels) {
        mPanels = panels;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        // Draw grid lines according to number of panels.
        switch(mPanels) {
            case 1: {
                // Draw two vertical lines of single panel length.
                canvas.drawLine(mPanelLength, 0, mPanelLength, mPanelLength, mPaint);
                canvas.drawLine(mPanelLength * 2, 0, mPanelLength * 2, mPanelLength, mPaint);
                break;
            }
            case 2: {
                // Draw two vertical lines of double panel length.
                canvas.drawLine(mPanelLength, 0, mPanelLength, mPanelLength * 2, mPaint);
                canvas.drawLine(mPanelLength * 2, 0, mPanelLength * 2, mPanelLength * 2, mPaint);
                // Draw one horizontal line of full square length.
                canvas.drawLine(0, mPanelLength, mSquareLength, mPanelLength, mPaint);
                break;
            }
            case 3: {
                // Draw two vertical lines of full square length.
                canvas.drawLine(mPanelLength, 0, mPanelLength, mSquareLength, mPaint);
                canvas.drawLine(mPanelLength * 2, 0, mPanelLength * 2, mSquareLength, mPaint);
                // Draw two horizontal lines of full square length.
                canvas.drawLine(0, mPanelLength, mSquareLength, mPanelLength, mPaint);
                canvas.drawLine(0, mPanelLength * 2, mSquareLength, mPanelLength * 2, mPaint);
                break;
            }
        }
    }
}
