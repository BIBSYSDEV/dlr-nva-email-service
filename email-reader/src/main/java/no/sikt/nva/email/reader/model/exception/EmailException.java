package no.sikt.nva.email.reader.model.exception;



public class EmailException extends RuntimeException {

    private final String bucket;
    private final String objectKey;

    public EmailException(String message, String bucket, String objectKey) {
        super(message);
        this.bucket = bucket;
        this.objectKey = objectKey;
    }

    public EmailException(String message, String bucket, String objectKey, Throwable throwable) {
        super(message, throwable);
        this.bucket = bucket;
        this.objectKey = objectKey;
    }

    public String getBucket() {
        return bucket;
    }

    public String getObjectKey() {
        return objectKey;
    }
}
