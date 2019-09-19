package com.example.bluetoothcontrol;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener ,View.OnClickListener {

    // GUI Components
    private TextView mBluetoothStatus;
    private TextView textView;
    private Button mListPairedDevicesBtn;
    private Button mDiscoverBtn;
    private BluetoothAdapter mBTAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private ArrayAdapter<String> mBTArrayAdapter;
    private ListView mDevicesListView;
    private ImageView forward;
    private ImageView left;
    private ImageView right;
    private ImageView stop;
    private ImageView back;

    private SensorManager sensorManager;
    private Sensor sensor;

    private RelativeLayout Con;
    private LinearLayout set;
    private LinearLayout second;
    private LinearLayout buttons;
    private LinearLayout tilt;
    private RadioButton bcontrol;
    private RadioButton tcontrol;
    private static Handler mHandler; // Our main handler that will receive callback notifications
    private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier
    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status
    private  int command =0;
    Handler handler;
    int click=0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothStatus = (TextView)findViewById(R.id.bluetoothStatus);
        mDiscoverBtn = (Button)findViewById(R.id.discover);
        mListPairedDevicesBtn = (Button)findViewById(R.id.PairedBtn);
        forward=(ImageView) findViewById(R.id.forward);
        left=(ImageView) findViewById(R.id.left);
        right =(ImageView)findViewById(R.id.right);
        stop =(ImageView)findViewById(R.id.stop);
        back=(ImageView)findViewById(R.id.back);
        set =(LinearLayout)findViewById(R.id.setting);
        Con =(RelativeLayout)findViewById(R.id.controls);
        second=(LinearLayout)findViewById(R.id.second);

        //declaring Sensor Manager and sensor type
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.unregisterListener(MainActivity.this);
        bcontrol=(RadioButton)findViewById(R.id.button);
        tcontrol=(RadioButton)findViewById(R.id.accelerometer);
        buttons=(LinearLayout)findViewById(R.id.moveButton);
        tilt=(LinearLayout)findViewById(R.id.tiltControl);
        //locate views
        textView =  findViewById(R.id.txt);
        mBTArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setTextColor(Color.WHITE);
                return textView;
            }
        };

        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio
        mDevicesListView = (ListView)findViewById(R.id.devicesListView);
        mDevicesListView.setAdapter(mBTArrayAdapter);// assign model to view
        mDevicesListView.setOnItemClickListener(mDeviceClickListener);
        final Switch sw = (Switch) findViewById(R.id.switch1);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    bluetoothOn(sw);
                } else {
                    bluetoothOff(sw);
                }
            }
        });

        mHandler = new Handler(){
            public void handleMessage(Message msg){


                if(msg.what == CONNECTING_STATUS){
                    if(msg.arg1 == 1){
                        mBluetoothStatus.setText("Connected to Device: " + (String)(msg.obj));
                        set.setVisibility(View.GONE);
                        Con.setVisibility(View.VISIBLE);
                    }
                    else
                        mBluetoothStatus.setText("Connection Failed");
                }
            }
        };
        if (mBTArrayAdapter == null) {
            // Device does not support Bluetooth
            mBluetoothStatus.setText("Status: Bluetooth not found");
            Toast.makeText(getApplicationContext(),"Bluetooth device not found!",Toast.LENGTH_SHORT).show();
        }
        else {
            forward.setOnClickListener(this);
            left.setOnClickListener(this);
            right.setOnClickListener(this);
            stop.setOnClickListener(this);
            bcontrol.setOnClickListener(this);
            tcontrol.setOnClickListener(this);
            mListPairedDevicesBtn.setOnClickListener(this);
            mDiscoverBtn.setOnClickListener(this);
            back.setOnClickListener(this);


        }
    }
    @Override
    public void onClick(View v)
    {
        if(v.getId()==R.id.button)
        {
            buttons.setVisibility(View.VISIBLE);
            tilt.setVisibility(View.GONE);
            sensorManager.unregisterListener(MainActivity.this);
        }
        else if (v.getId()==R.id.accelerometer)
        {   if(click!=0) {
                handler.removeCallbacksAndMessages(null);
            }
            buttons.setVisibility(View.GONE);
            tilt.setVisibility(View.VISIBLE);
            sensorManager.registerListener(MainActivity.this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        else if(v.getId()==R.id.PairedBtn)
        {
            listPairedDevices(v);
        }
        else if (v.getId()==R.id.discover)
        {
            discover(v);
        }
        else if(v.getId()==R.id.forward)
        {
            command = 1;
            click++;
            checkConnect(command);
        }
        else if (v.getId()==R.id.left)
        {
            command = 2;
            click++;
            checkConnect(command);
        }
        else if (v.getId()==R.id.right)
        {
            command = 3;
            click++;
            checkConnect(command);
        }
        else if(v.getId()==R.id.stop)
        {
            command = 4;
            click++;
            checkConnect(command);
        }
        else if (v.getId()==R.id.back)
        {

            set.setVisibility(View.VISIBLE);
            Con.setVisibility(View.GONE);


        }
    }

    private void bluetoothOn(View view){
        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            mBluetoothStatus.setText("Bluetooth enabled");
            Toast.makeText(getApplicationContext(),"Bluetooth turned on",Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(getApplicationContext(),"Bluetooth is already on", Toast.LENGTH_SHORT).show();
        }
    }

    // Enter here after user selects "yes" or "no" to enabling radio
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        super.onActivityResult(requestCode, resultCode, Data);
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                mBluetoothStatus.setText("Enabled");
            } else
                mBluetoothStatus.setText("Disabled");
        }
    }

    private void bluetoothOff(View view){
        mBTAdapter.disable(); // turn off
        mBluetoothStatus.setText("Bluetooth disabled");
        Toast.makeText(getApplicationContext(),"Bluetooth turned Off", Toast.LENGTH_SHORT).show();
    }

    private void discover(View view){
        // Check if the device is already discovering
        if(mBTAdapter.isDiscovering()){
            mBTAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(),"Discovery stopped",Toast.LENGTH_SHORT).show();
        }
        else{
            if(mBTAdapter.isEnabled()) {
                mBTArrayAdapter.clear(); // clear items
                mBTAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
                registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
            else{
                Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
            }
        }
    }

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mBTArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    private void listPairedDevices(View view){
        mPairedDevices = mBTAdapter.getBondedDevices();
        if(mBTAdapter.isEnabled()) {
            // put it's one to the adapter
            for (BluetoothDevice device : mPairedDevices)
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());

            Toast.makeText(getApplicationContext(), "Show Paired Devices", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            set.setVisibility(View.GONE);
            Con.setVisibility(View.VISIBLE);

            if(!mBTAdapter.isEnabled()) {
                Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }

            mBluetoothStatus.setText("Connecting...");
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0,info.length() - 17);

            // Spawn a new thread to avoid blocking the GUI one
            new Thread()
            {
                public void run() {
                    boolean fail = false;

                    BluetoothDevice device = mBTAdapter.getRemoteDevice(address);

                    try {
                        mBTSocket = createBluetoothSocket(device);
                    } catch (IOException e) {
                        fail = true;
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                    // Establish the Bluetooth socket connection.
                    try {
                        mBTSocket.connect();
                    } catch (IOException e) {
                        try {
                            fail = true;
                            mBTSocket.close();
                            mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                    .sendToTarget();
                        } catch (IOException e2) {
                            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if(fail == false) {
                        mConnectedThread = new ConnectedThread(mBTSocket);
                        mConnectedThread.start();

                        mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                                .sendToTarget();
                    }
                }
            }.start();

        }

    };

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connection with BT device using UUID
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.available();
                    if(bytes != 0) {
                        SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                        bytes = mmInStream.available(); // how many bytes are ready to be read?
                        bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget(); // Send the obtained bytes to the UI activity
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    break;
                }
            }
        }
        public void write(String input) {
            byte[] bytes = input.getBytes();
            try {
                mmOutStream.write(bytes);

            } catch (IOException e) {

            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        if (Math.abs(x) > Math.abs(y)) {
            if (x < 0) {
                textView.setText("Turn Right");
                textView.setTextColor(getResources().getColor(R.color.green));
                if(mConnectedThread != null) //First check to make sure thread created
                    mConnectedThread.write("3");
                Log.i("Info","3");
            }
            if (x > 0) {
                textView.setText("Turn Left");
                textView.setTextColor(getResources().getColor(R.color.blue));
                if(mConnectedThread != null) //First check to make sure thread created

                    mConnectedThread.write("2");
                Log.i("Info","2");
            }
        } else {
            if (y < 0) {
                textView.setText("Move Forward");
                textView.setTextColor(getResources().getColor(R.color.red));
                if(mConnectedThread != null) //First check to make sure thread created
                    mConnectedThread.write("1");
                Log.i("Info","1");
            }
            if (y > 0) {
                textView.setText("Stop");
                textView.setTextColor(getResources().getColor(R.color.yellow));
                if(mConnectedThread != null) //First check to make sure thread created
                    mConnectedThread.write("4");
                Log.i("Info","4");
            }
        }
        if (x > (-2) && x < (2) && y > (-2) && y < (2)) {
            textView.setText("No Change");
            textView.setTextColor(getResources().getColor(R.color.white));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //unregister Sensor listener
        sensorManager.unregisterListener(MainActivity.this);
    }
    public void checkConnect(final int command) {
        if(click!=1){
            handler.removeCallbacksAndMessages(null);}
        handler = new Handler();

        handler.postDelayed(new Runnable(){
            public void run(){
                if (mConnectedThread != null)//First check to make sure thread created
                    mConnectedThread.write(Integer.toString(command));
                Log.i("Info", Integer.toString(command));
                handler.postDelayed(this, 1000);
            }
        }, 1000);





    }

}

