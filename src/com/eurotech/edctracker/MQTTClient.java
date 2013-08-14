package com.eurotech.edctracker;

import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.Semaphore;

import android.os.Environment;

public class MQTTClient extends Thread implements MQTTInterface 
{
	private int nStatus;
	private int nKeepalive;
	private int nPingTimeout;
	private boolean bRunning;
	private int nReconnect;
	private Semaphore mSemaphore;
	private FileWriter mLogFile;

	private int nTXRemove;
	private int nTXInsert;
	private MQTTMessage mTXQueue[];
	
	private TXThread mTXThread;
	
	public MQTTClient()
	{
		nStatus = 0;
		nTXInsert = 0;
		nTXRemove = 0;
		nKeepalive = 0;
		bRunning = true;
		nPingTimeout = 0;
		
		try {
			mLogFile = new FileWriter(Environment.getExternalStorageDirectory().toString() + "/EDCTracker.log", true);
			LOGMessage("Logging started");
		} catch (IOException e) { e.printStackTrace(); }

		mSemaphore = new Semaphore(0);
		mTXQueue = new MQTTMessage[32];
		
		mTXThread = new TXThread(this);
		
		mTXThread.start();
		
		start();
	}

	public int getStatus(){ return nStatus; }
	
	//====================================================================================================================
	private int PostMessage(MQTTMessage mMessage)
	{
		if(((nTXInsert + 1 ) & 0x1f) != nTXRemove)
		{
			mTXQueue[nTXInsert] = mMessage;
			nTXInsert = (nTXInsert + 1) & 0x1f;
			mSemaphore.release();
			return 1;
		}
		return 0;
	}

	//====================================================================================================================
	public MQTTMessage GetMessage()
	{
		MQTTMessage mMessage;

		try {
			mSemaphore.acquire();
			mMessage = mTXQueue[nTXRemove];
			nTXRemove = (nTXRemove + 1) & 0x1f;
			return mMessage;
		}
		
		catch (InterruptedException e) {
			return null;
		}
	}
	
	//====================================================================================================================
	public void Connect()
	{
		MQTTMessage m;
		
		m = new MQTTMessage(1);
		
		m.nPort = 1883;
		m.AssetID = "GT-S5830";
		m.nKeepAlive = 90;
//		m.URL = "192.168.1.16";
		m.URL = "broker-sandbox.everyware-cloud.com";
		m.Password = "C!oud1";
		m.User = "eurotechltd_1";

		LOGMessage("Connect");
		PostMessage(m);
	}

	//====================================================================================================================
	public void Disconnect()
	{
		MQTTMessage m;
		
		LOGMessage("Disconnect");
		m = new MQTTMessage(2); PostMessage(m);
	}
	
	//====================================================================================================================
	public void Publish(String topic, EDCPayload payload, int qos, int retain)
	{
		MQTTMessage m;
		
		m = new MQTTMessage(3); 

		m.Topic = topic;
		m.Payload = payload;

		LOGMessage("Publish");
		PostMessage(m);
	}
	
	//====================================================================================================================
	public void Subscribe()
	{
		MQTTMessage m;
		
		m = new MQTTMessage(4); PostMessage(m);
	}
	
	//====================================================================================================================
	public void UnSubscribe()
	{
		MQTTMessage m;
		
		m = new MQTTMessage(5); PostMessage(m);
	}

	//====================================================================================================================
	public void Terminate()
	{
		MQTTMessage m;
		bRunning = false;
		m = new MQTTMessage(0); PostMessage(m);
	}
	
	//====================================================================================================================
	public void run()
	{
		MQTTMessage m;
		
		nPingTimeout = 0;
		while(bRunning)
		{
			try {
				Thread.sleep(10);
				
				if(nPingTimeout != 0)
				{
					nPingTimeout = nPingTimeout - 1;
					if(nPingTimeout == 0)
					{
						LOGMessage("Ping reply timeout");
						Disconnect();
						nStatus = 2;
						Connect();
					}
				}
				
				if(nKeepalive != 0)
				{
					nKeepalive = nKeepalive - 1;
					if(nKeepalive == 0)
					{
						nPingTimeout = 500;
						LOGMessage("Send Ping request");
						m = new MQTTMessage(6); PostMessage(m);
					}
				}
				
				if(nReconnect != 0)
				{
					nReconnect = nReconnect - 1;
					if(nReconnect == 0)
						Connect();
				}
			}
			catch (InterruptedException e){ e.printStackTrace(); }
		}
	}

	//====================================================================================================================
	public void RXCallback(int nCode)
	{
		EDCPayload mPayload;
		switch(nCode)
		{
			case 1 :	nPingTimeout = 0;					// PINGRESP received
						nKeepalive = 6000;
						LOGMessage("PINGRESP received");
						break;
						
			case 2 :	nStatus = 1;
						nKeepalive = 6000;					// CONACK received
						LOGMessage("CONACK received");
						mPayload = new EDCPayload();
						mPayload.addMetric("display_name"); mPayload.setMetric(0, "EDC Tracker");
						Publish("$EDC/eurotechltd_1/GT-S5830/MQTT/BIRTH", mPayload, 0, 0);
						break;
						
			case 3 :	nStatus = 2;
						LOGMessage("Connect failed");
						nReconnect = 1000;
						break;
		}
	}

	//====================================================================================================================
	public void TXCallback() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void LOGMessage(String s)
	{
		Date d;
		
		try {
			d = new Date();
			mLogFile.write(d.toString() + "   " + s + "\n"); mLogFile.flush();
		}
		
		catch(IOException e) { e.printStackTrace(); }	
	}
}
