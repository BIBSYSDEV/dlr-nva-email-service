package no.sikt.nva.email.reader.handler;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import io.vavr.control.Try;
import no.sikt.nva.email.reader.mapper.messagebodyreader.EmailParser;
import no.sikt.nva.email.reader.mapper.messagebodyreader.MultipartReader;
import no.sikt.nva.email.reader.mapper.messagebodyreader.ScopusEmailValidator;
import no.sikt.nva.email.reader.model.exception.EmailException;
import no.sikt.nva.email.reader.service.FileRetriever;
import no.sikt.nva.email.reader.service.ZipFileRetriever;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.apache.james.mime4j.dom.Message;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Set;
import java.util.stream.Collectors;


public class ScopusEmailReader implements RequestHandler<S3Event, Set<URI>> {


    public static final String UNABLE_TO_DOWNLOAD_FILE = "Unable to download file";
    public static final String COULD_NOT_PERSIST_FILE_IN_S_3_BUCKET = "Could not persist file in s3 bucket";
    private static final String S3_URI_TEMPLATE = "s3://%s/%s";
    private static final int SINGLE_EXPECTED_RECORD = 0;
    private final S3Client s3Client;

    private final FileRetriever fileRetriever;
    private final String scopusZipBucket;

    @JacocoGenerated
    public ScopusEmailReader() {
        this(S3Driver.defaultS3Client().build(),
                new ZipFileRetriever(HttpClient.newBuilder().build()),
                new Environment().readEnv("SCOPUS_ZIP_BUCKET"));
    }

    public ScopusEmailReader(S3Client s3Client, FileRetriever fileRetriever, String scopusZipBucket) {
        this.s3Client = s3Client;
        this.fileRetriever = fileRetriever;
        this.scopusZipBucket = scopusZipBucket;
    }

    @Override
    public Set<URI> handleRequest(S3Event event, Context context) {
        return Try.of(() -> getEmailFromS3(event))
                .mapTry(email -> extractMessage(event, email))
                .mapTry(message -> extractUrisFromMessage(event, message))
                .mapTry(uriSet -> downloadToBucketStorage(uriSet, event))
                .getOrElseThrow(throwable -> handleFailure(throwable, event));
    }

    private Set<URI> downloadToBucketStorage(Set<URI> uris, S3Event event) {
        var s3Driver = new S3Driver(s3Client, scopusZipBucket);
        return uris.stream().map(uri -> persistInBucket(uri, s3Driver, event)).collect(Collectors.toSet());
    }

    private URI persistInBucket(URI uri, S3Driver s3Driver, S3Event event) {
        var objectKey = getFileNameFromURL(uri);
        return persistFilesToS3(fileRetriever.retrieveFile(uri), objectKey, s3Driver, event);
    }

    private static UnixPath getFileNameFromURL(URI uri) {
        //The filename contains date and type of import (full abstract og delete list).
        return UnixPath.of(UriWrapper.fromUri(uri).getLastPathElement());
    }


    private URI persistFilesToS3(InputStream inputStream,
                                 UnixPath objectPath,
                                 S3Driver s3Driver,
                                 S3Event event) {
        try {
            return s3Driver.insertFile(objectPath, inputStream);
        } catch (Exception e) {
            throw new EmailException(COULD_NOT_PERSIST_FILE_IN_S_3_BUCKET,
                    extractBucketName(event),
                    extractObjectKey(event));
        }
    }

    private RuntimeException handleFailure(Throwable throwable,
                                           S3Event event) {
        return throwable instanceof EmailException emailException
                ? emailException
                : new EmailException(
                UNABLE_TO_DOWNLOAD_FILE,
                extractBucketName(event),
                extractObjectKey(event),
                throwable);
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
