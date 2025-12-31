package com.esports.arena.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RealtimeDatabaseService {

    private static final String DB_URL =
            "https://esports-arena-7fb9b-default-rtdb.firebaseio.com/";

    private static final ObjectMapper mapper = new ObjectMapper();

    // Write object to Firebase
    public static void write(String path, Object data) throws Exception {
        String url = DB_URL + "/" + path + ".json";

        String json = mapper.writeValueAsString(data);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.discarding());
    }

    // Read data from Firebase
    public static String read(String path) throws Exception {
        String url = DB_URL + "/" + path + ".json";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        return HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString())
                .body();
    }
}
