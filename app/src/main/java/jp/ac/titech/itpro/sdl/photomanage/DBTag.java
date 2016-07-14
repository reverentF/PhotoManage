package jp.ac.titech.itpro.sdl.photomanage;

import android.database.Cursor;

import java.util.List;

/**
 * Created by reverent on 16/07/11.
 */
public class DBTag {
    protected int id;        //tag id
    protected int image_id;  //id of DBImage
    protected String value;
    protected String update_ymdhi;

    public DBTag(int id, int image_id, String value, String update_ymghi){
        this.id = id;
        this.image_id = image_id;
        this.value = value;
        this.update_ymdhi = update_ymghi;
    }

    public DBTag(Cursor cursor){
        this.id = cursor.getInt(cursor.getColumnIndex(DBAdapter.T_TAG_COL_ID));
        this.image_id = cursor.getInt(cursor.getColumnIndex(DBAdapter.T_TAG_COL_IMAGE_ID));
        this.value = cursor.getString(cursor.getColumnIndex(DBAdapter.T_TAG_COL_VALUE));
        this.update_ymdhi = cursor.getString(cursor.getColumnIndex(DBAdapter.T_TAG_COL_LASTUPDATE));
    }

    public String getValue(){
        return this.value;
    }
    public int getId(){ return this.id; }

    //タグを整形して返す
    public static String implodeTags(List<DBTag> tags, String glue){
        String result = "";
        boolean first_flg = true;
        for(DBTag tag : tags){
            if(first_flg){
                first_flg = false;
            }else{
                result += glue;
            }
            result += tag.getValue();
        }

        return result;
    }
}
