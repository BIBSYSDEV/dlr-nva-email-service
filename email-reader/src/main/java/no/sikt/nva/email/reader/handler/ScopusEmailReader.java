package no.sikt.nva.email.reader.handler;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import no.sikt.nva.email.reader.mapper.messagebodyreader.EmailParser;
import no.sikt.nva.email.reader.mapper.messagebodyreader.MultipartReader;
import no.sikt.nva.email.reader.mapper.messagebodyreader.ScopusEmailValidator;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.paths.UriWrapper;
import org.apache.james.mime4j.dom.Message;
import software.amazon.awssdk.services.s3.S3Client;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.Set;


public class ScopusEmailReader implements RequestHandler<S3Event, Set<URI>> {


    private static final String S3_URI_TEMPLATE = "s3://%s/%s";
    private static final int SINGLE_EXPECTED_RECORD = 0;
    private final S3Client s3Client;

    @SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
    private final HttpClient httpClient; //TODO: implement downloading

    @JacocoGenerated
    public ScopusEmailReader() {
        this(S3Driver.defaultS3Client().build(),
                HttpClient.newBuilder().build());
    }

    public ScopusEmailReader(S3Client s3Client, HttpClient httpClient) {
        this.s3Client = s3Client;
        this.httpClient = httpClient;
    }

    @Override
    public Set<URI> handleRequest(S3Event event, Context context) {
        var emailString = getEmailFromS3(event);
        var message = extractMessage(event, emailString);
        return extractUrisFromMessage(event, message);
    }

    private Set<URI> extractUrisFromMessage(S3Event event, Message message) {
        var messageReader = new MultipartReader(message, extractBucketName(event), extractObjectKey(event));
        return messageReader.extractScopusURL();
    }

    private Message extractMessage(S3Event event, String emailString) {
        var message = EmailParser.parseEmail(emailString);
        validateMessage(message, event);
        return message;
    }

    private void validateMessage(Message message, S3Event event) {
        var mimeValidator = new ScopusEmailValidator(
                extractBucketName(event),
                extractObjectKey(event));
        mimeValidator.validateEmail(message);
    }

    private String getEmailFromS3(S3Event event) {
        var s3Driver = new S3Driver(s3Client, extractBucketName(event));
        var fileUri = createS3BucketUri(event);
        return s3Driver.getFile(UriWrapper.fromUri(fileUri).toS3bucketPath());
    }

    private String extractBucketName(S3Event event) {
        return event.getRecords().get(SINGLE_EXPECTED_RECORD).getS3().getBucket().getName();
    }

    private URI createS3BucketUri(S3Event s3Event) {
        return URI.create(String.format(S3_URI_TEMPLATE, extractBucketName(s3Event), extractObjectKey(s3Event)));
    }

    private String extractObjectKey(S3Event event) {
        return event.getRecords().get(SINGLE_EXPECTED_RECORD).getS3().getObject().getKey();
    }
}
