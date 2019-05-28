package com.wotosts.bleadvertisingtest;

import android.Manifest;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.wotosts.bleadvertisingtest.ble.BLEUtils;
import com.wotosts.bleadvertisingtest.ble.server.BluetoothAdvTestActivity;
import com.wotosts.bleadvertisingtest.ble.client.BluetoothClientTestActivity;

import java.security.Permission;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TedPermission.with(this)
                .setPermissionListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted() {

                    }

                    @Override
                    public void onPermissionDenied(List<String> deniedPermissions) {

                    }
                }).setRationaleMessage("권한 허용")
                .setPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
                .check();
    }

    public void onObserverBtnClicked(View view) {
        Intent intent = new Intent(this, BluetoothClientTestActivity.class);
        startActivity(intent);
    }

    public void onAdvertiserBtnClicked(View view) {
        Intent intent = new Intent(this, BluetoothAdvTestActivity.class);
        startActivity(intent);
    }
}
