package no.sikt.nva.email.reader.service;

import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ZipFileRetrieverTest {

    private ZipFileRetriever zipFileRetriever;
    private HttpClient httpClient;
    private HttpResponse okResponse;
    private HttpResponse badResponse;

    @BeforeEach
    void init() {
        httpClient = mock(HttpClient.class);
        zipFileRetriever = new ZipFileRetriever(httpClient);
        okResponse = createOkResponse();
        badResponse = createBadResponse();

    }

    @Test
    void shouldTryRetrievingFileSeveralTimesBeforeThrowingException() throws IOException, InterruptedException {
        var url = randomUri();
        mockResponseThatFailsTheFirstTimeButReturnsSuccessTheSecondTime(url);
        var inputStream = zipFileRetriever.retrieveFile(url);
        assertThat(inputStream,  not(equalTo(null)));
    }


    @SuppressWarnings("unchecked")
    private void mockResponseThatFailsTheFirstTimeButReturnsSuccessTheSecondTime(URI url) throws IOException, InterruptedException {
        when(httpClient.send(any(), any()))
                .thenReturn(badResponse)
                .thenReturn(okResponse);
    }


    @SuppressWarnings("unchecked")
    private HttpResponse<Object> createOkResponse() {
        var response = (HttpResponse<Object>) mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(response.body()).thenReturn(IoUtils.inputStreamFromResources("scopus.zip"));
        return response;
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<Object> createBadResponse() {
        var response = (HttpResponse<Object>) mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(HttpURLConnection.HTTP_BAD_METHOD);
        return response;
    }

}
