package com.cisco.iox.middleware.protocolHandler.mqtt;

import java.util.ArrayList;
import java.util.List;

import com.cisco.iox.middleware.dataschema.DataSchema;
import com.cisco.iox.middleware.dataschema.Field;
import com.cisco.iox.middleware.device.Device;
import com.cisco.iox.middleware.devicetype.DeviceType;
import com.cisco.iox.middleware.devicetype.Sensor;
import com.cisco.iox.middleware.parsers.Expression;
import com.cisco.iox.middleware.parsers.PayloadParser;

public class MqttTopic {
	
	private final String deviceId;
	private final DataSchema dataSchema;
	private final Sensor sensor;
	private final PayloadParser payloadParser;
    private List<Expression> expressions = new ArrayList<>();
    private List<Integer> fieldIndexes = new ArrayList<>();
	
	public MqttTopic(String deviceId, Sensor sensor, DataSchema dataSchema, PayloadParser payloadParser) {
		this.deviceId = deviceId;
		this.sensor = sensor;
		this.dataSchema = dataSchema;
		this.payloadParser = payloadParser;
	}
	
	public DataSchema getDataSchema() {
		return dataSchema;
	}

	public Sensor getSensor() {
		return sensor;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void addExpression(String expression, Field field) {
        final Expression expr = payloadParser.compile(expression, field.getType().getJavaClass());
        expressions.add(expr);
        fieldIndexes.add(field.getFieldOrder());
    }

	public PayloadParser getPayloadParser() {
		return payloadParser;
	}

	public List<Expression> getExpressions() {
		return expressions;
	}

	public List<Integer> getFieldIndexes() {
		return fieldIndexes;
	}

}
