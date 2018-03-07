package com.shrreya.instagrambanner.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.shrreya.instagrambanner.views.CameraGridView;
import com.shrreya.instagrambanner.views.CameraPreview;
import com.shrreya.instagrambanner.models.Constants;
import com.shrreya.instagrambanner.R;
import com.shrreya.instagrambanner.tasks.SavePhotoTask;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CameraActivity extends AppCompatActivity {

    private Camera mCamera;

    @BindView(R.id.camera_preview) FrameLayout mCameraPreview;
    @BindView(R.id.camera_grid_view) RelativeLayout mCameraGridView;
    @BindView(R.id.top_overlay) RelativeLayout mTopOverlay;
    @BindView(R.id.bottom_overlay) RelativeLayout mBottomOverlay;
    @BindView(R.id.flash_button) ImageButton mFlashButton;
    @BindView(R.id.capture_button) ImageButton mCaptureButton;
    @BindView(R.id.switch_camera_button) ImageButton mSwitchCameraButton;

    private String mCameraFacing = Constants.REAR;
    private String mFlashState = Constants.OFF;
    private int mDisplayOrientation, mSquareLength;

    public static final String LOG_TAG = "CameraActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Create the camera.
        createCamera(mCameraFacing);

        // Add a click listener to the capture button.
        mCaptureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCamera.takePicture(null, null, mPictureCallback);
                    }
                }
        );

        // Add a touch listener to the camera preview to implement tap to focus functionality of camera.
        mCameraPreview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (mCamera != null) {
                    mCamera.cancelAutoFocus();
                    Rect focusRect = calculateFocusArea(event.getX(), event.getY());

                    Camera.Parameters camParams = mCamera.getParameters();
                    camParams.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

                    if (camParams.getMaxNumFocusAreas() > 0) {
                        List<Camera.Area> mylist = new ArrayList<Camera.Area>();
                        mylist.add(new Camera.Area(focusRect, 1000));
                        camParams.setFocusAreas(mylist);
                    }

                    try {
                        mCamera.cancelAutoFocus();
                        mCamera.setParameters(camParams);
                        mCamera.autoFocus(new Camera.AutoFocusCallback() {
                            @Override
                            public void onAutoFocus(boolean success, Camera camera) {
                                if (camera.getParameters().getFocusMode().equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                                    Camera.Parameters parameters = camera.getParameters();
                                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                                    if (parameters.getMaxNumFocusAreas() > 0) {
                                        parameters.setFocusAreas(null);
                                    }
                                    camera.setParameters(parameters);
                                    camera.startPreview();
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return true;
            }
        });

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // Get the preview size.
        int previewWidth = mCameraPreview.getMeasuredWidth(), previewHeight = mCameraPreview.getMeasuredHeight();

        // Set the height of the overlays so that they make the preview a square.
        RelativeLayout.LayoutParams mTopOverlayParams = (RelativeLayout.LayoutParams) mTopOverlay.getLayoutParams();
        RelativeLayout.LayoutParams mBottomOverlayParams = (RelativeLayout.LayoutParams) mBottomOverlay.getLayoutParams();
        int overlayHeight = (previewHeight - previewWidth) / 2;
        mTopOverlayParams.height = overlayHeight;
        mBottomOverlayParams.height = overlayHeight;
        mTopOverlay.setLayoutParams(mTopOverlayParams);
        mBottomOverlay.setLayoutParams(mBottomOverlayParams);

        // Create camera grid view and set it as the view content.
        mSquareLength = previewWidth;
        CameraGridView view = new CameraGridView(this, mSquareLength);
        view.setPanels(Constants.DEFAULT_PANELS);
        mCameraGridView.addView(view);

    }

    @Override
    protected void onPause() {
        super.onPause();

        // Release the camera immediately on activity pause event.
        releaseCamera();

        // Removing the inserted view so when we come back to the app we won't have the views on top of each other.
        if(mCameraPreview.getChildCount() > 0)
            mCameraPreview.removeViewAt(0);
    }

    /* Helper function to prepare Camera and it's preview. */
    private void createCamera(String cameraType) {
        // Release previous instance of Camera.
        if(mCamera != null)
            releaseCamera();
        // Create an instance of Camera. Default is rear camera.
        mCamera = getCameraInstance(cameraType);

        if(mCamera != null) {
            // Create camera preview and set it as the view content.
            CameraPreview preview = new CameraPreview(this, mCamera);
            mCameraPreview.addView(preview);
        }
        // If camera hardware not available, display appropriate error message.
        else {
            Toast.makeText(this, R.string.camera_hardware_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    /* Helper function to get camera instance by type - front or rear. */
    public Camera getCameraInstance(String cameraType) {

        int cameraCount = 0;
        Camera camera = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();

        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            // Check for front camera availability.
            if(cameraType.equals(Constants.FRONT) && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    try {
                        camera = Camera.open(camIdx);
                        setCameraDisplayOrientation(camIdx, camera);
                        mSwitchCameraButton.setImageResource(R.drawable.ic_camera_rear_black_24dp);
                    } catch (RuntimeException e) {
                        Log.e(LOG_TAG, "Front camera failed to open: " + e.getMessage());
                    }
            }
            // Check for rear camera availability.
            else if (cameraType.equals(Constants.REAR) && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    try {
                        camera = Camera.open(camIdx);
                        setCameraDisplayOrientation(camIdx, camera);
                        mSwitchCameraButton.setImageResource(R.drawable.ic_camera_front_black_24dp);
                    } catch (RuntimeException e) {
                        Log.e(LOG_TAG, "Rear camera failed to open: " + e.getMessage());
                    }
            }
        }

        if(camera != null)
            mCameraFacing = cameraType;

        return camera;
    }

    /* Helper function to set camera display orientation neatly. */
    public void setCameraDisplayOrientation(int cameraId, android.hardware.Camera camera) {

        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        // Compensate the mirror according to camera type.
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mDisplayOrientation = (info.orientation + degrees) % 360;
            mDisplayOrientation = (360 - mDisplayOrientation) % 360;
        } else {
            mDisplayOrientation = (info.orientation - degrees + 360) % 360;
        }

        camera.setDisplayOrientation(mDisplayOrientation);
    }

    /* Helper function to calculate camera focus area. */
    private Rect calculateFocusArea(float x, float y) {
        int FOCUS_AREA_SIZE = 200;
        int left = clamp(Float.valueOf((x / mCameraPreview.getWidth()) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);
        int top = clamp(Float.valueOf((y / mCameraPreview.getHeight()) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);

        return new Rect(left, top, left + FOCUS_AREA_SIZE, top + FOCUS_AREA_SIZE);
    }

    /* Helper function to clamp camera focus area size. */
    private int clamp(int touchCoordinate, int focusAreaSize) {
        int result;
        if (Math.abs(touchCoordinate) + focusAreaSize/2 > 1000){
            if (touchCoordinate > 0){
                result = 1000 - focusAreaSize/2;
            } else {
                result = -1000 + focusAreaSize/2;
            }
        } else{
            result = touchCoordinate - focusAreaSize/2;
        }
        return result;
    }

    /* Helper function to release camera for other applications. */
    private void releaseCamera() {
        if (mCamera != null){
            mCamera.release();
            mCamera = null;
        }
    }

    /* Function to handle click on switch camera button. */
    @OnClick(R.id.switch_camera_button)
    public void switchCameraClicked() {
        // Remove current camera preview.
        if(mCameraPreview.getChildCount() > 0)
            mCameraPreview.removeViewAt(0);
        // Switch between front and rear cameras.
        if(mCameraFacing.equals(Constants.REAR))
            createCamera(Constants.FRONT);
        else if(mCameraFacing.equals(Constants.FRONT))
            createCamera(Constants.REAR);
    }

    /* Function to handle click on flash button. */
    @OnClick(R.id.flash_button)
    public void flashClicked() {

        // Check availability of flash light hardware.
        if(this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            Camera.Parameters camParams = mCamera.getParameters();

            // Turn flash on or off accordingly.
            if (mFlashState.equals(Constants.OFF)) {
                camParams.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                mFlashButton.setImageResource(R.drawable.ic_flash_on_black_24dp);
                mFlashState = Constants.ON;
            } else if (mFlashState.equals(Constants.ON)) {
                camParams.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mFlashButton.setImageResource(R.drawable.ic_flash_off_black_24dp);
                mFlashState = Constants.OFF;
            }

            mCamera.setParameters(camParams);
        }
        // If flash light hardware not available, display appropriate error message.
        else {
            Toast.makeText(this, R.string.flash_hardware_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    /* Callback for capture photo. */
    private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            // Launch async task to save photo in background. Start Picture Edit Activity when task completed.
            SavePhotoTask task = new SavePhotoTask(new SavePhotoTaskResponse() {
                @Override
                public void usePhoto(final String photoPath) {
                    startPhotoEditActivity(photoPath);
                }
            });
            task.execute(processPhoto(data));
        }
    };

    /* Helper function to rotate & crop photo according to camera preview before saving to storage. */
    private byte[] processPhoto(byte[] photo) {
        // Load the bitmap from the byte array
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap original = BitmapFactory.decodeByteArray(photo, 0, photo.length, options);

        Matrix matrix = new Matrix();
        // Adjust mirror for front camera before rotation.
        if(mCameraFacing.equals(Constants.FRONT)) {
            float[] mirrorY = { -1, 0, 0, 0, 1, 0, 0, 0, 1 };
            Matrix matrixMirrorY = new Matrix();
            matrixMirrorY.setValues(mirrorY);
            matrix.postConcat(matrixMirrorY);
        }
        // Rotate photo to set orientation.
        matrix.postRotate(mDisplayOrientation);
        Bitmap rotated = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);
        original.recycle();

        // Crop out top part of photo.
        int cropHeight = mTopOverlay.getLayoutParams().height;
        Bitmap scaled, cropped;
        // Resize photo with maintained aspect ratio. This is required on tablets for responsiveness.
        if(rotated.getWidth() < mSquareLength) {
            Log.d("Testing", "Image scaled");
            float aspectRatio = rotated.getHeight() / (float) rotated.getWidth();
            int scaledHeight = Math.round(mSquareLength * aspectRatio);
            scaled = Bitmap.createScaledBitmap(rotated, mSquareLength, scaledHeight, true);
            rotated.recycle();
            int newHeight = scaledHeight - cropHeight;
            cropped = Bitmap.createBitmap(scaled, 0, cropHeight, mSquareLength, newHeight);
            scaled.recycle();
        }
        // No resizing required otherwise.
        else {
            int newHeight = rotated.getHeight() - cropHeight;
            cropped = Bitmap.createBitmap(rotated, 0, cropHeight, mSquareLength, newHeight);
            rotated.recycle();
        }

        // Compress final cropped bitmap to bytes.
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        cropped.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    /* Interface for asynctask response. */
    public interface SavePhotoTaskResponse {
        void usePhoto(String photoPath);
    }

    /* Helper function to start photo edit activity with intent. */
    public void startPhotoEditActivity(String photoPath) {
        Intent intent = new Intent(this, PhotoEditActivity.class);
        intent.putExtra(Constants.NEW_PHOTO, true);
        intent.putExtra(Constants.PHOTO_PATH, photoPath);
        startActivity(intent);
    }

    /* Function to handle click on custom back button of activity. */
    @OnClick(R.id.back_to_main_activity_button)
    public void backToMainActivity() {
        super.onBackPressed();
    }

    /* Function to handle click on library button. */
    @OnClick(R.id.back_to_library_button)
    public void backToLibrary() {
        // Start pick from library activity with intent.
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, Constants.PHOTO_PICK_REQUEST);
    }

    /* Callback for pick photo from library action completed. */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK && requestCode == Constants.PHOTO_PICK_REQUEST) {

            // Get uri of image picked from library, put within intent and start photo edit activity.
            Uri photoUri = data.getData();
            Intent intent = new Intent(this, PhotoEditActivity.class);
            intent.putExtra(Constants.NEW_PHOTO, false);
            intent.putExtra(Constants.PHOTO_URI, photoUri.toString());
            startActivity(intent);

        }
    }
}
