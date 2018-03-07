package com.shrreya.instagrambanner.activities;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.RelativeLayout;

import com.shrreya.instagrambanner.tasks.BreakPhotosTask;
import com.shrreya.instagrambanner.models.BreakPhotosTaskParams;
import com.shrreya.instagrambanner.views.CameraGridView;
import com.shrreya.instagrambanner.models.Constants;
import com.shrreya.instagrambanner.R;
import com.shrreya.instagrambanner.views.ScrollableImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PhotoEditActivity extends AppCompatActivity {

    @BindView(R.id.photo_view) RelativeLayout mPhotoView;
    @BindView(R.id.top_bar) RelativeLayout mTopBar;
    @BindView(R.id.bottom_bar) RelativeLayout mBottomBar;
    @BindView(R.id.flexible_grid_view) RelativeLayout mFlexibleGridView;

    private ScrollableImageView mScrollableImageView;
    private CameraGridView mCameraGridView;
    private Bitmap mFullPhoto;
    private int mPanels = Constants.DEFAULT_PANELS, mPhotoHeight, mPanelHeight, mScreenWidth, mScreenHeight;

    private final String LOG_TAG = "PhotoEditActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_edit);
        ButterKnife.bind(this);

        // Initialize scrollable image view.
        mScrollableImageView = new ScrollableImageView(this);

        // Initialize bitmap options.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        // Get screen dimensions.
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mScreenHeight = displayMetrics.heightPixels;
        mScreenWidth = displayMetrics.widthPixels;

        // Check if photo is captured from camera or picked from library.
        Bundle extras = getIntent().getExtras();
        Boolean isNewPhoto = extras.getBoolean(Constants.NEW_PHOTO);

        // Initialize bitmap.
        Bitmap photo = null;

        // If photo is newly captured from camera.
        if(isNewPhoto) {
            // Retrieve and check if photo file exists.
            String photoPath = Environment.getExternalStorageDirectory() + File.separator + Constants.photoFileName;
            File photoFile = new File(photoPath);
            if (photoFile.exists()) {
                // Create full size bitmap from file and rotate according to orientation.
                photo = BitmapFactory.decodeFile(photoFile.getAbsolutePath(), options);

                try {

                    // Check orientation of photo.
                    ExifInterface exif = new ExifInterface(photoPath);
                    int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    int rotationInDegrees = exifToDegrees(rotation);

                    // If photo has rotation angle, perform bitmap rotation operation.
                    if(rotationInDegrees != 0) {
                        Bitmap rotated = rotatePhoto(rotationInDegrees, photo);
                        photo.recycle();
                        photo = rotated;
                    }

                } catch(IOException e) {
                    Log.e(LOG_TAG, "Exception in retrieving photo from URI: " + e.getMessage());
                }


                // Delete file after creating bitmap.
                photoFile.delete();
            } else {
                Log.e(LOG_TAG, "Photo file not found!");
            }
        }

        // If photo is picked from library.
        else {
            // Retrieve photo URI.
            Uri photoUri = Uri.parse(extras.getString(Constants.PHOTO_URI));
            String photoPath = getRealPathFromURI(photoUri);
            try {
                photo = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);

                // Check orientation of photo.
                ExifInterface exif = new ExifInterface(photoPath);
                int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                int rotationInDegrees = exifToDegrees(rotation);

                // If photo has rotation angle, perform bitmap rotation operation.
                if(rotationInDegrees != 0) {
                    Bitmap rotated = rotatePhoto(rotationInDegrees, photo);
                    photo.recycle();
                    photo = rotated;
                }

            } catch (IOException e) {
                Log.e(LOG_TAG, "Exception in retrieving photo from URI: " + e.getMessage());
            }
        }

        // Get full photo after scaling.
        mFullPhoto = scalePhoto(photo);

        // Calculate panel height from photo view height which must be equal to screen width.
        mPhotoHeight = mScreenWidth;
        mPanelHeight = mPhotoHeight / Constants.DEFAULT_PANELS;

        // Set initial flexible grid view with default panels.
        mCameraGridView = new CameraGridView(this, mPhotoHeight);
        mCameraGridView.setPanels(mPanels);
        mFlexibleGridView.addView(mCameraGridView);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Set photo view according to number of panels chosen.
        setPhotoView(mPanelHeight, mPanels);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // Set the height of the bars so that they align with the full square photo view.
        RelativeLayout.LayoutParams topBarLayoutParams = (RelativeLayout.LayoutParams) mTopBar.getLayoutParams();
        RelativeLayout.LayoutParams bottomBarLayoutParams = (RelativeLayout.LayoutParams) mBottomBar.getLayoutParams();
        int barHeight = (mScreenHeight - mPhotoHeight) / 2;
        topBarLayoutParams.height = barHeight;
        bottomBarLayoutParams.height = barHeight;
        mTopBar.setLayoutParams(topBarLayoutParams);
        mBottomBar.setLayoutParams(bottomBarLayoutParams);

        // Reset flexible grid view according to number of panels chosen.
        setFlexibleGridView(mPanels);
    }

    /* Helper function to get file path from URI. */
    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) {
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    /* Helper function to retrieve rotation angle of photo in degrees. */
    private int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90)
            return 90;
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180)
            return 180;
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270)
            return 270;
        return 0;
    }

    /* Helper function to rotate photo by required angle in degrees. */
    private Bitmap rotatePhoto(int rotationInDegrees, Bitmap photo) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotationInDegrees);
        return Bitmap.createBitmap(photo, 0, 0, photo.getWidth(), photo.getHeight(), matrix, true);
    }

    /* Helper function to scale photo appropriately. */
    private Bitmap scalePhoto(Bitmap photo) {
        Bitmap scaled = null;
        if(photo != null) {
            int photoWidth = photo.getWidth(), photoHeight = photo.getHeight();
            int targetWidth, targetHeight;

            // Scale full size bitmap to screen size for landscape mode photo.
            if (photoWidth > photoHeight) {
                targetHeight = mScreenWidth;
                Float aspectRatio = photoWidth/ (float) photoHeight;
                targetWidth = (int) (aspectRatio * targetHeight);
            }
            // Scale full size bitmap to screen size for portrait mode photo.
            else if (photoWidth < photoHeight) {
                targetWidth = mScreenWidth;
                Float aspectRatio = photoHeight/ (float) photoWidth;
                targetHeight = (int) (aspectRatio * targetWidth);
            }
            // Scale full size bitmap to screen size for square mode photo.
            else {
                targetWidth = mScreenWidth;
                targetHeight = mScreenWidth;
            }

            scaled = Bitmap.createScaledBitmap(photo, targetWidth, targetHeight, true);

        }
        return scaled;
    }

    /* Helper function to set photo view according to number of panels chosen. */
    private void setPhotoView(int panelHeight, int panels) {

        // Remove previously set photo if any.
        if(mScrollableImageView.getParent() != null)
            mPhotoView.removeViewAt(0);

        // Set new photo view size with chosen number of panels.
        int photoSize = panelHeight * panels;
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mPhotoView.getLayoutParams();
        params.height = photoSize;
        params.width = mScreenWidth;
        mPhotoView.setLayoutParams(params);

        // Set bitmap on new scrollable image view and add to photo view layout.
        mScrollableImageView = new ScrollableImageView(this);
        mScrollableImageView.setImage(mFullPhoto);
        mPhotoView.addView(mScrollableImageView);
    }

    /* Helper function to set flexible grid view according to number of panels chosen. */
    private void setFlexibleGridView(int panels) {
        mCameraGridView.setPanels(panels);
        mCameraGridView.invalidate();
    }

    /* Function to handle click on grid button. */
    @OnClick(R.id.grid_button)
    public void changeGrid() {
        // Increase or reduce panels.
        if(mPanels > 1)
            mPanels -= 1;
        else
            mPanels = 3;

        // Reset photo view and flexible grid view.
        setPhotoView(mPanelHeight, mPanels);
        setFlexibleGridView(mPanels);
    }

    /* Function to handle click on go to upload activity button. */
    @OnClick(R.id.go_to_upload_activity_button)
    public void breakPhotos() {
        // Get start coordinates of edited scrollable image and create bitmap of appropriate size.
        int x = 0 - Math.round(mScrollableImageView.getX());
        int y = 0 - Math.round(mScrollableImageView.getY());
        Bitmap finalPicture = Bitmap.createBitmap(mFullPhoto, x, y, mScreenWidth, mPanelHeight * mPanels);

        // Compress bitmap to bytes for processing by asynctask.
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        finalPicture.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] photo = stream.toByteArray();

        // Launch async task to break and save photos in background. Start Upload Activity when task completed.
        BreakPhotosTaskParams params = new BreakPhotosTaskParams(photo, mPanels, mPanelHeight);
        BreakPhotosTask task = new BreakPhotosTask(this, new BreakPhotosTaskResponse() {
            @Override
            public void onBreakComplete() {
                startUploadActivity();
            }
        });
        task.execute(params);
    }

    /* Interface for asynctask response. */
    public interface BreakPhotosTaskResponse {
        void onBreakComplete();
    }

    /* Helper function to start upload activity with intent. */
    public void startUploadActivity() {
        Intent intent = new Intent(this, UploadActivity.class);
        intent.putExtra(Constants.PANELS, mPanels);
        intent.putExtra(Constants.PANEL_LENGTH, mPanelHeight);
        startActivity(intent);
    }

    /* Function to handle click on custom back button of activity. */
    @OnClick(R.id.back_to_main_activity_button)
    public void backToMainActivity() {
        super.onBackPressed();
    }

    /* Function to handle click on rotate button. */
    @OnClick(R.id.rotate_button)
    public void performRotate() {
        mFullPhoto = rotatePhoto(Constants.ROTATE_LEFT, mFullPhoto);
        setPhotoView(mPanelHeight, mPanels);
    }
}
