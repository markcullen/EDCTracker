package com.eurotech.edctracker;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

public class GPSService extends Service implements LocationListener
{
	public class ServiceBinder extends Binder
	{
		public GPSService getServerInstance()
		{
			return GPSService.this;
		}
	}
	
	private WakeLock mLock;
	private boolean bStarted;
	private MQTTClient mClient;
	private ServiceBinder mBinder = new ServiceBinder();

	@Override
	public IBinder onBind(Intent arg0)
	{
		return mBinder;
	}

	//============================================================================================
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		PowerManager pm;
		LocationManager lm;
		
		super.onStartCommand(intent, flags, startId);

		if (!bStarted)
		{
			bStarted = true;
			
			lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 40, this);
					
			pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GPSLock");
			mLock.acquire();

			mClient.Connect();
		}
		return(START_STICKY);
	}

	public void closeService()
	{
		EDCPayload mPayload;
		mClient.LOGMessage("Close Service called");
		try {
			mPayload = new EDCPayload();
		
			mPayload.addMetric("display_name"); mPayload.setMetric(0, "EDC Tracker");
		
			mClient.Publish("$EDC/eurotechltd_1/GT-S5830/MQTT/DC", mPayload, 0, 0);
			Thread.sleep(500);
			mClient.Disconnect();
			mClient.Terminate();
		}
		catch(InterruptedException e){ e.printStackTrace(); }
	}

	public int getStatus()
	{
		return mClient.getStatus();
	}
	
	//============================================================================================
	@Override
	public void onDestroy()
	{
		closeService();
		mLock.release();
		super.onDestroy();
	}


	//============================================================================================
	@Override
	public void onCreate()
	{
		super.onCreate();

		bStarted = false;
		mClient = new MQTTClient();
	}

	@Override
	public void onLocationChanged(Location arg0)
	{
		EDCPayload mPayload;

		if(arg0.getAccuracy() < 25)
		{
			mPayload = new EDCPayload();
			mPayload.setCourse(0, arg0.getSpeed());
			mPayload.setPosition(arg0.getLatitude(), arg0.getLongitude(), 0.0);
			
			mClient.Publish("eurotechltd_1/GT-S5830/Position", mPayload, 0, 0);
		}
	}

	@Override
	public void onProviderDisabled(String arg0)
	{
	}

	@Override
	public void onProviderEnabled(String arg0)
	{
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2)
	{
	}
}
