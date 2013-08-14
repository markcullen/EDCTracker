package com.eurotech.edctracker;

import com.eurotech.cloud.message.protobuf.EdcPayloadProto.EdcPayload;


public class MQTTMessage
{
	public int nCode;
	public int nPort;
	public int nKeepAlive;
	
	public String URL;
	public String Topic;
	public String User;
	public String AssetID;
	public String Password;
	public EDCPayload Payload;
	
	public MQTTMessage(int nMessage)
	{
		nCode = nMessage;
	}
}
