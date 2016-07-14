package jp.ac.titech.itpro.sdl.photomanage;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private AppCompatDelegate delegate;
    public static final int MAX_TAG = 3; //MainActivityで表示するタグの数
    GridView mGrid;
    SearchView mSearchView;
    private SimpleCursorAdapter mSearchSuggestAdapter;
    static DBAdapter dbAdapter;
    static List<DBImage> imageList;
    static List<Bitmap> bitmapList;
    static Cursor suggestCursor;

    myAdapter gridAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initialize
        dbAdapter = new DBAdapter(this);
        imageList = new ArrayList<DBImage>();
        bitmapList = new ArrayList<Bitmap>();

        //権限の確認
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1203);
            return;
        }


        /*
         *  GridView
         */
        mGrid = (GridView) findViewById(R.id.myGrid);
        gridAdapter = new myAdapter(getApplicationContext());
        mGrid.setAdapter(gridAdapter);
        mGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                Intent objIntent = new Intent(getApplicationContext(), imageDetailActivity.class);
                objIntent.putExtra("image_id", imageList.get(position).getId());
                startActivity(objIntent);
            }
        });


        /*
         *  Search View
         */
        //TODO 長いのでfunction化
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.search_view);

        mSearchView = (SearchView) toolbar.getMenu().findItem(R.id.menu_search).getActionView();
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                String[] queries = s.split("[\\s]+"); //スペース区切り
                findDBImages(queries); //検索
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (s.equals("")) {
                    //入力が空 -> 全表示
                    loadDBImages();
                }else{
                    //入力内容からタグの候補をDBから取得
                    Cursor c = dbAdapter.getTagSuggestions(s);
                    suggestCursor = c;
                    mSearchSuggestAdapter.changeCursor(c);
                }
                return false;
            }
        });
        mSearchView.setQueryHint("search tags");
        mSearchView.setIconifiedByDefault(true);
        mSearchView.setSubmitButtonEnabled(true);
        mSearchView.setQueryRefinementEnabled(true);
        final String[] from = new String[] {DBAdapter.T_TAG_COL_VALUE};
        final int[] to = new int[] {android.R.id.text1};
        mSearchSuggestAdapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1,
                null,
                from,
                to,
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        mSearchView.setSuggestionsAdapter(mSearchSuggestAdapter);
        //検索候補選択時のListener
        mSearchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionClick(int position) {
                suggestCursor.moveToPosition(position);
                String tag = suggestCursor.getString(suggestCursor.getColumnIndex(DBAdapter.T_TAG_COL_VALUE));
                mSearchView.setQuery(tag, true);
                return true;
            }

            @Override
            public boolean onSuggestionSelect(int position) {
                suggestCursor.moveToPosition(position);
                String tag = suggestCursor.getString(suggestCursor.getColumnIndex(DBAdapter.T_TAG_COL_VALUE));
                mSearchView.setQuery(tag, true);
                return true;
            }
        });



        //DBから画像の読み出し
        dbAdapter.open();
        updateDBImages();
        loadDBImages();
    }

    @Override
    public void onPause() {
        super.onPause();
        dbAdapter.close();
    }

    @Override
    public void onResume() {
        super.onResume();
        dbAdapter.open();
        updateDBImages();
        loadDBImages();
    }

    //load Images with Tags from DB
    protected void loadDBImages() {
        imageList.clear();
        Cursor cursor = dbAdapter.getAllImages();
        if (cursor.moveToFirst()) {
            do {
                DBImage image = new DBImage(cursor);
                int image_id = image.getId();
                image.addTag(dbAdapter.getTagsByImageID(image_id, MAX_TAG));
                imageList.add(image);
            } while (cursor.moveToNext());
        }
        updateBitMapList();
        gridAdapter.notifyDataSetChanged();
    }

    //find Images by search from DB
    protected void findDBImages(String[] queries) {
        imageList.clear();
        Cursor cursor = dbAdapter.findImage(queries);
        if (cursor.moveToFirst()) {
            do {
                DBImage image = new DBImage(cursor);
                int image_id = image.getId();
                image.addTag(dbAdapter.getTagsByImageID(image_id, MAX_TAG));
                imageList.add(image);
            } while (cursor.moveToNext());
        }
        updateBitMapList();
        gridAdapter.notifyDataSetChanged();
    }

    //find new Images and update DB
    protected void updateDBImages() {
        //レコードの取得
        Cursor cursor = new CursorLoader(getApplicationContext(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null).loadInBackground();
        cursor.moveToFirst();

        do {
            //タイトルを取得
            int titleCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
            String title = cursor.getString(titleCol);
            if (!dbAdapter.ExistsImage(title)) {
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
        public ImageView gridImageView;
        public TextView gridTextView;
        public TextView gridTagView;
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

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.grid_item, null);
                holder = new ViewHolder();
                holder.gridImageView = (ImageView) convertView.findViewById(R.id.grid_imageview);
                holder.gridTextView = (TextView) convertView.findViewById(R.id.grid_textview);
                holder.gridTagView = (TextView) convertView.findViewById(R.id.grid_tagview);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            if (position < imageList.size()) {
                DBImage image = imageList.get(position);
                holder.gridImageView.setImageBitmap(bitmapList.get(position));
                holder.gridTextView.setText(image.getTitle());
                holder.gridTagView.setText(DBTag.implodeTags(image.getTags(MAX_TAG), " "));
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
    }

    public boolean onSubmitQuery(String queryText) {
        return false;
    }

    public void updateBitMapList() {
        bitmapList.clear();
        for (DBImage image : imageList) {
            try {
                Bitmap tmpBmp = MediaStore.Images.Media.getBitmap(getContentResolver(), image.getUri());
                bitmapList.add(tmpBmp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}