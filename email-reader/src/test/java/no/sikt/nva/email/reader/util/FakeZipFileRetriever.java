package no.sikt.nva.email.reader.util;

import no.sikt.nva.email.reader.service.FileRetriever;
import nva.commons.core.ioutils.IoUtils;

import java.io.InputStream;
import java.net.URI;

public class FakeZipFileRetriever implements FileRetriever {

    private static final String PATH_TO_SAMPLE_ZIP = "scopus.zip";

    @Override
    public InputStream retrieveFile(URI uri) {
        return IoUtils.inputStreamFromResources(PATH_TO_SAMPLE_ZIP);
    }
}
