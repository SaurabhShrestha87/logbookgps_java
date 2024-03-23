package com.baeldung;

import com.baeldung.service.ProcessService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpHeaders;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


public class ProcessServiceTest {

    @Test
    public void testConnectEmulatorToSSERequest() throws IOException, InterruptedException {

        ProcessService processService = ProcessService.getInstance();
        // Act
        processService.connectEmulatorToSSERequest("ce6eb5d6-ab87-4de2-8ec1-d2609f537589");

        // Assert
        // Add your assertions here based on what you expect the method to do
    }
}