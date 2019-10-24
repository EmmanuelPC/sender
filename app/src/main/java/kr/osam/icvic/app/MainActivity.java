package kr.osam.icvic.app;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.naver.speech.clientapi.SpeechRecognitionResult;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import kr.osam.icvic.app.utils.AudioWriterPCM;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 10; // 블루투스 활성화 상태
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String CLIENT_ID = "txj1wxtmhg";

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = { Manifest.permission.RECORD_AUDIO };

    private BluetoothAdapter mBluetoothAdapter;
    private Set<BluetoothDevice> mDevices;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothSocket mBluetoothSocket = null;
    private OutputStream mOutputStream = null;

    private RecognitionHandler mHandler;
    private NaverRecognizer mNaverRecognizer;
    private AudioWriterPCM mWriter;
    private String mResult;

    private TextView mTvSTTResult;
    private TextView mTvSTTHint;
    private ImageButton mBtnStartSTT;

    private void handleMessage(Message msg) {
        switch (msg.what) {
            case R.id.clientReady:
                // 음성인식 준비 가능
                mTvSTTHint.setText(R.string.stt_ready);
                mWriter = new AudioWriterPCM(Environment.getExternalStorageDirectory().getAbsolutePath() + "/NaverSpeechTest");
                mWriter.open("Test");
                break;
            case R.id.audioRecording:
                mTvSTTHint.setText(R.string.stt_progress);
                mWriter.write((short[]) msg.obj);
                break;
            case R.id.partialResult:
                mResult = (String) (msg.obj);
                mTvSTTResult.setText(mResult);
                break;
            case R.id.finalResult: // 최종 인식 결과
                SpeechRecognitionResult speechRecognitionResult = (SpeechRecognitionResult) msg.obj;
                List<String> results = speechRecognitionResult.getResults();
                mResult = results.get(0);
                mTvSTTResult.setText(mResult);
                setAnimation(false).start();
                sendData(mResult);
                mTvSTTHint.setText(R.string.stt_success);
                break;
            case R.id.recognitionError:
                String errNo = msg.obj.toString();
                if (mWriter != null) mWriter.close();
                mTvSTTHint.setText("에러가 발생했습니다 ERROR " + errNo);
                mBtnStartSTT.setEnabled(true);
                break;
            case R.id.clientInactive:
                if (mWriter != null) {
                    mWriter.close();
                    // Set Delay
                    new android.os.Handler().postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                mTvSTTHint.setText(R.string.stt_ready);
                            }
                        },
                        1200
                    );
                }
                mBtnStartSTT.setEnabled(true);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        // View binding
        mTvSTTResult = (TextView) findViewById(R.id.tv_stt_result);
        mTvSTTHint = (TextView) findViewById(R.id.tv_stt_hint);
        mBtnStartSTT = (ImageButton) findViewById(R.id.btn_start_stt);

        // Naver Clova STT
        mHandler = new RecognitionHandler(this);
        mNaverRecognizer = new NaverRecognizer(this, mHandler, CLIENT_ID);

        // Activate Bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "블루투스를 지원하지 않습니다.", Toast.LENGTH_LONG).show();
        } else {
            if (mBluetoothAdapter.isEnabled()) {
                selectBluetoothDevice();
            } else {
                Intent intent =  new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_ENABLE_BT);
            }
        }

        // Event handler
        mBtnStartSTT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mNaverRecognizer.getSpeechRecognizer().isRunning()) {
                    Log.d(TAG, "PRESS_BUTTON_TO_START");
                    // set text value
                    mResult = "";
                    mTvSTTResult.setText("");
                    setAnimation(true).start();
                    mNaverRecognizer.recognize();
                } else {
                    Log.d(TAG, "PRESS_BUTTON_TO_STOP");
                    mTvSTTHint.setText(R.string.stt_stop);
                    mBtnStartSTT.setEnabled(false);
                    mNaverRecognizer.getSpeechRecognizer().stop();
                }
           }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mNaverRecognizer.getSpeechRecognizer().initialize();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mResult = "";
        mTvSTTResult.setText("");
        mTvSTTHint.setText(R.string.stt_ready);
        mBtnStartSTT.setEnabled(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mNaverRecognizer.getSpeechRecognizer().release();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_ENABLE_BT:
                if(resultCode == RESULT_OK) selectBluetoothDevice();
                else Toast.makeText(getApplicationContext(), "블루투스가 꺼져있습니다.", Toast.LENGTH_LONG).show();
        }
    }

    // Declare handler for handling SpeechRecognizer thread's Messages.
    static class RecognitionHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;
        RecognitionHandler(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                activity.handleMessage(msg);
            }
        }
    }

    public void selectBluetoothDevice() {
        mDevices = mBluetoothAdapter.getBondedDevices();
        int pairedDeviceCount = mDevices.size();
        if (pairedDeviceCount == 0) {
            // 페어링 진행
            Toast.makeText(getApplicationContext(), "블루투스 페어링을 해야합니다.", Toast.LENGTH_LONG).show();
        } else {
            // Connect to BT automatically
            for (BluetoothDevice bluetoothDevice : mDevices) {
                // 사전에 블루투스 이름을 'li-fi'로 설정
                if (bluetoothDevice.getName().equals("li-fi")) {
                    connectDevice(bluetoothDevice);
                }
            }
        }
    }

    public void connectDevice(BluetoothDevice device) {
        mBluetoothDevice = device;
        // Generate UUID
        UUID uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        // Rfcomm 채널을 통해 블루투스 디바이스와 통신하는 소켓 생성
        try {
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            mBluetoothSocket.connect();
            Toast.makeText(getApplicationContext(), "기기와 연결되었습니다.", Toast.LENGTH_SHORT).show();
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
    public AnimatorSet setAnimation(boolean isListening) {
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.setDuration(600);
        if (isListening) {
            ObjectAnimator translateUp = ObjectAnimator.ofFloat(mBtnStartSTT, "translationY", 0f, -500f);
            ObjectAnimator scaleUp = ObjectAnimator.ofPropertyValuesHolder(mBtnStartSTT,
                    PropertyValuesHolder.ofFloat("scaleX", 1.25f),
                    PropertyValuesHolder.ofFloat("scaleY", 1.25f));
            animatorSet.playTogether(translateUp, scaleUp);
        } else {
            ObjectAnimator translateDown = ObjectAnimator.ofFloat(mBtnStartSTT, "translationY", -500f, 0f);
            ObjectAnimator scaleDown = ObjectAnimator.ofPropertyValuesHolder(mBtnStartSTT,
                    PropertyValuesHolder.ofFloat("scaleX", 0.8f),
                    PropertyValuesHolder.ofFloat("scaleY", 0.8f));
            animatorSet.playTogether(translateDown, scaleDown);
        }
        return animatorSet;
    }
}
