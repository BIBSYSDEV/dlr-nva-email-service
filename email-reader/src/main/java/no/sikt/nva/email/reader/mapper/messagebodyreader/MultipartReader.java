package no.sikt.nva.email.reader.mapper.messagebodyreader;

import io.vavr.control.Try;
import no.sikt.nva.email.reader.model.exception.EmailException;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.SingleBody;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.MultipartImpl;

import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;


public class MultipartReader {

    public static final String COULD_NOT_PARSE_EMAIL = "Could not parse email";
    public static final String NO_URL_PRESENT_IN_MESSAGE = "No URLs present in message";
    private static final String NOT_CITED_BY_REGEX = "^((?!ANI-CITEDBY).)*$";
    private static final String REGEX_PATTERN_VALID_URL =
            "https://sccontent-scudd-delivery-prod\\.s3\\.amazonaws\\.com"
                    + "/sccontent-scudd-delivery-prod/[\\w.\\-/:#?=&;%~+]+";
    private final Message message;
    private final String bucket;
    private final String objectKey;


    public MultipartReader(Message message, String bucket, String objectKey) {
        this.message = message;
        this.bucket = bucket;
        this.objectKey = objectKey;
    }


    public Set<URI> extractScopusURL() {
        var messageBody = getMultipartBody();
        var bodyString = extractMessageAsString(messageBody);
        var uriSet = extractUrisFromBody(bodyString);
        throwExceptionIfSetIsEmpty(uriSet);
        return uriSet;
    }

    private String extractMessageAsString(MultipartImpl messageBody) {
        return String.join(StringUtils.EMPTY_STRING, extractBodyPartsTexts(messageBody));
    }

    private List<String> extractBodyPartsTexts(MultipartImpl messageBody) {
        return messageBody.getBodyParts().stream().map(this::getTextFromBodyPart).toList();
    }

    private String getTextFromBodyPart(Entity bodyPart) {
        return getBodyText((BodyPart) bodyPart);
    }

    private Set<URI> extractUrisFromBody(String bodyString) {
        Set<URI> uriSet = new HashSet<>();
        var pattern = Pattern.compile(REGEX_PATTERN_VALID_URL);
        var matcher = pattern.matcher(bodyString);
        while (matcher.find()) {
            String uriString = matcher.group();
            if (isNotCitedByUriString(uriString)) {
                var uri = UriWrapper.fromUri(uriString).getUri();
                uriSet.add(uri);
            }

        }
        return uriSet;
    }


    private MultipartImpl getMultipartBody() {
        if (!(message.getBody() instanceof MultipartImpl multipart)) {
            throw new EmailException(COULD_NOT_PARSE_EMAIL, bucket, objectKey);
        }
        return multipart;
    }

    private void throwExceptionIfSetIsEmpty(Set<URI> uriSet) {
        if (uriSet.isEmpty()) {
            throw new EmailException(NO_URL_PRESENT_IN_MESSAGE, bucket, objectKey);
        }
    }

    private boolean isNotCitedByUriString(String uriString) {
        var pattern = Pattern.compile(NOT_CITED_BY_REGEX);
        var matcher = pattern.matcher(uriString);
        return matcher.find();
    }

    private String getBodyText(BodyPart bodypart) {
        return Try.of(() -> (TextBody) bodypart.getBody())
                .mapTry(SingleBody::getInputStream)
                .mapTry(InputStream::readAllBytes)
                .mapTry(String::new)
                .get();
    }
}
