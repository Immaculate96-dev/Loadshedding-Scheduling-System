package wethinkcode.schedule;

import java.time.LocalDate;
import java.io.IOException;
import java.net.ServerSocket;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kong.unirest.HttpResponse;
import kong.unirest.HttpStatus;
import kong.unirest.Unirest;
import org.junit.jupiter.api.*;
import wethinkcode.loadshed.common.transfer.ScheduleDO;
import wethinkcode.loadshed.common.mq.test.NullTopicReceiver;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * I am an API / functional test of the ScheduleService. I am not a unit test.
 */
@Tag( "functional" )
public class ScheduleServiceAPITest
{
    public static final int TEST_PORT = 0;

    private static ScheduleService testSvc;

    private static NullTopicReceiver nullTopicReceiver;

    @BeforeAll
    public static void initJsonMapper(){
        final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule( new JavaTimeModule() );
        mapper.disable( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS );
        Unirest.config().setObjectMapper( new kong.unirest.jackson.JacksonObjectMapper( mapper ) );
    }

    @BeforeAll
    public static void initTestScheduleFixture() throws IOException{
        testSvc = new ScheduleService().initialise();
        TEST_PORT = findAvailablePort();
        testSvc.start( TEST_PORT );
    }

// thia method finds unused network port that test server can use
    private static int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    @AfterAll
    public static void destroyTestFixture(){
        testSvc.stop();
    }

    @Test
    public void getSchedule_someTown(){
        HttpResponse<ScheduleDO> response = Unirest
            .get( serverUrl() + "/Eastern%20Cape/Gqeberha/4" )
            .asObject( ScheduleDO.class );
        assertEquals( HttpStatus.OK, response.getStatus());

        ScheduleDO schedule = response.getBody();
        assertEquals( 4, schedule.numberOfDays() );
        assertEquals( LocalDate.now(), schedule.getStartDate() );
    }

    @Test
    public void getSchedule_nonexistentTown(){
        HttpResponse<ScheduleDO> response = Unirest
            .get( serverUrl() + "/Mars/Elonsburg/4" )
            .asObject( ScheduleDO.class );
        assertEquals( HttpStatus.NOT_FOUND, response.getStatus() );
        assertEquals( 0, response.getBody().numberOfDays() );
    }

    @Test
    public void illegalStage(){
        HttpResponse<ScheduleDO> response = Unirest
            .get( serverUrl() + "/Western%20Cape/Knysna/42" )
            .asObject( ScheduleDO.class );
        assertEquals( HttpStatus.BAD_REQUEST, response.getStatus() );
    }

    private String serverUrl(){
        return "http://localhost:" + TEST_PORT;
    }
}
