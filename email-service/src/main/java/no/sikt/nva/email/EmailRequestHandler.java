package no.sikt.nva.email;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import java.net.HttpURLConnection;
import no.sikt.nva.email.model.EmailRequest;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;

public class EmailRequestHandler extends ApiGatewayHandler<EmailRequest, String> {

    public static final String SUCCESS_MESSAGE = "email sent successfully";
    public static final String COULD_NOT_SEND_EMAIL_MESSAGE = "could not send email";
    public static final String BOTH_TEXT_AND_TEXT_HTML_ARE_MISSING_FROM_REQUEST_BODY_ERROR_MESSAGE =
        "Invalid request body, both text and text_html are missing";

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
    protected String processInput(EmailRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        logger.info("processing request");
        return "hello world";
    }

    @Override
    protected Integer getSuccessStatusCode(EmailRequest input, String output) {
        return HttpURLConnection.HTTP_OK;
    }
}
