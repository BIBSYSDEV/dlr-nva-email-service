package no.sikt.nva.email;

import static no.sikt.nva.email.EmailRequestHandler.COULD_NOT_SEND_EMAIL_MESSAGE;
import static no.sikt.nva.email.EmailRequestHandler.DEFAULT_FROM_ADDRESS_ENVIRONMENT_VARIABLE_NAME;
import static no.sikt.nva.email.EmailRequestHandler.EMAIL_LOG_INFO_TRACK_ID;
import static no.sikt.nva.email.EmailRequestHandler.SUCCESS_MESSAGE;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.AccountSendingPausedException;
import com.amazonaws.services.simpleemail.model.ConfigurationSetDoesNotExistException;
import com.amazonaws.services.simpleemail.model.ConfigurationSetSendingPausedException;
import com.amazonaws.services.simpleemail.model.MailFromDomainNotVerifiedException;
import com.amazonaws.services.simpleemail.model.MessageRejectedException;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;
import java.net.HttpURLConnection;
import java.util.stream.Stream;
import no.sikt.nva.email.model.EmailRequest;
import no.unit.nva.stubs.FakeContext;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

class EmailRequestHandlerTest {

    private FakeContext context;
    private EmailRequestHandler handler;
    private AmazonSimpleEmailService amazonSimpleEmailService;
    private TestAppender appender;
    private EmailRequest emailRequest;
    private Environment environment;
    private String defaultAddress;

    @BeforeEach
    public void init() {
        this.context = new FakeContext();
        this.environment = new Environment();
        this.defaultAddress = environment.readEnv(DEFAULT_FROM_ADDRESS_ENVIRONMENT_VARIABLE_NAME);
        this.amazonSimpleEmailService = Mockito.mock(AmazonSimpleEmailService.class);
        this.appender = LogUtils.getTestingAppenderForRootLogger();
        this.handler = new EmailRequestHandler(amazonSimpleEmailService, environment);
        this.emailRequest = new EmailRequest("test@test.no",
                                             "test1@test.no",
                                             "test2@test.no",
                                             "test3.test.no",
                                             randomString(),
                                             randomString(),
                                             randomString());
    }

    //Request body validation happens at ApiGateway according to specifications in ./docs/openapi.yaml,
    // so no need for programmatic validation of input fields.
    @Test
    public void sendsEmailSuccessfullyWhenAmazonSimpleEmailServiceIsNotThrowingException() throws ApiGatewayException {
        var trackId = randomString();
        var sendEmailResult = new SendEmailResult();
        sendEmailResult.setMessageId(trackId);
        Mockito.when(amazonSimpleEmailService.sendEmail(any(SendEmailRequest.class))).thenReturn(sendEmailResult);
        var response = handler.processInput(emailRequest, new RequestInfo(), context);
        assertThat(response, is(equalTo(SUCCESS_MESSAGE)));
        Mockito.verify(amazonSimpleEmailService, times(1)).sendEmail(any(SendEmailRequest.class));
        assertThat(handler.getSuccessStatusCode(emailRequest, response), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(appender.getMessages(), containsString(String.format(EMAIL_LOG_INFO_TRACK_ID, trackId)));
    }

    @Test
    public void usesDefaultFromAddressWhenFromAddressIsNotSpecified() throws ApiGatewayException {
        emailRequest.setFromAddress(null);
        var trackId = randomString();
        var sendEmailResult = new SendEmailResult();
        sendEmailResult.setMessageId(trackId);
        Mockito.when(amazonSimpleEmailService.sendEmail(any(SendEmailRequest.class))).thenReturn(sendEmailResult);
        var response = handler.processInput(emailRequest, new RequestInfo(), context);
        assertThat(response, is(equalTo(SUCCESS_MESSAGE)));
        Mockito.verify(amazonSimpleEmailService, times(1))
            .sendEmail(argThat(new SendEmailRequestMatcher(new SendEmailRequest().withSource(defaultAddress))));
        assertThat(handler.getSuccessStatusCode(emailRequest, response), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(appender.getMessages(), containsString(String.format(EMAIL_LOG_INFO_TRACK_ID, trackId)));
    }

    @ParameterizedTest
    @MethodSource("providedAmazonSesExceptions")
    public void sendsErrorBackWhenEmailRequestFails(Exception exception) {
        Mockito.when(amazonSimpleEmailService.sendEmail(any(SendEmailRequest.class))).thenThrow(exception);
        var apiGatewayException = assertThrows(ApiGatewayException.class, () -> handler.processInput(emailRequest,
                                                                                                     new RequestInfo(),
                                                                                                     context));
        assertThat(apiGatewayException.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_INTERNAL_ERROR)));
        assertThat(apiGatewayException.getMessage(), containsString(COULD_NOT_SEND_EMAIL_MESSAGE));
        assertThat(appender.getMessages(), containsString(exception.getMessage()));
    }

    private static Stream<Arguments> providedAmazonSesExceptions() {
        return Stream.of(Arguments.of(
                             new MessageRejectedException(randomString())),
                         Arguments.of(
                             new MailFromDomainNotVerifiedException(randomString())),
                         Arguments.of(new ConfigurationSetDoesNotExistException(randomString())),
                         Arguments.of(new ConfigurationSetSendingPausedException(randomString())),
                         Arguments.of(new AccountSendingPausedException(randomString())));
    }

    class SendEmailRequestMatcher implements ArgumentMatcher<SendEmailRequest> {

        private final transient SendEmailRequest left;
        transient String leftFromAddress = "";
        transient String rightFromAddress = "";

        public SendEmailRequestMatcher(SendEmailRequest left) {
            this.left = left;
        }

        @Override
        public boolean matches(SendEmailRequest right) {
            leftFromAddress = left.getSource();
            rightFromAddress = right.getSource();
            return leftFromAddress.equals(rightFromAddress);
        }

        public String toString() {
            return "Request body matcher. Expected from address: "
                   + leftFromAddress
                   + ", received from_address:"
                   + rightFromAddress;
        }
    }
}