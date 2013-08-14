package com.eurotech.edctracker;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.view.Menu;
import android.view.View;

import com.eurotech.edctracker.GPSService.ServiceBinder;

public class EDCTracker extends Activity
{
	private GPSService mService;
	private myTimer mTimer;
	
	//==================================================================================
	private class myTimer extends CountDownTimer
	{
		public myTimer(long millisInFuture, long countDownInterval)
		{
			super(millisInFuture, countDownInterval);
		}

		public void onFinish()
		{
			switch(mService.getStatus())
			{
				case 0 :	findViewById(R.id.start_button).setBackgroundColor(Color.GRAY);
							break;
							
				case 1 :	findViewById(R.id.start_button).setBackgroundColor(Color.GREEN);
							break;

				case 2 :	findViewById(R.id.start_button).setBackgroundColor(Color.RED);
							break;			
			}
			start();
		}

		public void onTick(long arg0)
		{
		}
	};
	
	//==================================================================================
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edctracker);
	}

	//==================================================================================
	ServiceConnection mConnection = new ServiceConnection()
	{
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			ServiceBinder mBinder = (ServiceBinder)service;
			mService = mBinder.getServerInstance();
		}
		
		public void onServiceDisconnected(ComponentName name)
		{
			
		}
	};
	
	//==================================================================================
	public void onStart()
	{
		super.onStart();
		mTimer = new myTimer(10000, 1000);
		mTimer.start();
		Intent mIntent = new Intent(this, GPSService.class);
		bindService(mIntent, mConnection, BIND_AUTO_CREATE);
	}
	
	//==================================================================================
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_edctracker, menu);
		return true;
	}

	//==================================================================================
	public void onButtonConnect(View v)
	{
		startService(new Intent(this, GPSService.class));
	}
	
	//==================================================================================
	public void onButtonDisconnect(View v)
	{
		stopService(new Intent(this, GPSService.class));
	}
}
