package com.cisco.iox.middleware.protocolHandler.mqtt;

import static io.moquette.parser.netty.Utils.VERSION_3_1;
import static io.moquette.parser.netty.Utils.VERSION_3_1_1;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.moquette.proto.messages.ConnAckMessage;
import io.moquette.proto.messages.ConnectMessage;
import io.moquette.proto.messages.DisconnectMessage;
import io.moquette.proto.messages.PubAckMessage;
import io.moquette.proto.messages.PubCompMessage;
import io.moquette.proto.messages.PubRecMessage;
import io.moquette.proto.messages.PubRelMessage;
import io.moquette.proto.messages.PublishMessage;
import io.moquette.proto.messages.SubAckMessage;
import io.moquette.proto.messages.SubscribeMessage;
import io.moquette.proto.messages.UnsubAckMessage;
import io.moquette.proto.messages.UnsubscribeMessage;
import io.moquette.proto.messages.AbstractMessage.QOSType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cisco.iox.middleware.plugin.mqtt.moquette.ConnectionDescriptor;
import com.cisco.iox.middleware.plugin.mqtt.moquette.NettyChannel;
import com.cisco.iox.middleware.plugin.mqtt.moquette.ServerChannel;

public class MqttProtoccolProcessor {

	private Map<String, ConnectionDescriptor> m_clientIDs = new ConcurrentHashMap<>();

	private static final Logger LOG = LoggerFactory.getLogger(MqttProtoccolProcessor.class);

	public void processConnect(ServerChannel session, ConnectMessage msg) {
		String clientID = msg.getClientID();
		LOG.debug("CONNECT for client <{}>", clientID);
		if (msg.getProtocolVersion() != VERSION_3_1 && msg.getProtocolVersion() != VERSION_3_1_1) {
			ConnAckMessage badProto = new ConnAckMessage();
			badProto.setReturnCode(ConnAckMessage.UNNACEPTABLE_PROTOCOL_VERSION);
			LOG.warn("processConnect sent bad proto ConnAck");
			session.write(badProto);
			session.close(false);
			return;
		}

		if (clientID == null || clientID.length() == 0) {
			badClientId(session);
			return;
		} 
		
		if(!MqttHandlerContext.mqttDevices.containsKey(clientID)) {
			LOG.warn("For Client ID {} no devices are configured", clientID);
			badClientId(session);
		}
		
		closeExistingConnection(clientID);

		ConnectionDescriptor connDescr = new ConnectionDescriptor(clientID, session, msg.isCleanSession());
		m_clientIDs.put(clientID, connDescr);
		int keepAlive = msg.getKeepAlive();
		LOG.debug("Connect with keepAlive {} s", keepAlive);
		session.setAttribute(NettyChannel.ATTR_KEY_KEEPALIVE, keepAlive);
		session.setAttribute(NettyChannel.ATTR_KEY_CLEANSESSION, msg.isCleanSession());
		// used to track the client in the publishing phases.
		session.setAttribute(NettyChannel.ATTR_KEY_CLIENTID, clientID);
		LOG.debug("Connect create session <{}>", session);
		session.setIdleTime(Math.round(keepAlive * 1.5f));

		ConnAckMessage okResp = new ConnAckMessage();
		okResp.setReturnCode(ConnAckMessage.CONNECTION_ACCEPTED);
		session.write(okResp);
	}

	public void closeExistingConnection(String clientID) {
		if (m_clientIDs.containsKey(clientID)) {
			LOG.warn("Found an existing connection with same client ID <{}>, forcing to close", clientID);
			// clean the subscriptions if the old used a cleanSession = true
			ServerChannel oldSession = m_clientIDs.get(clientID).getSession();
			boolean cleanSession = (Boolean) oldSession.getAttribute(NettyChannel.ATTR_KEY_CLEANSESSION);
			oldSession.setAttribute(NettyChannel.ATTR_KEY_SESSION_STOLEN, true);
			oldSession.close(false);
			LOG.debug("Existing connection with same client ID <{}>, forced to close", clientID);
		}
	}

	private void badClientId(ServerChannel session) {
		ConnAckMessage okResp = new ConnAckMessage();
		okResp.setReturnCode(ConnAckMessage.IDENTIFIER_REJECTED);
		session.write(okResp);
	}

	public void processSubscribe(NettyChannel session, SubscribeMessage msg) {
		// ack the client
		SubAckMessage ackMessage = new SubAckMessage();
		ackMessage.setMessageID(msg.getMessageID());
		ackMessage.addType(QOSType.FAILURE);
		session.write(ackMessage);
	}

	public void processUnsubscribe(NettyChannel session, UnsubscribeMessage msg) {
		// ack the client
		UnsubAckMessage ackMessage = new UnsubAckMessage();
		Integer messageID = msg.getMessageID();
		ackMessage.setMessageID(messageID);
		LOG.info("replying with UnsubAck to MSG ID {}", messageID);
		session.write(ackMessage);

	}

	public void processPublish(ServerChannel session, PublishMessage msg) {
		LOG.trace("PUB --PUBLISH--> SRV executePublish invoked with {}", msg);
		String clientID = (String) session.getAttribute(NettyChannel.ATTR_KEY_CLIENTID);
		final String topic = msg.getTopicName();
		// check if the topic can be wrote
		String user = (String) session.getAttribute(NettyChannel.ATTR_KEY_USERNAME);
		// TODO authentication

		PublishEvent.onPublish(clientID, msg);
		/*
		 * if (m_authorizator.canWrite(topic, user, clientID)) {
		 * 
		 * m_interceptor.notifyTopicPublished(msg, clientID); } else {
		 * LOG.debug("topic {} doesn't have write credentials", topic); }
		 */

	}

	public void processPubRec(NettyChannel nettyChannel, PubRecMessage msg) {
		// TODO Auto-generated method stub

	}

	public void processPubComp(NettyChannel nettyChannel, PubCompMessage msg) {
		// TODO Auto-generated method stub

	}

	public void processPubRel(NettyChannel nettyChannel, PubRelMessage msg) {
		// TODO Auto-generated method stub

	}

	public void processDisconnect(NettyChannel session, DisconnectMessage msg) {
		String clientID = (String) session.getAttribute(NettyChannel.ATTR_KEY_CLIENTID);
		boolean cleanSession = (Boolean) session.getAttribute(NettyChannel.ATTR_KEY_CLEANSESSION);
		if (cleanSession) {
			// cleanup topic subscriptions
		}
		m_clientIDs.remove(clientID);
		session.close(true);

		LOG.info("DISCONNECT client <{}> with clean session {}", clientID, cleanSession);
	}

	public void processPubAck(ServerChannel session, PubAckMessage msg) {
		String clientID = (String) session.getAttribute(NettyChannel.ATTR_KEY_CLIENTID);
		int messageID = msg.getMessageID();
		// Remove the message from message store
		// m_messagesStore.removeMessageInSession(clientID, messageID);
	}

}
