package wethinkcode.loadshed.common.test;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class MqTopicSender{
    private final String brokerUrl;
    private final String topicName;
    private Connection connection;
    private Session session;
    private MessageProducer producer;

    public MqTopicSender(String brokerUrl, String topicName) {
        this.brokerUrl = brokerUrl;
        this.topicName = topicName;
    }

    public void start() throws JMSException {
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
        connection = connectionFactory.createConnection();
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = session.createTopic(topicName);
        producer = session.createProducer(topic);
    }

    public void sendMessage(String message) throws JMSException {
        TextMessage textMessage = session.createTextMessage(message);
        producer.send(textMessage);
    }

    public void stop() throws JMSException {
        if (producer != null) producer.close();
        if (session != null) session.close();
        if (connection != null) connection.close();
    }
}