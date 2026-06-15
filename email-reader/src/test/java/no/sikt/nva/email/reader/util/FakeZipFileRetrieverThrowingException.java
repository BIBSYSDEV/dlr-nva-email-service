package no.sikt.nva.email.reader.util;

import java.io.InputStream;
import java.net.URI;
import no.sikt.nva.email.reader.service.FileRetriever;

public class FakeZipFileRetrieverThrowingException implements FileRetriever {
  @Override
  public InputStream retrieveFile(URI uri) {
    throw new UnsupportedOperationException("I don't work");
  }
}
