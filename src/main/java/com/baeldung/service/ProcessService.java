package com.baeldung.service;

import com.baeldung.model.Emulator;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ProcessService {

    private static ProcessService instance;
    // Thread pool map to store the running emulators ** 20 threads for up-to 20 emulators **
    Map<String, ExecutorService> emulatorThreadPool = new HashMap<>();
    Map<String, Process> runningEmulators = new HashMap<>();

    private ProcessService() {

    }

    public static ProcessService getInstance() {
        if (instance == null) {
            instance = new ProcessService();
        }
        return instance;
    }

    private synchronized void connectEmulatorSseAndUpdate(Emulator emulator) throws IOException, InterruptedException {
        EmulatorControl emulatorControl = new EmulatorControl();
        emulatorControl.run("localhost", emulator.getId(), emulator.getName());
    }

    public Boolean startEmulator(Emulator emulator) {
        if (runningEmulators.containsKey(emulator.getName())) {
            return true;
        }
        Process process;
        try {
            // emulator -port 5554 -avd Pixel_2_API_29
            ProcessBuilder builder = new ProcessBuilder("emulator", "-port", String.valueOf(emulator.getId()), "-avd", emulator.getName());
            process = builder.start();
            final Process finalProcess = process;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(finalProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    if (line.contains("Successfully loaded snapshot") || line.contains("Boot completed")) {
                        System.out.println("Emulator started");
                        runningEmulators.put(emulator.getName(), finalProcess);
                        break;

                    }
                }
                // execute the emulator control in a new thread and add to the thread pool map
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                emulatorThreadPool.put(emulator.getName(), executorService);
                executorService.execute(() -> {
                    try {
                        connectEmulatorSseAndUpdate(emulator);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                        throw new RuntimeException("Error while reading emulator output", e);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Error while reading emulator output", e);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error while starting emulator", e);
        }
    }

    public Boolean stopEmulator(Emulator emulator) {
        try {
            if (runningEmulators.containsKey(emulator.getName())) {
                Process process = runningEmulators.get(emulator.getName());
                process.destroyForcibly();
                runningEmulators.remove(emulator.getName());
            }
            if (emulatorThreadPool.containsKey(emulator.getName())) {
                emulatorThreadPool.get(emulator.getName()).shutdownNow();
                emulatorThreadPool.remove(emulator.getName());
            }
        } catch (Exception e) {
            System.out.println("Error while stopping emulator" + e.getMessage());
            return false;
        }
        return true;
    }

    public List<Emulator> getEmulatorsList() {
        List<Emulator> emulators = new ArrayList<>();
        try {
            // Get all available devices
            ProcessBuilder builder = new ProcessBuilder("emulator", "-list-avds");
            Process process = builder.start();
            List<String> allDevices = readOutput(process.getInputStream());

            // Get currently running devices
            builder = new ProcessBuilder("adb", "devices");
            process = builder.start();
            List<String> runningDevices = readOutput(process.getInputStream());

            int port = 5553; /// 5554 - 5682
            // Create Emulator objects for all devices
            for (String device : allDevices) {
                port++;
                boolean isRunning = runningDevices.contains(device);
                emulators.add(new Emulator(port, device, isRunning));
                System.out.println("Device: " + device + " is running: " + isRunning);
            }
        } catch (IOException e) {
            System.out.println("Error while executing command" + e.getMessage());
        }
        return emulators;
    }

    private List<String> readOutput(InputStream inputStream) throws IOException {
        // read the output from the input stream and save as a list of strings
        String output = new String(inputStream.readAllBytes());
        // split the output by new line and filter out lines that are empty or more than 10 characters
        return Arrays.stream(output.split("\n")).filter(line -> !line.isEmpty() && line.length() <= 40).collect(Collectors.toList());
    }

    public void connectEmulatorToSSERequest(String emulatorName) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/emulator/sse/" + emulatorName)).build();
        System.out.println("Connecting to SSE");
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(HttpResponse::body).thenAccept(data -> {
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(data);
                double lat = jsonObject.getDouble("lat");
                double lon = jsonObject.getDouble("lon");
                System.out.println("Lat: " + lat + " Lon: " + lon);
            } catch (JSONException e) {
                System.out.println("Error while parsing JSON" + e.getMessage());
                throw new RuntimeException(e);
            }
        });
        // wait for the response
        Thread.sleep(1000);
    }

}