package com.cisco.iox.middleware.protocolHandler.mqtt;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cisco.iox.middleware.dataschema.DataSchema;
import com.cisco.iox.middleware.dataschema.Field;
import com.cisco.iox.middleware.device.Device;
import com.cisco.iox.middleware.devicetype.ContentHandler;
import com.cisco.iox.middleware.devicetype.ContentMapping;
import com.cisco.iox.middleware.devicetype.DeviceType;
import com.cisco.iox.middleware.devicetype.Sensor;
import com.cisco.iox.middleware.parsers.PayloadParser;
import com.cisco.iox.middleware.parsers.PayloadParsers;
import com.cisco.iox.middleware.plugin.AbstractDeviceProtocolHandler;
import com.cisco.iox.middleware.plugin.ProtocolPluginException;
import com.cisco.iox.middleware.plugin.mqtt.moquette.NettyAcceptor;
import com.cisco.iox.mlib.messaging.Service;

@Service(MqttProtocolHandler.PROTOCOLHANDLER_MQTT)
public class MqttProtocolHandler extends AbstractDeviceProtocolHandler {

	private static final Logger LOG = LoggerFactory.getLogger(MqttProtocolHandler.class);

	/**
	 * Name of the protocol
	 */
	private static final String PROTOCOL = "mqtt";

	public static final String CONTENT_TYPE = "contentType";

	/**
	 * Name of the protocol Handler
	 */
	protected static final String PROTOCOLHANDLER_MQTT = "custom:service:protocolHandler:mqtt-broker";

	private NettyAcceptor nettyAcceptor;

	public MqttProtocolHandler() {
		super(PROTOCOLHANDLER_MQTT, PROTOCOL);
	}

	@Override
	protected void onPluginInit() throws ProtocolPluginException {
	}

	@Override
	protected void onPluginStart() throws ProtocolPluginException {
		Map<String, Object> config = getModuleDetails().getModuleConfiguration();
		String ip = (String) config.get("ip");
		int port = ((Number) config.get("port")).intValue();
		if (port <= 0)
			port = 1883;

		LOG.info("starting mqtt broker on {}:{}", ip, port);
		nettyAcceptor = new NettyAcceptor();
		processor = new MqttProtoccolProcessor();
		try {
			nettyAcceptor.initialize(processor, ip, port);
		} catch (IOException e) {
			throw new ProtocolPluginException("Error in starting MQTT Server", e);
		}
		MqttHandlerContext.getInstance().setProtocolHandler(this);
	}

	@Override
	protected void onPluginStop() throws ProtocolPluginException {
		nettyAcceptor.close();
	}

	private static final String TOPIC = "topicName";

	private MqttProtoccolProcessor processor;

	@Override
	protected List<String> validate(DeviceType deviceType, Map<String, DataSchema> dataSchemasMap) throws Exception {
		for (Sensor sensor : deviceType.getSensors()) {
			final DataSchema dataSchema = dataSchemasMap.get(sensor.getDataSchemaId());
			final ContentHandler contentHandler = sensor.getContentHandler();
			
			for (ContentMapping contentMapping : contentHandler.getContentMappings()) {
				Field field = dataSchema.getField(contentMapping.getFieldName());
				Map<String, Object> props = new HashMap<>();
				props.put(CONTENT_TYPE, contentHandler.getContentType());
				props.putAll(contentHandler.getProtocolProperties());
				props.putAll(contentMapping.getProtocolProperties());
				String contentType = (String) props.get(CONTENT_TYPE);
				if (contentType == null)
					return Collections.singletonList(
							"contentType is missing for field " + field.getName() + " in sensor " + sensor.getName());
				Object topicProp = props.get(TOPIC);
				
				if (topicProp == null)
					return Collections.singletonList(
							"Property " + TOPIC + "  is missing for field " + field.getName() + " in sensor " + sensor.getName());
				String topicName = topicProp.toString().trim();
				
				for (String deviceId :  MqttHandlerContext.mqttDevices.keySet()) {
					Map<String, MqttTopic> topicMap = MqttHandlerContext.mqttDevices.get(deviceId);
					if(topicMap.containsKey(topicName)) {
						return Collections.singletonList("Topic " + topicName + " already configured in another deviceType ");
					}
				}
			}
		}

		return Collections.emptyList();
	}
	
	@Override
	protected List<String> validate(Device device, DeviceType deviceType, Map<String, DataSchema> dataSchemasMap) throws Exception {
		return validate(deviceType, dataSchemasMap);
	}

	@Override
	protected synchronized void onDeviceAdd(Device device, DeviceType deviceType, Map<String, DataSchema> dataSchemaMap) throws ProtocolPluginException {
		Map<String, MqttTopic> mqttTopics = new HashMap<>();

		List<Sensor> sensors = deviceType.getSensors();
		for (Sensor sensor : sensors) {
			DataSchema dataSchema = dataSchemaMap.get(sensor.getDataSchemaId());

			ContentHandler contentHandler = sensor.getContentHandler();
			for (ContentMapping contentMapping : contentHandler.getContentMappings()) {
				Field field = dataSchema.getField(contentMapping.getFieldName());
				Map<String, Object> props = new HashMap<>();
				props.put(CONTENT_TYPE, contentHandler.getContentType());

				props.putAll(contentHandler.getProtocolProperties());
				props.putAll(contentMapping.getProtocolProperties());

				if (contentMapping.getContentType() != null) {
					props.put(CONTENT_TYPE, contentMapping.getContentType());
				}
				String contentType = (String) props.get(CONTENT_TYPE);
				PayloadParser payloadParser = PayloadParsers.get(contentType);
				String topic = props.get(TOPIC).toString();
				MqttTopic mqttTopic = mqttTopics.get(topic);
				if (mqttTopic == null) {
					mqttTopic = new MqttTopic(device.getDeviceId(), sensor, dataSchema, payloadParser);
					mqttTopics.put(topic, mqttTopic);
				}
				mqttTopic.addExpression(contentMapping.getExpression(), field);
			}
		}
		MqttHandlerContext.mqttDevices.put(device.getDeviceId(), mqttTopics);
	}

	@Override
	protected void onDeviceRemove(Device device) throws ProtocolPluginException {
		// handle device getting removed. Mostly if any connection is
		// established , close those connections
		processor.closeExistingConnection(device.getDeviceId());
		Map<String, MqttTopic> remove = MqttHandlerContext.mqttDevices.remove(device.getDeviceId());
	}
}
