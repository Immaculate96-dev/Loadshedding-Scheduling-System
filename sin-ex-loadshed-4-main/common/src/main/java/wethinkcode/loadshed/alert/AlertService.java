//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package wethinkcode.loadshed.alert;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import kong.unirest.HttpRequestWithBody;
import kong.unirest.Unirest;
import org.apache.activemq.ActiveMQConnectionFactory;

public class AlertService implements ExceptionListener {
    private static final Logger LOGGER = Logger.getLogger("loadshed.alert");
    private static final String NTFY_TOPIC = "loadshed.alert";
    private static final String NTFY_URL = "https://ntfy.sh/loadshed.alert";

    public static void main(String[] args) throws Exception {
        (new AlertService()).start();
    }

    public void start() throws JMSException {
        LOGGER.info("Starting Alert Service...");
        Connection connection = null;
        Session session = null;

        try {
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("admin", "admin", "tcp://localhost:61616");
            connection = connectionFactory.createConnection();
            connection.setExceptionListener(this);
            connection.start();
            session = connection.createSession(false, 1);
            Destination queue = session.createQueue("loadshed.alert");
            MessageConsumer consumer = session.createConsumer(queue);
            LOGGER.info("Listening on Queue: loadshed.alert");
            consumer.setMessageListener((message) -> {
                if (message instanceof TextMessage) {
                    try {
                        String alertMessage = ((TextMessage)message).getText();
                        this.handleAlert(alertMessage);
                    } catch (JMSException e) {
                        LOGGER.log(Level.SEVERE, "Failed to process alert message.", e);
                    }
                }

            });
        } catch (JMSException e) {
            LOGGER.log(Level.SEVERE, "JMS Setup failed.", e);
            throw e;
        }
    }

    private void handleAlert(String message) {
        LOGGER.log(Level.SEVERE, "!!! CRITICAL ALERT RECEIVED !!! Message: " + message);

        try {
            ((HttpRequestWithBody)Unirest.post("https://ntfy.sh/loadshedalert").header("Content-Type", "text/plain")).body(message).asEmpty();
            LOGGER.info("Alert successfully sent to ntfy.sh topic: loadshedalert");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to send alert to ntfy.sh.", e);
        }

    }

    public void onException(JMSException exception) {
        LOGGER.log(Level.SEVERE, "JMS Connection Exception.", exception);
    }
}
