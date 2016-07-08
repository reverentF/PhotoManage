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
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    GridView mGrid;

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

        //レコードの取得
        Cursor cursor  = new CursorLoader(getApplicationContext(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null).loadInBackground();

        cursor.moveToFirst();

        // init for loop
        int fieldIndex;
        Long id;
        int cnt = 0, VolMax = 0;
        HashMap<Integer, Uri> uriMap = new HashMap<Integer, Uri>(); //URIをMapで管理する

        do {
            //カラムIDの取得
            fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
            id = cursor.getLong(fieldIndex);

            //IDからURIを取得
            Uri bmpUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
            uriMap.put(cnt, bmpUri);
            cnt++;
        } while (cursor.moveToNext());

        VolMax = --cnt;
        cnt = 0;

        /* Setting GridView */
        mGrid = (GridView) findViewById(R.id.myGrid);
        mGrid.setAdapter(new myAdapter(getApplicationContext(), uriMap, VolMax));
        mGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            private HashMap<Integer, Uri> uriMap;
            public AdapterView.OnItemClickListener setUriMap(HashMap<Integer, Uri> uriMap) {
                this.uriMap = uriMap;
                return this;
            }
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                Intent objIntent = new Intent(getApplicationContext(),imageDetailActivity.class);
                objIntent.setData(uriMap.get(position));
                startActivity(objIntent);
            }
        }.setUriMap(uriMap));
    }

    private static class ViewHolder {
        public ImageView hueImageView;
        public TextView hueTextView;
    }

    // GridView用のCustomAdapter
    public class myAdapter extends BaseAdapter {
        private ContentResolver cr;
        private HashMap<Integer, Uri> hm;
        private LayoutInflater mLayoutInflater;
        private int MAX;
        private Bitmap tmpBmp;
        ImageView imageView;


        public myAdapter(Context context, HashMap<Integer, Uri> _hm, int max) {
            cr = context.getContentResolver();
            hm = _hm;
            // MAX = max;
            MAX = 30;
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
                if(position < hm.size()) {
                    tmpBmp = MediaStore.Images.Media.getBitmap(cr, hm.get(position));
                    holder.hueImageView.setImageBitmap(tmpBmp);
                    holder.hueTextView.setText("test");
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return convertView;
        }

        public final int getCount() {
            return hm.size();
        }

        public final Object getItem(int position) {
            return position;
        }

        public final long getItemId(int position) {
            return position;
        }

        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent objIntent = new Intent(getApplicationContext(),imageDetailActivity.class);
            objIntent.setData(hm.get(position));
            startActivity(objIntent);
        }
    }
}