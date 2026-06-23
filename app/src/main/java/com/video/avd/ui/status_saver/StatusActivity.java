package com.video.avd.ui.status_saver;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.video.avd.R;
import com.video.avd.utils.AppPreference;
import com.video.avd.utils.AppUtils;
import java.util.Objects;


public class StatusActivity extends AppCompatActivity {

    Boolean isBusiness = false;

    private static final int REQUEST_PERMISSIONS = 1234;
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @SuppressLint("InlinedApi")
    private static final String[] NOTIFICATION_PERMISSION = {
            Manifest.permission.POST_NOTIFICATIONS
    };

    private static final int NOTIFICATION_REQUEST_PERMISSIONS = 4;

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {

                    Intent data = result.getData();

                    assert data != null;

                    getContentResolver().takePersistableUriPermission(
                            data.getData(),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    AppUtils.INSTANCE.setStatusPermission(true);
                    if (isBusiness){
                        AppPreference.INSTANCE.setPermissionForStatusBusiness(this, true);
                    }else {
                        AppPreference.INSTANCE.setPermissionForStatus(this, true);
                    }

                    Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
                    finish();

                }
                else{
                    finish();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.status_test);
        isBusiness = getIntent().getBooleanExtra("isBusiness", false);
      //  AppUtils.INSTANCE.hideSystemUI(this);
        AppUtils.INSTANCE.hideNavigationBar(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS && grantResults.length > 0) {
            if (arePermissionDenied()) {
                ((ActivityManager) Objects.requireNonNull(this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
                recreate();
            }
        }
    }

    private boolean arePermissionDeniedOld() {

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            return getContentResolver().getPersistedUriPermissions().size() <= 0;
        }

        for (String permissions : PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), permissions) != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }


   public Boolean arePermissionDenied(){
        if (isBusiness){
            if (AppPreference.INSTANCE.isPermissionGrantedForStatusBusiness(this)){
                return false;
            }else {
                return true;
            }
        }else {
            if (AppPreference.INSTANCE.isPermissionGrantedForStatus(this)){
                return false;
            }else {
                return true;
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && arePermissionDenied()) {

            // If Android 10+
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                requestPermissionQ();
                return;
            }

            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
        }
        else {
            finish();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(NOTIFICATION_PERMISSION,
                    NOTIFICATION_REQUEST_PERMISSIONS);
        }

        if (CommonStatusUtils.INSTANCE.APP_DIR == null || CommonStatusUtils.INSTANCE.APP_DIR.isEmpty()) {
            CommonStatusUtils.INSTANCE.APP_DIR = getExternalFilesDir("StatusDownloader").getPath();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void requestPermissionQ() {
        try {
            StorageManager sm = (StorageManager) getSystemService(Context.STORAGE_SERVICE);

            Intent intent = sm.getPrimaryStorageVolume().createOpenDocumentTreeIntent();
            String startDir = "Android%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses";
            if (isBusiness){
                 startDir = "Android%2Fmedia%2Fcom.whatsapp.w4b%2FWhatsApp Business%2FMedia%2F.Statuses";
            }else {
                startDir = "Android%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses";
            }

            Uri uri = intent.getParcelableExtra("android.provider.extra.INITIAL_URI");
            String scheme = uri.toString();
            scheme = scheme.replace("/root/", "/document/");
            scheme += "%3A" + startDir;
            uri = Uri.parse(scheme);
            Log.d("URI", uri.toString());
            intent.putExtra("android.provider.extra.INITIAL_URI", uri);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            activityResultLauncher.launch(intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @Override
    public void onBackPressed() {
    finish();
    }

}
