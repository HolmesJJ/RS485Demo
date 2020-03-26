package com.sensetime;

import android.Manifest;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;


import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.sensetime.RS485Demo.R;
import com.snatik.storage.Storage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private final static String TAG = "MainActivity";

    private RS485Device mRs485Device;

    private TextView mReceiveByteTxv;
    private TextView mSendByteTxv;
    private EditText mSendHexEdTx;
    private Button mClearBtn;
    private Button mMatchBtn1;
    private Button mMatchBtn2;
    private Button mMatchBtn3;
    private Button mAccessGrantedBtn;
    private Button mAccessDeniedBtn;
    private Button mAccessPinBtn;
    private RadioGroup mParitySettingRGroup;

    private DialogProperties properties;
    private Storage storage;

    private static final int REC_PERMISSION = 100;
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    //获取权限
    private void getPermission() {
        if (EasyPermissions.hasPermissions(this, PERMISSIONS)) {
            //已经打开权限
            CustomToast.show(this, "已经申请相关权限");
        } else {
            //没有打开相关权限、申请权限
            EasyPermissions.requestPermissions(this, "需要获取您的使用权限", 1, PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

        CustomToast.show(this, "相关权限获取成功");
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {

        CustomToast.show(this, "请同意相关权限，否则功能无法使用");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViews();

        getPermission();

        initRS485(115200,0,8,1);
        addListeners();

        properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File("/");
        properties.error_dir = new File("/");
        properties.offset = new File("/");
        properties.extensions = new String[]{"jp2"};
        storage = new Storage(getApplicationContext());

        // RESPONSE_ACCESS_GRANTED: 0x01-0x52-0x00-0xA1-0xF2
        Log.i(TAG, "RESPONSE_ACCESS_GRANTED -----------------");
        Log.i(TAG, "RESPONSE_ACCESS_GRANTED " +
                HexByteUtil.HexToByte("01") + ", " +
                HexByteUtil.HexToByte("52") + ", " +
                HexByteUtil.HexToByte("00") + ", " +
                HexByteUtil.HexToByte("A1") + ", " +
                HexByteUtil.HexToByte("F2"));
        Log.i(TAG, "RESPONSE_ACCESS_GRANTED Result 1, 82, 0, -95, -14" + ", CheckSum: " + (1 ^ 82 ^ 0 ^ -95));

        // RESPONSE_ACCESS_DENIED: 0x01-0x52-0x00-0xA2-0xF1
        Log.i(TAG, "RESPONSE_ACCESS_DENIED -----------------");
        Log.i(TAG, "RESPONSE_ACCESS_DENIED " +
                HexByteUtil.HexToByte("01") + ", " +
                HexByteUtil.HexToByte("52") + ", " +
                HexByteUtil.HexToByte("00") + ", " +
                HexByteUtil.HexToByte("A2") + ", " +
                HexByteUtil.HexToByte("F1"));
        Log.i(TAG, "RESPONSE_ACCESS_GRANTED Result 1, 82, 0, -94, -15" + ", CheckSum: " + (1 ^ 82 ^ 0 ^ -94));

        // RESPONSE_PIN: 0x01-0x52-0x00-0xA2-0xF1
        Log.i(TAG, "RESPONSE_PIN -----------------");
        Log.i(TAG, "RESPONSE_PIN " +
                HexByteUtil.HexToByte("01") + ", " +
                HexByteUtil.HexToByte("54") + ", " +
                HexByteUtil.HexToByte("00") + ", " +
                HexByteUtil.HexToByte("AA") + ", " +
                HexByteUtil.HexToByte("FF"));
        Log.i(TAG, "RESPONSE_PIN Result 1, 84, 0, -86, -1" + ", CheckSum: " + (1 ^ 84 ^ 0 ^ -86));

        // REQUEST_MATCHED: 0x01-0x52-0x00-0xA2-0xF1
        Log.i(TAG, "REQUEST_MATCHED -----------------");
        Log.i(TAG, "REQUEST_MATCHED " +
                HexByteUtil.HexToByte("01") + ", " +
                HexByteUtil.HexToByte("51") + ", " +
                HexByteUtil.HexToByte("00") + ", " +
                HexByteUtil.HexToByte("B1") + ", " +
                HexByteUtil.HexToByte("E1"));
        Log.i(TAG, "REQUEST_MATCHED Result 1, 81, 0, -79, -31" + ", CheckSum: " + (1 ^ 81 ^ 0 ^ -79));

        Log.i(TAG, "REQUEST_MISMATCHED -----------------");
        Log.i(TAG, "REQUEST_MISMATCHED " +
                HexByteUtil.HexToByte("01") + ", " +
                HexByteUtil.HexToByte("51") + ", " +
                HexByteUtil.HexToByte("00") + ", " +
                HexByteUtil.HexToByte("BF") + ", " +
                HexByteUtil.HexToByte("EF"));
        Log.i(TAG, "REQUEST_MISMATCHED Result 1, 81, 0, -65, -17" + ", CheckSum: " + (1 ^ 81 ^ 0 ^ -65));

        Log.i(TAG, "REQUEST_MATCH_TIMEOUT -----------------");
        Log.i(TAG, "REQUEST_MATCH_TIMEOUT " +
                HexByteUtil.HexToByte("01") + ", " +
                HexByteUtil.HexToByte("51") + ", " +
                HexByteUtil.HexToByte("00") + ", " +
                HexByteUtil.HexToByte("FF") + ", " +
                HexByteUtil.HexToByte("AF"));
        Log.i(TAG, "REQUEST_MATCH_TIMEOUT Result 1, 81, 0, -1, -81" + ", CheckSum: " + (1 ^ 81 ^ 0 ^ -1));

        Log.i(TAG, "REQUEST_PIN_TIMEOUT -----------------");
        Log.i(TAG, "REQUEST_PIN_TIMEOUT " +
                HexByteUtil.HexToByte("01") + ", " +
                HexByteUtil.HexToByte("54") + ", " +
                HexByteUtil.HexToByte("00") + ", " +
                HexByteUtil.HexToByte("FF") + ", " +
                HexByteUtil.HexToByte("AA"));
        Log.i(TAG, "REQUEST_PIN_TIMEOUT Result 1, 84, 0, -1, -86" + ", CheckSum: " + (1 ^ 84 ^ 0 ^ -1));
    }

    private void findViews(){
        mReceiveByteTxv = findViewById(R.id.main_receive_byte_txv);
        mSendHexEdTx = findViewById(R.id.main_send_hex_edit);
        mSendByteTxv = findViewById(R.id.main_send_byte_txv);
        mClearBtn = findViewById(R.id.main_clear_btn);
        mMatchBtn1 = findViewById(R.id.match_btn1);
        mMatchBtn2 = findViewById(R.id.match_btn2);
        mMatchBtn3 = findViewById(R.id.match_btn3);
        mAccessGrantedBtn = findViewById(R.id.access_granted_btn);
        mAccessDeniedBtn = findViewById(R.id.access_denied_btn);
        mAccessPinBtn = findViewById(R.id.access_pin_btn);
        mParitySettingRGroup = findViewById(R.id.main_parity_setting_rgroup);
    }

    private void addListeners(){

        mClearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSendByteTxv.setText("送出Byte為 ： ");
                mReceiveByteTxv.setText("收到數據 : ");
            }
        });

        mMatchBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSendHexEdTx.getText().toString().equals("")){
                    ShowMessage("輸入不得為空");
                } else if (mSendHexEdTx.getText().toString().length() != 8){
                    ShowMessage("卡号长度为8");
                } else {
                    try {
                        byte[] card = new byte[16];
                        card[0] = HexByteUtil.HexToByte("01");
                        card[1] = HexByteUtil.HexToByte("51");
                        card[2] = HexByteUtil.HexToByte("09");
                        card[3] = HexByteUtil.HexToByte("00");

                        String icNumber = mSendHexEdTx.getText().toString();
                        for(int i = 0; i < icNumber.length(); i++){
                            char ch = icNumber.charAt(i);
                            card[i + 3] = (byte)ch;
                            Log.i(TAG, "abc: " + i + "   " + (byte)ch);
                        }

                        card[12] = HexByteUtil.HexToByte("44");
                        card[13] = HexByteUtil.HexToByte("7E");
                        card[14] = HexByteUtil.HexToByte("40");
                        card[15] = HexByteUtil.HexToByte("00");

                        byte[] raw = new byte[16384];
                        Log.i(TAG,"raw.length: " + raw.length);
                        byte[] data = new byte[card.length + raw.length];
                        System.arraycopy(card, 0, data, 0, card.length);
                        System.arraycopy(raw, 0, data, card.length, raw.length);

                        System.out.println("送出Byte长度為 ： " + data.length);
                        // mSendByteTxv.setText("送出Byte為 ： " + Arrays.toString(data));
                        System.out.println("送出Byte為 ： " + Arrays.toString(data));
                        mRs485Device.sendData(data);
                    }catch (Exception e){
                        ShowMessage("輸入字串有誤");
                        e.printStackTrace();
                    }
                }
            }
        });

        mMatchBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mSendHexEdTx.getText().toString().equals("")){
                    ShowMessage("輸入不得為空");
                }  else if (mSendHexEdTx.getText().toString().length() != 8){
                    ShowMessage("卡号长度为8");
                } else {
                    showFileDialog();
                }
            }
        });

        mMatchBtn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mSendHexEdTx.getText().toString().equals("")){
                    ShowMessage("輸入不得為空");
                } else if (mSendHexEdTx.getText().toString().length() != 8){
                    ShowMessage("卡号长度为8");
                } else {
                    try {
                        byte[] card = new byte[14];
                        card[0] = HexByteUtil.HexToByte("01");
                        card[1] = HexByteUtil.HexToByte("51");
                        card[2] = HexByteUtil.HexToByte("09");
                        card[3] = HexByteUtil.HexToByte("00");

                        String icNumber = mSendHexEdTx.getText().toString();
                        for(int i = 0; i < icNumber.length(); i++){
                            char ch = icNumber.charAt(i);
                            card[i + 3] = (byte)ch;
                            Log.i(TAG, "abc: " + i + "   " + (byte)ch);
                        }

                        card[12] = HexByteUtil.HexToByte("44");
                        card[13] = HexByteUtil.HexToByte("7E");

                        byte[] data = new byte[card.length];
                        System.arraycopy(card, 0, data, 0, card.length);

                        System.out.println("送出Byte长度為 ： " + data.length);
                        // mSendByteTxv.setText("送出Byte為 ： " + Arrays.toString(data));
                        System.out.println("送出Byte為 ： " + Arrays.toString(data));
                        mRs485Device.sendData(data);
                    }catch (Exception e){
                        ShowMessage("輸入字串有誤");
                        e.printStackTrace();
                    }
                }
            }
        });

        mAccessGrantedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] data = new byte[]{HexByteUtil.HexToByte("01"), HexByteUtil.HexToByte("52"), HexByteUtil.HexToByte("00"), HexByteUtil.HexToByte("A1"), HexByteUtil.HexToByte("F2")};
                mSendByteTxv.setText("送出Byte為 ： " + Arrays.toString(data));
                System.out.println("送出Byte為 ： " + Arrays.toString(data));
                mRs485Device.sendData(data);
            }
        });

        mAccessDeniedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] data = new byte[]{HexByteUtil.HexToByte("01"), HexByteUtil.HexToByte("52"), HexByteUtil.HexToByte("00"), HexByteUtil.HexToByte("A2"), HexByteUtil.HexToByte("F1")};
                mSendByteTxv.setText("送出Byte為 ： " + Arrays.toString(data));
                System.out.println("送出Byte為 ： " + Arrays.toString(data));
                mRs485Device.sendData(data);
            }
        });

        mAccessPinBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] data = new byte[]{HexByteUtil.HexToByte("01"), HexByteUtil.HexToByte("54"), HexByteUtil.HexToByte("00"), HexByteUtil.HexToByte("AA"), HexByteUtil.HexToByte("FF")};
                mSendByteTxv.setText("送出Byte為 ： " + Arrays.toString(data));
                System.out.println("送出Byte為 ： " + Arrays.toString(data));
                mRs485Device.sendData(data);
            }
        });

        mParitySettingRGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                destroyRS485();
                switch(checkedId){
                    case R.id.main_none_parity_rbtn:
                        initRS485(115200,0,8,1);
                        break;
                    case R.id.main_odd_parity_rbtn:
                        initRS485(115200,1,8,1);
                        break;
                    case R.id.main_even_parity_rbtn:
                        initRS485(115200,2,8,1);
                        break;
                }
            }
        });

    }

    private void initRS485(int baudrate, int parity, int dataBits, int stopBit){
        mRs485Device = new RS485Device();
        mRs485Device.createDevice(baudrate, parity, dataBits, stopBit);
        mRs485Device.setRS485ReceiveListener(new RS485Device.RS485ReceiveListener() {
            @Override
            public void onReceive(final byte[] data) {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        ShowMessage(Arrays.toString(data));

//                        mReceiveByteTxv.setText("收到數據 : " + Arrays.toString(data).replace(","," ")
//                                .replace("[","")
//                                .replace("]",""));

                        mReceiveByteTxv.setText("收到數據 : " + HexByteUtil.bytesToHex(data));
                    }
                });
            }
        });
    }

    private void destroyRS485(){
        mRs485Device.destroyDevice();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void ShowMessage(String sMsg) {
        Toast.makeText(this, sMsg, Toast.LENGTH_SHORT).show();
    }

    private void showFileDialog() {

        FilePickerDialog dialog = new FilePickerDialog(MainActivity.this,properties);
        dialog.setTitle("Select a File");
        dialog.setDialogSelectionListener(new FilePickerDialog.DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                String filename = files[0].substring(files[0].lastIndexOf("/")+1);
                File targetFile = new File(getFilesDir().getAbsolutePath() + "/" + filename);
                File sourceFile = new File(files[0]);
                int file_size = Integer.parseInt(String.valueOf(sourceFile.length() / 1024));
                if (file_size < 8) {
                    if (storage.copy(sourceFile.getAbsolutePath(), targetFile.getAbsolutePath())){
                        Log.i("showFileDialog", sourceFile.getAbsolutePath());
                        Log.i("showFileDialog", targetFile.getAbsolutePath());
                        CustomToast.show(MainActivity.this, "Succeed");
                        try {
                            InputStream is = new FileInputStream(targetFile);
                            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                            int nRead;
                            byte[] raw = new byte[16384];
                            while ((nRead = is.read(raw, 0, raw.length)) != -1) {
                                buffer.write(raw, 0, nRead);
                            }
                            byte[] image = buffer.toByteArray();

                            Log.i(TAG,"image.length: " + image.length);

                            int first = image.length / 256;
                            int second = (image.length - (first * 256));

                            byte[] card = new byte[16];
                            card[0] = HexByteUtil.HexToByte("01");
                            card[1] = HexByteUtil.HexToByte("51");
                            card[2] = HexByteUtil.HexToByte("09");
                            card[3] = HexByteUtil.HexToByte("00");

                            String icNumber = mSendHexEdTx.getText().toString();
                            for(int i = 0; i < icNumber.length(); i++){
                                char ch = icNumber.charAt(i);
                                card[i + 3] = (byte)ch;
                                Log.i(TAG, "abc: " + i + "   " + (byte)ch);
                            }

                            card[12] = HexByteUtil.HexToByte("44");
                            card[13] = HexByteUtil.HexToByte("7E");
                            card[14] = (byte) first;
                            card[15] = (byte) second;

                            byte[] data = new byte[card.length + image.length];
                            System.arraycopy(card, 0, data, 0, card.length);
                            System.arraycopy(image, 0, data, card.length, image.length);

                            System.out.println("送出Byte长度為 ： " + data.length);
                            // mSendByteTxv.setText("送出Byte為 ： " + Arrays.toString(data));
                            System.out.println("送出Byte為 ： " + Arrays.toString(data));
                            mRs485Device.sendData(data);
                        } catch (Exception e){
                            ShowMessage("輸入字串有誤");
                            e.printStackTrace();
                        }
                    } else {
                        CustomToast.show(MainActivity.this, "Failed");
                    }
                } else {
                    CustomToast.show(MainActivity.this, "File too big: " + file_size + "kb");
                }
            }

            @Override
            public void onCancel() {

            }
        });
        dialog.show();
    }
}
