package no.sikt.nva.email;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;
import java.net.HttpURLConnection;
import java.util.Objects;
import no.sikt.nva.email.model.EmailRequest;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailRequestHandler extends ApiGatewayHandler<EmailRequest, String> {

    public static final String SUCCESS_MESSAGE = "email sent successfully";
    public static final String COULD_NOT_SEND_EMAIL_MESSAGE = "could not send email";
    public static final String BOTH_TEXT_AND_TEXT_HTML_ARE_MISSING_FROM_REQUEST_BODY_ERROR_MESSAGE =
        "Invalid request body, both text and text_html are missing";
    public static final String EMAIL_LOG_INFO_TRACK_ID = "email sent with track id %s";
    public static final String UTF_8 = "UTF-8";
    private static final Logger logger = LoggerFactory.getLogger(EmailRequestHandler.class);
    private final AmazonSimpleEmailService amazonSimpleEmailService;

    public EmailRequestHandler(AmazonSimpleEmailService amazonSimpleEmailService) {
        super(EmailRequest.class);
        this.amazonSimpleEmailService = amazonSimpleEmailService;
    }

    @JacocoGenerated
    public EmailRequestHandler() {
        this(AmazonSimpleEmailServiceClientBuilder.standard()
                 .withRegion(Regions.EU_WEST_1).build());
    }

    @Override
    protected String processInput(EmailRequest emailRequest, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        validateRequestBody(emailRequest);
        var sendEmailResult = sendEmail(emailRequest);
        logger.info(String.format(EMAIL_LOG_INFO_TRACK_ID, sendEmailResult.getMessageId()));
        return SUCCESS_MESSAGE;
    }

    @Override
    protected Integer getSuccessStatusCode(EmailRequest input, String output) {
        return HttpURLConnection.HTTP_OK;
    }

    private SendEmailResult sendEmail(EmailRequest emailRequest) throws EmailException {
        return attempt(() -> createSesEmailRequest(emailRequest))
                   .map(amazonSimpleEmailService::sendEmail)
                   .orElseThrow(this::logFailureAndThrowEmailException);
    }

    @SuppressWarnings("PMD.InvalidLogMessageFormat")
    private EmailException logFailureAndThrowEmailException(Failure<SendEmailResult> failure) {
        logger.warn(COULD_NOT_SEND_EMAIL_MESSAGE, failure.getException());
        return new EmailException(COULD_NOT_SEND_EMAIL_MESSAGE, HttpURLConnection.HTTP_INTERNAL_ERROR);
    }

    private SendEmailRequest createSesEmailRequest(EmailRequest emailRequest) {
        return new SendEmailRequest()
                   .withDestination(getDestination(emailRequest))
                   .withMessage(getMessage(emailRequest))
                   .withSource(emailRequest.getFromAddress());
    }

    private Message getMessage(EmailRequest emailRequest) {
        return new Message()
                   .withBody(new Body()
                                 .withHtml(createContent(emailRequest.getTextHtml()))
                                 .withText(createContent(emailRequest.getText())))
                   .withSubject(createContent(emailRequest.getSubject()));
    }

    private Content createContent(String data) {
        return new Content()
                   .withCharset(UTF_8)
                   .withData(
                       data);
    }

    private Destination getDestination(EmailRequest emailRequest) {
        return new Destination()
                   .withToAddresses(emailRequest.getToAddress())
                   .withCcAddresses(emailRequest.getCc())
                   .withBccAddresses(emailRequest.getBcc());
    }

    @SuppressWarnings("PMD.InvalidLogMessageFormat")
    private void validateRequestBody(EmailRequest emailRequest) throws EmailException {
        if (Objects.isNull(emailRequest.getText())
            && Objects.isNull(emailRequest.getTextHtml())) {
            logger.warn(BOTH_TEXT_AND_TEXT_HTML_ARE_MISSING_FROM_REQUEST_BODY_ERROR_MESSAGE);
            throw new EmailException(BOTH_TEXT_AND_TEXT_HTML_ARE_MISSING_FROM_REQUEST_BODY_ERROR_MESSAGE,
                                     HttpURLConnection.HTTP_BAD_REQUEST);
        }
    }
}
