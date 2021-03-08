package com.example.myapplication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.view.View;
import android.net.Uri;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

public class MainActivity extends Activity  {
    private static final String TAG = "hc06";
    private ImageView imgSet, imgset;
    private LinearLayout layout;
    private TextView text1,text2,text3,text4,text5,text6;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private String str = "";
    private ConnectedThread mConnectedThread;
    private ToggleButton button;

    // SPP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // MAC-address of Bluetooth module (you must edit this line)
    private static String address = "98:D3:41:FD:4E:7C";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("TEST", "...onCreate - try connect...");

        setContentView(R.layout.activity_main);

        imgSet = (ImageView) findViewById(R.id.imageSet);
        imgset = (ImageView) findViewById(R.id.imageset);
        layout = (LinearLayout) findViewById(R.id.layout);
        text1 = (TextView) findViewById(R.id.text1);
        text2 = (TextView) findViewById(R.id.text2);
        text3 = (TextView) findViewById(R.id.text3);
        text4 = (TextView) findViewById(R.id.text4);
        text5 = (TextView) findViewById(R.id.text5);
        text6 = (TextView) findViewById(R.id.text6);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        button = (ToggleButton)findViewById(R.id.toggleButton);

        checkBTState();
        ConnectingBluetooth();

        Button button1 = (Button) findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.kma.go.kr/"));
                startActivity(myIntent);
            }
        });

        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(button.isChecked()==true){
                    mConnectedThread.write("1");
                    Toast.makeText(MainActivity.this, "OFF", Toast.LENGTH_SHORT).show();
                } else {
                    mConnectedThread.write("2");
                    Toast.makeText(MainActivity.this, "ON", Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if (Build.VERSION.SDK_INT >= 10) {
            try {
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[]{UUID.class});
                return (BluetoothSocket) m.invoke(device, MY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "Could not create Insecure RFComm Connection", e);
            }
        }
        return device.createRfcommSocketToServiceRecord(MY_UUID);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("TEST", "...onResume - try connect...");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "...In onPause()...");
        try {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }

    private void checkBTState() {
        if (btAdapter == null) {
            errorExit("Fatal Error", "Bluetooth not support");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private void errorExit(String title, String message) {
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    private void ConnectingBluetooth() {
        BluetoothDevice device = btAdapter.getRemoteDevice(address);
        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }

        btAdapter.cancelDiscovery();

        Log.d(TAG, "...Connecting...");
        try {
            btSocket.connect();
            Toast.makeText(getApplicationContext(), "블루투스 연결 성공!", Toast.LENGTH_LONG).show();
            Log.d(TAG, "....Connection ok...");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        Log.d(TAG, "...Create Socket...");

        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
        mConnectedThread.write("0"); //안드로이드 <-> 아두이노 블루투스 연결 성공했을 때,
        Log.d("TEST", "...스레드 실행 - try connect...");
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String strBuf = new String(buffer, 0, bytes);

                    Log.d("test", "strBuf:" + strBuf);

                    for (int i = 0; i < bytes; i++) {
                        if (strBuf.charAt(i) == '#') { //먼지센서값
                            str = str.replace("#", "");
                            showMessage(str, "Dust");
                            str = "";
                            break;
                        } else if (strBuf.charAt(i) == '!') { // 온도값
                            str = str.replace("!", "");
                            showMessage(str, "Temperature");
                            str = "";
                            break;
                        } else if (strBuf.charAt(i) == '%') { // 습도값
                            str = str.replace("%", "");
                            showMessage(str, "Humidity");
                            Log.d("test1234", "str:" + str);
                            str = "";
                            break;
                        } else {
                            str += strBuf.charAt(i);
                            Log.d("test", "str:" + str);
                        }
                    }

                } catch (IOException e) {
                    break;
                }


            }
        }


        public void write(String message) {
            Log.d(TAG, "...Data to send: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d(TAG, "...Error data send: " + e.getMessage() + "...");
            }
        }
    }

    // 메시지 화면에 표시
    public void showMessage(String strMsg, String tmp) {
        // 메시지 텍스트를 핸들러에 전달
        Message msg = Message.obtain(mHandler, 0, strMsg);
        Log.d("DUST", "showmessage:" + strMsg);

        if (tmp == "Dust") {
            mHandler.sendMessage(msg);
        } else if (tmp == "Temperature") {
            mHandler2.sendMessage(msg);
        } else if (tmp == "Humidity") {
            mHandler3.sendMessage(msg);
        }
        Log.d("tag1", strMsg);
    }

    // 메시지 화면 출력을 위한 핸들러
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                String strMsg = (String) msg.obj;
                Log.d("test12", strMsg);
                Double dust = Double.parseDouble(strMsg);
                if (dust >= 0.01 && dust <= 30.0) {
                    String strColor = "#81BEF7";
                    layout.setBackgroundColor(Color.parseColor(strColor));
                    imgSet.setImageResource(R.drawable.love);
                    text1.setText("현재 상태 : 좋음");
                    text2.setText("공기 완전 좋아요~");
                    text6.setText("오늘은 좋은공기와 함께하세요~");
                } else if (dust > 30.0 && dust <= 80.0) {
                    String strColor = "#43CA00";
                    layout.setBackgroundColor(Color.parseColor(strColor));
                    imgSet.setImageResource(R.drawable.good);
                    text1.setText("현재 상태 : 보통");
                    text2.setText("나쁘지 않아요");
                    text6.setText("오늘은 몸상태 유의하여 활동하세요!");
                } else if (dust > 80.0 && dust <= 150.0) {
                    String strColor = "#FE9A2E";
                    layout.setBackgroundColor(Color.parseColor(strColor));
                    imgSet.setImageResource(R.drawable.soso);
                    text1.setText("현재 상태 : 나쁨");
                    text2.setText("공기가 탁해요");
                    text6.setText("오늘은 외출시 마스크를 꼭 착용하세요!");
                } else if (dust > 150.0) {
                    String strColor = "#FA5858";
                    layout.setBackgroundColor(Color.parseColor(strColor));
                    imgSet.setImageResource(R.drawable.bad);
                    text1.setText("현재 상태 : 매우 나쁨");
                    text2.setText("위험!! 환기 시켜주세요!!");
                    text6.setText("오늘은 바깥활동을 자제해주세요!");
                }
                text3.setText("먼지 농도 : " + dust + "㎍/m³");
            }
        }
    };

    Handler mHandler2 = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                String strMsg = (String) msg.obj;
                Log.d("test12", strMsg);
                Double tem = Double.parseDouble(strMsg);
                if (tem == 0.0) {
                    text4.setText("온도 : " + "분석중....");

                }
                if (tem <= 5.9) {
                    imgset.setImageResource(R.drawable.clothes1);
                } else if (tem > 6 && tem <= 9.9) {
                    imgset.setImageResource(R.drawable.clothes2);
                } else if (tem > 9.9 && tem <= 11.9) {
                    imgset.setImageResource(R.drawable.clothes3);
                } else if (tem > 11.9 && tem <= 16.9) {
                    imgset.setImageResource(R.drawable.clothes4);
                } else if (tem > 16.9 && tem <= 19.9) {
                    imgset.setImageResource(R.drawable.clothes5);
                } else if (tem > 19.9 && tem <= 22.9) {
                    imgset.setImageResource(R.drawable.clothes6);
                } else if (tem > 22.9 && tem <= 26.9) {
                    imgset.setImageResource(R.drawable.clothes7);
                } else if (tem > 26.9) {
                    imgset.setImageResource(R.drawable.clothes8);
                }
                text4.setText("온도 : " + tem + "℃");
            }
        }
    };

    Handler mHandler3 = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                String strMsg = (String) msg.obj;
                Double hum = Double.parseDouble(strMsg);
                if (hum == 0.0) {
                    text5.setText("습도 : " + "분석중....");

                }
                text5.setText("습도:" + hum + "%");
            }
        }
    };

}