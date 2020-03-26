package com.sensetime;

import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android_serialport_api.SerialPortReader;

public class RS485Device {


    private final static String enableSendCmd = "echo 1 >> /sys/class/gpio/gpio154/value";
    private final static String enableReceiveCmd = "echo 0 >> /sys/class/gpio/gpio154/value";

    private RS485ReceiveListener mListener;

    private HandlerThread mSendDataThread;
    private Handler mSendDataHandler;


    public void createDevice(int baudrate, int parity, int dataBits, int stopBit) {
        SerialPortReader.getInstance().init(baudrate, parity, dataBits, stopBit);
        SerialPortReader.getInstance().setOnReceiveDataListener(new SerialPortReader.OnReceiveDataListener() {

            @Override
            public void onReceive(byte[] data, long dataLength) {
                if(mListener != null){
                    mListener.onReceive(data);
                }
            }
        });
        mSendDataThread = new HandlerThread("sendRS485Data");
        mSendDataThread.setPriority(8);
        mSendDataThread.start();
        mSendDataHandler = new Handler(mSendDataThread.getLooper());
        execRootCmd(enableReceiveCmd);
    }

    public void destroyDevice() {

        if (mSendDataHandler != null) {
            mSendDataHandler.removeCallbacks(null);
        }
        if (mSendDataThread != null) {
            mSendDataThread.quit();
        }
        SerialPortReader.getInstance().release();
    }

    public void setRS485ReceiveListener(RS485ReceiveListener listener){

        mListener = listener;
    }

    public void sendData(final byte[] data){

        mSendDataHandler.post(new Runnable() {

            @Override
            public void run() {
                execRootCmd(enableSendCmd);
                SerialPortReader.getInstance().sendData(data);
                mSendDataHandler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        execRootCmd(enableReceiveCmd);
                    }
                },500);

            }
        });

    }

    private String execRootCmd(String cmd) {

        Process p = null;
        try {
            p = Runtime.getRuntime().exec( new String[]{"sh", "-c", cmd});
        } catch (IOException e) {
            e.printStackTrace();
        }
        String data = "";
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader er = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String line = "";
        String error = "";
        try {
            while ((line = in.readLine()) != null
                    && !TextUtils.isEmpty(line)) {
                data += line + "\n";
            }
            while ((error = er.readLine()) != null
                    && !TextUtils.isEmpty(error)) {
                data += error + "\n";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;

    }

    public interface RS485ReceiveListener{

        void onReceive(byte[] data);
    }
}

