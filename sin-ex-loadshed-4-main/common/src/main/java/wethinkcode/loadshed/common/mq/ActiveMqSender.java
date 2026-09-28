package wethinkcode.loadshed.common.mq;

import javax.jms.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActiveMqSender {
    private static final Logger LOGGER = Logger.getLogger("loadshed.alert.sender");

    public static void sendAlert(String alertMessage) {
        try {
            var connection = new org.apache.activemq.ActiveMQConnectionFactory(MQ.USER, MQ.PASSWD, MQ.URL).createConnection();
            try {
                connection.start();

                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                try {

                    // CRITICAL: Creating a Producer for the QUEUE
                    Destination queue = session.createQueue(MQ.ALERT_QUEUE);
                    MessageProducer producer = session.createProducer(queue);
                    try {
                        producer.setDeliveryMode(DeliveryMode.PERSISTENT); // Ensure delivery
                        TextMessage message = session.createTextMessage(alertMessage);
                        producer.send(message);
                        LOGGER.info("Sent alert to queue: " + alertMessage);
                    } finally {
                        producer.close();
                    }
                } finally {
                    session.close();
                }
            } finally {
                connection.close();
            }
        } catch (JMSException e) {
            // Log that the alert itself failed to send
            LOGGER.log(Level.SEVERE, "CRITICAL: FAILED to send alert message to MQ!", e);
        }
    }
}