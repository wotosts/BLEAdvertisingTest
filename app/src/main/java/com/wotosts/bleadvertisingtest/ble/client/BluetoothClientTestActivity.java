package com.wotosts.bleadvertisingtest.ble.client;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.wotosts.bleadvertisingtest.R;
import com.wotosts.bleadvertisingtest.ble.BLEUtils;
import com.wotosts.bleadvertisingtest.databinding.ActivityBluetoothClientTestBinding;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BluetoothClientTestActivity extends AppCompatActivity {

    long SCAN_PERIOD = 10000;      // ms

    private ActivityBluetoothClientTestBinding binding;

    private BluetoothAdapter bleAdapter;
    private BluetoothLeScanner scanner;
    private boolean scanning;
    private Handler handler;
    private LeResultListAdapter leResultListAdapter;
    private BluetoothGatt gatt;
    private boolean echo;
    private boolean connected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_bluetooth_client_test);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bleAdapter = bluetoothManager.getAdapter();
        scanner = bleAdapter.getBluetoothLeScanner();
        handler = new Handler();

        boolean bleEnabled = BLEUtils.checkBluetoothAdapter(this);
        if (!bleEnabled) {
            Toast.makeText(this, "Turn on Bluetooth", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        leResultListAdapter = new LeResultListAdapter(new LeResultListAdapter.ItemClickListener() {
            @Override
            public void onItemClicked(ScanResult result) {
                connectDevice(result);
            }
        });
        binding.rvDevice.setAdapter(leResultListAdapter);
        scanLeDevice(true);
    }

    @Override
    protected void onPause() {
        super.onPause();

        scanLeDevice(false);
        leResultListAdapter.clear();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bluetooth_scan_menu, menu);
        if (!scanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                leResultListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    private void scanLeDevice(boolean enable) {
        if (enable) {
            disconnectGattServer();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanning = false;
                    disconnectGattServer();
                    scanner.stopScan(leScanCallback);
                    connectDeviceByRssi();
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            scanning = true;
            leResultListAdapter.clear();
            scanner.startScan(buildScanFilters(), buildScanSettings(), leScanCallback);
            //scanner.startScan(leScanCallback);
        } else {
            scanning = false;
            disconnectGattServer();
            scanner.stopScan(leScanCallback);
            connectDeviceByRssi();
        }
        invalidateOptionsMenu();
    }

    private List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();

        ScanFilter.Builder builder = new ScanFilter.Builder();
        // Comment out the below line to see all BLE devices around you
        builder.setServiceUuid(BLEUtils.Gatt_UUID);
        scanFilters.add(builder.build());

        return scanFilters;
    }

    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        return builder.build();
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public void disconnectGattServer() {
        connected = false;

        leResultListAdapter.setConnected(null);
        leResultListAdapter.notifyDataSetChanged();

        if (gatt != null) {
            gatt.disconnect();
            gatt.close();
        }
    }

    private void connectDeviceByRssi() {
        disconnectGattServer();
        Collections.sort(leResultListAdapter.getResultList(), new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult o1, ScanResult o2) {
                return -1 * Integer.compare(o1.getRssi(), o2.getRssi());
            }
        });

        if (leResultListAdapter.getItemCount() != 0)
            connectDevice(leResultListAdapter.getResultList().get(0));
    }

    private void connectDevice(ScanResult result) {
        ScanResult preResult = leResultListAdapter.getConnected();

        if (preResult != null && preResult.equals(result)) {
            disconnectGattServer();
        } else {
            BluetoothDevice device = result.getDevice();

            gatt = device.connectGatt(this, false, gattCallback);

            if (gatt != null) {
                leResultListAdapter.setConnected(result);
                leResultListAdapter.notifyDataSetChanged();
                setConnected(true);
            }
        }
    }

    private void readMessage(BluetoothGattCharacteristic characteristic) {
        if (!connected)
            return;

        boolean success = gatt.readCharacteristic(characteristic);
        if (success) {
            handler.post(() -> Toast.makeText(this, "Read Request", Toast.LENGTH_LONG).show());
        }
    }

    public void onSendMessageBtnClicked(View view) {
        if (!connected || !echo) {
            return;
        }

        BluetoothGattService service = gatt.getService(BLEUtils.Service_UUID.getUuid());
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(BLEUtils.Service_Characteristic_UUID.getUuid());

        if (characteristic == null) {
            disconnectGattServer();
            return;
        }

        String message = binding.etClient.getText().toString();

        byte[] messageBytes = message.getBytes();
        if (messageBytes.length == 0) {
            return;
        }

        characteristic.setValue(messageBytes);
        boolean success = gatt.writeCharacteristic(characteristic);
        if (success) {
            Toast.makeText(this, "Write Request", Toast.LENGTH_LONG).show();
        }
    }

    public void showToast(String msg) {
        handler.post(() -> Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show());
    }

    // ble랑 일반 bluetooth 동시에 스캔못함
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            super.onScanResult(callbackType, result);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    leResultListAdapter.addResult(result);
                    leResultListAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);

            for (ScanResult result : results)
                leResultListAdapter.addResult(result);
            leResultListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (status == BluetoothGatt.GATT_FAILURE) {
                disconnectGattServer();
                return;
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnectGattServer();
                return;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                setConnected(true);
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                disconnectGattServer();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                return;
            }

            BluetoothGattService service = gatt.getService(BLEUtils.Service_UUID.getUuid());
            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
            for (BluetoothGattCharacteristic characteristic : characteristics) {
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                enableCharacteristicNotification(gatt, characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handler.post(() -> Toast.makeText(getApplicationContext(), "onCharacteristicWrite Success", Toast.LENGTH_SHORT).show());
            } else
                disconnectGattServer();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] messageBytes = characteristic.getValue();
                try {
                    final String message = "Read " + new String(messageBytes, "UTF-8");
                    showToast(message);
                    handler.post(() -> binding.etClient.setText(message));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            readMessage(characteristic);
            byte[] messageBytes = characteristic.getValue();
            try {
                final String messageStr = new String(messageBytes, "UTF-8");
                if (messageStr.equals("Disconnect")) {
                    handler.post(() -> {
                        disconnectGattServer();
                        leResultListAdapter.clear();
                        binding.rvDevice.invalidate();
                    });
                }

                showToast(messageStr);
                handler.post(() -> binding.etClient.setText(messageStr));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        private void enableCharacteristicNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            boolean characteristicWriteSuccess = gatt.setCharacteristicNotification(characteristic, true);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BLEUtils.Service_Descriptor_UUID.getUuid());
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);

            if (characteristicWriteSuccess) {
                if (BLEUtils.uuidMatches(characteristic.getUuid().toString(), BLEUtils.Service_Characteristic_UUID.toString())) {
                    echo = true;
                }
            }
        }
    };
}
