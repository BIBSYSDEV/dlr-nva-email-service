package no.sikt.nva.email.reader.util;

import no.sikt.nva.email.reader.service.FileRetriever;

import java.io.InputStream;
import java.net.URI;

public class FakeZipFileRetrieverThrowingException implements FileRetriever {
    @Override
    public InputStream retrieveFile(URI uri) {
        throw new UnsupportedOperationException("I don't work");
    }
}
