package com.wotosts.bleadvertisingtest.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.ParcelUuid;

public class BLEUtils {
    public static final ParcelUuid Gatt_UUID = ParcelUuid
            .fromString("3ede00c2-667a-4f4c-b5c9-931953772c24");

    public static final ParcelUuid Service_UUID = ParcelUuid
            .fromString("c933b9c9-22ca-4c3d-a2fd-fb8928cde26d");

    public static final ParcelUuid Service_Characteristic_UUID = ParcelUuid
            .fromString("4df4445f-7482-4fe6-b89f-0ceab7ef140f");
    public static final ParcelUuid Service_Descriptor_UUID = ParcelUuid
            .fromString("4df44460-7482-4fe6-b89f-0ceab7ef140f");

    public static final String PROXIMITY_UNKNOWN = "Unknown";
    public static final String PROXIMITY_NEAR = "Near";
    public static final String PROXIMITY_FAR = "Far";
    public static final String PROXIMITY_IMMEDIATE = "Immediate";

    public static boolean uuidMatches(String uuidString, String... matches) {
        for (String match : matches) {
            if (uuidString.equalsIgnoreCase(match)) {
                return true;
            }
        }

        return false;
    }

    public static boolean checkBluetoothAdapter(Context context) {

        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();

        // 블루투스 활성화여부 확인
        if (adapter == null || !adapter.isEnabled())
            return false;
        else
            return true;
    }

    public static float calculateAccuracy(int txPower, float rssi) {
        if (rssi == 0) {
            return -1.0f; // if we cannot determine accuracy, return -1.
        }

        if(txPower == Integer.MIN_VALUE)
            return -1.0f;

        double ratio = rssi*1.0/txPower;
        if (ratio < 1.0) {
            return (float)Math.pow(ratio,10);
        }
        else {
            float accuracy =  (float) ((0.89976f)*Math.pow(ratio,7.7095f) + 0.111f);
            return accuracy;
        }
    }

    public static String calculateProximity(double accuracy) {
        if (accuracy < 0) {
            return PROXIMITY_UNKNOWN;
            // is this correct?  does proximity only show unknown when accuracy is negative?  I have seen cases where it returns unknown when
            // accuracy is -1;
        }
        if (accuracy < 0.5 ) {
            return PROXIMITY_IMMEDIATE;
        }
        // forums say 3.0 is the near/far threshold, but it looks to be based on experience that this is 4.0
        else if (accuracy <= 4.0) {
            return PROXIMITY_NEAR;
        }
        // if it is > 4.0 meters, call it far
        return PROXIMITY_FAR;

    }
}
