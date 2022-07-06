package no.sikt.nva.email;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailRequestHandler implements RequestHandler<Void, String> {

    private static final Logger logger = LoggerFactory.getLogger(EmailRequestHandler.class);

    @JacocoGenerated
    public EmailRequestHandler() {
    }


    @Override
    public String handleRequest(Void input, Context context) {
        logger.info("Request received");
        return "hello world";
    }
}
