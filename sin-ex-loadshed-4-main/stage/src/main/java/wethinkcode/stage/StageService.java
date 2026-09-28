package wethinkcode.stage;


import com.google.common.annotations.VisibleForTesting;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import wethinkcode.loadshed.common.transfer.StageDO;
import wethinkcode.loadshed.common.mq.MqTopicSender;
import wethinkcode.loadshed.spikes.TopicSender;
import javax.jms.JMSException;
import java.util.HashMap;
import java.util.Map;


/**
 * I provide a REST API that reports the current loadshedding "stage". I provide two endpoints:
 * <dl>
 * <dt>GET /stage
 * <dd>report the current stage of loadshedding as a JSON serialisation of a {@code StageDO} data/transfer object
 * <dt>POST /stage
 * <dd>set a new loadshedding stage/level by POSTing a JSON-serialised {@code StageDO} instance as the body of the
 * request.
 * </ul>
 */
public class StageService
{
    public static final int DEFAULT_STAGE = 0; // no loadshedding. Ha!

    public static final int DEFAULT_PORT = 7001;

    public static final String MQ_TOPIC_NAME = "stage";

    public int stage;

    public MqTopicSender mqTopicSender;


    public static void main( String[] args ){
        final StageService svc = new StageService().initialise();
        svc.start();
    }

    private int loadSheddingStage;

    private Javalin server;

    private int servicePort;

    @VisibleForTesting
    public StageService initialise(){
        return initialise( DEFAULT_STAGE );
    }

    @VisibleForTesting
    public StageService initialise( int initialStage ){
        loadSheddingStage = initialStage;
        assert loadSheddingStage >= 0;
        this.mqTopicSender = null;

        server = initHttpServer();
        return this;
    }
    @VisibleForTesting
    public StageService initialise(int stage, MqTopicSender sender) {
        this.stage = stage;
        this.mqTopicSender = sender;
        server = initHttpServer();
        // start listening or do something setup-related
        return this;
    }

    public void start(){
        start( DEFAULT_PORT );
    }

    @VisibleForTesting
    public void start( int networkPort ){
        servicePort = networkPort;
        run();
    }

    public void stop(){
        server.stop();
    }

    public void run(){
        server.start( servicePort );
    }

    private Javalin initHttpServer(){
        return Javalin.create()
            .get( "/stage", this::getCurrentStage )
            .post( "/stage", this::setNewStage );
    }

    private Context getCurrentStage( Context ctx ){
        return ctx.json( new StageDO( loadSheddingStage ) );
    }

    private Context setNewStage( Context ctx )throws JMSException{
        final StageDO stageData = ctx.bodyAsClass( StageDO.class );
        final int newStage = stageData.getStage();
        if( newStage >= 0 ){
            loadSheddingStage = newStage;
            broadcastStageChangeEvent( ctx );
            ctx.status( HttpStatus.OK );
        }else{
            ctx.status( HttpStatus.BAD_REQUEST );
        }
        return ctx.json( new StageDO( loadSheddingStage ) );
    }

    private void broadcastStageChangeEvent( Context ctx ) throws JMSException{
        final StageDO stageData = ctx.bodyAsClass( StageDO.class );
        final int newStage = stageData.getStage();

        // Use injected sender (for tests) or create real one (for production)
        if (mqTopicSender != null) {
            // Test mode - use the injected NullTopicSender
            mqTopicSender.send(String.valueOf(newStage));
        } else {
            // Production mode - use real TopicSender
            TopicSender topicSender = new TopicSender();
            Map<String, Integer> stage = Map.of("stage", newStage);
            topicSender.sendMessages(stage);
        }

    }

}
