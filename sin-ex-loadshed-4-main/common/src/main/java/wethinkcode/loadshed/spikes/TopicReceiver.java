package wethinkcode.loadshed.spikes;

import javax.jms.*;


import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;
import wethinkcode.loadshed.common.mq.MQ;

/**
 * I am a small "maker" app for receiving MQ messages from the Stage Service by
 * subscribing to a Topic.
 */
public class TopicReceiver implements Runnable
{
    private static long NAP_TIME = 2000; //ms

    public static final String MQ_TOPIC_NAME = "stage";

    public static String text = null;

    public static void main( String[] args ){
        final TopicReceiver app = new TopicReceiver();
        app.run();
    }

    private static boolean running = true;

    private static Connection connection;

    @Override
    public void run(){
        setUpMessageListener();
        while( running ){
            System.out.println( "Still doing stufff..." );
            snooze();
        }
        closeConnection();
        System.out.println( "Bye..." );
    }

    public static void setUpMessageListener(){
        try{
            final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory( MQ.URL );
            connection = factory.createConnection( MQ.USER, MQ.PASSWD );

            final Session session = connection.createSession( false, Session.AUTO_ACKNOWLEDGE );
            final Destination dest = session.createTopic( MQ_TOPIC_NAME ); // <-- NB: Topic, not Queue!

            final MessageConsumer receiver = session.createConsumer( dest );
            receiver.setMessageListener(m -> {
                        if (m instanceof ActiveMQTextMessage) {
                            try {
                                String text = ((TextMessage) m).getText();
                                TopicReceiver.text = text;
                                System.out.println("Received text in topic: " + TopicReceiver.text);

                                if(text.equals("SHUTDOWN")){
                                    running = false;
                                }
                            } catch (JMSException e) {
                                e.printStackTrace();
                            }
                        } else {
                            System.out.println("Received non-text message");
                        }

                    }
            );
            connection.start();

        }catch( JMSException erk ){
            throw new RuntimeException( erk );
        }
    }

    // to be called in webserver
    public static String getTopicText(){
        setUpMessageListener();
        return text;
    }

    private void snooze(){
        try{
            Thread.sleep( NAP_TIME );
        }catch( InterruptedException eek ){
            // meh...
        }
    }

    private void closeConnection(){
        if( connection != null ) try{
            connection.close();
        }catch( JMSException ex ){
            // meh
        }
    }

}
