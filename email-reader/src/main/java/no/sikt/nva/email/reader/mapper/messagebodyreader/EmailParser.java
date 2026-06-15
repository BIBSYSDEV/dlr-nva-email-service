package no.sikt.nva.email.reader.mapper.messagebodyreader;

import io.vavr.control.Try;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.message.DefaultMessageBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class EmailParser {



    private EmailParser() {

    }

    public static Message parseEmail(String emailString) {
        return Try.of(() -> extractMimeMessage(emailString))
                .get();
    }

    private static Message extractMimeMessage(String emailString) throws IOException {
        return new DefaultMessageBuilder().parseMessage(new ByteArrayInputStream(emailString.getBytes(StandardCharsets.UTF_8)));
    }


}
