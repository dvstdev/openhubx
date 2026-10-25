package com.darktile.openhubx.auth.qr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Unit tests for {@link QrLoginLink} deep-link extraction. */
public class QrLoginLinkTest {

    @Test
    public void rawEnvelopePassesThrough() throws Exception {
        assertEquals("eyJ2IjoxfQ", QrLoginLink.extractEnvelope("eyJ2IjoxfQ"));
        assertFalse(QrLoginLink.isLoginLink("eyJ2IjoxfQ"));
    }

    @Test
    public void extractsDParam() throws Exception {
        assertEquals("abc_-", QrLoginLink.extractEnvelope("openhubx://login?d=abc_-"));
    }

    @Test
    public void toleratesSlashOtherParamsFragmentAndCase() throws Exception {
        assertEquals("abc", QrLoginLink.extractEnvelope("openhubx://login/?x=1&d=abc&y=2"));
        assertEquals("abc", QrLoginLink.extractEnvelope("openhubx://login?d=abc#frag"));
        assertEquals("abc", QrLoginLink.extractEnvelope("  OpenHubX://Login?d=abc  "));
        assertTrue(QrLoginLink.isLoginLink("OPENHUBX://LOGIN?d=1"));
    }

    @Test
    public void percentDecodesPadding() throws Exception {
        assertEquals("abc==", QrLoginLink.extractEnvelope("openhubx://login?d=abc%3D%3D"));
    }

    @Test(expected = QrLoginParseException.class)
    public void otherPathRejected() throws Exception {
        QrLoginLink.extractEnvelope("openhubx://loginx?d=abc");
    }

    @Test(expected = QrLoginParseException.class)
    public void badPercentEncodingRejected() throws Exception {
        QrLoginLink.extractEnvelope("openhubx://login?d=%zz");
    }
}
