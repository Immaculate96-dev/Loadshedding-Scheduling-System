package wethinkcode.loadshed.common.test;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MqTopicReceiver {
    private final String brokerUrl;
    private final String topicName;
    private Connection connection;
    private Session session;
    private MessageConsumer consumer;
    private final LinkedBlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

    public MqTopicReceiver(String brokerUrl, String topicName) {
        this.brokerUrl = brokerUrl;
        this.topicName = topicName;
    }

    public void start() throws JMSException {
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
        connection = connectionFactory.createConnection();
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = session.createTopic(topicName);
        consumer = session.createConsumer(topic);

        // Set up message listener to put messages in queue
        consumer.setMessageListener(message -> {
            try {
                if (message instanceof TextMessage) {
                    String text = ((TextMessage) message).getText();
                    messageQueue.put(text);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public String receiveMessage(long timeout) throws JMSException {
        try {
            return messageQueue.poll(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public void stop() throws JMSException {
        if (consumer != null) consumer.close();
        if (session != null) session.close();
        if (connection != null) connection.close();
        messageQueue.clear();
    }
}