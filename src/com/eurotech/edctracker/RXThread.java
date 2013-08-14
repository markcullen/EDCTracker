package com.eurotech.edctracker;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class RXThread extends Thread
{
	private InputStream mInput;
	private MQTTInterface mInterface;

	public RXThread(MQTTInterface callbacks, Socket socket)
	{
		try {
			mInput = socket.getInputStream();
			mInterface = callbacks;
		}
		catch(IOException e){ e.printStackTrace(); }
	}

	//================================================================================================
	//
	private int GetSize() throws IOException
	{
		int digit;
		int value;
		int multiplier;
		
		value = 0;
		multiplier = 1; 
		
		do
		{
		  digit = mInput.read(); 
		  value = value + (digit & 0x7f ) * multiplier; 
		  multiplier = multiplier * 128;
		}
		while ((digit & 0x80) != 0);
		return value;
	}
	
	//================================================================================================
	//
	public void run()
	{
		int data;
		int size;
		
		try {
			data = mInput.read();
			
			while(data != -1)
			{
				size = GetSize();
				switch(data & 0xf0)
				{
					case 0x20 :	mInterface.RXCallback(2);	// CONNACK
								break;
								
					case 0x30 :		// PUBLISH
								break;
					
					case 0x40 :	break;					// PUBACK
					
					case 0x50 :	break;			// PUBREC

					case 0x60 :	break;			// PUBREL
					
					case 0x70 :	break;			// PUBCOMP
					
					case 0x80 :	break;			// SUBSCRIBE

					case 0x90 :		// SUBACK
								break;
					
					case 0xa0 :	break;			// UNSUBSCRIBE
					
					case 0xb0 :	break;			// UNSUBACK

					case 0xc0 :	break;			// PINGREQ

					case 0xd0 :	mInterface.RXCallback(1);	// PINGRESP
								break;

					case 0xe0 :	break;			// DISCONNECT
				}
				data = mInput.read();
			}
			size = 3;
		}
		
		catch (IOException e)
		{
			e.printStackTrace();
		}
    }
}
