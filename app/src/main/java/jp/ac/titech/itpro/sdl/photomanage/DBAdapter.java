package jp.ac.titech.itpro.sdl.photomanage;

/**
 * Created by reverent on 16/07/11.
 */

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.database.DatabaseUtils;
import android.net.Uri;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.MediaStore;

public class DBAdapter {
    static final String DATABASE_NAME = "photomanage.db";
    static final int DATABASE_VERSION = 1;
    //t_image
    public static final String T_IMAGE_TABLE_NAME = "t_image";
    public static final String T_IMAGE_COL_ID = "_id";
    public static final String T_IMAGE_COL_URI = "uri";
    public static final String T_IMAGE_COL_TITLE = "title";
    public static final String T_IMAGE_COL_LASTUPDATE = "update_ymdhi";
    //t_tag
    public static final String T_TAG_TABLE_NAME = "t_tag";
    public static final String T_TAG_COL_ID = "_id";
    public static final String T_TAG_COL_IMAGE_ID = "image_id";
    public static final String T_TAG_COL_VALUE = "valule";
    public static final String T_TAG_COL_LASTUPDATE = "update_ymdhi";
    //
    protected final Context context;
    protected DatabaseHelper dbHelper;
    protected SQLiteDatabase db;

    public DBAdapter(Context context) {
        this.context = context;
        dbHelper = new DatabaseHelper(this.context);
    }

    //
    // SQLiteOpenHelper
    //
    private static class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            //create t_image
            db.execSQL("CREATE TABLE " + T_IMAGE_TABLE_NAME +
                    " (" + T_IMAGE_COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                         + T_IMAGE_COL_URI + " TEXT NOT NULL,"
                         + T_IMAGE_COL_TITLE + " TEXT,"
                         + T_IMAGE_COL_LASTUPDATE + " TEXT NOT NULL); "
            );

            //create t_tag
            db.execSQL("CREATE TABLE " + T_TAG_TABLE_NAME +
                    " (" + T_TAG_COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + T_TAG_COL_IMAGE_ID + " INTEGER NOT NULL,"
                    + T_TAG_COL_VALUE + " TEXT NOT NULL,"
                    + T_IMAGE_COL_LASTUPDATE + " TEXT NOT NULL); "
            );

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + T_IMAGE_TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + T_TAG_TABLE_NAME);
            onCreate(db);
        }
    }

    //
    // Adapter Methods
    //
    public DBAdapter open() {
        db = dbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        dbHelper.close();
    }


    /*
     *  Methods for t_image
     */
    //TODO Cursor返したりList返したりバラバラだからどちらかに統一

    //select
    public Cursor getAllImages() {
        return db.query(T_IMAGE_TABLE_NAME, null, null, null, null, null, null);
    }
    public DBImage getImage(int image_id) {
        Cursor cursor = db.query(T_IMAGE_TABLE_NAME, null, T_IMAGE_COL_ID + "=" + image_id, null, null, null, null);
        if(!cursor.moveToNext()){
            return null;
        }else{
            DBImage image =  new DBImage(cursor);
            image.addTag(this.getAllTagsByImageID(image_id));
            return image;
        }
    }

    /*
     * Check exists the image on DB
     * @param : title
     * @return : true -> exist image which has same title
     */
    public boolean ExistsImage(String title){
        return DatabaseUtils.queryNumEntries(db, T_IMAGE_TABLE_NAME, T_IMAGE_COL_TITLE + "=\"" + title + "\" ") > 0;
    }

    //find by tag
    public Cursor findImage(String []queries) {
        //find tag by query
        String whereStr = "";
        for(String query : queries){
            if(whereStr != ""){
                whereStr += " AND ";
            }
            whereStr += T_TAG_COL_VALUE + " LIKE '%" + query + "%' ";
        }

        Cursor cursor_tag;
        cursor_tag = db.query(T_TAG_TABLE_NAME, new String[] {T_TAG_COL_IMAGE_ID}, whereStr, null, null, null, null);
        List<Integer> image_ids = new ArrayList<Integer>();
        if(cursor_tag.moveToFirst()) {
            do {
                int col_image_id = cursor_tag.getColumnIndex(T_TAG_COL_IMAGE_ID);
                int image_id = cursor_tag.getInt(col_image_id);
                if (!image_ids.contains(image_id)) {
                    image_ids.add(image_id);
                }
            } while (cursor_tag.moveToNext());
        }

        //find image by image_id
        String inStr = " IN ( ";
        int cnt = 0;
        for(Integer image_id : image_ids){
            if(cnt != 0){
                inStr += ", ";
            }
            inStr += image_id;
            cnt++;
        }
        inStr += ") ";
        //find image by tag or image_title
        String imgWhereStr = T_IMAGE_COL_ID + inStr + " OR ( ";
        boolean isFirst = true;
        for(String query : queries){
            if(!isFirst){
                imgWhereStr += " AND ";
            }else{
                isFirst = false;
            }
            imgWhereStr += T_IMAGE_COL_TITLE + " LIKE '%" + query + "%' ";
        }
        imgWhereStr += " ) ";
        return db.query(T_IMAGE_TABLE_NAME, null, imgWhereStr, null, null, null, null);
    }

    //insert
    public void insertImage(Uri uri, String title) {
        Date dateNow = new Date();
        SimpleDateFormat format=new SimpleDateFormat("yyyy/MM/dd/HH:mm");

        ContentValues values = new ContentValues();
        values.put(T_IMAGE_COL_URI, uri.toString());
        values.put(T_IMAGE_COL_TITLE, title);
        values.put(T_IMAGE_COL_LASTUPDATE, format.format(dateNow));
        db.insertOrThrow(T_IMAGE_TABLE_NAME, null, values);
    }

    //delete
    public boolean deleteImage(int image_id) {
        deleteAllTags(image_id);
        return db.delete(T_IMAGE_TABLE_NAME, T_IMAGE_COL_ID + "=" + image_id, null) > 0;
    }

    /*
     *  Methods for t_tag
     */

    //select
    public List<DBTag> getTagsByImageID(int image_id, int limit) {
        Cursor cursor_tag;
        if(limit == 0){
            //limit == 0 -> all
            cursor_tag =  db.query(T_TAG_TABLE_NAME, null, T_TAG_COL_IMAGE_ID + "=" + image_id, null, null, null, null);
        }else{
            cursor_tag =  db.query(T_TAG_TABLE_NAME, null, T_TAG_COL_IMAGE_ID + "=" + image_id, null, null, null, null, ""+ limit);
        }

        List<DBTag> tags = new ArrayList<DBTag>();
        if(cursor_tag.moveToFirst()){
            do{
                tags.add(new DBTag(cursor_tag));
            }while(cursor_tag.moveToNext());
        }
        return tags;
    }
    public List<DBTag> getAllTagsByImageID(int image_id) {
        return this.getTagsByImageID(image_id, 0);
    }

    public Cursor getTag(int tag_id) {
        return db.query(T_IMAGE_TABLE_NAME, null, T_TAG_COL_ID + "=" + tag_id, null, null, null, null);
    }

    //insert
    public void insertTag(int image_id, String value) {
        Date dateNow = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd/HH:mm");

        ContentValues values = new ContentValues();
        values.put(T_TAG_COL_IMAGE_ID, image_id);
        values.put(T_TAG_COL_VALUE, value);
        values.put(T_TAG_COL_LASTUPDATE, format.format(dateNow));
        db.insertOrThrow(T_TAG_TABLE_NAME, null, values);
    }

    //delete by image_id
    public boolean deleteAllTags(int image_id) {
        return db.delete(T_TAG_TABLE_NAME, T_TAG_COL_IMAGE_ID + "=" + image_id, null) > 0;
    }

    //delete by tag_id
    public boolean deleteTag(int tag_id) {
        return db.delete(T_TAG_TABLE_NAME, T_TAG_COL_ID + "=" + tag_id, null) > 0;
    }

}