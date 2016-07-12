package jp.ac.titech.itpro.sdl.photomanage;

import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by reverent on 16/07/11.
 */
public class DBImage {
    protected int id;        //id for DB
    protected Uri image_uri; //画像URI(id付き)
    protected String title;
    protected String update_ymdhi;
    protected List<DBTag> tags;

    public DBImage(int id, Uri image_uri, String title, String update_ymghi){
        this.id = id;
        this.image_uri = image_uri;
        this.title = title;
        this.update_ymdhi = update_ymghi;
        this.tags = new ArrayList<DBTag>();
    }

    public DBImage(Cursor cursor){
        this.id = cursor.getInt(cursor.getColumnIndex(DBAdapter.T_IMAGE_COL_ID));
        this.image_uri = Uri.parse(cursor.getString(cursor.getColumnIndex(DBAdapter.T_IMAGE_COL_URI)));
        this.title = cursor.getString(cursor.getColumnIndex(DBAdapter.T_IMAGE_COL_TITLE));
        this.update_ymdhi = cursor.getString(cursor.getColumnIndex(DBAdapter.T_IMAGE_COL_LASTUPDATE));
        this.tags = new ArrayList<DBTag>();
    }

    public int getId(){
        return this.id;
    }

    public Uri getUri(){
        return this.image_uri;
    }

    public String getTitle(){
        return this.title;
    }

    public void addTag(List<DBTag> tags){
        this.tags.addAll(tags);
    }

    public void addTag(DBTag tag){
        this.tags.add(tag);
    }

    //clean tags and add tags
    //TODO Arraylist->HashSetにしてequals実装して
    public void updateTag(List<DBTag> tags){
        this.tags.clear();
        this.tags.addAll(tags);
    }

    public List<DBTag> getAllTags(){
        return this.getTags(this.tags.size());
    }

    public List<DBTag> getTags(int limit){
        int num_tags = limit > this.tags.size() ? this.tags.size() : limit;
        return this.tags.subList(0, num_tags);
    }
}
