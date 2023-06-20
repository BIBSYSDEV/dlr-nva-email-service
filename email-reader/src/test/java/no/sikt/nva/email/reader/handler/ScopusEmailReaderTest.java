package no.sikt.nva.email.reader.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import no.sikt.nva.email.reader.model.exception.EmailException;
import no.sikt.nva.email.reader.util.EmailGenerator;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.stubs.WiremockHttpClient;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.apache.james.mime4j.MimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;


import static no.sikt.nva.email.reader.mapper.messagebodyreader.MultipartReader.NO_URL_PRESENT_IN_MESSAGE;
import static no.sikt.nva.email.reader.mapper.messagebodyreader.ScopusEmailValidator.COULD_NOT_PARSE_EMAIL;
import static no.sikt.nva.email.reader.mapper.messagebodyreader.ScopusEmailValidator.COULD_NOT_VERIFY_EMAIL;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class ScopusEmailReaderTest {

    public static final long SOME_FILE_SIZE = 100L;
    public static final Context CONTEXT = mock(Context.class);
    private static final String INPUT_BUCKET_NAME = "some-input-bucket-name";
    private static final S3EventNotification.UserIdentityEntity EMPTY_USER_IDENTITY = null;
    private static final S3EventNotification.RequestParametersEntity EMPTY_REQUEST_PARAMETERS = null;
    private static final S3EventNotification.ResponseElementsEntity EMPTY_RESPONSE_ELEMENTS = null;
    private S3Driver s3Driver;
    private FakeS3Client s3Client;

    private ScopusEmailReader handler;
    private String validEmail;

    public static Stream<Arguments> generateInvalidSenderEmail() throws MimeException, IOException {
        return Stream.of(Arguments.of(EmailGenerator.generateEmailWithInvalidSubject()),
                Arguments.of(EmailGenerator.generateEmailWithoutSpfHeader()),
                Arguments.of(EmailGenerator.generateEmailWithInvalidFromHeader()));
    }

    @BeforeEach
    void init() throws MimeException, IOException {
        s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, INPUT_BUCKET_NAME);
        handler = new ScopusEmailReader(s3Client, WiremockHttpClient.create());
        validEmail = EmailGenerator.generateValidEmail();
    }


    @Test
    void shouldThrowExceptionIfItCannotParseTheEmail() throws IOException {
        var s3EventNotContainingAnEmail = createS3Event(randomString());
        var exception = assertThrows(EmailException.class, () -> handler.handleRequest(s3EventNotContainingAnEmail, CONTEXT));
        assertThat(exception.getMessage(), containsString(COULD_NOT_VERIFY_EMAIL));
        assertThat(exception.getObjectKey(), is(equalTo(extractObjectKey(s3EventNotContainingAnEmail))));
    }


    @ParameterizedTest(name = "should throw exception when email sender is not from Elsevier email account")
    @MethodSource("generateInvalidSenderEmail")
    void shouldThrowExceptionWhenEmailSenderIsNotFromElsevierEmailAccount(String invalidEmails) throws IOException {
        var s3EventNotFromSikt = createS3Event(invalidEmails);
        var exception = assertThrows(EmailException.class, () -> handler.handleRequest(s3EventNotFromSikt, CONTEXT));
        assertThat(exception.getMessage(), containsString(COULD_NOT_VERIFY_EMAIL));
        assertThat(exception.getObjectKey(), is(equalTo(extractObjectKey(s3EventNotFromSikt))));
        assertThat(exception.getBucket(), is(equalTo(INPUT_BUCKET_NAME)));
    }

    @Test
    void shouldThrowExceptionIfEmailDoesNotHaveContentTypeMultipart() throws IOException, MimeException {
        var s3EventNotFromSikt = createS3Event(EmailGenerator.generateNonMultipartEmail());
        var exception = assertThrows(EmailException.class, () -> handler.handleRequest(s3EventNotFromSikt, CONTEXT));
        assertThat(exception.getMessage(), containsString(COULD_NOT_PARSE_EMAIL));
        assertThat(exception.getObjectKey(), is(equalTo(extractObjectKey(s3EventNotFromSikt))));
        assertThat(exception.getBucket(), is(equalTo(INPUT_BUCKET_NAME)));
    }


    @Test
    void shouldThrowExceptionIfEmailDoesNotContainBodyWithURI() throws IOException, MimeException {
        var s3EventNotFromSikt = createS3Event(EmailGenerator.generateEmailWithoutScopusLinks());
        var exception = assertThrows(EmailException.class, () -> handler.handleRequest(s3EventNotFromSikt, CONTEXT));
        assertThat(exception.getMessage(), containsString(NO_URL_PRESENT_IN_MESSAGE));
        assertThat(exception.getObjectKey(), is(equalTo(extractObjectKey(s3EventNotFromSikt))));
        assertThat(exception.getBucket(), is(equalTo(INPUT_BUCKET_NAME)));
    }

    @Test
    void shouldNotDownloadCitedByUri() throws IOException {
        var s3EventNotFromSikt = createS3Event(validEmail);
        var actualUrl = handler.handleRequest(s3EventNotFromSikt, CONTEXT);
        assertThat(actualUrl, not(contains(UriWrapper.fromUri("https://sccontent-scudd-delivery-prod.s3.amazonaws.com/sccontent-scudd-delivery-prod/cristin_1/2023-6-14/cristin_1_2023-6-14_ANI-CITEDBY.zip?my-query-param=should-be-preserved").getUri())));
    }

    //TODO: implement download feature.
//    @Test
//    void shouldThrowExceptionIfTheURIsInTheEmailIsNotDownloadable() throws IOException {
//        var s3EventNotFromSikt = createS3Event(validEmail);
//        var exception = assertThrows(EmailException.class, () -> handler.handleRequest(s3EventNotFromSikt, CONTEXT));
//        assertThat(exception.getMessage(), containsString(NO_URL_PRESENT_IN_MESSAGE));
//        assertThat(exception.getObjectKey(), is(equalTo(extractObjectKey(s3EventNotFromSikt))));
//        assertThat(exception.getBucket(), is(equalTo(INPUT_BUCKET_NAME)));
//
//    }

    @Test
    void shouldReturnASetOfDownloadsURIWhenTheEmailContainsTheURI() throws IOException {
        var s3EventNotFromSikt = createS3Event(validEmail);
        var actualUrl = handler.handleRequest(s3EventNotFromSikt, CONTEXT);
        var expectedUrls = urlsInValidEmailTxt();
        assertThat(actualUrl, containsInAnyOrder(expectedUrls.toArray()));
    }

    private Set<URI> urlsInValidEmailTxt() {
        return Set.of(UriWrapper.fromUri("https://sccontent-scudd-delivery-prod.s3.amazonaws.com/sccontent-scudd-delivery-prod/cristin_1/2023-6-14/cristin_1_2023-6-14_ANI-ITEM-full-format-xml.zip?my-query-param=should-be-preserved").getUri(),
                UriWrapper.fromUri("https://sccontent-scudd-delivery-prod.s3.amazonaws.com/sccontent-scudd-delivery-prod/cristin_1/2023-6-14/cristin_1_2023-6-14_ANI-ITEM-delete.zip?my-query-param=should-be-preserved").getUri());
    }


    private URI insertFileToS3(String fileContent) throws IOException {
        return s3Driver.insertFile(UnixPath.of("lpcgajc9s3sl1ne93ro0m7hoqgqo3d3fvrom2no1"), fileContent);
    }

    private S3Event createS3Event(String content) throws IOException {
        var s3ObjectKey = getObjectKey(insertFileToS3(content));
        var s3EventNotification = createS3Entity(s3ObjectKey);
        var eventNotification = new S3EventNotification.S3EventNotificationRecord(randomString(),
                randomString(),
                randomString(),
                Instant.now().toString(),
                randomString(),
                EMPTY_REQUEST_PARAMETERS,
                EMPTY_RESPONSE_ELEMENTS,
                s3EventNotification,
                EMPTY_USER_IDENTITY);
        return new S3Event(List.of(eventNotification));

    }

    private String getObjectKey(URI uri) {
        return UriWrapper.fromUri(uri).toS3bucketPath().toString();
    }

    private S3EventNotification.S3Entity createS3Entity(String expectedObjectKey) {
        var bucket = new S3EventNotification.S3BucketEntity(INPUT_BUCKET_NAME, EMPTY_USER_IDENTITY, randomString());
        var object = new S3EventNotification.S3ObjectEntity(expectedObjectKey,
                SOME_FILE_SIZE,
                randomString(),
                randomString(),
                randomString());
        var schemaVersion = randomString();
        return new S3EventNotification.S3Entity(randomString(), bucket, object, schemaVersion);
    }

    private String extractObjectKey(S3Event s3EventNotContainingAnEmail) {
        return s3EventNotContainingAnEmail.getRecords().get(0).getS3().getObject().getKey();
    }


}
