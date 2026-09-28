package wethinkcode.web;

import com.google.common.annotations.VisibleForTesting;
import io.javalin.Javalin;
import wethinkcode.loadshed.spikes.TopicReceiver;
import wethinkcode.schedule.ScheduleService;
import wethinkcode.stage.StageService;

import javax.jms.*;
import javax.jms.Queue;

import org.apache.activemq.ActiveMQConnectionFactory;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.json.JSONArray;

public class WebService {

    public static final int DEFAULT_PORT = 7100;
    private static final String ALERT_QUEUE = "loadshed.alert";

    private HttpClient httpClient = HttpClient.newHttpClient();
    private ExternalAPIClient externalApiClient;

    private Javalin server;
    private int servicePort;

    @VisibleForTesting
    WebService initialise(){
        // Pass the sendAlert method as callback
        externalApiClient = new ExternalAPIClient(httpClient, this::sendAlert);
        server = configureHttpServer();
        return this;
    }

    public static void main(String[] args){
        WebService svc = new WebService().initialise();
        TopicReceiver.setUpMessageListener();
        svc.start();
    }

    public void start() { start(DEFAULT_PORT); }

    @VisibleForTesting
    void start(int networkPort){
        servicePort = networkPort;
        run();
    }

    public void stop() { server.stop(); }

    public void run() { server.start(servicePort); }

    //ALERT
    private void sendAlert(String messageText) {
        ConnectionFactory factory = new ActiveMQConnectionFactory("admin", "admin", "tcp://localhost:61616");

        Connection connection = null;
        Session session = null;
        try {
            connection = factory.createConnection();
            connection.start();

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(ALERT_QUEUE);
            MessageProducer producer = session.createProducer(queue);

            TextMessage message = session.createTextMessage(messageText);
            producer.send(message);

        } catch (JMSException e) {
            e.printStackTrace();
        } finally {
            // Close resources properly
            try {
                if (session != null) session.close();
                if (connection != null) connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }

    // HTTP SERVER
    private Javalin configureHttpServer() {
        HashMap<String, Object> viewModel = new HashMap<>();
        server = Javalin.create(config -> config.showJavalinBanner = true);

        //  this appears on the home page
        server.get("/", ctx -> {
            int stage = externalApiClient.getStage();
            Collection<String> provincesResponse = externalApiClient.getProvinces();

            List<String> sanProvinces = List.of(
                    "Gauteng", "Western Cape", "KwaZulu-Natal", "Northern Cape",
                    "Eastern Cape", "Free State", "Limpopo", "North West", "Mpumalanga"
            );

            List<String> provinces = provincesResponse.stream()
                    .filter(sanProvinces::contains)
                    .collect(Collectors.toList());

            Map<String, String> provinceUrlMap = new HashMap<>();
            for (String province : provinces) {
                String encodedProvince = new URI(null, null, province, null).getRawPath();
                provinceUrlMap.put(province, encodedProvince);
            }

            viewModel.put("loadsheddingstage", stage);
            viewModel.put("provinces", provinces);
            viewModel.put("provinceUrlMap", provinceUrlMap);
            ctx.render("/templates/stage.html", viewModel);
        });

        // Towns inside province
        server.get("/towns/{province}", ctx -> {
            String province = URLDecoder.decode(ctx.pathParam("province"), StandardCharsets.UTF_8);
            ArrayList<String> towns = externalApiClient.getTownsInAProvince(province);
            int stage = externalApiClient.getStage();
            String encodedProvince = new URI(null, null, province, null).getRawPath();

            viewModel.put("towns", towns);
            viewModel.put("stage", stage);
            viewModel.put("encodedProvince", encodedProvince);
            ctx.render("/templates/towns.html", viewModel);
        });

        // Schedule per town
        server.get("{town}/{province}/{stage}/schedule", ctx -> {
            String town = URLDecoder.decode(ctx.pathParam("town"), StandardCharsets.UTF_8);
            String province = URLDecoder.decode(ctx.pathParam("province"), StandardCharsets.UTF_8);
            int stage = Integer.parseInt(ctx.pathParam("stage"));

            // Convert kong.unirest.json.JSONObject to org.json.JSONObject
            org.json.JSONObject schedule = new org.json.JSONObject(externalApiClient.getScheduleForTown(town, province, stage).toString());
            viewModel.put("schedule", schedule);
            viewModel.put("scheduleDisplay", formatScheduleDisplay(schedule));
            ctx.render("/templates/schedule.html", viewModel);
        });

        return server;
    }

    //SCHEDULE DISPLAY
    private List<List<String>> formatScheduleDisplay(JSONObject schedule) {
        JSONArray days = schedule.getJSONArray("days");
        List<List<String>> scheduleDisplay = new ArrayList<>();

        for (int i = 0; i < days.length(); i++) {
            JSONObject day = days.getJSONObject(i);
            JSONArray slots = day.getJSONArray("slots");
            List<String> slotStrings = new ArrayList<>();

            for (int j = 0; j < slots.length(); j++) {
                JSONObject slot = slots.getJSONObject(j);
                JSONArray start = slot.getJSONArray("start");
                JSONArray end = slot.getJSONArray("end");

                slotStrings.add(String.format("%02d:%02d - %02d:%02d",
                        start.getInt(0), start.getInt(1),
                        end.getInt(0), end.getInt(1)
                ));
            }
            scheduleDisplay.add(slotStrings);
        }

        return scheduleDisplay;
    }
}
