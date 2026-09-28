package wethinkcode.loadshed.common.mq;

import javax.jms.MessageListener;

public class MqTopicReceiver {

    public MqTopicReceiver init(String topicName, MessageListener listener) {
        // connect to topic and register listener
        return this;
    }

    public static void close(){

    }
}
