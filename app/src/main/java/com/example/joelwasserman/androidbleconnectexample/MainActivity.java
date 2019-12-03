package com.example.joelwasserman.androidbleconnectexample;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    class GoProMessage{
        public String uuid;
        public byte[] message = {};
        public int type = 1;

        final static int READ = 0;
        final static int WRITE = 1;
        final static int NOTIFICATION = 2;

        GoProMessage(String uuid, byte[] message) {
            this.uuid = uuid;
            this.message = message;
        }

        GoProMessage(int type, String uuid, byte[] message) {
            this.uuid = uuid;
            this.message = message;
            this.type = type;
        }

        GoProMessage(int type, String uuid) {
            this.uuid = uuid;
            this.type = type;
        }

        @Override
        public String toString() {
            return "(" + this.uuid + ") " + new String(this.message);
        }
    }

    // Service: b5f90001-aa8d-11e3-9046-0002a5d5c51b
    final static String HANDLE_0x0023_ONBOARD_WIFI_SSID = "b5f90002-aa8d-11e3-9046-0002a5d5c51b";
    final static String HANDLE_0x0025_ONBOARD_WIFI_PASSWORD = "b5f90003-aa8d-11e3-9046-0002a5d5c51b";
    final static String HANDLE_0x0027_ONBOARD_WIFI_SWITCH = "b5f90004-aa8d-11e3-9046-0002a5d5c51b";
    final static String HANDLE_0x0029_ONBOARD_WIFI_STATE = "b5f90005-aa8d-11e3-9046-0002a5d5c51b";
    final static String HANDLE_0x002c_ONBOARD_WIFI_HTTP_PASS = "b5f90006-aa8d-11e3-9046-0002a5d5c51b";

    // Service: 0000fea6-0000-1000-8000-00805f9b34fb
    final static String HANDLE_0x002f_COMMAND = "b5f90072-aa8d-11e3-9046-0002a5d5c51b";
    final static String HANDLE_0x0032_COMMAND_RESPONSE = "b5f90073-aa8d-11e3-9046-0002a5d5c51b"; // Notify 0x0031?

    final static String HANDLE_0x0034_SET_SETTING = "b5f90074-aa8d-11e3-9046-0002a5d5c51b";
    final static String HANDLE_0x0037_SET_SETTING_RESPONSE = "b5f90075-aa8d-11e3-9046-0002a5d5c51b";

    final static String HANDLE_0x0039_QUERY_REQUEST = "b5f90076-aa8d-11e3-9046-0002a5d5c51b";
    final static String HANDLE_0x003c_QUERY_REQUEST_RESPONSE = "b5f90077-aa8d-11e3-9046-0002a5d5c51b";

    final static String HANDLE_0x003e_SENSOR_DATA = "b5f90078-aa8d-11e3-9046-0002a5d5c51b";
    final static String HANDLE_0x0041_SENSOR_DATA_RESPONSE = "b5f90079-aa8d-11e3-9046-0002a5d5c51b";

    // Service: b5f90090-aa8d-11e3-9046-0002a5d5c51b
    final static String HANDLE_0x0044_COMMAND_NEW = "b5f90091-aa8d-11e3-9046-0002a5d5c51b"; // Newer Command?
    final static String HANDLE_0x0047_COMMAND_RESPONSE_NEW = "b5f90092-aa8d-11e3-9046-0002a5d5c51b"; // Newer CommandResponse?

    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    Button startScanningButton;
    Button stopScanningButton;
    TextView peripheralTextView;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    Boolean btScanning = false;
    int deviceIndex = 0;
    ArrayList<BluetoothDevice> devicesDiscovered = new ArrayList<BluetoothDevice>();
    EditText deviceIndexInput;
    Button connectToDevice;
    Button disconnectDevice;
    BluetoothGatt bluetoothGatt;

    Context context;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    public Map<String, String> uuids = new HashMap<String, String>();

    // Stops scanning after 5 seconds.
    private Handler mHandler = new Handler();
    private static final long SCAN_PERIOD = 5000;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    Queue<GoProMessage> messagesToSend = new LinkedList<>();

    private boolean writingToBluetooth = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        peripheralTextView = (TextView) findViewById(R.id.PeripheralTextView);
        peripheralTextView.setMovementMethod(new ScrollingMovementMethod());
        deviceIndexInput = (EditText) findViewById(R.id.InputIndex);
        deviceIndexInput.setText("0");

        connectToDevice = (Button) findViewById(R.id.ConnectButton);
        connectToDevice.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                connectToDeviceSelected();
            }
        });

        disconnectDevice = (Button) findViewById(R.id.DisconnectButton);
        disconnectDevice.setVisibility(View.INVISIBLE);
        disconnectDevice.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                disconnectDeviceSelected();
            }
        });

        startScanningButton = (Button) findViewById(R.id.StartScanButton);
        startScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startScanning();
            }
        });

        stopScanningButton = (Button) findViewById(R.id.StopScanButton);
        stopScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopScanning();
            }
        });
        stopScanningButton.setVisibility(View.INVISIBLE);

        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();

        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        context = this;

        }

    public static byte[] concat(byte[]... bytes) {

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        for (int i = 0; i < bytes.length; i++) {
            try {
                output.write(bytes[i]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return output.toByteArray();
    }

    public static String byteArrayToHex(byte[] a) {

        try {
            StringBuilder sb = new StringBuilder(a.length * 2);
            for (byte b : a)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch(NullPointerException e) {

        }

        return "";
    }

    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result.getDevice().getName() == null) {
                return;
            }

            peripheralTextView.append("Index: " + deviceIndex + ", Device Name: " + result.getDevice().getName() + ", MAC: " + result.getDevice().getAddress() + " rssi: " + result.getRssi() + "\n");
            devicesDiscovered.add(result.getDevice());
            deviceIndex++;
            // auto scroll for text view
            final int scrollAmount = peripheralTextView.getLayout().getLineTop(peripheralTextView.getLineCount()) - peripheralTextView.getHeight();
            // if there is no need to scroll, scrollAmount will be <=0
            if (scrollAmount > 0) {
                peripheralTextView.scrollTo(0, scrollAmount);
            }

            if (result.getDevice().getName().equals("GoPro 2761")) {
                stopScanning();
                bluetoothGatt = result.getDevice().connectGatt(context, false, btleGattCallback);
            }
        }
    };

    // Device connect call back
    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            // this will get called anytime you perform a read or write characteristic operation
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    peripheralTextView.append("device read or wrote to\n");
                }
            });

            System.out.println("Characteristic changed: " + characteristic.getUuid() + ", value: " + byteArrayToHex(characteristic.getValue()) + "(" + new String(characteristic.getValue()) + ")");

            // Wait for Device ID to be sent
            if(byteArrayToHex(characteristic.getValue()).startsWith("0403810801")) {
                // ??
                messagesToSend.add(new GoProMessage(GoProMessage.WRITE, HANDLE_0x002f_COMMAND, new byte[]{0x04, (byte) 0xf1, 0x7d, 0x08, 0x04}));
            }

            if(byteArrayToHex(characteristic.getValue()).startsWith("04f1fd0801")) {
                // ??
                messagesToSend.add(new GoProMessage(GoProMessage.WRITE, HANDLE_0x002f_COMMAND, new byte[]{0x03, 0x07, 0x01, 0x01}));
            }

            if(byteArrayToHex(characteristic.getValue()).startsWith("020702")) { // normally 021700
                messagesToSend.add(new GoProMessage(GoProMessage.WRITE, HANDLE_0x0044_COMMAND_NEW, new byte[]{0x02, 0x02, 0x02}));
            }

            if(byteArrayToHex(characteristic.getValue()).startsWith("06028208011002")) {
                // ??
                messagesToSend.add(new GoProMessage(GoProMessage.WRITE, HANDLE_0x0039_QUERY_REQUEST, new byte[]{0x02, (byte)0xf5, 0x74}));
            }

            if(byteArrayToHex(characteristic.getValue()).startsWith("12f5f40800100018002000280c280728043001")) {
                // ??
                messagesToSend.add(new GoProMessage(GoProMessage.WRITE, HANDLE_0x0039_QUERY_REQUEST, new byte[]{0x06, (byte)0xf5, 0x72, 0x08, 0x01, 0x08, 0x02}));
            }

            // Connect to Happy Snail after 020202 (and others?) are done (first packet: 06028208011002, done packet like: 0a020b0805100118032005)
            if(byteArrayToHex(characteristic.getValue()).startsWith("0a020b0805100")) {
//                // Scan for SSID's
//                messagesToSend.add(new GoProMessage(GoProMessage.WRITE, HANDLE_0x0044_COMMAND_NEW, new byte[]{0x04, 0x02, (byte) 0x8b, 0x08, 0x01}));
//                messagesToSend.add(new GoProMessage(GoProMessage.WRITE, HANDLE_0x0044_COMMAND_NEW, new byte[]{0x08, 0x02, 0x03, 0x08, 0x00, 0x10, 0x64, 0x18, 0x01}));
//            }
//
//            // Look for the end packet terminator (maybe?) in the SSID results list
//            if(byteArrayToHex(characteristic.getValue()).contains("100020")) {
//                // Not in btsnoop_hci.gopro-app.happy snail.beep-1-1080p-60w.log?
////                messagesToSend.add(new GoProMessage(GoProMessage.WRITE, HANDLE_0x002f_COMMAND, new byte[]{0x02, (byte) 0xf1, 0x78}));
////
////            }
////            if(byteArrayToHex(characteristic.getValue()).endsWith("04f1f80801")) {
//                // Connect to SSID
                messagesToSend.add(new GoProMessage(GoProMessage.WRITE, HANDLE_0x0044_COMMAND_NEW, concat(new byte[]{ 0x11, 0x02, 0x04, 0x0a, 0x0b }, "Happy Snail".getBytes(), new byte[]{0x10, 0x00})));
            }

            // Send something (?) after connection is made (first packet: 080284080110021814, done packet like: 04020c0805)
            if (byteArrayToHex(characteristic.getValue()).startsWith("04020c08")) {
//                messagesToSend.add(new GoProMessage(GoProMessage.WRITE, HANDLE_0x0044_COMMAND_NEW, new byte[]{0x04, 0x02, (byte)0x8c, 0x08, 0x01}));
//
//                // Some information request? Probably not required?
//                messagesToSend.add(new GoProMessage(GoProMessage.WRITE, HANDLE_0x0039_QUERY_REQUEST, new byte[]{0x02, (byte)0xf5, 0x74}));
//
//            }
//
//            // Possibly unrelated? ---->
//            //    059300460126 comes in on handle 0x003b, which seems to be in response to a request ages ago but might need to be sent to find out when ready?
//            // Send RTMP Server after the thing above is done (first packet: 059300180104, done packet: 059300460126)
//            // if (byteArrayToHex(characteristic.getValue()).equals("059300460126")) { // <-- unrelated status response maybe? Not in original dump
//            if (byteArrayToHex(characteristic.getValue()).equals("12f5f40800100018002000280c280728043001")) {

                // GPCAMERA_LIVE_STREAM_SETUP
                String rtmpEndpoint = "rtmp://192.168.1.38/live12345689/goprowhooscoopypoopwhoop123456789";
                byte shouldLocallyRecord = 0x00;
                byte streamResolution = 0x07; // 0x04 = 480p, 0x07 = 720p, 0x0c = 1080p

                // Construct the payload - it seems to be field_hex then value (except for strings which are field_hex then string_length then value)

                // Don't know what this field is??
                // - 0x79 is normal (puts into stream mode and returns 04f1f90801)
                // - 0x80 returns 02f102 error
                // - 0x78 returns 04f1f80801 (no stream mode)
                // - 0x77 returns 04f1f70801 (no stream mode)
                // - 0x71 returns 04f1f10800 (no stream mode)
                byte[] payload = new byte[]{(byte) 0xf1, (byte)0x79 };

                payload = concat(payload, new byte[]{0x0a, (byte) rtmpEndpoint.length()}, rtmpEndpoint.getBytes());
                payload = concat(payload, new byte[]{0x10, shouldLocallyRecord});
                payload = concat(payload, new byte[]{0x18, streamResolution});

                // Prepend message type (0x20) and payload length header to the front
                payload = concat(new byte[]{0x20, (byte)payload.length}, payload);

                // Send initial packet with message type
                messagesToSend.add(new GoProMessage(GoProMessage.WRITE, HANDLE_0x002f_COMMAND, Arrays.copyOfRange(payload, 0, 20)));

                int messageOffset = 0;
                int MESSAGE_SIZE = 19; // 20 bytes - 1 byte continuation header (0x80++)

                for (int bytePosition = 20; bytePosition < payload.length; bytePosition += MESSAGE_SIZE) {
                    byte[] dataChunk = concat(new byte[]{(byte) (0x80 + messageOffset)}, Arrays.copyOfRange(payload, bytePosition, Math.min(bytePosition + MESSAGE_SIZE, payload.length)));

                    messagesToSend.add(new GoProMessage(GoProMessage.WRITE, HANDLE_0x002f_COMMAND, dataChunk));

                    messageOffset++;
                }
            }

            // Signal go live after RTMP config acknowledgement
            if (byteArrayToHex(characteristic.getValue()).equals("04f1f90801")) {
                messagesToSend.add(new GoProMessage(GoProMessage.WRITE, HANDLE_0x002f_COMMAND, new byte[]{0x03, 0x01, 0x01, 0x01}));    // SHUTTER_ON
            }

            sendNextMessage();
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            // this will get called when a device connects or disconnects
            System.out.println(newState);
            switch (newState) {
                case 0:
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            peripheralTextView.append("device disconnected\n");
                            connectToDevice.setVisibility(View.VISIBLE);
                            disconnectDevice.setVisibility(View.INVISIBLE);
                        }
                    });
                    break;
                case 2:
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            peripheralTextView.append("device connected\n");
                            connectToDevice.setVisibility(View.INVISIBLE);
                            disconnectDevice.setVisibility(View.VISIBLE);
                        }
                    });

                    // discover services and characteristics for this device
                    bluetoothGatt.discoverServices();

                    break;
                default:
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            peripheralTextView.append("we encounterned an unknown state, uh oh\n");
                        }
                    });
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            // this will get called after the client initiates a 			BluetoothGatt.discoverServices() call
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    peripheralTextView.append("device services have been discovered\n");
                }
            });

            displayGattServices(bluetoothGatt.getServices());

            sendNextMessage();
        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }

            sendNextMessage();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            System.out.println("Characteristic Written - status: " + status + ", uuid: " + characteristic.getUuid() + ", value: " + byteArrayToHex(characteristic.getValue()) + "(" + new String(characteristic.getValue()) + ")");

            writingToBluetooth = false;

            sendNextMessage();
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            System.out.println("onDescriptorRead: " + descriptor.getUuid() + ", status: " + status + ", value: " + new String(descriptor.getValue()));

            sendNextMessage();
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            System.out.println("onDescriptorWrite: " + descriptor.getUuid() + ", status: " + status + ", value: " + new String(descriptor.getValue()));

            sendNextMessage();
        }
    };

    private void sendNextMessage() {
        if (writingToBluetooth) {
            return;
        }

        GoProMessage nextMessage = messagesToSend.poll();

        if (nextMessage == null) {
            return;
        }

        List<BluetoothGattService> services = bluetoothGatt.getServices();
        for (BluetoothGattService service : services) {
            for (final BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                if (characteristic.getUuid().toString().equals(nextMessage.uuid)) {
                    System.out.print("Sending next message to " + nextMessage.uuid);

                    boolean operationSuccess = false;

                    switch (nextMessage.type) {
                        case GoProMessage.WRITE:
                            System.out.println(" (write): " + byteArrayToHex(nextMessage.message) + "(" + new String(nextMessage.message) + ")");
                            boolean setValue = characteristic.setValue(nextMessage.message);
                            System.out.println("- setValue: " + setValue);
                            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

                            operationSuccess = bluetoothGatt.writeCharacteristic(characteristic);

                            if (operationSuccess) {
                                writingToBluetooth = true;
                            }

                            break;

                        case GoProMessage.READ:
                            System.out.println(" (read)");
                            operationSuccess = bluetoothGatt.readCharacteristic(characteristic);

                            break;

                        case GoProMessage.NOTIFICATION:
                            System.out.println(" (notification)");
                            bluetoothGatt.setCharacteristicNotification(characteristic, true);

                            UUID uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

                            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(uuid);
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            operationSuccess = bluetoothGatt.writeDescriptor(descriptor);

                            break;
                    }

                    System.out.println("- Success? " + operationSuccess);

                    if (!operationSuccess) {
                        sendNextMessage();
                    }
                }
            }
        }
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {

        System.out.println("broadcastUpdate: " + characteristic.getUuid() + ", value: " + byteArrayToHex(characteristic.getValue()) + "(" + new String(characteristic.getValue()) + ")");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    public void startScanning() {
        System.out.println("start scanning");
        btScanning = true;
        deviceIndex = 0;
        devicesDiscovered.clear();
        peripheralTextView.setText("");
        peripheralTextView.append("Started Scanning\n");
        startScanningButton.setVisibility(View.INVISIBLE);
        stopScanningButton.setVisibility(View.VISIBLE);

        messagesToSend.clear();

        messagesToSend.add(new GoProMessage(GoProMessage.NOTIFICATION, HANDLE_0x0032_COMMAND_RESPONSE));
        messagesToSend.add(new GoProMessage(GoProMessage.NOTIFICATION, HANDLE_0x0037_SET_SETTING_RESPONSE));
        messagesToSend.add(new GoProMessage(GoProMessage.NOTIFICATION, HANDLE_0x003c_QUERY_REQUEST_RESPONSE));
        messagesToSend.add(new GoProMessage(GoProMessage.NOTIFICATION, HANDLE_0x0047_COMMAND_RESPONSE_NEW));

        messagesToSend.add(new GoProMessage(GoProMessage.WRITE, HANDLE_0x0044_COMMAND_NEW, concat(new byte[]{ 0x0e, 0x03, 0x01, 0x08, 0x00, 0x12, 0x08 }, "SM-N950F".getBytes())));

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.startScan(leScanCallback);
            }
        });

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScanning();
            }
        }, SCAN_PERIOD);
    }

    public void stopScanning() {
        System.out.println("stopping scanning");
        peripheralTextView.append("Stopped Scanning\n");
        btScanning = false;
        startScanningButton.setVisibility(View.VISIBLE);
        stopScanningButton.setVisibility(View.INVISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
    }

    public void connectToDeviceSelected() {
        peripheralTextView.append("Trying to connect to device at index: " + deviceIndexInput.getText() + "\n");
        int deviceSelected = Integer.parseInt(deviceIndexInput.getText().toString());
        bluetoothGatt = devicesDiscovered.get(deviceSelected).connectGatt(this, false, btleGattCallback);
    }

    public void disconnectDeviceSelected() {
        peripheralTextView.append("Disconnecting from device\n");
        bluetoothGatt.disconnect();
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {

            final String uuid = gattService.getUuid().toString();
            System.out.println("Service discovered: " + uuid);
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    peripheralTextView.append("Service disovered: "+uuid+"\n");
                }
            });
            new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic :
                    gattCharacteristics) {

                final String charUuid = gattCharacteristic.getUuid().toString();
                System.out.println("Characteristic discovered for service: " + charUuid);
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        peripheralTextView.append("Characteristic discovered for service: "+charUuid+"\n");
                    }
                });

            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.example.joelwasserman.androidbleconnectexample/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.example.joelwasserman.androidbleconnectexample/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}
