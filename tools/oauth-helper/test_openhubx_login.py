"""Offline tests for openhubx_login.py (fake tokens, fake transport; no network)."""

import json
import os
import stat
import shutil
import subprocess
import unittest

import openhubx_login as h

NOW = 1_700_000_000


class FakePost:
    """Scripted stand-in for _post_form: returns queued bodies, records calls."""

    def __init__(self, bodies):
        self.bodies = list(bodies)
        self.calls = []

    def __call__(self, url, fields):
        self.calls.append((url, dict(fields)))
        return self.bodies.pop(0)


class FakeClock:
    def __init__(self):
        self.t = 0.0
        self.sleeps = []

    def sleep(self, s):
        self.sleeps.append(s)
        self.t += s

    def time(self):
        return self.t


class EnvelopeTest(unittest.TestCase):
    def test_ghapp_envelope_with_refresh(self):
        env = h.build_envelope("ghu_fake", NOW, 28800, "ghr_fake", 15897600, "Iv1.c",
                               nonce="n")
        self.assertEqual(env, {"v": 1, "typ": "ghtok", "tt": "ghapp", "tok": "ghu_fake",
                               "exp": NOW + 28800, "nonce": "n", "rt": "ghr_fake",
                               "rexp": NOW + 15897600, "cid": "Iv1.c"})

    def test_no_refresh_omits_rt(self):
        env = h.build_envelope("ghu_fake", NOW, 28800, "ghr_fake", 15897600, "Iv1.c",
                               include_refresh=False)
        for k in ("rt", "rexp", "cid"):
            self.assertNotIn(k, env)
        self.assertEqual(len(env["nonce"]), 16)

    def test_non_expiring_token_has_null_exp(self):
        self.assertIsNone(h.build_envelope("ghu_fake", NOW)["exp"])

    def test_round_trip_unpadded_base64url(self):
        env = h.build_envelope("ghu_fake", NOW, 28800, nonce="n")
        enc = h.encode_envelope(env)
        self.assertNotIn("=", enc)
        self.assertRegex(enc, r"^[A-Za-z0-9_-]+$")
        self.assertEqual(h.decode_envelope(enc), env)
        self.assertEqual(h.decode_envelope(h.LINK_PREFIX + enc), env)

    def test_missing_token_rejected(self):
        with self.assertRaises(h.HelperError):
            h.build_envelope("", NOW)

    def test_matches_phone_fixture(self):
        # Same literal is parsed by QrLoginPayloadParserTest.helperOutput_parses.
        env = h.build_envelope("ghu_FAKE", NOW, 28800, "ghr_FAKE", 15897600, "Iv1.fake",
                               nonce="0123456789abcdef")
        self.assertEqual(h.encode_envelope(env),
                         "eyJ2IjoxLCJ0eXAiOiJnaHRvayIsInR0IjoiZ2hhcHAiLCJ0b2siOiJnaHVfRkFLRSIs"
                         "ImV4cCI6MTcwMDAyODgwMCwibm9uY2UiOiIwMTIzNDU2Nzg5YWJjZGVmIiwicnQiOiJnaHJfRkFL"
                         "RSIsInJleHAiOjE3MTU4OTc2MDAsImNpZCI6Ikl2MS5mYWtlIn0")


class DeviceFlowTest(unittest.TestCase):
    DEVICE = {"device_code": "dc", "user_code": "ABCD-EFGH",
              "verification_uri": "https://github.com/login/device",
              "expires_in": 900, "interval": 5}

    def test_device_code_sends_client_id_only(self):
        post = FakePost([self.DEVICE])
        self.assertEqual(h.request_device_code("Iv1.c", post=post), self.DEVICE)
        self.assertEqual(post.calls, [(h.DEVICE_CODE_URL, {"client_id": "Iv1.c"})])

    def test_device_flow_disabled_is_explained(self):
        post = FakePost([{"error": "device_flow_disabled"}])
        with self.assertRaisesRegex(h.HelperError, "enable 'Device Flow'"):
            h.request_device_code("Iv1.c", post=post)

    def test_poll_honors_interval_and_slow_down(self):
        post = FakePost([{"error": "authorization_pending"},
                         {"error": "slow_down", "interval": 10},
                         {"error": "slow_down"},
                         {"access_token": "ghu_fake", "expires_in": 28800}])
        clock = FakeClock()
        tok = h.poll_for_token("Iv1.c", self.DEVICE, post=post,
                               sleep=clock.sleep, clock=clock.time)
        self.assertEqual(tok["access_token"], "ghu_fake")
        self.assertEqual(clock.sleeps, [5, 5, 10, 15])
        url, fields = post.calls[0]
        self.assertEqual(url, h.TOKEN_URL)
        self.assertEqual(fields, {"client_id": "Iv1.c", "device_code": "dc",
                                  "grant_type": h.DEVICE_GRANT})
        self.assertNotIn("client_secret", fields)

    def test_poll_stops_at_deadline(self):
        device = dict(self.DEVICE, expires_in=12)
        post = FakePost([{"error": "authorization_pending"}] * 10)
        clock = FakeClock()
        with self.assertRaisesRegex(h.HelperError, "expired"):
            h.poll_for_token("Iv1.c", device, post=post, sleep=clock.sleep, clock=clock.time)
        self.assertEqual(len(post.calls), 2)

    def test_poll_access_denied(self):
        post = FakePost([{"error": "access_denied"}])
        clock = FakeClock()
        with self.assertRaisesRegex(h.HelperError, "access_denied"):
            h.poll_for_token("Iv1.c", self.DEVICE, post=post, sleep=clock.sleep,
                             clock=clock.time)

    def test_post_refuses_other_hosts(self):
        with self.assertRaises(h.HelperError):
            h._post_form("https://example.com/x", {})


class RenderTest(unittest.TestCase):
    def test_html_is_offline_and_embeds_payload(self):
        env = h.build_envelope("ghu_fake", NOW, 28800, "ghr_fake", 15897600, "Iv1.c")
        enc = h.encode_envelope(env)
        html = h.render_html(enc, env, h.load_qr_js())
        self.assertIn("connect-src 'none'", html)
        self.assertIn(json.dumps(enc), html)
        self.assertIn("Kazuhiko Arase", html)
        self.assertNotIn("ghu_fake", html)  # only the base64url form appears
        self.assertNotIn("http://", html.split("<script>")[0])

    def test_private_file_mode(self):
        path = h.write_private_html("<html></html>")
        try:
            self.assertEqual(stat.S_IMODE(os.stat(path).st_mode), 0o600)
            self.assertEqual(stat.S_IMODE(os.stat(os.path.dirname(path)).st_mode), 0o700)
        finally:
            shutil.rmtree(os.path.dirname(path))

    @unittest.skipUnless(shutil.which("node"), "node not installed")
    def test_vendored_encoder_builds_qr_for_payload(self):
        env = h.build_envelope("ghu_" + "x" * 36, NOW, 28800, "ghr_" + "y" * 76,
                               15897600, "Iv23li" + "z" * 14)
        enc = h.encode_envelope(env)
        js = h.load_qr_js() + (
            "\nvar q=qrcode(0,'M');q.addData(%s,'Byte');q.make();"
            "console.log(q.getModuleCount());" % json.dumps(enc))
        out = subprocess.run(["node", "-e", js], capture_output=True, text=True, check=True)
        self.assertGreaterEqual(int(out.stdout.strip()), 21)


if __name__ == "__main__":
    unittest.main()
