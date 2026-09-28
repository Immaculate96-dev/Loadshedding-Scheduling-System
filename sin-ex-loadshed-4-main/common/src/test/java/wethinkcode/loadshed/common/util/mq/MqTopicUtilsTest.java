package wethinkcode.loadshed.common.util.mq;

import org.junit.jupiter.api.Test;
import wethinkcode.loadshed.common.mq.MqTopicReceiver;
import wethinkcode.loadshed.common.mq.MqTopicSender;
import wethinkcode.loadshed.common.mq.test.MqTestFixture;
import wethinkcode.loadshed.common.mq.test.NullTopicSender;

public class MqTopicUtilsTest {

    @Test
    void someTest(){
        MqTopicSender mqTopicSender = new MqTopicSender();
    }

    @Test
    void someOtherTest(){
        MqTopicReceiver mqTopicReceiver = new MqTopicReceiver();
    }

    @Test
    void anothertest(){
        MqTestFixture mqTestFixture = new MqTestFixture();
    }

    @Test
    void onemore(){
        NullTopicSender nullTopicSender = new NullTopicSender();
    }
}
