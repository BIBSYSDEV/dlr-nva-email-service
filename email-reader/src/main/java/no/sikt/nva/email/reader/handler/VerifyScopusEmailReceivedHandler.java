package no.sikt.nva.email.reader.handler;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import no.sikt.nva.email.reader.mapper.messagebodyreader.EmailParser;
import no.sikt.nva.email.reader.mapper.messagebodyreader.ScopusEmailValidator;
import no.sikt.nva.email.reader.model.exception.NoScopusEmailsReceivedException;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.attempt.Failure;
import nva.commons.core.paths.UnixPath;
import org.apache.james.mime4j.dom.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;

public class VerifyScopusEmailReceivedHandler
    implements RequestHandler<ScheduledEvent, Void> {

    private static final Logger logger = LoggerFactory.getLogger(VerifyScopusEmailReceivedHandler.class);

    private static final String NON_SCOPUS_EMAIL_FOUND = "NON SCOPUS EMAIL FOUND {}";

    //we expect to receive one email once a week; and we have object expiration set to 20 days.
    private static final Integer GENEROUS_LIMIT_OF_EXPECTED_KEYS_IN_BUCKET = 10;
    private final S3Client s3Client;
    private final String bucketName;

    @JacocoGenerated
    public VerifyScopusEmailReceivedHandler() {
        this(S3Driver.defaultS3Client().build(),
             new Environment().readEnv("SCOPUS_EMAIL_BUCKET_NAME"));
    }

    public VerifyScopusEmailReceivedHandler(S3Client s3Client,
                                            String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @Override
    public Void handleRequest(ScheduledEvent scheduledEvent, Context context) {
        if (didNotReceiveScopusEmail()) {
            emitAlarm();
        }
        return null;
    }

    private static boolean isWithinLast24Hours(S3Object s3Object) {
        var twentyFourHoursAgo = Instant.now().minus(24, ChronoUnit.HOURS);
        return s3Object.lastModified().isAfter(twentyFourHoursAgo);
    }

    private void emitAlarm() {
        throw new NoScopusEmailsReceivedException();
    }

    private boolean didNotReceiveScopusEmail() {
        return getObjectsYoungerThan24Hours()
                   .stream()
                   .noneMatch(this::validateScopusEmail);
    }

    private boolean validateScopusEmail(S3Object s3Object) {
        return attempt(() -> getFileFromS3(s3Object))
                   .map(EmailParser::parseEmail)
                   .map(message -> validateEmail(message, s3Object.key()))
                   .orElse(this::logErrorAndReturnFalse);
    }

    private String getFileFromS3(S3Object s3Object) {
        var s3Driver = new S3Driver(s3Client, bucketName);
        return s3Driver.getFile(UnixPath.of(s3Object.key()));
    }

    private Boolean logErrorAndReturnFalse(Failure<Boolean> fail) {
        logger.error(NON_SCOPUS_EMAIL_FOUND, fail.getException());
        return false;
    }

    private boolean validateEmail(Message email, String objectKey) {
        var scopusEmailValidator = new ScopusEmailValidator(bucketName, objectKey);
        scopusEmailValidator.validateEmail(email);
        return true;
    }

    private List<S3Object> getObjectsYoungerThan24Hours() {
        var response = s3Client.listObjectsV2(createListObjectRequest());
        return response.contents()
                   .stream()
                   .filter(VerifyScopusEmailReceivedHandler::isWithinLast24Hours)
                   .toList();
    }

    private ListObjectsV2Request createListObjectRequest() {
        return ListObjectsV2Request
                   .builder()
                   .prefix(StringUtils.EMPTY_STRING)
                   .maxKeys(GENEROUS_LIMIT_OF_EXPECTED_KEYS_IN_BUCKET)
                   .bucket(bucketName)
                   .build();
    }
}
