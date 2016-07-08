package jp.ac.titech.itpro.sdl.photomanage;

/**
 * Created by reverent on 16/07/09.
 */
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.GridView;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.io.IOException;

public class imageDetailActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_detail);

        // 現在のintentを取得する
        Intent intent = getIntent();
        Uri image_uri = intent.getData();
        ContentResolver cr = getContentResolver();
        try {
            Bitmap tmpBmp = MediaStore.Images.Media.getBitmap(cr ,image_uri);
            ImageView viewImage = (ImageView) findViewById(R.id.imageView);
            viewImage.setImageBitmap(tmpBmp);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}