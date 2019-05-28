package com.wotosts.bleadvertisingtest.ble.server;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;


import com.wotosts.bleadvertisingtest.R;
import com.wotosts.bleadvertisingtest.ble.BLEUtils;
import com.wotosts.bleadvertisingtest.ble.client.BluetoothClientTestActivity;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BLEAdvertiserService extends Service {

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        BLEAdvertiserService getService() {
            return BLEAdvertiserService.this;
        }
    }

    interface BLEAdvertiseCallback {
        void addConnectedDevice(BluetoothDevice device);

        void removeConnectedDevice(BluetoothDevice device);
    }

    private BLEAdvertiseCallback activityCallback;

    // 10 minutes
    public final static long TIMEOUT = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);
    public final static int FOREGROUND_NOTI_ID = 1;
    public static final int ADVERTISING_TIMED_OUT = 6;
    public static final String ADVERTISING_FAILED =
            "com.wotosts.bleadvertisingtest Advertising_failed";

    public static final String ADVERTISING_FAILED_EXTRA_CODE = "failureCode";

    private List<BluetoothDevice> devicesList;
    private List<BluetoothGatt> deviceGatt;

    private BluetoothGattServer gattServer;
    private BluetoothLeAdvertiser advertiser;
    private BluetoothGattCharacteristic serverCharacteristic;
    private BluetoothGattService service;
    private AdvertiseCallback callback;
    private Handler handler;
    private Runnable timeoutRunnable;

    public static boolean running = false;

    public BLEAdvertiserService() {
        handler = new Handler();
    }

    @Override
    public void onCreate() {
        running = true;
        init();
        startAdvertising();
        setTimeout();
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        running = false;
        stopAdvertising();
        stopServer();
        handler.removeCallbacks(timeoutRunnable);
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return binder;
    }

    public void registerCallback(BLEAdvertiseCallback callback) {
        activityCallback = callback;
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (BluetoothDevice device : devicesList)
                    activityCallback.addConnectedDevice(device);

                Toast.makeText(getApplicationContext(), "Connected Devices " + devicesList.size(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public void removeCallback() {
        activityCallback = null;
    }

    private void init() {
        devicesList = new ArrayList<>();
        deviceGatt = new ArrayList<>();

        if (advertiser == null) {
            final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null) {
                BluetoothAdapter adapter = bluetoothManager.getAdapter();
                if (adapter != null) {
                    advertiser = adapter.getBluetoothLeAdvertiser();

                    gattServer = bluetoothManager.openGattServer(this, serverCallback);

                    setupServer();
                }
            }
        }
    }

    private void setupServer() {
        service = new BluetoothGattService(BLEUtils.Service_UUID.getUuid(), BluetoothGattService.SERVICE_TYPE_PRIMARY);
        serverCharacteristic = new BluetoothGattCharacteristic(BLEUtils.Service_Characteristic_UUID.getUuid(),
                BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(BLEUtils.Service_Descriptor_UUID.getUuid(), BluetoothGattDescriptor.PERMISSION_READ);
        descriptor.setValue("test".getBytes());

        serverCharacteristic.addDescriptor(descriptor);
        service.addCharacteristic(serverCharacteristic);
        gattServer.addService(service);
    }

    private void stopServer() {
        if (gattServer != null)
            gattServer.close();
    }

    public void disconnectDevice(BluetoothDevice device) {
        gattServer.cancelConnection(device);
    }


    // update connected device list on ui
    public void addDevice(final BluetoothDevice device) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                devicesList.add(device);
                if (activityCallback != null) {
                    activityCallback.addConnectedDevice(device);
                    Toast.makeText(getApplicationContext(), "Connected Devices " + devicesList.size(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void removeDevice(final BluetoothDevice device) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                devicesList.remove(device);
                if (activityCallback != null)
                    activityCallback.removeConnectedDevice(device);
                Toast.makeText(getApplicationContext(), "Connected Devices " + devicesList.size(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setTimeout() {
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                sendFailureIntent(ADVERTISING_TIMED_OUT);
                stopSelf();
            }
        };

        handler.postDelayed(timeoutRunnable, TIMEOUT);
    }

    private void goForeground() {
        Intent notificationIntent = new Intent(this, BluetoothClientTestActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new Notification.Builder(this)
                .setContentTitle("Advertising via BLE")
                .setContentText("This device is discoverable to others nearby.")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(FOREGROUND_NOTI_ID, notification);
    }

    @TargetApi(26)
    private void goForegroundOreo() {
        String NOTIFICATION_CHANNEL_ID = "com.wotosts.bleadvertisingtest";
        String channelName = "Lumini Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Advertising via BLE")
                .setContentText("This device is discoverable to others nearby.")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }

    private AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);        // advertising 주기 조절
        settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM);       // -7
        settingsBuilder.setConnectable(true);
        settingsBuilder.setTimeout(0);
        return settingsBuilder.build();
    }

    private AdvertiseData buildAdvertiseData() {

        /**
         * Note: There is a strict limit of 31 Bytes on packets sent over BLE Advertisements.C
         *  This includes everything put into AdvertiseData including UUIDs, device info, &
         *  arbitrary service or manufacturer data.
         *  Attempting to send packets over this limit will result in a failure with error code
         *  AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE. Catch this error in the
         *  onStartFailure() method of an AdvertiseCallback implementation.
         */

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.addServiceUuid(BLEUtils.Gatt_UUID);

        return dataBuilder.build();
    }

    private AdvertiseData buildScanResponseData() {
        return new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(true)
                .build();
    }

    private void startAdvertising() {
        // notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            goForegroundOreo();
        else
            goForeground();

        if (callback == null) {
            AdvertiseSettings settings = buildAdvertiseSettings();
            AdvertiseData data = buildAdvertiseData();
            AdvertiseData scanResponse = buildScanResponseData();
            callback = new AdvertiseCallback() {
                @Override
                public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                    super.onStartSuccess(settingsInEffect);
                }

                @Override
                public void onStartFailure(int errorCode) {
                    super.onStartFailure(errorCode);

                    sendFailureIntent(errorCode);
                    stopSelf();
                }
            };

            if (advertiser != null)
                advertiser.startAdvertising(settings, data, scanResponse, callback);
        }
    }

    private void stopAdvertising() {
        if (advertiser != null) {
            advertiser.stopAdvertising(callback);
            callback = null;
        }
    }

    private void sendFailureIntent(int errorCode) {
        Intent failureIntent = new Intent();
        failureIntent.setAction(ADVERTISING_FAILED);
        failureIntent.putExtra(ADVERTISING_FAILED_EXTRA_CODE, errorCode);
        sendBroadcast(failureIntent);
    }

    public void sendMessage(String msg) {
        // msg to byte
        serverCharacteristic.setValue(msg);
        for (BluetoothDevice device : devicesList) {
            gattServer.notifyCharacteristicChanged(device, serverCharacteristic, false);
        }
    }

    public void startNewActivity(Context context, String packageName) {
        if (packageName.contains("chrome")) {
            packageName = "com.android.chrome";
        }

        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(intent);
        }
    }

    BluetoothGattServerCallback serverCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                addDevice(device);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                removeDevice(device);
            }
        }

        @Override
        public void onCharacteristicWriteRequest(final BluetoothDevice device, final int requestId, final BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite, boolean responseNeeded, int offset, final byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);

            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (characteristic.getUuid().equals(BLEUtils.Service_Characteristic_UUID.getUuid())) {
                        // send response to client
                        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
                        String messageStr = "";
                        try {
                            messageStr = new String(value, "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }

                        // open app
                        startNewActivity(getApplicationContext(), messageStr);

                        // change characteristic data
                        messageStr = "Server Response " + messageStr;
                        characteristic.setValue(messageStr);
                        Toast.makeText(getApplicationContext(), device.getAddress() + " " + messageStr + " sent", Toast.LENGTH_SHORT).show();

                        for (BluetoothDevice client : devicesList) {
                            gattServer.notifyCharacteristicChanged(client, characteristic, false);
                        }
                    }
                }
            });
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

            handler.post(() -> Toast.makeText(getApplicationContext(), "Server onCharacteristicReadRequest", Toast.LENGTH_SHORT).show());
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
        }
    };
}
