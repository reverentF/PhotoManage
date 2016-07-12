package jp.ac.titech.itpro.sdl.photomanage;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    GridView mGrid;
    static DBAdapter dbAdapter;
    static List<DBImage> imageList;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //権限の確認
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 1203);
            return;
        }

        //DB接続
        dbAdapter = new DBAdapter(this);
        imageList = new ArrayList<DBImage>();
        dbAdapter.open();
        updateDBImages();
        loadDBImages();

        /* Setting GridView */
        mGrid = (GridView) findViewById(R.id.myGrid);
        mGrid.setAdapter(new myAdapter(getApplicationContext()));
        mGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                Intent objIntent = new Intent(getApplicationContext(),imageDetailActivity.class);
                objIntent.setData(imageList.get(position).getUri());
                startActivity(objIntent);
            }
        });
    }

    //load Images with Tags from DB
    protected void loadDBImages(){
        imageList.clear();
        Cursor cursor = dbAdapter.getAllImages();
        if(cursor.moveToFirst()) {
            do {
                imageList.add(new DBImage(cursor));
            } while (cursor.moveToNext());
        }
    }

    //find new Images and update DB
    protected void updateDBImages(){
        //レコードの取得
        Cursor cursor  = new CursorLoader(getApplicationContext(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null).loadInBackground();
        cursor.moveToFirst();

        do {
            //タイトルを取得
            int titleCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
            String title = cursor.getString(titleCol);
            if(!dbAdapter.ExistsImage(title)){
                //カラムIDの取得
                int fieldCol = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                Long id = cursor.getLong(fieldCol);
                //IDからURIを取得
                Uri bmpUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                dbAdapter.insertImage(bmpUri, title);
            }
        } while (cursor.moveToNext());
    }


    private static class ViewHolder {
        public ImageView hueImageView;
        public TextView hueTextView;
    }

    // GridView用のCustomAdapter
    public class myAdapter extends BaseAdapter {
        private ContentResolver cr;
        private LayoutInflater mLayoutInflater;
        private int MAX;
        private Bitmap tmpBmp;
        ImageView imageView;


        public myAdapter(Context context) {
            cr = context.getContentResolver();
            mLayoutInflater = LayoutInflater.from(context);
        }

        public View getView(int position, View convertView, ViewGroup parent){
            ViewHolder holder;
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.grid_item, null);
                holder = new ViewHolder();
                holder.hueImageView = (ImageView)convertView.findViewById(R.id.hue_imageview);
                holder.hueTextView = (TextView)convertView.findViewById(R.id.hue_textview);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }
            try {
                if(position < imageList.size()) {
                    DBImage image = imageList.get(position);
                    tmpBmp = MediaStore.Images.Media.getBitmap(cr, image.getUri());
                    holder.hueImageView.setImageBitmap(tmpBmp);
                    holder.hueTextView.setText(image.getTitle());
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return convertView;
        }

        public final int getCount() {
            return imageList.size();
        }

        public final Object getItem(int position) {
            return position;
        }

        public final long getItemId(int position) {
            return position;
        }

        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent objIntent = new Intent(getApplicationContext(),imageDetailActivity.class);
            objIntent.setData(imageList.get(position).getUri());
            startActivity(objIntent);
        }
    }
}