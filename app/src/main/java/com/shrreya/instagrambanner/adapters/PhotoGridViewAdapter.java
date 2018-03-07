package com.shrreya.instagrambanner.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.shrreya.instagrambanner.models.Constants;
import com.shrreya.instagrambanner.R;

import java.io.File;

public class PhotoGridViewAdapter extends BaseAdapter {

    private Context mContext;
    private int mPhotos, mPanelLength;

    public PhotoGridViewAdapter(Context context, int panels, int panelLength) {

        mContext = context;
        mPhotos = panels * Constants.DEFAULT_PANELS;
        mPanelLength = panelLength;
    }

    @Override
    public int getCount() {
        return mPhotos;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view;

        if (convertView == null) {

            view = inflater.inflate(R.layout.photo_grid_view_item, null);

            ImageView photo = (ImageView) view.findViewById(R.id.photo_grid_item_image);
            TextView photoNumber = (TextView) view.findViewById(R.id.photo_grid_item_number);

            String filePath = Environment.getExternalStorageDirectory() + File.separator;
            String number = Integer.toString(mPhotos - position);
            File file = new File(filePath + Constants.gridPhotoPrefix + number + Constants.photoFileFormat);
            if (file.exists()) {
                // Create bitmap from file.
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                photo.setLayoutParams(new RelativeLayout.LayoutParams(mPanelLength, mPanelLength));
                // Set photo and photo number in grid item.
                photo.setImageBitmap(bitmap);
                photoNumber.setText(number);
            }

        } else {

            view = convertView;
        }

        return view;
    }

}