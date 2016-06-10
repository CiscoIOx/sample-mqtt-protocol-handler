## Overview 
This tutorial provides sample MQTT protocol handler that provides a simple mqtt server functionality on which sensors can publish data on. 

## Build

Use Maven to build the java project by running the below command.

    mvn clean install

Maven build will copy the distribution binaries under target/mqtt-protocol-handler-1.0-distro folder


Before you can publish data on MQTT topic, you have to provision all data models to kickstart data acquisition.

## Deploy 
Use IOx management tools to manage the lifecycle of the protocol handler

## TEST

```
mosquitto_pub -h <iox-node-ip> -p <mqtt-port> -m {\"temperature\":100} -t temperature -i deviceA
```

Above command is publishing a json message to topic i.e "temperature" and setting client id as name of the provisioned device. 

Client Id is needed as mqtt custom protocol handler identifies for which device a message is getting published based on the client id.
