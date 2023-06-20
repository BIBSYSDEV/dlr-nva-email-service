package no.sikt.nva.email.reader.mapper.messagebodyreader;

import no.sikt.nva.email.reader.model.exception.EmailException;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.stream.Field;

import java.util.Optional;
import java.util.regex.Pattern;


public class ScopusEmailValidator {
    public static final String RECEIVED_SPF_HEADER = "Received-SPF";
    public static final String COULD_NOT_PARSE_EMAIL = "Could not parse email";

    public static final String COULD_NOT_VERIFY_EMAIL = "Could not verify email";
    public static final String VALID_FROM_LOCAL_PART = "ELSRAPTechSPFDataDefenders";
    public static final String VALID_FROM_DOMAIN = "elsevier.com";
    public static final String VALID_SUBJECT = "Scopus Data available for downloading";
    private static final String SPF_CHECK = "^pass \\(spfCheck: domain of sikt\\.no";
    private final String bucket;
    private final String objectKey;

    public ScopusEmailValidator(String bucket, String objectKey) {
        this.bucket = bucket;
        this.objectKey = objectKey;
    }

    public void validateEmail(Message message) {
        validateHeaders(message);
    }

    private boolean isNotFromElsevier(Mailbox sender) {
        return !VALID_FROM_LOCAL_PART.equalsIgnoreCase(sender.getLocalPart())
                || !VALID_FROM_DOMAIN.equalsIgnoreCase(sender.getDomain());
    }

    private boolean wrongSender(Message email) {
        return Optional.ofNullable(email.getFrom())
                .map(mailboxes -> mailboxes.get(0))
                .map(this::isNotFromElsevier)
                .orElse(true);
    }

    private boolean hasExpectedValue(Field spfHeader) {
        var pattern = Pattern.compile(SPF_CHECK);
        var matcher = pattern.matcher(spfHeader.getBody());
        return matcher.find();

    }

    private boolean wrongSubject(Message email) {
        return !VALID_SUBJECT.equals(email.getSubject());
    }


    private void validateHeaders(Message email) {
        if (invalidReceivedSpfHeader(email) || wrongSubject(email) || wrongSender(email)) {
            throw new EmailException(COULD_NOT_VERIFY_EMAIL, bucket, objectKey);
        }
    }

    private boolean invalidReceivedSpfHeader(Message email) {
        var spfHeaders = email.getHeader().getFields(RECEIVED_SPF_HEADER);
        return spfHeaders.stream().noneMatch(this::hasExpectedValue);
    }

}
