package no.sikt.nva.email;

import nva.commons.apigateway.exceptions.ApiGatewayException;

public class EmailException extends ApiGatewayException {

    private final Integer responseStatusCode;

    public EmailException(String message, Integer responseStatusCode) {
        super(message);
        this.responseStatusCode = responseStatusCode;
    }

    @Override
    protected Integer statusCode() {
        return responseStatusCode;
    }
}
