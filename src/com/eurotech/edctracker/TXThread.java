package com.eurotech.edctracker;

import java.net.Socket;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import android.util.Log;

import com.eurotech.cloud.message.protobuf.EdcPayloadProto.EdcPayload;

public class TXThread extends Thread
{
	private Socket mSocket;
	private RXThread mRXThread;
	private OutputStream mOutput;
	private boolean bRunning = true;
	private boolean bConnected = false;
	
	private MQTTInterface mInterface;

	public TXThread(MQTTInterface callbacks)
	{
//		mSocket = socket;
		mInterface = callbacks;
	}

	//================================================================================================
	//
	private void SendString(String string) throws IOException
	{
		int nLength = string.length();
		
		if(nLength > 0)
		{
			mOutput.write(nLength / 256);
			mOutput.write(nLength % 256);
			mOutput.write(string.getBytes());
		}
	}
	
	//================================================================================================
	//
	private void PutSize(int nSize) throws IOException
	{
		int n;
		
		do {
			n = nSize % 128;				// Calculates value for next digit
			nSize = nSize / 128;			// Calculate remainder if any

			if(nSize > 0)					// If value is not completely contained in first byte
				n = n + 0x80;				// Set the top bit of this digit to indicate follow on

			mOutput.write(n);
		}
		while(nSize > 0);
	}
	
	//============================================================================================================================
	private void OnConnect(MQTTMessage message)
	{
		int nLength;
		
		InetSocketAddress address;
		byte[] mConnectData = new byte[] {0x00, 0x06, 0x4d, 0x51, 0x49, 0x73, 0x64, 0x70, 0x03, 0x02, 0x00, 0x00};
	
		System.out.printf("OnConnect called.\n");
		
		if(bConnected == false)
		{
			try {
				mSocket = new Socket();
				address = new InetSocketAddress(message.URL, message.nPort);
						
				mSocket.connect(address, 2000);				// Connect to the given host (2 second timeout)
				mOutput = mSocket.getOutputStream();		// Create an output stream on top of this socket.

				mRXThread = new RXThread(mInterface, mSocket);	// Create a thread to listen on the newly connected socket
				mRXThread.start();
				
				mOutput = mSocket.getOutputStream();			// Create an output stream on top of this socket.

				nLength = 14 + message.AssetID.length();		// Variable header (12) + string data + 2 for string length
				
				// If a user name was given set the user flag in the header and add the string length to the size.
				if(message.User.length() > 0)
				{
					nLength = nLength + 2 + message.User.length();
					mConnectData[9] = (byte)(mConnectData[9] | 0x80); 
				}
						
				// If a password was given set the user flag in the header and add the string length to the size.
				if(message.Password.length() > 0)
				{
					nLength = nLength + 2 + message.Password.length();
					mConnectData[9] = (byte)(mConnectData[9] | 0x40); 
				}

				// Set keepalive timeout in bytes 10 & 11 of the variable header
				mConnectData[10] = (byte)(message.nKeepAlive / 256);
				mConnectData[11] = (byte)(message.nKeepAlive % 256);
				
				// Send fixed header for CONNECT (always 0x10) followed by the remaining length field size
				mOutput.write(0x10); PutSize(nLength);

				mOutput.write(mConnectData);				// Send the variable header
				SendString(message.AssetID);
				SendString(message.User);
				SendString(message.Password);
				bConnected = true;
			}
			
			catch(IOException e)
			{
				e.printStackTrace();
				mInterface.RXCallback(3);
			}
		}
	}

	//====================================================================================================================
	private void OnDisConnect(MQTTMessage message)
	{
		if(bConnected)
		{
			try {
				mOutput.write(0xe0);
				mOutput.write(0x00);
				mSocket.close();
			}
			catch(IOException e) { e.printStackTrace(); }

			bConnected = false;
		}
	}

	//====================================================================================================================
	private void OnPublish(MQTTMessage message)
	{
		int nLength;
		EdcPayload mPayload;
		
		if(bConnected == true)
		{
			try {
				mPayload = message.Payload.build();

				nLength = mPayload.getSerializedSize() + message.Topic.length() + 2;
		
				mOutput.write(0x30);
				PutSize(nLength);
				SendString(message.Topic);
				mPayload.writeTo(mOutput);
			}
			catch(IOException e){ e.printStackTrace(); }
		}
	}

	//====================================================================================================================
	private void OnSubscribe(MQTTMessage message)
	{
		System.out.printf("OnSubscribe called.\n");
	}
	
	//====================================================================================================================
	private void OnUnSubscribe(MQTTMessage message)
	{
		System.out.printf("OnUnSubscribe called.\n");
	}

	//====================================================================================================================
	private void OnPing(MQTTMessage message)
	{
		if(bConnected == true)
		{
			try {
				mOutput.write(0xc0); mOutput.write(0x00);
			}
			catch(IOException e){ e.printStackTrace(); }
		}
	}

	//====================================================================================================================
	public void run()
	{
		MQTTMessage message;
		
		while(bRunning)
		{
			message = mInterface.GetMessage();
			switch(message.nCode)
			{
				case 0 :	bRunning = false;
							System.out.printf("Terminate\n");
							break;
							
				case 1 :	OnConnect(message);
							break;

				case 2 :	OnDisConnect(message);
							break;

				case 3 :	OnPublish(message);
							break;
							
				case 4 :	OnSubscribe(message);
							break;

				case 5 :	OnUnSubscribe(message);
							break;
							
				case 6 :	OnPing(message);
							break;
			}
		}
	}
}
