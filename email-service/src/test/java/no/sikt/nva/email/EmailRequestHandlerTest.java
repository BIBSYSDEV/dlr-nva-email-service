package no.sikt.nva.email;

import no.unit.nva.stubs.FakeContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmailRequestHandlerTest {
    private FakeContext context;
    private EmailRequestHandler handler;

    @BeforeEach
    public void init() {
        this.context = new FakeContext();
        this.handler = new EmailRequestHandler();
    }

    @AfterEach
    public void tearDown() {

    }

    @Test
    void shouldReturnHandleUriWhenInputIsValidUri() {
        handler.handleRequest(null, context);
    }

}