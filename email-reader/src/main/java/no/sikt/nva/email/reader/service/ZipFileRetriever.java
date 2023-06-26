package no.sikt.nva.email.reader.service;


import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.vavr.control.Try;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Supplier;

public class ZipFileRetriever implements FileRetriever {

    private final HttpClient httpClient;

    public ZipFileRetriever(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public InputStream retrieveFile(URI uri) {
        var retryRegistry = RetryRegistry.ofDefaults();
        var retryWithDefaultConfig = retryRegistry.retry("sendRequest");
        Supplier<InputStream> supplier = () -> sendRequest(uri);
        return Try.ofSupplier(Retry.decorateSupplier(retryWithDefaultConfig, supplier)).get();
    }

    private InputStream sendRequest(URI uri) {
        return Try.of(() -> httpClient.send(createRequest(uri), HttpResponse.BodyHandlers.ofInputStream()))
                .mapTry(this::getBodyFromResponse).get();
    }

    private InputStream getBodyFromResponse(HttpResponse<InputStream> response) {
        if (response.statusCode() != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Request failed with status code: " + response.statusCode());
        }
        return response.body();
    }


    private HttpRequest createRequest(URI uri) {
        return HttpRequest.newBuilder()
                .GET()
                .setHeader("Accept", "application/zip")
                .uri(uri)
                .build();
    }
}
