package com.eurotech.edctracker;

import java.net.Socket;

public interface MQTTInterface {

	public MQTTMessage GetMessage();
	
	public void RXCallback(int nCode);
	
	public void LOGMessage(String s);
	
	public void TXCallback();
}
