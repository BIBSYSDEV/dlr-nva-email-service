package no.sikt.nva.email.reader.util;

import no.unit.nva.stubs.FakeS3Client;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;


public class FakeS3ClientThrowingExceptionWhenInsertingZipFile extends FakeS3Client {
    private boolean firstPutObjectRequest = true;


    @Override
    public PutObjectResponse putObject(PutObjectRequest putObjectRequest, RequestBody requestBody) {
        return allowInsertS3Event(putObjectRequest, requestBody);
    }

    private PutObjectResponse allowInsertS3Event(PutObjectRequest putObjectRequest, RequestBody requestBody) {
        if (firstPutObjectRequest) {
            firstPutObjectRequest = false;
            return super.putObject(putObjectRequest, requestBody);
        } else {
            throw new UnsupportedOperationException("I don't work");
        }
    }
}
