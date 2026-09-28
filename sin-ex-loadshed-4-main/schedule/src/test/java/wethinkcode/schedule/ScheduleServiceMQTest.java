package wethinkcode.schedule;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import wethinkcode.stage.StageService;

import java.io.IOException;

public class ScheduleServiceMQTest {
    private static StageService stageService;
    private static ScheduleService server = new ScheduleService();
    public static final int TEST_PORT = 7777;


    @BeforeAll
    public static void startServer() throws IOException {
        server = new ScheduleService().initialise();
        server.start( TEST_PORT );
    }

    @AfterAll
    public static void stopServer(){
        server.stop();
    }
}
