package com.shrreya.instagrambanner.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.shrreya.instagrambanner.models.Constants;

public class ScrollableImageView extends View {

    private Display mDisplay;
    private Bitmap mImage;

    /* Current x and y of the touch */
    private float mCurrentX = 0;
    private float mCurrentY = 0;

    /* Calculated difference */
    private float mTotalX = 0;
    private float mTotalY = 0;

    /* The touch distance change from the current touch */
    private float mDeltaX = 0;
    private float mDeltaY = 0;

    private int mPadding;

    private Paint paint = new Paint();

    public ScrollableImageView(Context context) {
        super(context);
        initScrollImageView(context);
    }

    public ScrollableImageView(Context context, AttributeSet attributeSet) {
        super(context);
        initScrollImageView(context);
    }

    private void initScrollImageView(Context context) {
        mDisplay = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        mPadding = Constants.DEFAULT_PADDING;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = measureDimensions(widthMeasureSpec, mDisplay.getWidth());
        int height = measureDimensions(heightMeasureSpec, mDisplay.getHeight());
        setMeasuredDimension(width, height);
    }

    private int measureDimensions(int measureSpec, int size) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = size;
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    public Bitmap getImage() {
        return mImage;
    }

    public void setImage(Bitmap image) {
        mImage = image;
    }

    public int getPadding() {
        return mPadding;
    }

    public void setPadding(int padding) {
        mPadding = padding;
    }

    public float getX() {
        return mTotalX;
    }

    public float getY() {
        return mTotalY;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        // Get coordinates of touch event.
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mCurrentX = event.getRawX();
            mCurrentY = event.getRawY();
        }
        else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float x = event.getRawX();
            float y = event.getRawY();

            mDeltaX = x - mCurrentX;
            mDeltaY = y - mCurrentY;

            mCurrentX = x;
            mCurrentY = y;

            invalidate();
        }

        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (mImage == null) {
            return;
        }

        // Scroll horizontally.
        float newTotalX = mTotalX + mDeltaX;
        if (mPadding > newTotalX && newTotalX > getMeasuredWidth() - mImage.getWidth() - mPadding)
            mTotalX += mDeltaX;

        // Scroll vertically.
        float newTotalY = mTotalY + mDeltaY;
        if (mPadding > newTotalY && newTotalY > getMeasuredHeight() - mImage.getHeight() - mPadding)
            mTotalY += mDeltaY;

        canvas.drawBitmap(mImage, mTotalX, mTotalY, paint);
    }
}
