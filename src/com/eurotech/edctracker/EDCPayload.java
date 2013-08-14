package com.eurotech.edctracker;

import java.io.IOException;
import java.io.InputStream;

import com.eurotech.cloud.message.protobuf.EdcPayloadProto.EdcPayload;
import com.eurotech.cloud.message.protobuf.EdcPayloadProto.EdcPayload.EdcMetric;
import com.eurotech.cloud.message.protobuf.EdcPayloadProto.EdcPayload.EdcPosition;
import com.google.protobuf.InvalidProtocolBufferException;

public class EDCPayload {

	private EdcPayload.Builder pb;

	public EDCPayload() {
		pb = EdcPayload.newBuilder();
	}
	
	public void setTimestamp()
	{
	}
	
	public void setCourse(double heading, double speed)
	{
		EdcPosition.Builder position;
		
		position = pb.getPositionBuilder();
		position.setHeading(heading);
		position.setSpeed(speed);
	}
	
	public void setPosition(double lattitude, double longitude, double altitude)
	{
		EdcPosition.Builder position;
		
		position = pb.getPositionBuilder();
		position.setLongitude(longitude);
		position.setLatitude(lattitude);
		position.setAltitude(altitude);
	}
	
	
	
	public int addMetric(String name)
	{
		EdcMetric.Builder mb;
		mb = pb.addMetricBuilder();
		mb.setName(name);
		return pb.getMetricCount() - 1;
	}
	
	public void setMetric(int index, int value)
	{		
		EdcMetric.Builder mb;
		mb = pb.getMetricBuilder(index);
		
		mb.setIntValue(value);
		mb.setType(EdcMetric.ValueType.INT32);
	}
	
	public void setMetric(int ID, String value)
	{		
		EdcMetric.Builder mb;
		mb = pb.getMetricBuilder(ID);
		
		mb.setStringValue(value);
		mb.setType(EdcMetric.ValueType.STRING);
	}

	public int getIntMetric(int ID)
	{
		return pb.getMetric(ID).getIntValue();
	}
	
	public void setBody()
	{
	}
	
	public EdcPayload build()
	{
		return pb.build();
	}
	
	public void parse(InputStream input) throws IOException
	{
		pb.clear();
		pb.mergeFrom(input);
	}
	
	public void parse(byte[] array, int offset, int size) throws InvalidProtocolBufferException
	{
		pb.clear();
		pb.mergeFrom(array, offset, size);
	}
}
