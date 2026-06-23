package com.video.avd.ui.player.subtitle;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.video.avd.R;
import com.video.avd.ui.player.PlayerVideoActivity;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/*
   * This class is used to pick files from 12 and higher
   * */
public class MediaStoreChooserActivity extends Activity {

    public static final String BUCKET_ID = "BUCKET_ID";
    public static final String SUBTITLES = "SUBTITLES";
    public static final String TITLE = "TITLE";

    final int REQUEST_PERMISSION_STORAGE = 0;

    Integer bucketId;
    String title;

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFinishOnTouchOutside(false);
        // Set the layout params for the window
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        Intent intent = getIntent();

        if (intent.hasExtra(BUCKET_ID)) {
            this.bucketId = intent.getIntExtra(BUCKET_ID, Integer.MIN_VALUE);
        }

        this.title = intent.getStringExtra(TITLE);

        String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        if (Build.VERSION.SDK_INT >= 33 && getApplicationContext().getApplicationInfo().targetSdkVersion >= 33) {
            permission = Manifest.permission.READ_MEDIA_VIDEO;
        }

      if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            start();
        } else {
            requestPermissions(new String[]{permission}, REQUEST_PERMISSION_STORAGE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            setResult(RESULT_OK, data);
            finish();
        } else {
            start();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void start() {
        if (bucketId == null) {
            showBuckets();
        } else {
            showFiles(bucketId);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    start();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    HashMap<Integer, String> query(String projectionId, String projectionName, String selection) {
        HashMap<Integer, String> sortedMap = new LinkedHashMap<>();
        try{
            Uri collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
            collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL);
            HashMap<Integer, String> hashMap = new HashMap<>();
            try (Cursor cursor = getContentResolver().query(collection, new String[] { projectionId, projectionName }, selection, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnId = cursor.getColumnIndex(projectionId);
                    int columnName = cursor.getColumnIndex(projectionName);

                    do {
                        int id = cursor.getInt(columnId);
                        String name = cursor.getString(columnName);
                        if (name == null) {
                            continue;
                        }
                        if (!hashMap.containsKey(id)) {
                            hashMap.put(id, name);
                        }
                    } while (cursor.moveToNext());
                }
            }
            // Sort map by value
            List<Map.Entry<Integer, String>> list = new LinkedList<>(hashMap.entrySet());
            Collections.sort(list, (o1, o2) -> o1.getValue().compareToIgnoreCase(o2.getValue()));

            for (Map.Entry<Integer, String> map : list) {
                sortedMap.put(map.getKey(), map.getValue());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return sortedMap;
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    void showBuckets() {
        try{
            String selection = "";
            selection += MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_SUBTITLE;
            HashMap<Integer, String> buckets = query(MediaStore.MediaColumns.BUCKET_ID, MediaStore.MediaColumns.BUCKET_DISPLAY_NAME, selection);

            Integer[] bucketIds = buckets.keySet().toArray(new Integer[0]);
            String[] bucketDisplayNames = buckets.values().toArray(new String[0]);

            AlertDialog.Builder alertDialogBuilder;
            if (buckets.size() == 0) {
                alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setMessage("No files found");
            } else {
                //   alertDialogBuilder = new AlertDialog.Builder(this, R.style.MediaStoreChooserDialog);
                alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setTitle(getString(R.string.select_file));
                alertDialogBuilder.setItems(bucketDisplayNames, (dialogInterface, i) -> {
                    Intent intent = new Intent(MediaStoreChooserActivity.this, MediaStoreChooserActivity.class);
                    intent.putExtra(SUBTITLES, true);
                    intent.putExtra(BUCKET_ID, bucketIds[i]);
                    intent.putExtra(TITLE, bucketDisplayNames[i]);
                    startActivityForResult(intent, 0);
                });
            }
            Log.e("SearchDialog", "Media Chooser Activity:");
            alertDialogBuilder.setOnCancelListener(dialogInterface -> finish());
            alertDialogBuilder.show();

        }catch (Exception e){
            Log.e("SearchDialog", "Media Chooser Activity: "+e);
            e.printStackTrace();
        }

    }
    @RequiresApi(api = Build.VERSION_CODES.R)
    void showFiles(int bucketId) {
        try{
            String selection = MediaStore.MediaColumns.BUCKET_ID + "=" + bucketId;

            selection += " AND " + MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_SUBTITLE;
            HashMap<Integer, String> files = query(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, selection);

            Integer[] ids = files.keySet().toArray(new Integer[0]);
            String[] displayNames = files.values().toArray(new String[0]);

            //AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this, R.style.MediaStoreChooserDialog);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            if (title != null) {
                alertDialogBuilder.setTitle(title);
            }
            alertDialogBuilder.setItems(displayNames, (dialogInterface, i) -> {
                Uri contentUri;
                contentUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL, ids[i]);
                String[] projection = {MediaStore.MediaColumns.DATA};
                Cursor cursor = getContentResolver().query(contentUri, projection, null, null, null);
                String filePath = "";

                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                    filePath = cursor.getString(columnIndex);
                    cursor.close();

                } else {

                }


                if (!filePath.isEmpty()){
                    if (!Objects.equals(PlayerVideoActivity.subTitleUri.getValue(), filePath)) {
                        PlayerVideoActivity.subTitleUri.setValue(filePath);
                        Intent data = new Intent("RESULT", Uri.parse(filePath));
                        setResult(RESULT_OK, data);
                        finishAndRemoveTask();
                        Log.e("SearchDialog","Media Chooser path:"+filePath);
//                        Log.e("xxxxxt", "file Path: "+filePath);
                    }
                }

            });
            alertDialogBuilder.setOnCancelListener(dialogInterface -> finish());
            alertDialogBuilder.show();
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
