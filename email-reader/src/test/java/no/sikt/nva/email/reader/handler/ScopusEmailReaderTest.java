package no.sikt.nva.email.reader.handler;

import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class ScopusEmailReaderTest {

    @Test
    void dummyTest(){
        var something = new ScopusEmailReader();
        var result = something.startup();
        assertThat(result, is(equalTo("hello world")));
    }
}
