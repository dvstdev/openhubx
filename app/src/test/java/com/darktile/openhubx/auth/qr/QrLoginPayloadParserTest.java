package com.darktile.openhubx.auth.qr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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

    @Test
    public void ghappEnvelope_roundTripsRefreshFields() throws Exception {
        QrLoginPayload original = new QrLoginPayload(QrLoginPayload.TT_GHAPP, "ghu_abc",
                NOW + 28800, null, "n0nce", "ghr_def", NOW + 15897600, "Iv1.client");

        QrLoginPayload parsed = parser.parse(original.serialize(), NOW);

        assertEquals("ghapp", parsed.getTt());
        assertEquals("ghu_abc", parsed.getTok());
        assertEquals(Long.valueOf(NOW + 28800), parsed.getExp());
        assertEquals("ghr_def", parsed.getRt());
        assertEquals(Long.valueOf(NOW + 15897600), parsed.getRexp());
        assertEquals("Iv1.client", parsed.getCid());
        assertTrue(parsed.isRefreshable(NOW));
    }

    @Test
    public void ghappWithoutRefresh_parsesAndIsNotRefreshable() throws Exception {
        String json = "{\"v\":1,\"typ\":\"ghtok\",\"tt\":\"ghapp\",\"tok\":\"ghu_x\","
                + "\"exp\":" + (NOW + 60) + ",\"nonce\":\"n\"}";

        QrLoginPayload parsed = parser.parse(encode(json), NOW);

        assertEquals("ghu_x", parsed.getTok());
        assertNull(parsed.getRt());
        assertFalse(parsed.isRefreshable(NOW));
    }

    @Test
    public void ghappRefreshExpiredOrNoClientId_isNotRefreshable() throws Exception {
        QrLoginPayload rtExpired = new QrLoginPayload("ghapp", "ghu_x", NOW + 60, null, "n",
                "ghr_y", NOW, "Iv1.c");
        QrLoginPayload noCid = new QrLoginPayload("ghapp", "ghu_x", NOW + 60, null, "n",
                "ghr_y", NOW + 100, null);
        QrLoginPayload patWithRt = new QrLoginPayload("pat", "ghp_x", NOW + 60, null, "n",
                "ghr_y", NOW + 100, "Iv1.c");

        assertFalse(parser.parse(rtExpired.serialize(), NOW).isRefreshable(NOW));
        assertFalse(parser.parse(noCid.serialize(), NOW).isRefreshable(NOW));
        assertFalse(parser.parse(patWithRt.serialize(), NOW).isRefreshable(NOW));
    }

    @Test
    public void legacyPatEnvelope_stillParses() throws Exception {
        // Exact 5.0 generator output shape: no rt/rexp/cid fields.
        String json = "{\"v\":1,\"typ\":\"ghtok\",\"tt\":\"pat\",\"tok\":\"ghp_x\","
                + "\"exp\":" + (NOW + 60) + ",\"scope\":\"repo\",\"nonce\":\"n\"}";

        QrLoginPayload parsed = parser.parse(encode(json), NOW);

        assertEquals("pat", parsed.getTt());
        assertNull(parsed.getRt());
        assertNull(parsed.getRexp());
        assertNull(parsed.getCid());
    }

    @Test
    public void ghappExpiredAccessToken_rejected() {
        QrLoginPayload expired = new QrLoginPayload("ghapp", "ghu_x", NOW - 1, null, "n",
                "ghr_y", NOW + 100, "Iv1.c");
        assertRejected(expired.serialize(), "token expired");
    }

    @Test
    public void deepLink_parsesSameAsRawEnvelope() throws Exception {
        QrLoginPayload original = new QrLoginPayload("ghapp", "ghu_abc", NOW + 60, null, "n");
        String link = "openhubx://login?d=" + original.serialize();

        QrLoginPayload parsed = parser.parse(link, NOW);

        assertEquals("ghu_abc", parsed.getTok());
    }

    @Test
    public void helperOutput_parses() throws Exception {
        // Produced by tools/oauth-helper/openhubx_login.py build_envelope/encode_envelope
        // with fake tokens at now=1700000000 (see that file's test suite).
        String fromHelper = "eyJ2IjoxLCJ0eXAiOiJnaHRvayIsInR0IjoiZ2hhcHAiLCJ0b2siOiJnaHVfRkFLRSIs"
                + "ImV4cCI6MTcwMDAyODgwMCwibm9uY2UiOiIwMTIzNDU2Nzg5YWJjZGVmIiwicnQiOiJnaHJfRkFL"
                + "RSIsInJleHAiOjE3MTU4OTc2MDAsImNpZCI6Ikl2MS5mYWtlIn0";

        QrLoginPayload parsed = parser.parse(fromHelper, NOW);

        assertEquals("ghapp", parsed.getTt());
        assertEquals("ghu_FAKE", parsed.getTok());
        assertEquals(Long.valueOf(NOW + 28800), parsed.getExp());
        assertEquals("ghr_FAKE", parsed.getRt());
        assertEquals(Long.valueOf(NOW + 15897600), parsed.getRexp());
        assertEquals("Iv1.fake", parsed.getCid());
        assertTrue(parsed.isRefreshable(NOW));
    }

    @Test
    public void deepLink_rejectsMissingData() {
        assertRejected("openhubx://login", "missing login link data");
        assertRejected("openhubx://login?d=", "missing login link data");
        assertRejected("openhubx://login?x=1", "missing login link data");
    }

    @Test
    public void deepLink_rejectsGarbageData() {
        assertRejected("openhubx://login?d=!!!", "malformed base64url");
        assertRejected("openhubx://login?d=" + encode("{bad"), "malformed JSON");
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
