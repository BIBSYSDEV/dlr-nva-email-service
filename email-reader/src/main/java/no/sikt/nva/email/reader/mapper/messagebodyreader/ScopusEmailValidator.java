package no.sikt.nva.email.reader.mapper.messagebodyreader;

import no.sikt.nva.email.reader.model.exception.EmailException;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.stream.Field;

import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScopusEmailValidator {

    public static final String RECEIVED_SPF_HEADER = "Received-SPF";
    public static final String COULD_NOT_PARSE_EMAIL = "Could not parse email";
    public static final String COULD_NOT_VERIFY_EMAIL = "Could not verify email";
    public static final String VALID_FROM_LOCAL_PART = "ELSRAPTechSPFDataDefenders";
    public static final String VALID_FROM_DOMAIN = "elsevier.com";
    public static final String VALID_SUBJECT = "Scopus Data available for downloading";
    private static final Logger logger = LoggerFactory.getLogger(ScopusEmailValidator.class);
    private static final String SPF_CHECK = "^Pass \\(protection\\.outlook\\.com";
    public static final String INVALID_SPF_HEADERS_IN_EMAIL_ERROR_MESSAGE = "Invalid spf headers in email";
    public static final String WRONG_SUBJECT_RECEIVED_S_SHOULD_HAVE_BEEN_MESSAGE = "Wrong subject received: {}, "
                                                                                   + "should have been: {}";
    public static final String SIKT_OUTLOOK_DOMAIN = "sikt.no";
    private final String bucket;
    private final String objectKey;

    public ScopusEmailValidator(String bucket, String objectKey) {
        this.bucket = bucket;
        this.objectKey = objectKey;
    }

    public void validateEmail(Message message) {
        validateHeaders(message);
    }

    private boolean isNotFromElsevierNorSikt(Mailbox sender){
        return isNotFromElsevier(sender) && isNotFromSikt(sender);
        
    }

    private boolean isNotFromSikt(Mailbox sender) {
        return !SIKT_OUTLOOK_DOMAIN.equalsIgnoreCase(sender.getDomain());
    }

    private boolean isNotFromElsevier(Mailbox sender) {
        return !VALID_FROM_LOCAL_PART.equalsIgnoreCase(sender.getLocalPart())
               || !VALID_FROM_DOMAIN.equalsIgnoreCase(sender.getDomain());
    }

    private boolean wrongSender(Message email) {
        var wrongSender =
            Optional.ofNullable(email.getFrom())
                .map(mailboxes -> mailboxes.get(0))
                .map(this::isNotFromElsevierNorSikt)
                .orElse(true);
        if (wrongSender) {
            logger.error("Wrong sender in email");
        }
        return wrongSender;
    }

    private boolean hasSpfHeaderFromSikt(Field spfHeader) {
        var pattern = Pattern.compile(SPF_CHECK);
        var matcher = pattern.matcher(spfHeader.getBody());
        return matcher.find();
    }

    private boolean wrongSubject(Message email) {
        var invalidSubject = !VALID_SUBJECT.equals(email.getSubject());
        if (invalidSubject) {
            logger.error(WRONG_SUBJECT_RECEIVED_S_SHOULD_HAVE_BEEN_MESSAGE, email.getSubject(), VALID_SUBJECT);
        }
        return invalidSubject;
    }

    private void validateHeaders(Message email) {
        if (invalidReceivedSpfHeader(email) || wrongSubject(email) || wrongSender(email)) {
            throw new EmailException(COULD_NOT_VERIFY_EMAIL, bucket, objectKey);
        }
    }

    private boolean invalidReceivedSpfHeader(Message email) {
        var spfHeaders = email.getHeader().getFields(RECEIVED_SPF_HEADER);
        var invalidSpfHeaders = spfHeaders.stream().noneMatch(this::hasSpfHeaderFromSikt);
        if (invalidSpfHeaders) {
            logger.error(INVALID_SPF_HEADERS_IN_EMAIL_ERROR_MESSAGE);
        }
        return invalidSpfHeaders;
    }
}
