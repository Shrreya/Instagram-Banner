package com.shrreya.instagrambanner.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.shrreya.instagrambanner.models.Constants;
import com.shrreya.instagrambanner.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    /* Function to handle click on camera button. */
    @OnClick(R.id.camera_button)
    public void launchCamera() {
        // Check camera hardware availability.
        if(checkCameraHardware(this)) {
            // Camera hardware available! Check camera and storage permissions granted.
            List<String> listPermissionsNeeded = checkPermissionsGranted(this, true);
            if(!listPermissionsNeeded.isEmpty())
                // Request permissions needed & start camera activity.
                requestPermissionsNeeded(this, listPermissionsNeeded, true);
            else
                // Permissions granted already, so start camera activity.
                startPhotoActivity(true);
        } else {
            // Camera hardware unavailable! Display appropriate error message.
            Toast.makeText(this, R.string.camera_hardware_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    /* Function to handle click on library button. */
    @OnClick(R.id.library_button)
    public void openLibrary() {
        // Check storage permission granted.
        List<String> listPermissionsNeeded = checkPermissionsGranted(this, false);
        if(!listPermissionsNeeded.isEmpty())
            // Request permission needed & start pick from library activity.
            requestPermissionsNeeded(this, listPermissionsNeeded, false);
        else
            // Permission granted already, so start pick from library activity.
            startPhotoActivity(false);
    }

    /* Helper function to check the availability of camera hardware. */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
            return true;
        else
            return false;
    }

    /* Helper function to check the permissions granted for using camera hardware and/or external storage. */
    private List<String> checkPermissionsGranted(Context context, boolean checkCameraPermission) {

        List<String> listPermissionsNeeded = new ArrayList<>();
        if (checkCameraPermission && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        return listPermissionsNeeded;
    }

    /* Helper function to request permissions for using camera hardware and/or external storage. */
    private void requestPermissionsNeeded(Activity activity, List<String> listPermissionsNeeded, boolean cameraPermission) {
        int requestCode;
        if(cameraPermission)
            requestCode = Constants.CAMERA_PERMISSIONS_REQUEST;
        else
            requestCode = Constants.LIBRARY_PERMISSIONS_REQUEST;

        ActivityCompat.requestPermissions(activity,
                listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                requestCode);
    }

    /* Callback for permissions request result. */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if((requestCode == Constants.CAMERA_PERMISSIONS_REQUEST || requestCode == Constants.LIBRARY_PERMISSIONS_REQUEST) && grantResults.length > 0) {
            for(int i = 0; i < grantResults.length; i++) {
                if(grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    // Permission denied! Display appropriate error message.
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            // Start camera or pick from library activity based on request code.
            if(requestCode == Constants.CAMERA_PERMISSIONS_REQUEST)
                startPhotoActivity(true);
            else
                startPhotoActivity(false);
        }
    }

    /* Helper function to start camera or pick from library activity with an intent. */
    private void startPhotoActivity(boolean cameraChosen) {

        // Start camera activity.
        if(cameraChosen) {
            // Save captured photo to a file and generate URI.
            String photoPath = Environment.getExternalStorageDirectory() + File.separator + Constants.photoFileName;
            File photoFile = new File(photoPath);
            Uri photoUri = FileProvider.getUriForFile(this, Constants.FILE_PROVIDER, photoFile);
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(cameraIntent, Constants.PHOTO_CAPTURE_REQUEST);
            }
        }

        // Start pick from library activity.
        else {
            Intent libraryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            libraryIntent.setType("image/*");
            startActivityForResult(libraryIntent, Constants.PHOTO_PICK_REQUEST);
        }
    }

    /* Callback for photo pick action completed. */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Check result success and start photo edit activity with intent.
        if (resultCode == RESULT_OK) {

            Intent photoEditIntent = new Intent(this, PhotoEditActivity.class);

            switch(requestCode) {
                case Constants.PHOTO_CAPTURE_REQUEST:
                    photoEditIntent.putExtra(Constants.NEW_PHOTO, true);
                    break;
                case Constants.PHOTO_PICK_REQUEST:
                    Uri photoUri = data.getData();
                    photoEditIntent.putExtra(Constants.NEW_PHOTO, false);
                    photoEditIntent.putExtra(Constants.PHOTO_URI, photoUri.toString());
                    break;
            }

            startActivity(photoEditIntent);
        }
    }
}
