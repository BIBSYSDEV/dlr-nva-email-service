package no.sikt.nva.email.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class EmailRequest {

    private String fromAddress;
    private String toAddress;
    private String cc;
    private String bcc;
    private String subject;
    private String text;
    private String textHtml;

    @JsonCreator
    public EmailRequest(@JsonProperty("from_address") String fromAddress,
                        @JsonProperty("to_address") String toAddress,
                        @JsonProperty("cc") String cc,
                        @JsonProperty("bcc") String bcc,
                        @JsonProperty("subject") String subject,
                        @JsonProperty("text") String text,
                        @JsonProperty("text_html") String textHtml) {
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.cc = cc;
        this.bcc = bcc;
        this.subject = subject;
        this.text = text;
        this.textHtml = textHtml;
    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public String getCc() {
        return cc;
    }

    public void setCc(String cc) {
        this.cc = cc;
    }

    public String getBcc() {
        return bcc;
    }

    public void setBcc(String bcc) {
        this.bcc = bcc;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTextHtml() {
        return textHtml;
    }

    public void setTextHtml(String textHtml) {
        this.textHtml = textHtml;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getToAddress(),
                            getCc(),
                            getBcc(),
                            getSubject(),
                            getText(),
                            getTextHtml(),
                            getFromAddress());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EmailRequest)) {
            return false;
        }
        EmailRequest that = (EmailRequest) o;
        return Objects.equals(getToAddress(), that.getToAddress())
               && Objects.equals(getCc(), that.getCc())
               && Objects.equals(getBcc(), that.getBcc())
               && Objects.equals(getSubject(), that.getSubject())
               && Objects.equals(getText(), that.getText())
               && Objects.equals(getTextHtml(), that.getTextHtml())
               && Objects.equals(getFromAddress(), that.getFromAddress())
            ;
    }
}
