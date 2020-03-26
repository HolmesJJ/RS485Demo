package android_serialport_api;
/*
 * Copyright 2009 Cedric Priscal
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class SerialPort {

	private static final String TAG = "SerialPort";

	/*
	 * Do not remove or rename the field mFd: it is used by native method close();
	 */
	protected FileDescriptor mFd;
	protected FileInputStream mFileInputStream;
	protected FileOutputStream mFileOutputStream;

	public SerialPort(File device, int baudrate, int parity, int dataBits, int stopBit) throws SecurityException, IOException {

		/* Check access permission */
		if (!device.canRead() || !device.canWrite()) {
			Log.i(TAG, "serial device can not write or read");
			try {
				/* Missing read/write permission, trying to chmod the file */
				Process su;
				su = Runtime.getRuntime().exec("/system/xbin/su");
				String cmd = "chmod 666 " + device.getAbsolutePath() + "\n"
						+ "exit\n";
				su.getOutputStream().write(cmd.getBytes());
				if ((su.waitFor() != 0) || !device.canRead()
						|| !device.canWrite()) {
					throw new SecurityException();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		mFd = open(device.getAbsolutePath(), baudrate, parity, dataBits, stopBit);
		if (mFd == null) {
			Log.e(TAG, "native open returns null");
			throw new IOException();
		} else {
			Log.d(TAG, "Serial port open success");
		}

		mFileInputStream = new FileInputStream(mFd);
		mFileOutputStream = new FileOutputStream(mFd);
	}


	protected void closeSerialPort() {
		if (mFileOutputStream != null) {
			try {
				mFileOutputStream.flush();
				mFileOutputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG, "cat exception: " + e.toString() + " while close mFileOutputStream");
			}
		}

		if (mFileInputStream != null) {
			try {
				mFileInputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG, "cat exception: " + e.toString() + " while close mFileInputStream");
			}
		}

		close();
		Log.d(TAG, "closeSerialPort: serial port close");
	}

	protected void sendBytes2SerialPort(byte[] bytes) {
		if (mFileOutputStream != null && null != bytes) {
			Log.i(TAG, Arrays.toString(bytes).replace(","," ")
					.replace("[","")
					.replace("]",""));
			try {
				mFileOutputStream.write(bytes);
			} catch (IOException e) {
				Log.i(TAG,"send Bytes failed");
				e.printStackTrace();
			}
		}
	}

	// Getters and setters
	public InputStream getInputStream() {
		return mFileInputStream;
	}

	public OutputStream getOutputStream() {
		return mFileOutputStream;
	}

	// JNI
	private native static FileDescriptor open(String path, int baudrate, int parity, int dataBits, int stopBit);

	private native static FileDescriptor setOpt(int path, int baudrate, int parity, int dataBits, int stopBit);

	public native void close();

	static {
        Log.i(TAG, "loadLibrary..............");
		System.loadLibrary("serial_port");
	}
}
