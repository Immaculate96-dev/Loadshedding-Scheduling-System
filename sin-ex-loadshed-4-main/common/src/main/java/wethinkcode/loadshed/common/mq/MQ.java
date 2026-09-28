package wethinkcode.loadshed.common.mq;

/**
 * I contain a few variables and definitions common to all the MQ utility classes.
 */
public interface MQ
{
    static final String URL = "tcp://localhost:61616";

    static final String USER = "admin";

    static final String PASSWD = "admin";

    //Adding the missing topic constant for the stage module
    static final String STAGE_TOPIC = "loadshed.stage";

    // adding THIS NEW CONSTANT for the Alert Service queue
    static final String ALERT_QUEUE = "loadshed.alert";

}
