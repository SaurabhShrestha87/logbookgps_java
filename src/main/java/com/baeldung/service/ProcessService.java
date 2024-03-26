package com.baeldung.service;

import com.baeldung.model.Emulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ProcessService {

    private static final ReentrantLock lock = new ReentrantLock();
    private static volatile ProcessService instance;
    Logger logger = Logger.getLogger(ProcessService.class.getName());
    Map<String, ExecutorService> emulatorThreadPool = new ConcurrentHashMap<>();
    Map<String, Process> runningEmulators = new ConcurrentHashMap<>();
    Map<String, EmulatorControl> emulatorControlMap = new ConcurrentHashMap<>();

    private ProcessService() {

    }

    public static ProcessService getInstance() {
        if (instance == null) {
            lock.lock();
            try {
                if (instance == null) instance = new ProcessService();
            } finally {
                lock.unlock();
            }
        }
        return instance;
    }

    private void connectEmulatorSseAndUpdate(Emulator emulator) throws IOException, InterruptedException {
        EmulatorControl emulatorControl = new EmulatorControl();
        emulatorControl.run(emulator.getId(), emulator.getName());
        emulatorControlMap.put(emulator.getName(), emulatorControl);
    }


    public Boolean startEmulator(Emulator emulator) {
        if (runningEmulators.containsKey(emulator.getName())) {
            return true;
        }
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        emulatorThreadPool.put(emulator.getName(), executorService);
        Runnable task = () -> {
            try {
                Process finalProcess;
                ProcessBuilder builder = new ProcessBuilder("emulator", "-port", String.valueOf(emulator.getId()), "-avd", emulator.getName());
                finalProcess = builder.start();
                logger.log(Level.INFO, "Starting emulator: " + emulator.getName());
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(finalProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("Successfully loaded snapshot") || line.contains("Boot completed")) {
                            logger.log(Level.INFO, emulator.getName() + "Emulator started successfully");
                            runningEmulators.put(emulator.getName(), finalProcess);
                            break;
                        }
                    }
                    connectEmulatorSseAndUpdate(emulator);
                } catch (IOException | InterruptedException e) {
                    logger.log(Level.SEVERE, "Error while reading emulator output", e);
                    throw new RuntimeException("Error while reading emulator output", e);
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error while starting emulator", e);
                throw new RuntimeException("Error while starting emulator", e);
            }
        };
        executorService.submit(task);
        return true;
    }

    public Boolean stopEmulator(Emulator emulator) {
        logger.log(Level.INFO, "Stopping emulator: " + emulator.getName());
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
            if (emulatorControlMap.containsKey(emulator.getName())) {
                emulatorControlMap.get(emulator.getName()).stop();
                emulatorControlMap.remove(emulator.getName());
            }
        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Error while stopping emulator", e);
            throw new RuntimeException("Error while stopping emulator", e);
        }
        return true;
    }

    public List<Emulator> getEmulatorsList() {
        List<Emulator> emulators = new ArrayList<>();
        try {
            ProcessBuilder builder = new ProcessBuilder("emulator", "-list-avds");
            Process process = builder.start();
            List<String> allDevices = readOutput(process.getInputStream());

            builder = new ProcessBuilder("adb", "devices");
            process = builder.start();
            List<String> runningDevices = readOutput(process.getInputStream());

            int port = 5554;
            for (String device : allDevices) {
                boolean isRunning = runningDevices.contains(device);
                emulators.add(new Emulator(port, device, isRunning));
                port = port + 2;
                logger.log(Level.INFO, "Emulator: " + device + " is running: " + isRunning);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while getting emulators list", e);
            throw new RuntimeException("Error while getting emulators list", e);
        }
        return emulators;
    }


    private List<String> readOutput(InputStream inputStream) throws IOException {
        // read the output from the input stream and save as a list of strings
        String output = new String(inputStream.readAllBytes());
        // split the output by new line and filter out lines that are empty or more than 10 characters
        return Arrays.stream(output.split("\n")).filter(line -> !line.isEmpty() && line.length() <= 40).collect(Collectors.toList());
    }
}
