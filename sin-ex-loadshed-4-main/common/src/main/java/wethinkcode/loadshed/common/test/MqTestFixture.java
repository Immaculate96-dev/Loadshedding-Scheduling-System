package wethinkcode.loadshed.common.test;

import org.apache.activemq.broker.BrokerService;

public class MqTestFixture {
    private BrokerService broker;
    private String brokerUrl = "tcp://localhost:61616";
    private boolean started = false;

    public void start() {
        if (started) {
            return;
        }

        try {
            broker = new BrokerService();
            broker.addConnector(brokerUrl);
            broker.setPersistent(false);
            broker.setUseJmx(false);
            broker.setDataDirectory("target/activemq-data");
            broker.start();
            broker.waitUntilStarted();
            started = true;

            // Small delay to ensure broker is fully ready
            Thread.sleep(500);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start embedded broker", e);
        }
    }

    public void stop() {
        if (!started) {
            return;
        }

        try {
            if (broker != null) {
                broker.stop();
                broker.waitUntilStopped();
            }
            started = false;
        } catch (Exception e) {
            throw new RuntimeException("Failed to stop embedded broker", e);
        }
    }

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public boolean isStarted() {
        return started;
    }
}