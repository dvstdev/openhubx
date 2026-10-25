package com.darktile.openhubx.http.error;

/**
 * A GitHub 403 response. On this app it almost always means the signed-in token
 * lacks the scope/permission for the resource (e.g. notifications are not readable
 * with fine-grained or GitHub App tokens), so callers show a hint rather than a
 * retryable error.
 */
public class HttpForbiddenError extends Exception {
    public HttpForbiddenError() {
        super("HTTP 403");
    }
}
