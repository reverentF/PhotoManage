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
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.makeText;

public class imageDetailActivity extends Activity {
    static DBAdapter dbAdapter;
    private static final String TAG = imageDetailActivity.class.getSimpleName();
    DBImage image;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

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
        Button addTagButton = (Button) findViewById(R.id.bt_addtag);
        addTagButton.setOnClickListener(addTagClickListener);
        Button autoTagButton = (Button) findViewById(R.id.bt_autotag);
        autoTagButton.setOnClickListener(autoTagClickListener);
        Button tweetButton = (Button) findViewById(R.id.bt_tweet);
        tweetButton.setOnClickListener(tweetClickListener);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void loadImage(int image_id) {
        ContentResolver cr = getContentResolver();

        try {
            image = dbAdapter.getImage(image_id);
            if (image != null) {
                //set image
                Bitmap bmpImage = MediaStore.Images.Media.getBitmap(cr, image.getUri());
                ImageView viewImage = (ImageView) findViewById(R.id.imageView);
                viewImage.setImageBitmap(bmpImage);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadTags() {
        //set tag
        image.updateTag(dbAdapter.getAllTagsByImageID(image.getId()));
        List<DBTag> tags = image.getAllTags();
        TextView viewTags = (TextView) findViewById(R.id.tags);
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

    private View.OnClickListener autoTagClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            uploadImage(image.getUri());
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
            } catch (ActivityNotFoundException e) {
                makeText(imageDetailActivity.this, "Twitter was not found", LENGTH_LONG).show();
            }
        }
    };

    /****************
     * Cloud Vision API
     ****************/

    public void uploadImage(Uri uri) {
        if (uri != null) {
            try {
                // scale the image to save on bandwidth
                Bitmap bitmap =
                        scaleBitmapDown(
                                MediaStore.Images.Media.getBitmap(getContentResolver(), uri),
                                1200);

                callCloudVision(bitmap);

            } catch (IOException e) {
                makeText(imageDetailActivity.this, "image_picker_error", LENGTH_LONG).show();
            }
        } else {
            makeText(imageDetailActivity.this, "image_picker_gives_null_image", LENGTH_LONG).show();
        }
    }

    private void callCloudVision(final Bitmap bitmap) throws IOException {
        // Switch text to loading
        makeText(imageDetailActivity.this, R.string.uploading, LENGTH_LONG).show();

        // Do the real work in an async task, because we need to use the network anyway
        new AsyncTask<Object, Void, List<String>>() {
            @Override
            protected List<String> doInBackground(Object... params) {
                try {
                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                    Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                    builder.setVisionRequestInitializer(new
                            VisionRequestInitializer(getString(R.string.api_brouser_key)));
                    Vision vision = builder.build();

                    BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                            new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
                        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

                        // Add the image
                        Image base64EncodedImage = new Image();
                        // Convert the bitmap to a JPEG
                        // Just in case it's a format that Android understands but Cloud Vision
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                        byte[] imageBytes = byteArrayOutputStream.toByteArray();

                        // Base64 encode the JPEG
                        base64EncodedImage.encodeContent(imageBytes);
                        annotateImageRequest.setImage(base64EncodedImage);

                        // add the features we want
                        annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                            Feature labelDetection = new Feature();
                            labelDetection.setType("LABEL_DETECTION");
                            labelDetection.setMaxResults(3);
                            add(labelDetection);

                            Feature ocr = new Feature();
                            ocr.setType("TEXT_DETECTION");
                            ocr.setMaxResults(3);
                            add(ocr);

                            Feature landMarkDetection = new Feature();
                            landMarkDetection.setType("LANDMARK_DETECTION");
                            landMarkDetection.setMaxResults(3);
                            add(landMarkDetection);
                        }});

                        // Add the list of one thing to the request
                        add(annotateImageRequest);
                    }});

                    Vision.Images.Annotate annotateRequest =
                            vision.images().annotate(batchAnnotateImagesRequest);
                    // Due to a bug: requests to Vision API containing large images fail when GZipped.
                    annotateRequest.setDisableGZipContent(true);
                    Log.d(TAG, "created Cloud Vision request object, sending request");

                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    return convertResponseToString(response);

                } catch (GoogleJsonResponseException e) {
                    Log.d(TAG, "failed to make API request because " + e.getContent());
                } catch (IOException e) {
                    Log.d(TAG, "failed to make API request because of other IOException " +
                            e.getMessage());
                }
                return new ArrayList<String>();
            }

            protected void onPostExecute(List<String> results) {
                //結果を表示
                if(results == null || results.isEmpty()){
                    makeText(imageDetailActivity.this, "find nothing", LENGTH_LONG).show();
                    return;
                }
                //ここでタグに入れる
                for(String result : results){
                    dbAdapter.insertTag(image.getId(), result);
                }
                loadTags();
            }
        }.execute();
    }

    public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    private List<String> convertResponseToString(BatchAnnotateImagesResponse response) {
        //APIの結果を整形
        List<String> messages = new ArrayList<String>();

        AnnotateImageResponse results = response.getResponses().get(0);
        List<EntityAnnotation> labels = new ArrayList<EntityAnnotation>();
        if(results.getLabelAnnotations() != null){
            labels.addAll(results.getLabelAnnotations());
        }
        if(results.getTextAnnotations() != null){
		    labels.addAll(results.getTextAnnotations());
        }
        if(results.getLandmarkAnnotations() != null) {
            labels.addAll(results.getLandmarkAnnotations());
        }
        if (labels != null) {
            for (EntityAnnotation label : labels) {
                String message = String.format("%s", label.getDescription());
                messages.add(message);
            }
        }

        return messages;
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "imageDetail Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://jp.ac.titech.itpro.sdl.photomanage/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "imageDetail Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://jp.ac.titech.itpro.sdl.photomanage/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}