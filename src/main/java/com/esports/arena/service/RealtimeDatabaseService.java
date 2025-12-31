package com.esports.arena.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.esports.arena.util.EnvLoader;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class RealtimeDatabaseService {

        private static final String DB_URL = "https://esports-arena-7fb9b-default-rtdb.firebaseio.com/";
        private static final String TOKEN = EnvLoader.get("FIREBASE_DB_TOKEN");
        private static final HttpClient CLIENT = HttpClient.newHttpClient();
        private static final ObjectMapper MAPPER = new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        private static URI buildUri(String path) {
                String base = DB_URL.endsWith("/") ? DB_URL : DB_URL + "/";
                String auth = (TOKEN == null || TOKEN.isBlank()) ? "" : "?auth=" + URLEncoder.encode(TOKEN, StandardCharsets.UTF_8);
                return URI.create(base + path + ".json" + auth);
        }

        public static <T> T read(String path, Class<T> type) throws Exception {
                HttpRequest request = HttpRequest.newBuilder()
                                .uri(buildUri(path))
                                .GET()
                                .build();

                String body = CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).body();
                if (type == String.class) {
                        return type.cast(body);
                }
                return MAPPER.readValue(body, type);
        }

        public static <T> T read(String path, TypeReference<T> type) throws Exception {
                HttpRequest request = HttpRequest.newBuilder()
                                .uri(buildUri(path))
                                .GET()
                                .build();

                String body = CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).body();
                return MAPPER.readValue(body, type);
        }

        // Read collection that handles both Firebase array and object responses
        public static <T> Map<String, T> readCollection(String path, Class<T> valueType) throws Exception {
                HttpRequest request = HttpRequest.newBuilder()
                                .uri(buildUri(path))
                                .GET()
                                .build();

                String body = CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).body();
                
                if (body == null || "null".equals(body.trim())) {
                        return new HashMap<>();
                }

                JsonNode node = MAPPER.readTree(body);
                Map<String, T> result = new HashMap<>();

                if (node.isArray()) {
                        // Firebase returned an array (numeric sequential keys)
                        for (int i = 0; i < node.size(); i++) {
                                JsonNode element = node.get(i);
                                if (element != null && !element.isNull()) {
                                        T value = MAPPER.treeToValue(element, valueType);
                                        result.put(String.valueOf(i), value);
                                }
                        }
                } else if (node.isObject()) {
                        // Firebase returned an object (string keys)
                        node.fields().forEachRemaining(entry -> {
                                try {
                                        T value = MAPPER.treeToValue(entry.getValue(), valueType);
                                        result.put(entry.getKey(), value);
                                } catch (Exception e) {
                                        System.err.println("Error parsing entry: " + e.getMessage());
                                }
                        });
                }

                return result;
        }

        public static String readRaw(String path) throws Exception {
                return read(path, String.class);
        }

        public static void write(String path, Object data) throws Exception {
                sendWithBody("PUT", path, data);
        }

        public static void patch(String path, Object data) throws Exception {
                sendWithBody("PATCH", path, data);
        }

        public static void delete(String path) throws Exception {
                HttpRequest request = HttpRequest.newBuilder()
                                .uri(buildUri(path))
                                .DELETE()
                                .build();
                CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
        }

        // Atomically increments a numeric counter using ETag-based compare-and-set.
        public static long nextId(String counterPath) throws Exception {
                int attempts = 0;
                while (attempts++ < 5) {
                        HttpRequest getReq = HttpRequest.newBuilder()
                                        .uri(buildUri(counterPath))
                                        .header("X-Firebase-ETag", "true")
                                        .GET()
                                        .build();

                        HttpResponse<String> getResp = CLIENT.send(getReq, HttpResponse.BodyHandlers.ofString());
                        String etag = getResp.headers().firstValue("etag").orElse(null);
                        Long current = parseLong(getResp.body());
                        long next = (current == null ? 0L : current) + 1;

                        if (etag == null) {
                                // No ETag returned; accept the value and continue.
                                write(counterPath, next);
                                return next;
                        }

                        HttpRequest putReq = HttpRequest.newBuilder()
                                        .uri(buildUri(counterPath))
                                        .header("If-Match", etag)
                                        .header("Content-Type", "application/json")
                                        .PUT(HttpRequest.BodyPublishers.ofString(String.valueOf(next)))
                                        .build();

                        HttpResponse<Void> putResp = CLIENT.send(putReq, HttpResponse.BodyHandlers.discarding());
                        if (putResp.statusCode() == 200 || putResp.statusCode() == 204) {
                                return next;
                        }

                        if (putResp.statusCode() != 412) {
                                throw new IllegalStateException("Failed to increment counter: HTTP " + putResp.statusCode());
                        }
                        // 412 means ETag mismatch; retry.
                }
                throw new IllegalStateException("Failed to increment counter after retries");
        }

        private static void sendWithBody(String method, String path, Object data) throws Exception {
                String json = MAPPER.writeValueAsString(data);
                HttpRequest request = HttpRequest.newBuilder()
                                .uri(buildUri(path))
                                .header("Content-Type", "application/json")
                                .method(method, HttpRequest.BodyPublishers.ofString(json))
                                .build();
                CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
        }

        private static Long parseLong(String body) {
                try {
                        if (body == null || body.isBlank() || "null".equals(body.trim())) {
                                return null;
                        }
                        return MAPPER.readValue(body, Long.class);
                } catch (Exception e) {
                        return null;
                }
        }
}
