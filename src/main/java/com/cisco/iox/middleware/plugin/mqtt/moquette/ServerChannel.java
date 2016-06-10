package com.cisco.iox.middleware.plugin.mqtt.moquette;

import io.netty.util.AttributeKey;

public interface ServerChannel {
    
    Object getAttribute(AttributeKey<Object> key);
    
    void setAttribute(AttributeKey<Object> key, Object value);
    
    void setIdleTime(int idleTime);
    
    void close(boolean immediately);
    
    void write(Object value);
}
