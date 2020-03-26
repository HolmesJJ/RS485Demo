package android_serialport_api;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class SerialPortReader {

    private final String TAG = "SerialPort";
    private static SerialPortReader sInstance;
    private String mComName = "/dev/ttyS2";
    private final int mFlag = 0;
    private SerialPort mSerialPort;
    private ReadThread mReadThread;
    private HandlerThread mSendDataThread;
    private Handler mSendDataHandler;
    private OnReceiveDataListener mListener;
    private SerialPortReader() {}

    public static SerialPortReader getInstance() {
        if (sInstance == null) {
            sInstance = new SerialPortReader();
        }

        return sInstance;
    }

    public void setOnReceiveDataListener(OnReceiveDataListener listener){
        this.mListener = listener;
    }

    public void init(int baudrate, int parity, int dataBits, int stopBit) {
        try {
            mSerialPort = new SerialPort(new File(mComName), baudrate, parity, dataBits, stopBit);
            mSendDataThread = new HandlerThread("SerialPortReader");
            mSendDataThread.setPriority(5);
            mSendDataThread.start();
            mSendDataHandler = new Handler(mSendDataThread.getLooper());
            mReadThread = new ReadThread();
            mReadThread.start();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "serial port suffer exception when start, exception -> "
                    + e.getClass() + "   msg is " + e.getMessage());
            Log.e(TAG, "open serial port, parameter is (" + mComName + " " + baudrate + ")");
        }

    }


    public void release() {
        mReadThread.interrupt();
        if (mSerialPort != null) {
            mSerialPort.closeSerialPort();
            mSerialPort = null;
        }
        if (mSendDataHandler != null) {
            mSendDataHandler.removeCallbacks(null);
        }
        if (mSendDataThread != null) {
            mSendDataThread.quit();
        }
        Log.d(TAG, "close: serialPort close");
    }

    public boolean sendData(byte[] data) {
        if (mSerialPort == null) {
            return false;
        }
        mSerialPort.sendBytes2SerialPort(data);
        return true;
    }

    public interface OnReceiveDataListener{
        void onReceive(byte[] data, long dataLength);
    }


    private class ReadThread extends Thread {
        private SendRunnable runnable = new SendRunnable();
        //第一次运行线程时设置成true
        private boolean beginning = false;
        //缓冲区()
        byte[] buffer = new byte[1024];
        @Override
        public void run() {
            super.run();

            while (!isInterrupted()) {
                int size;
                try {

                    if (mSerialPort.mFileInputStream == null) {
                        return;
                    }

                    //读取数据,同时获取数据长度(数据长度不是数组长度,而是实际接收到的数据长度),数据被读取到了缓冲区 buffer中
                    size = mSerialPort.mFileInputStream.read(buffer);
                    if (size > 0) {
                        Log.i(TAG,"接收数据长度:" + size);
                        //临时数组,将缓冲区buffer中的有效数据读取出来,临时数据长度就是接收到的数据长度。
                        byte[] temp = new byte[size];
                        System.arraycopy(buffer, 0, temp, 0, size);
                        //具体注释见init方法
                        runnable.init(temp, size);
                        //如果程序第一次运行
                        if (!beginning) {
                            //运行runnable,只在第一次执行,如果重复执行虽不会抛出异常,但是也无法正常执行功能
                            if(mSendDataThread.isAlive()){
                                mSendDataHandler.post(runnable);
                            }
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public class SendRunnable implements Runnable {
        private byte[] lastBuffer;
        int time = 0;
        boolean work = true;
        private int lastBufferLength;

        //断包处理逻辑包含其中
        public void init(byte[] buffer, int size) {

            if (lastBuffer == null) {
                lastBuffer = buffer;
            } else {
                lastBufferLength = lastBuffer.length;
                byte[] temp = new byte[lastBufferLength + size];
                //先拷贝之前的数据
                System.arraycopy(lastBuffer, 0, temp, 0, lastBufferLength);
                //再拷贝刚接收到的数据
                System.arraycopy(buffer, 0, temp, lastBufferLength, size);
                lastBuffer = null;
                lastBuffer = temp;
                temp = null;
            }
            work = true;
            time = 0;
        }

        public void reStart() {
            work = true;
            time = 0;
        }

        public void stop() {
            work = false;
            time = 0;
        }
        //接收完成后重置完整消息缓冲区
        public void reset() {
            work = false;
            time = 0;
            lastBuffer = null;
        }

        @Override
        public void run() {
            while (work) {
                try {
                    Thread.sleep(20);
                    time += 20;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (time >= 200) {
                    byte[] finalBuffer = lastBuffer;
                    reset();
                    //业务处理方法
                    if(mListener != null){
                        mListener.onReceive(finalBuffer, finalBuffer.length);
                    }
                }
            }

        }
    }


}
