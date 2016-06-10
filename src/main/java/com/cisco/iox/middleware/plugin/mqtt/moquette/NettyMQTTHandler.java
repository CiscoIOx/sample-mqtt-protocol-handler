package com.cisco.iox.middleware.plugin.mqtt.moquette;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import io.netty.handler.codec.CorruptedFrameException;
import io.moquette.proto.messages.*;
import io.moquette.proto.Utils;

import static io.moquette.proto.messages.AbstractMessage.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cisco.iox.middleware.protocolHandler.mqtt.MqttProtoccolProcessor;

/**
 *
 * @author andrea
 */
@Sharable
public class NettyMQTTHandler extends ChannelInboundHandlerAdapter {
    
    private static final Logger LOG = LoggerFactory.getLogger(NettyMQTTHandler.class);
    private final MqttProtoccolProcessor m_processor;

    public NettyMQTTHandler(MqttProtoccolProcessor processor) {
        m_processor = processor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        AbstractMessage msg = (AbstractMessage) message;
        LOG.trace("Received a message of type {}", Utils.msgType2String(msg.getMessageType()));
        try {
            switch (msg.getMessageType()) {
                case CONNECT:
                    m_processor.processConnect(new NettyChannel(ctx), (ConnectMessage) msg);
                    break;
                case SUBSCRIBE:
                    m_processor.processSubscribe(new NettyChannel(ctx), (SubscribeMessage) msg);
                    break;
                case UNSUBSCRIBE:
                    m_processor.processUnsubscribe(new NettyChannel(ctx), (UnsubscribeMessage) msg);
                    break;
                case PUBLISH:
                    m_processor.processPublish(new NettyChannel(ctx), (PublishMessage) msg);
                    break;
                case PUBREC:
                    m_processor.processPubRec(new NettyChannel(ctx), (PubRecMessage) msg);
                    break;
                case PUBCOMP:
                    m_processor.processPubComp(new NettyChannel(ctx), (PubCompMessage) msg);
                    break;
                case PUBREL:
                    m_processor.processPubRel(new NettyChannel(ctx), (PubRelMessage) msg);
                    break;
                case DISCONNECT:
                    m_processor.processDisconnect(new NettyChannel(ctx), (DisconnectMessage) msg);
                    break;
                case PUBACK:
                    m_processor.processPubAck(new NettyChannel(ctx), (PubAckMessage) msg);
                    break;
                case PINGREQ:
                    PingRespMessage pingResp = new PingRespMessage();
                    ctx.writeAndFlush(pingResp);
                    break;
            }
        } catch (Exception ex) {
            LOG.error("Bad error in processing the message", ex);
        }
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String clientID = (String) NettyUtils.getAttribute(ctx, NettyChannel.ATTR_KEY_CLIENTID);
        if (clientID != null && !clientID.isEmpty()) {
            //if the channel was of a correctly connected client, inform messaging
            //else it was of a not completed CONNECT message or sessionStolen
            boolean stolen = false;
            Boolean stolenAttr = (Boolean) NettyUtils.getAttribute(ctx, NettyChannel.ATTR_KEY_SESSION_STOLEN);
            if (stolenAttr != null && stolenAttr == Boolean.TRUE) {
                stolen = stolenAttr;
            }
            // TODO m_processor.processConnectionLost(new LostConnectionEvent(clientID, stolen));
        }
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof CorruptedFrameException) {
            //something goes bad with decoding
            LOG.warn("Error decoding a packet, probably a bad formatted packet, message: " + cause.getMessage());
        } else {
            LOG.error("Ugly error on networking", cause);
        }
        ctx.close();
    }
}
