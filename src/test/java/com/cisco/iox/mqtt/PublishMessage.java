package com.cisco.iox.mqtt;

public class PublishMessage {

	int qos = 0;
    static String broker;
    
    static String clientId = ""; // should be set to deviceId
	private static String noOfMessages;
    
/*    MemoryPersistence persistence = new MemoryPersistence();
    private MqttClient client;
    private static PublishMessage me = new PublishMessage();
    ThreadPoolExecutor threadPoolExecutor;
*/
    public static void main(String[] args) throws Exception {
	  if(args.length < 3) {
		  throw new Exception("Usage java PublishMessage <brokerIP> <deviceId> <noOfMessages>");
	  }
	  broker = args[0];
	  clientId = args[1];
	  noOfMessages = args[2]; 
	  
	  /*for (int i = 0; i < args.length; i++) {
          try {
              MqttMessage message = new MqttMessage(jsondata.toJSONString().getBytes());
              message.setQos(qos);
              client.publish(topicName, message);
          } catch (MqttException me) {
              System.out.println("reason " + me.getReasonCode());
              System.out.println("msg " + me.getMessage());
              System.out.println("loc " + me.getLocalizedMessage());
              System.out.println("cause " + me.getCause());
              System.out.println("excep " + me);
              me.printStackTrace();
          }
	  }*/
	}
}
