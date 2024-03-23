package com.baeldung.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class EmulatorControl {

    public void run(String authToken, String emulatorHost, int emulatorPort, String name) throws IOException, InterruptedException {
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
        // Send geo fix commands in a loop till current thread is interrupted
        do {
            // Replace <longitude> and <latitude> with desired coordinates
            double longitude = -122.084 + (Math.random() * 0.001);
            double latitude = 37.421998 + (Math.random() * 0.001);
            writer.println("geo fix " + longitude + " " + latitude);
            System.out.println("geo fix command sent: " + longitude + ", " + latitude);
            Thread.sleep(1000); // Delay for 1 second
        } while (!Thread.currentThread().isInterrupted());
        // Close the socket
        socket.close();
    }
}
