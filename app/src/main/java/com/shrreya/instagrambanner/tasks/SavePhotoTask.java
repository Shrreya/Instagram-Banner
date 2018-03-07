package com.shrreya.instagrambanner.tasks;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.shrreya.instagrambanner.activities.CameraActivity;
import com.shrreya.instagrambanner.models.Constants;

import java.io.File;
import java.io.FileOutputStream;

public class SavePhotoTask extends AsyncTask<byte[], Void, String> {

    private CameraActivity.SavePhotoTaskResponse mDelegate = null;

    private final String LOG_TAG = "SavePhotoTask";

    public SavePhotoTask(CameraActivity.SavePhotoTaskResponse response) {
        mDelegate = response;
    }

    @Override
    protected String doInBackground(byte[]... jpeg) {

        String photoPath = Environment.getExternalStorageDirectory() + File.separator + Constants.photoFileName;
        File photo=new File(photoPath);

        // Delete previously used photo if any.
        if (photo.exists()) {
            photo.delete();
        }

        // Write to file.
        try {
            FileOutputStream fos = new FileOutputStream(photo.getPath());
            fos.write(jpeg[0]);
            fos.close();
            return photoPath;
        }
        catch (java.io.IOException e) {
            Log.e(LOG_TAG, "Exception occurred in saving photo : "  + e.getMessage());
        }
        return null;
    }

    @Override
    protected void onPostExecute(String photoPath) {
        // Send photo path to asynctask response delegate implemented in activity.
        mDelegate.usePhoto(photoPath);
    }
}
