package com.shrreya.instagrambanner.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;

import com.shrreya.instagrambanner.activities.PhotoEditActivity;
import com.shrreya.instagrambanner.models.BreakPhotosTaskParams;
import com.shrreya.instagrambanner.models.Constants;
import com.shrreya.instagrambanner.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class BreakPhotosTask extends AsyncTask<BreakPhotosTaskParams, Void, String> {

    private PhotoEditActivity.BreakPhotosTaskResponse mDelegate = null;
    private Context mContext;
    private ProgressDialog mProgressDialog;

    public BreakPhotosTask(Context context, PhotoEditActivity.BreakPhotosTaskResponse response) {
        mContext = context;
        mDelegate = response;
    }

    @Override
    protected void onPreExecute() {
        // Display progress dialog before async task begins.
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setMessage(mContext.getString(R.string.creating_grid));
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    @Override
    protected String doInBackground(BreakPhotosTaskParams... params) {
        byte[] photo = params[0].getPhoto();
        int panels = params[0].getPanels();
        int panelLength = params[0].getPanelLength();

        // Create bitmap from parameters received.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeByteArray(photo, 0, photo.length, options);

        // Initialise x and y coordinates for cropping.
        int x = 0, y = 0;

        String filePath = Environment.getExternalStorageDirectory() + File.separator;

        // Iterating through rows in bitmap. Reverse numbering for upload to Instagram ordering logic.
        for(int i = (panels * 3); i > 0; i-- ) {

            // Crop square bitmap of panel length and save to file with corresponding number.
            Bitmap cropped = Bitmap.createBitmap(bitmap, x, y, panelLength, panelLength);
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(filePath + Constants.gridPhotoPrefix + Integer.toString(i) + Constants.photoFileFormat);
                cropped.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // For every new row, reset x coordinate to 0 and increment y coordinate by panel length.
            if((i-1) % 3 == 0) {
                x = 0;
                y += panelLength;
            }
            // For same row increment only x coordinate by panel length.
            else {
                x += panelLength;
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        // Dismiss progress dialog after asynctask completes.
        mProgressDialog.dismiss();
        // Call asynctask response handler implemented in activity.
        mDelegate.onBreakComplete();
    }
}
