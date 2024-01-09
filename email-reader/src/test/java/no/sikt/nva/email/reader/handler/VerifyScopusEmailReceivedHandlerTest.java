package no.sikt.nva.email.reader.handler;

import static no.sikt.nva.email.reader.handler.VerifyScopusEmailReceivedHandler.ALARM_MESSAGE;
import static no.sikt.nva.email.reader.handler.VerifyScopusEmailReceivedHandler.ALARM_SUBJECT;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import no.sikt.nva.email.reader.util.EmailGenerator;
import no.unit.nva.stubs.FakeContext;
import nva.commons.core.ioutils.IoUtils;
import org.apache.james.mime4j.MimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

public class VerifyScopusEmailReceivedHandlerTest {

    private static final String OBJECT_KEY = "someObjectKey";
    private static final String SNS_TOPIC_ARN = "someSnsTopicArn";
    private static final PublishRequest ALARM_PUBLISHED = PublishRequest.builder()
                                                              .topicArn(SNS_TOPIC_ARN)
                                                              .message(ALARM_MESSAGE)
                                                              .subject(ALARM_SUBJECT)
                                                              .build();
    private final String BUCKET_NAME = "someBucketName";

    private final FakeContext context = new FakeContext() {
        @Override
        public String getInvokedFunctionArn() {
            return randomString();
        }
    };
    private final ScheduledEvent SCHEDULED_EVENT = new ScheduledEvent();
    private S3Client s3Client;
    private VerifyScopusEmailReceivedHandler handler;
    private SnsAsyncClient snsAsyncClient;

    @BeforeEach
    void init() {
        //Because lastModifiedFlag in listObject response is needed, the FaceS3Client is not usable
        s3Client = mock(S3Client.class);
        snsAsyncClient = mock(SnsAsyncClient.class);
        handler = new VerifyScopusEmailReceivedHandler(s3Client,
                                                       snsAsyncClient,
                                                       BUCKET_NAME,
                                                       SNS_TOPIC_ARN);
    }

    @Test
    void shouldEmitEventWhenThereIsNoObjectYoungerThan24hours() {
        stubObjectKeyListResponse(createOldS3Object());
        var alarm = handler.handleRequest(SCHEDULED_EVENT, context);
        assertThat(alarm, samePropertyValuesAs((ALARM_PUBLISHED)));
        verify(snsAsyncClient, times(1)).publish(any(PublishRequest.class));
    }

    @Test
    void shouldEmitEventWhenThereIsNoScopusEmailObjectInBucket() {
        stubObjectKeyListResponse(freshObject());
        stubs3Content(randomString());
        var alarm = handler.handleRequest(SCHEDULED_EVENT, context);
        assertThat(alarm, samePropertyValuesAs(ALARM_PUBLISHED));
        verify(snsAsyncClient, times(1)).publish(any(PublishRequest.class));

    }

    @Test
    void shouldReturnNothingWhenThereIsAScopusEmailInBucketThatIsYoungerThan24Hours()
        throws MimeException, IOException {
        stubObjectKeyListResponse(freshObject());
        stubs3Content(EmailGenerator.generateValidEmail());
        var alarm = handler.handleRequest(SCHEDULED_EVENT, context);
        assertThat(alarm, is(nullValue()));
    }

    @SuppressWarnings("unchecked")
    private void stubs3Content(String content) {

        when(s3Client.getObject(any(GetObjectRequest.class),
                                any(ResponseTransformer.class)))
            .thenAnswer(invocationOnMock -> returnObject(invocationOnMock, content));
    }

    @SuppressWarnings("unchecked")
    private ResponseBytes<GetObjectResponse> returnObject(InvocationOnMock invocationOnMock,
                                                          String content) throws Exception {
        var contentsAsByteArray = content.getBytes(StandardCharsets.UTF_8);
        var response = GetObjectResponse
                           .builder()
                           .contentLength((long) contentsAsByteArray.length)
                           .build();
        var transformer =
            (ResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>>)
                invocationOnMock.getArgument(1);
        return transformer.transform(response,
                                     AbortableInputStream.create(IoUtils.stringToStream(content)));
    }

    private void stubObjectKeyListResponse(List<S3Object> s3Objects) {
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
            .thenReturn(
                ListObjectsV2Response
                    .builder()
                    .contents(s3Objects)
                    .build()
            );
    }

    private List<S3Object> createOldS3Object() {
        return List.of(S3Object.builder()
                           .key(OBJECT_KEY)
                           .lastModified(Instant.now().minus(25, ChronoUnit.HOURS))
                           .build());
    }

    private List<S3Object> freshObject() {
        return List.of(S3Object.builder()
                           .key(OBJECT_KEY)
                           .lastModified(Instant.now())
                           .build());
    }
}
