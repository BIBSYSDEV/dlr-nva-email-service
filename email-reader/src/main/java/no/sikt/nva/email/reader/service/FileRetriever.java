package no.sikt.nva.email.reader.service;


import java.io.InputStream;
import java.net.URI;

public interface FileRetriever {

    InputStream retrieveFile(URI uri);
}
