package com.baeldung.service;

import com.baeldung.model.Emulator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ProcessService {

    private static ProcessService instance;

    private ProcessService() {
    }

    public static ProcessService getInstance() {
        if (instance == null) {
            instance = new ProcessService();
        }
        return instance;
    }

    public Boolean startEmulator(Emulator emulator) {
        try {
            ProcessBuilder builder = new ProcessBuilder("emulator", "-avd", emulator.getName());
            Process process = builder.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            System.out.println("Error while starting emulator" + e.getMessage());
            return false;
        }
    }

    public Boolean stopEmulator(Emulator emulator) {
        try {
            ProcessBuilder builder = new ProcessBuilder("adb", "-s", emulator.getName(), "emu", "kill");
            Process process = builder.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            System.out.println("Error while stopping emulator" + e.getMessage());
            return false;
        }
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

            // Create Emulator objects for all devices
            for (String device : allDevices) {
                boolean isRunning = runningDevices.contains(device);
                emulators.add(new Emulator(0, device, isRunning));
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
        return Arrays.stream(output.split("\n")).filter(line -> !line.isEmpty() && line.length() <= 10).collect(Collectors.toList());
    }

}