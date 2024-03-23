package com.baeldung.service;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Flow;

public class EmulatorControl {
    public void run(String authToken, String emulatorHost, int emulatorPort, String name) throws IOException, InterruptedException {
        String sseUrl = "https://logbookgps.com:8081/emulator/sse/" + name;
        System.out.println("Starting emulator control at port : " + emulatorPort);
        // Connect to the emulator
        Socket socket = new Socket(emulatorHost, emulatorPort);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

        // Wait for the initial prompt
        String prompt = reader.readLine();
        System.out.println(prompt);

        // Send authentication command
        writer.println("auth " + authToken);
        System.out.println("Authentication command sent.");

        // Wait for the authentication response
        prompt = reader.readLine();
        System.out.println(prompt);

        // Create HttpClient and HttpRequest for the SSE endpoint
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(sseUrl)).header("Accept", "text/event-stream").build();

        // Send the request and handle the response synchronously
        client.sendAsync(request, HttpResponse.BodyHandlers.fromLineSubscriber(new Flow.Subscriber<String>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(String item) {
                System.out.println("Received SSE event: " + item); // data:{"status":"SUCCESS","latLng":{"latitude":37.74685445267021,"longitude":-122.48616612392027,"bearing":0.0}}
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
                            System.out.println("Received location update: " + latitude + ", " + longitude + " (bearing: " + bearing + ")");
                            writer.println("geo fix " + longitude + " " + latitude);
                        } else {
                            System.out.println("Received status: " + status);
                        }
                    } catch (JSONException e) {
                        System.out.println("Error while parsing JSON: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onComplete() {
                System.out.println("SSE connection closed.");
            }
        }));

        // Keep the method running until the current thread is interrupted
        while (!Thread.currentThread().isInterrupted()) {
            Thread.sleep(1000); // Delay for 1 second
        }

        // Close the socket
        socket.close();
    }
}