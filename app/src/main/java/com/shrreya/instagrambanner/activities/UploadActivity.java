package com.shrreya.instagrambanner.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.shrreya.instagrambanner.views.CameraGridView;
import com.shrreya.instagrambanner.models.Constants;
import com.shrreya.instagrambanner.adapters.PhotoGridViewAdapter;
import com.shrreya.instagrambanner.R;

import java.io.File;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.support.v4.content.FileProvider.getUriForFile;

public class UploadActivity extends AppCompatActivity {

    @BindView(R.id.photo_grid_view) GridView mPhotoGridView;
    @BindView(R.id.final_grid_view) RelativeLayout mFinalGridView;
    @BindView(R.id.instagram_msg_bar) RelativeLayout mInstagramMsgBar;
    @BindView(R.id.start_over_bar) RelativeLayout mStartOverBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);
        ButterKnife.bind(this);

        // Get number of panels and panel length from intent.
        Bundle extras = getIntent().getExtras();
        final int panels = extras.getInt(Constants.PANELS);
        int panelLength = extras.getInt(Constants.PANEL_LENGTH);

        // Set the height of the bars so that they align with the full square photo grid view.
        RelativeLayout.LayoutParams instagramMsgBarLayoutParams = (RelativeLayout.LayoutParams) mInstagramMsgBar.getLayoutParams();
        RelativeLayout.LayoutParams startOverBarLayoutParams = (RelativeLayout.LayoutParams) mStartOverBar.getLayoutParams();
        instagramMsgBarLayoutParams.height = panelLength;
        startOverBarLayoutParams.height = panelLength;
        mInstagramMsgBar.setLayoutParams(instagramMsgBarLayoutParams);
        mStartOverBar.setLayoutParams(startOverBarLayoutParams);


        // Set adapter on photo grid view with number of panels chosen.
        mPhotoGridView.setAdapter(new PhotoGridViewAdapter(this, panels, panelLength));

        // Handle clicks on photo grid view items to share to instagram.
        mPhotoGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                String photoNumber = Integer.toString(panels * Constants.DEFAULT_PANELS - position);
                shareToInstagram(photoNumber);
            }
        });

        // Draw final grid lines.
        CameraGridView finalGridView = new CameraGridView(this, panelLength * Constants.DEFAULT_PANELS);
        finalGridView.setPanels(panels);
        mFinalGridView.addView(finalGridView);
    }

    /* Helper function to share photo to Instagram. */
    private void shareToInstagram(String photoNumber) {

        // Create intent for sharing.
        Intent instagramIntent = new Intent(Intent.ACTION_SEND);
        instagramIntent.setType("image/*");

        // Get file from storage.
        String photoPath = Environment.getExternalStorageDirectory() + File.separator;
        File photoFile = new File(photoPath + Constants.gridPhotoPrefix + photoNumber + Constants.photoFileFormat);

        // Create content URI from file.
        Uri photoUri = getUriForFile(this, Constants.FILE_PROVIDER, photoFile);

        // Set content URI and Instagram package name in intent.
        instagramIntent.putExtra(Intent.EXTRA_STREAM, photoUri);
        instagramIntent.setPackage(Constants.INSTAGRAM_PACKAGE_NAME);

        // Check if Instagram is installed on device.
        PackageManager packManager = getPackageManager();
        List<ResolveInfo> resolvedInfoList = packManager.queryIntentActivities(instagramIntent,  PackageManager.MATCH_DEFAULT_ONLY);
        boolean resolved = false;
        for(ResolveInfo resolveInfo: resolvedInfoList){
            if(resolveInfo.activityInfo.packageName.startsWith(Constants.INSTAGRAM_PACKAGE_NAME)){
                instagramIntent.setClassName(
                        resolveInfo.activityInfo.packageName,
                        resolveInfo.activityInfo.name );
                resolved = true;
                break;
            }
        }

        // If installed start Instagram activity with intent.
        if(resolved) {
            startActivity(instagramIntent);
        }
        // If not installed, display appropriate error message to user.
        else{
            Toast.makeText(this, R.string.instagram_error_msg, Toast.LENGTH_LONG).show();
        }
    }

    /* Overriding system back button functionality to prevent user from going back to edit screen. */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    /* Function to handle click on start over button. */
    @OnClick(R.id.start_over_button)
    public void startOver() {
        // Go back to main activity. Clear all activity stack trace.
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

}
