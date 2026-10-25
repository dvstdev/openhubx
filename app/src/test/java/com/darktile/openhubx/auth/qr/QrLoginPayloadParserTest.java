package com.darktile.openhubx.auth.qr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.google.gson.Gson;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Unit tests for {@link QrLoginPayloadParser} envelope parsing and validation. */
public class QrLoginPayloadParserTest {

    private static final long NOW = 1_700_000_000L;

    private final QrLoginPayloadParser parser = new QrLoginPayloadParser();

    /** Encode arbitrary JSON to the unpadded base64url wire form. */
    private static String encode(String json) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void validPatEnvelope_roundTrips() throws Exception {
        QrLoginPayload original =
                new QrLoginPayload("pat", "ghp_abc123", NOW + 3600, "repo,user", "n0nce");

        QrLoginPayload parsed = parser.parse(original.serialize(), NOW);

        assertEquals(QrLoginPayload.VERSION, parsed.getV());
        assertEquals(QrLoginPayload.TYPE, parsed.getTyp());
        assertEquals("pat", parsed.getTt());
        assertEquals("ghp_abc123", parsed.getTok());
        assertEquals(Long.valueOf(NOW + 3600), parsed.getExp());
        assertEquals("repo,user", parsed.getScope());
        assertEquals("n0nce", parsed.getNonce());
    }

    @Test
    public void nullExp_neverExpires() throws Exception {
        QrLoginPayload original = new QrLoginPayload("pat", "ghp_abc123", null, null, "n0nce");

        QrLoginPayload parsed = parser.parse(original.serialize(), NOW);

        assertNull(parsed.getExp());
        assertEquals("ghp_abc123", parsed.getTok());
    }

    @Test
    public void acceptsPaddedBase64() throws Exception {
        String json = "{\"v\":1,\"typ\":\"ghtok\",\"tt\":\"pat\",\"tok\":\"ghp_x\",\"nonce\":\"n\"}";
        String padded = Base64.getUrlEncoder()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));

        QrLoginPayload parsed = parser.parse(padded, NOW);

        assertEquals("ghp_x", parsed.getTok());
    }

    @Test
    public void rejectsVersionMismatch() {
        String json = "{\"v\":2,\"typ\":\"ghtok\",\"tt\":\"pat\",\"tok\":\"ghp_x\",\"nonce\":\"n\"}";
        assertRejected(encode(json), "unsupported version");
    }

    @Test
    public void rejectsMissingTok() {
        String json = "{\"v\":1,\"typ\":\"ghtok\",\"tt\":\"pat\",\"nonce\":\"n\"}";
        assertRejected(encode(json), "missing token");
    }

    @Test
    public void rejectsEmptyTok() {
        String json = "{\"v\":1,\"typ\":\"ghtok\",\"tt\":\"pat\",\"tok\":\"\",\"nonce\":\"n\"}";
        assertRejected(encode(json), "missing token");
    }

    @Test
    public void rejectsWrongTyp() {
        String json = "{\"v\":1,\"typ\":\"other\",\"tt\":\"pat\",\"tok\":\"ghp_x\",\"nonce\":\"n\"}";
        assertRejected(encode(json), "unsupported type");
    }

    @Test
    public void rejectsExpiredExp() {
        QrLoginPayload expired = new QrLoginPayload("pat", "ghp_x", NOW - 1, null, "n");
        assertRejected(expired.serialize(), "token expired");
    }

    @Test
    public void rejectsExpEqualToNow() {
        QrLoginPayload atBoundary = new QrLoginPayload("pat", "ghp_x", NOW, null, "n");
        assertRejected(atBoundary.serialize(), "token expired");
    }

    @Test
    public void rejectsMalformedBase64() {
        assertRejected("!!!not base64!!!", "malformed base64url");
    }

    @Test
    public void rejectsMalformedJson() {
        assertRejected(encode("{not valid json"), "malformed JSON");
    }

    @Test
    public void rejectsEmptyPayload() {
        assertRejected("", "empty payload");
    }

    @Test
    public void rejectsNullPayload() {
        assertRejected(null, "empty payload");
    }

    @Test
    public void rejectsJsonNullLiteral() {
        assertRejected(encode("null"), "empty JSON");
    }

    /** Assert that parsing throws {@link QrLoginParseException} with a message containing {@code expectedFragment}. */
    private void assertRejected(String encoded, String expectedFragment) {
        try {
            parser.parse(encoded, NOW);
            fail("expected QrLoginParseException for: " + encoded);
        } catch (QrLoginParseException e) {
            if (e.getMessage() == null || !e.getMessage().contains(expectedFragment)) {
                fail("expected message containing '" + expectedFragment
                        + "' but was '" + e.getMessage() + "'");
            }
        }
    }
}
