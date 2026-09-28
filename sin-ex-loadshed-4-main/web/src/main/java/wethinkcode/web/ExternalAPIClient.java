package wethinkcode.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kong.unirest.json.JSONObject;
import wethinkcode.places.model.Town;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ExternalAPIClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Consumer<String> alertCallback;

    public ExternalAPIClient(HttpClient httpClient, Consumer<String> alertCallback) {
        this.httpClient = httpClient;
        this.alertCallback = alertCallback;
    }

    public Collection<String> getProvinces() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:7000/provinces"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            alertCallback.accept("Place-Name Service unavailable: " + e.getMessage());
            return new ArrayList<>(); // fallback
        }
    }

    public int getStage() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:7001/stage"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, Integer> map = objectMapper.readValue(response.body(), new TypeReference<Map<String, Integer>>() {});
            return map.get("stage");
        } catch (Exception e) {
            alertCallback.accept("Stage Service unavailable: " + e.getMessage());
            return 0; // fallback to Stage 0
        }
    }

    public ArrayList<String> getTownsInAProvince(String province) {
        try {
            String encodedProvince = new URI(null, null, province, null).getRawPath();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:7000/towns/" + encodedProvince))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            List<Town> townObjects = objectMapper.readValue(response.body(), new TypeReference<List<Town>>() {});

            ArrayList<String> towns = new ArrayList<>();
            for (Town town : townObjects) {
                towns.add(town.getName());
            }
            return towns;
        } catch (Exception e) {
            alertCallback.accept("Place-Name Service unavailable for province " + province + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public JSONObject getScheduleForTown(String town, String province, int stage) {
        try {
            String encodedTown = new URI(null, null, town, null).getRawPath();
            String encodedProvince = new URI(null, null, province, null).getRawPath();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:7002/" + encodedProvince + "/" + encodedTown + "/" + stage))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new JSONObject(response.body());
        } catch (Exception e) {
            alertCallback.accept("Schedule Service unavailable for " + town + ", " + province + ": " + e.getMessage());
            return new JSONObject(); // fallback
        }
    }
}
