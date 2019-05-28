package com.wotosts.bleadvertisingtest.ble.server;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.AdvertiseCallback;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.wotosts.bleadvertisingtest.R;
import com.wotosts.bleadvertisingtest.ble.BLEUtils;
import com.wotosts.bleadvertisingtest.databinding.ActivityBluetoothAdvTestBinding;


public class BluetoothAdvTestActivity extends AppCompatActivity {

    private ActivityBluetoothAdvTestBinding binding;
    private BroadcastReceiver receiver;

    private LeDeviceListAdapter adapter;
    private BLEAdvertiserService advertiserService;
    private boolean isBind;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_bluetooth_adv_test);
        adapter = new LeDeviceListAdapter(new LeDeviceListAdapter.ItemClickListener() {
            @Override
            public void onItemClicked(BluetoothDevice device) {
                advertiserService.disconnectDevice(device);
            }
        });
        binding.rvDevice.setAdapter(adapter);
        binding.switchAdv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean on = binding.switchAdv.isChecked();

                if (on) {
                    startAdvertising();
                } else {
                    stopAdvertising();
                }
            }
        });
        binding.btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = binding.etAdv.getText().toString();
                advertiserService.sendMessage(msg);
            }
        });

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int errorCode = intent.getIntExtra(BLEAdvertiserService.ADVERTISING_FAILED_EXTRA_CODE, -1);
                binding.switchAdv.setChecked(false);

                String errorMessage = "Error Occurred";
                switch (errorCode) {
                    case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                        errorMessage += " : Already Started.";
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                        errorMessage += " : too large data.";
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                        errorMessage += " : feature unsupported.";
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                        errorMessage += " : internal error.";
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                        errorMessage += " : too many advertisers.";
                        break;
                    case BLEAdvertiserService.ADVERTISING_TIMED_OUT:
                        errorMessage = " : time out.";
                        break;
                    default:
                        errorMessage += " : unknown error.";
                }

                Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        };

        boolean bleEnabled = BLEUtils.checkBluetoothAdapter(this);
        if(!bleEnabled) {
            Toast.makeText(this, "Turn on Bluetooth", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if(BLEAdvertiserService.running) {
            binding.switchAdv.setChecked(true);
        } else {
            binding.switchAdv.setChecked(false);
        }

        IntentFilter failureFilter = new IntentFilter(BLEAdvertiserService.ADVERTISING_FAILED);
        registerReceiver(receiver, failureFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    private void startAdvertising() {
        isBind = bindService(new Intent(getApplicationContext(), BLEAdvertiserService.class)
                , connection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Stops BLE Advertising by stopping {@code AdvertiserService}.
     */
    private void stopAdvertising() {
        if(isBind) {
            advertiserService.removeCallback();
            unbindService(connection);
            isBind = false;
        }
        binding.switchAdv.setChecked(false);
    }

    // broadcast
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            advertiserService = ((BLEAdvertiserService.LocalBinder) service).getService();
            advertiserService.registerCallback(advertiseCallback);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private BLEAdvertiserService.BLEAdvertiseCallback advertiseCallback = new BLEAdvertiserService.BLEAdvertiseCallback() {
        @Override
        public void addConnectedDevice(BluetoothDevice device) {
            adapter.addDevice(device);
            adapter.notifyDataSetChanged();
        }

        @Override
        public void removeConnectedDevice(BluetoothDevice device) {
            adapter.removeDevice(device);
            adapter.notifyDataSetChanged();
        }
    };
}
