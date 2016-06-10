package com.cisco.iox.middleware.protocolHandler.mqtt;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.moquette.proto.messages.PublishMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cisco.iox.middleware.dataschema.DataSchema;
import com.cisco.iox.middleware.devicetype.Sensor;
import com.cisco.iox.middleware.parsers.Expression;
import com.cisco.iox.middleware.parsers.TextPayload;
import com.cisco.iox.middleware.plugin.AbstractDeviceProtocolHandler;
import com.cisco.iox.middleware.tuple.Tuple;
import com.cisco.iox.middleware.tuple.TupleContext;
import com.cisco.iox.middleware.tuple.TupleContextBuilder;

public class PublishEvent {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(PublishEvent.class);

	public static void onPublish(String clientID, PublishMessage publishMessage) {
        AbstractDeviceProtocolHandler protocolHandler = MqttHandlerContext.getInstance().getProtocolHandler();

		String topicName = publishMessage.getTopicName();
		
		LOGGER.debug("Got message to publish  from clientId {} on topic - {}", clientID, topicName);
		
		ByteBuffer payload = publishMessage.getPayload();
		final String data = new String(payload.array(), payload.position(), payload.limit());
		// assuming clientId is deviceId
		Map<String, MqttTopic> map = MqttHandlerContext.mqttDevices.get(clientID);

		
		if (map != null) {
			MqttTopic mqttTopic = map.get(topicName);
			if (mqttTopic != null) {
				final Sensor sensor = mqttTopic.getSensor();
				final String deviceId = mqttTopic.getDeviceId();
				DataSchema dataSchema = mqttTopic.getDataSchema();
				List<Integer> fieldIndexes = mqttTopic.getFieldIndexes();
		        TupleContext tupleContext = TupleContextBuilder.createTupleContext(deviceId, sensor, dataSchema.getDataSchemaId());
				
				List<Expression> expressions = mqttTopic.getExpressions();
				final List<List<Object>> results = mqttTopic.getPayloadParser().extract(new TextPayload(data), expressions);
		        List<Tuple> tuples = new ArrayList<>();
		        for(int i=0; i<expressions.size(); i++){
		            final List<Object> list = results.get(i);
		            for(int j=0; j<list.size(); j++){
		                if(j>=tuples.size())
		                    tuples.add(new Tuple(dataSchema.getFields().size()));
		                Tuple tuple = tuples.get(j);
		                tuple.setValue(fieldIndexes.get(i), list.get(j));
		            }
		        }
		        tupleContext.setTimestamp(System.currentTimeMillis());
		        
				Tuple[] array = tuples.toArray(new Tuple[tuples.size()]);
				protocolHandler.publish(tupleContext, array);
			} else {
				LOGGER.error("Topic {} not configured for device {}", topicName, clientID);
			}
		} else {
			LOGGER.error("Device  {} not configured", clientID);
		}
	}
}
