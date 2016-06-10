package com.cisco.iox.middleware.protocolHandler.mqtt;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.cisco.iox.middleware.plugin.AbstractDeviceProtocolHandler;

public class MqttHandlerContext {

	private static MqttHandlerContext me = new MqttHandlerContext();
	
	static Map<String, Map<String, MqttTopic>> mqttDevices = new ConcurrentHashMap();

	AbstractDeviceProtocolHandler protocolHandler; 

	public AbstractDeviceProtocolHandler getProtocolHandler() {
		return protocolHandler;
	}

	private MqttHandlerContext() {
	}
	
	public static MqttHandlerContext getInstance() {
		return me;
	}
	
	public void setProtocolHandler(AbstractDeviceProtocolHandler protocolHandler) {
		this.protocolHandler = protocolHandler;
	}
}
