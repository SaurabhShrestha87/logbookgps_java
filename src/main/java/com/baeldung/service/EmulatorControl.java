package com.baeldung.service;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Flow;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.baeldung.view.SearchController.authToken;

public class EmulatorControl {
    HttpRequest request;
    Logger logger = Logger.getLogger(EmulatorControl.class.getName());
    Process process;

    PrintWriter writer;
    BufferedReader reader;

    public void run(int emulatorPort, String name) {
        // saurabh == 518XFP5YZ/85UOy7
        // pavlo == yiRczGu7CUqu62P7
        String sseUrl = "https://logbookgps.com:8081/emulator/sse/" + name;
        try {
            process = new ProcessBuilder("telnet", "localhost", String.valueOf(emulatorPort)).start();
            logger.log(Level.INFO, name + " made socket: " + emulatorPort);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            logger.log(Level.INFO, name + " made reader: " + emulatorPort);
            writer = new PrintWriter(process.getOutputStream(), true);
            logger.log(Level.INFO, name + " made writer: " + emulatorPort);
            // Wait for the initial prompt
            logger.log(Level.INFO, name + " : Waiting for telnet connection");
            String prompt = reader.readLine();
            // wait till we get OK in reader within 10 next lines
            for (int i = 0; i < 10; i++) {
                if (prompt.equals("OK")) {
                    logger.log(Level.INFO, name + " : Ok received.");
                    break;
                }
                prompt = reader.readLine();
            }

            // Send authentication command
            writer.println("auth " + authToken);
            logger.log(Level.INFO, name + " : Authentication command sent.");
            for (int i = 0; i < 10; i++) {
                if (prompt.equals("OK")) {
                    logger.log(Level.INFO, name + " : Ok received.");
                    break;
                }
                prompt = reader.readLine();
                if (i == 9) {
                    throw new RuntimeException("Error while authenticating emulator");
                }
            }
            enableSse(sseUrl, writer, reader);
        } catch (Exception e) {
            logger.log(Level.SEVERE, name + " : Error while running emulator control", e);
            throw new RuntimeException("Error while running emulator control", e);
        }
    }

    private void enableSse(String sseUrl, PrintWriter writer, BufferedReader reader) {
        HttpClient client = HttpClient.newHttpClient();
        logger.log(Level.WARNING, sseUrl + " : Enabling SSE");
        // Create HttpClient and HttpRequest for the SSE endpoint
        request = HttpRequest.newBuilder().uri(URI.create(sseUrl)).header("Accept", "text/event-stream").build();
        // Send the request and handle the response synchronously
        client.sendAsync(request, HttpResponse.BodyHandlers.fromLineSubscriber(new Flow.Subscriber<String>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(String item) {
                synchronized (writer) {
                    logger.log(Level.INFO, sseUrl + " : Received SSE event: " + item);
                    // check for heartbeats
                    if (item.isEmpty()) {
                        return;
                    }
                    if (item.startsWith(":")) {
                        return;
                    }
                    if (item.startsWith("data:")) {
                        try {
                            JSONObject json = new JSONObject(item.substring(5));
                            String status = json.getString("status");
                            if (status.equals("SUCCESS")) {
                                JSONObject latLng = json.getJSONObject("latLng");
                                double latitude = latLng.getDouble("latitude");
                                double longitude = latLng.getDouble("longitude");
                                double bearing = latLng.getDouble("bearing");
                                logger.log(Level.INFO, sseUrl + " : Received location: " + latitude + ", " + longitude + ", " + bearing);
                                writer.println("geo fix " + longitude + " " + latitude);
                                synchronized (reader) {
                                    logger.info(sseUrl + " : Location set successfully : " + reader.readLine());
                                }
                            } else {
                                logger.log(Level.WARNING, sseUrl + " : Error while receiving location: " + json.getString("message"));
                            }
                        } catch (JSONException e) {
                            logger.log(Level.SEVERE, sseUrl + " : Error while parsing JSON", e);
                            throw new RuntimeException("Error while parsing JSON", e);
                        } catch (IOException e) {
                            logger.log(Level.SEVERE, sseUrl + " : Error while reading output", e);
                            throw new RuntimeException("Error while reading output", e);
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable throwable) {
                logger.log(Level.SEVERE, sseUrl + " : Error while receiving SSE event", throwable);
                throw new RuntimeException("Error while receiving SSE event", throwable);
            }

            @Override
            public void onComplete() {
                logger.log(Level.INFO, sseUrl + " : SSE connection closed");
            }
        }));
    }

    public void stop() {
        request = null;
        if (writer != null) {
            writer.println("exit");
            writer.close();
        }
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error while closing reader", e);
                throw new RuntimeException("Error while closing reader", e);
            }
        }
        if (process != null) {
            process.destroyForcibly();
        }
    }
}