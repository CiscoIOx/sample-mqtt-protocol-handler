#!/bin/bash
if [ ! -d "dataDir" ]; then
mkdir dataDir
fi


export message_broker_export message_broker_IP_ADDRESS=10.78.106.211   
export message_broker_TCP_8080_PORT=40000

export PERSISTENT_DIR=dataDir

java $*  \
-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=6000 \
-Dio.netty.allocator.maxOrder=5 \
-cp ".:lib/*:resources/:lib/third-party/*" \
com.cisco.iox.middleware.Server
