package jp.ac.titech.itpro.sdl.photomanage;

/**
 * Created by reverent on 16/07/09.
 */
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.widget.Toast.*;

public class imageDetailActivity extends Activity {
    static DBAdapter dbAdapter;
    DBImage image;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_detail);

        // DB接続
        dbAdapter = new DBAdapter(this);
        dbAdapter.open();
        // intentからimage_idを受け取り
        Intent intent = getIntent();
        int image_id = intent.getIntExtra("image_id", 0);
        // DBからimage&tag読み出し
        loadImage(image_id);
        loadTags();

        //button
        Button addTagButton = (Button)findViewById(R.id.bt_addtag);
        addTagButton.setOnClickListener(addTagClickListener);
        Button tweetButton = (Button)findViewById(R.id.bt_tweet);
        tweetButton.setOnClickListener(tweetClickListener);
    }

    private void loadImage(int image_id){
        ContentResolver cr = getContentResolver();

        try {
            image = dbAdapter.getImage(image_id);
            if(image != null){
                //set image
                Bitmap bmpImage = MediaStore.Images.Media.getBitmap(cr ,image.getUri());
                ImageView viewImage = (ImageView) findViewById(R.id.imageView);
                viewImage.setImageBitmap(bmpImage);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadTags(){
        //set tag
        image.updateTag(dbAdapter.getAllTagsByImageID(image.getId()));
        List<DBTag> tags = image.getAllTags();
        TextView viewTags = (TextView)findViewById(R.id.tags);
        viewTags.setText(DBTag.implodeTags(tags, " "));
    }

    private View.OnClickListener addTagClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final EditText editView = new EditText(imageDetailActivity.this);
            new AlertDialog.Builder(imageDetailActivity.this)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setTitle("Edit New Tag")
                    .setView(editView)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            //DBにtag追加
                            dbAdapter.insertTag(image.getId(), editView.getText().toString());
                            loadTags(); //tag再読み込み
                        }
                    })
                    .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    })
                    .show();
        }
    };

    private View.OnClickListener tweetClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.setPackage("com.twitter.android");
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, image.getUri());
            try {
                startActivity(intent);
            }catch(ActivityNotFoundException e){
                makeText(imageDetailActivity.this, "Twitter was not found", LENGTH_LONG).show();
            }
        }
    };

}