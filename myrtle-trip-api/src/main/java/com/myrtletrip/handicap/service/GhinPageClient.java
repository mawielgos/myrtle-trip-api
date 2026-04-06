package com.myrtletrip.handicap.service;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Component
public class GhinPageClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String fetchPeerPage(String ghinNumber) throws IOException, InterruptedException {
        String url = "https://www.cdga.org/golf-handicaps/peer.asp?cmd=srch&ghin="
                + URLEncoder.encode(ghinNumber, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("User-Agent", "Mozilla/5.0")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("GHIN page request failed with status " + response.statusCode());
        }

        return response.body();
    }
}