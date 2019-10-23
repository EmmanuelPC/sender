package com.example.icvicapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 10; // 블루투스 활성화 상태
    private BluetoothAdapter mBluetoothAdapter;
    private Set<BluetoothDevice> mDevices;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothSocket mBluetoothSocket = null;
    private OutputStream mOutputStream = null;

    private TextView mTvStatus;
    private TextView mEtMessage;
    private Button mBtnSendMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // View settings
        mTvStatus = (TextView) findViewById(R.id.tv_bt_status);
        mEtMessage = (EditText) findViewById(R.id.et_message);
        mBtnSendMessage = (Button) findViewById(R.id.btn_send_msg);

        // Activate Bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            mTvStatus.setText("블루투스 사용 불가");
        } else {
            if (mBluetoothAdapter.isEnabled()) {
                selectBluetoothDevice();
            } else {
                Intent intent =  new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_ENABLE_BT);
            }
        }

        // Event handler
        mBtnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData(mEtMessage.getText().toString());
                mEtMessage.setText("");
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_ENABLE_BT:
                if(resultCode == RESULT_OK) selectBluetoothDevice();
                else mTvStatus.setText("블루투스 OFF");
        }
    }

    public void selectBluetoothDevice() {
        mTvStatus.setText("블루투스 ON");
        mDevices = mBluetoothAdapter.getBondedDevices();
        int pairedDeviceCount = mDevices.size();
        if (pairedDeviceCount == 0) {
            // 페어링 진행
            mTvStatus.setText("페이링이 되지 않음");
        } else {
            // Select Device
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("블루투스 기기 선택");
            List<String> list = new ArrayList<>();

            for (BluetoothDevice bluetoothDevice : mDevices) {
                list.add(bluetoothDevice.getName());
            }

            // Convert List into CharSequence
            final CharSequence[] charSequences = list.toArray(new CharSequence[list.size()]);
            list.toArray(new CharSequence[list.size()]);

            // click event listener
            builder.setItems(charSequences, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    connectDevice(charSequences[which].toString());
                }
            });

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }

    public void connectDevice(String deviceName) {
        for (BluetoothDevice device : mDevices) {
            if (deviceName.equals(device.getName())) {
                mBluetoothDevice = device;
                break;
            }
        }
        // Generate UUID
        UUID uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        // Rfcomm 채널을 통해 블루투스 디바이스와 통신하는 소켓 생성
        try {
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            mBluetoothSocket.connect();
            mTvStatus.setText(String.format("연결된 기기: %s", deviceName));
            // 데이터 송신 스트림
            mOutputStream = mBluetoothSocket.getOutputStream();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void sendData(String text) {
        text += "\n"; // 개행 문자 추가
        try {
            mOutputStream.write(text.getBytes());
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
