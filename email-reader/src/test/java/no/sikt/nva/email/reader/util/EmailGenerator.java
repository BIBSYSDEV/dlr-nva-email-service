package no.sikt.nva.email.reader.util;

import nva.commons.core.ioutils.IoUtils;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.field.DefaultFieldParser;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyPartBuilder;
import org.apache.james.mime4j.message.DefaultMessageWriter;
import org.apache.james.mime4j.message.MultipartBuilder;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.NameValuePair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static no.sikt.nva.email.reader.mapper.messagebodyreader.ScopusEmailValidator.VALID_FROM_DOMAIN;
import static no.sikt.nva.email.reader.mapper.messagebodyreader.ScopusEmailValidator.VALID_FROM_LOCAL_PART;
import static no.sikt.nva.email.reader.mapper.messagebodyreader.ScopusEmailValidator.VALID_SUBJECT;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;

public class EmailGenerator {

    public static final String VALID_EMAIL_BODY_PATH = "valid_email_body.txt";
    public static final String UTF_8 = "UTF-8";
    public static final String CHARSET = "charset";
    public static final String CONTENT_TYPE_TEXT_HTML = "text/html";
    public static final String TRANSFER_ENCODING_QUOTED_PRINTABLE = "quoted-printable";
    public static final String MIXED_SUB_TYPE = "mixed";
    private static final String VALID_RECEIVED_SPF_HEADER_PART = "Received-SPF: Pass (protection.outlook.com:";

    public static String generateValidEmail() throws MimeException, IOException {
        var message = Message.Builder.of()
                          .setBody(createValidMultipartBody())
                          .setSubject(VALID_SUBJECT)
                          .setFrom(createValidFromMailBox())
                          .setField(createValidReceivedSpfHeader())
                          .build();

        return writeMessageToString(message);
    }

    public static String generateValidEmailWithSiktSender() throws IOException, MimeException {
        var message = Message.Builder.of()
                          .setBody(createValidMultipartBody())
                          .setSubject(VALID_SUBJECT)
                          .setFrom(createValidFromMailBoxSikt())
                          .setField(createValidReceivedSpfHeader())
                          .build();

        return writeMessageToString(message);
    }

    public static String generateEmailWithInvalidSubject() throws IOException, MimeException {
        var message = Message.Builder.of()
                          .setBody(createValidMultipartBody())
                          .setSubject(randomString())
                          .setFrom(createValidFromMailBox())
                          .setField(createValidReceivedSpfHeader())
                          .build();

        return writeMessageToString(message);
    }

    public static String generateEmailWithInvalidFromHeader() throws IOException, MimeException {
        var message = Message.Builder.of()
                          .setBody(createValidMultipartBody())
                          .setSubject(VALID_SUBJECT)
                          .setFrom(createInvalidFromMailBox())
                          .setField(createValidReceivedSpfHeader())
                          .build();

        return writeMessageToString(message);
    }

    public static String generateEmailWithoutSpfHeader() throws IOException {
        var message = Message.Builder.of()
                          .setBody(createValidMultipartBody())
                          .setSubject(VALID_SUBJECT)
                          .setFrom(createValidFromMailBox())
                          .build();

        return writeMessageToString(message);
    }

    public static String generateNonMultipartEmail() throws IOException, MimeException {
        var message = Message.Builder.of()
                          .setBody(createTextBodyPart())
                          .setSubject(VALID_SUBJECT)
                          .setFrom(createValidFromMailBox())
                          .setField(createValidReceivedSpfHeader())
                          .build();

        return writeMessageToString(message);
    }

    public static String generateEmailWithoutScopusLinks() throws IOException, MimeException {
        var message = Message.Builder.of()
                          .setBody(createEmptyMultipartBody())
                          .setSubject(VALID_SUBJECT)
                          .setFrom(createValidFromMailBox())
                          .setField(createValidReceivedSpfHeader())
                          .build();

        return writeMessageToString(message);
    }

    private static Multipart createEmptyMultipartBody() throws IOException {
        return MultipartBuilder
                   .create(MIXED_SUB_TYPE)
                   .addBodyPart(BodyPartBuilder.create()
                                    .setBody(randomString(), StandardCharsets.UTF_8)
                                    .setContentType(CONTENT_TYPE_TEXT_HTML, new NameValuePair(CHARSET, UTF_8))
                                    .setContentTransferEncoding(TRANSFER_ENCODING_QUOTED_PRINTABLE)
                                    .build())
                   .build();
    }

    private static TextBody createTextBodyPart() {
        return BasicBodyFactory.INSTANCE.textBody(randomString());
    }

    private static Mailbox createInvalidFromMailBox() {
        return new Mailbox(randomString(), "example.com");
    }

    private static Multipart createValidMultipartBody() throws IOException {
        return MultipartBuilder
                   .create(MIXED_SUB_TYPE)
                   .addBodyPart(BodyPartBuilder.create()
                                    .setBody(readValidBodyText(), StandardCharsets.UTF_8)
                                    .setContentType(CONTENT_TYPE_TEXT_HTML, new NameValuePair(CHARSET, UTF_8))
                                    .setContentTransferEncoding(TRANSFER_ENCODING_QUOTED_PRINTABLE)
                                    .build())
                   .build();
    }

    private static String readValidBodyText() {
        return IoUtils.stringFromResources(Path.of(VALID_EMAIL_BODY_PATH));
    }

    private static String writeMessageToString(Message message) throws IOException {
        var outputStream = new ByteArrayOutputStream();
        var writer = new DefaultMessageWriter();
        writer.writeMessage(message, outputStream);
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    private static Field createValidReceivedSpfHeader() throws MimeException {
        return createReceivedSpfHeader();
    }

    private static Field createReceivedSpfHeader() throws MimeException {
        return DefaultFieldParser.parse(VALID_RECEIVED_SPF_HEADER_PART);
    }

    private static Mailbox createValidFromMailBox() {
        return new Mailbox(VALID_FROM_LOCAL_PART, VALID_FROM_DOMAIN);
    }

    private static Mailbox createValidFromMailBoxSikt() {
        return new Mailbox("someone", "sikt.no");
    }
}
