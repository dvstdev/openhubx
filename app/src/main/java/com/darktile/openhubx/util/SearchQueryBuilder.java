package com.darktile.openhubx.util;

import com.darktile.openhubx.mvp.model.SearchFilter;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Builds the GitHub search {@code q} parameter from the typed text plus a
 * {@link SearchFilter}. The result is already URL-encoded (terms joined with '+')
 * because {@code SearchService} sends {@code q} with {@code encoded = true}.
 * Only qualifiers that the target search endpoint understands are emitted.
 */
public final class SearchQueryBuilder {

    /** Which GitHub search endpoint the query is for. */
    public enum Target {
        REPOSITORIES, USERS, CODE, ISSUES
    }

    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern WHITESPACE = Pattern.compile("\\s");

    private SearchQueryBuilder() {
    }

    public static String build(String text, SearchFilter filter, Target target) {
        List<String> parts = new ArrayList<>();
        String trimmed = text == null ? "" : text.trim();
        if (!trimmed.isEmpty()) {
            parts.add(encode(trimmed));
        }
        if (filter != null && target != null) {
            addQualifiers(parts, filter, target);
        }
        return join(parts);
    }

    private static void addQualifiers(List<String> parts, SearchFilter f, Target target) {
        String language = normalizeSlug(f.getLanguage());
        if (language != null) {
            parts.add(qualifier("language", language));
        }
        if (target == Target.USERS) {
            return;
        }
        String owner = clean(f.getOwner());
        if (owner != null && owner.startsWith("@")) {
            owner = clean(owner.substring(1));
        }
        if (owner != null) {
            parts.add(qualifier(f.isOwnerIsOrg() ? "org" : "user", owner));
        }
        if (target != Target.REPOSITORIES) {
            return;
        }
        Integer stars = f.getMinStars();
        if (stars != null && stars > 0) {
            parts.add("stars:" + encode(">" + stars));
        }
        String pushed = clean(f.getPushedAfter());
        if (pushed != null && DATE.matcher(pushed).matches()) {
            parts.add("pushed:" + encode(">" + pushed));
        }
        switch (f.getForkMode()) {
            case INCLUDE:
                parts.add("fork:true");
                break;
            case ONLY:
                parts.add("fork:only");
                break;
            default:
                break;
        }
        String topic = clean(f.getTopic());
        if (topic != null) {
            parts.add(qualifier("topic", topic));
        }
        String license = clean(f.getLicense());
        if (license != null) {
            parts.add(qualifier("license", license.toLowerCase(java.util.Locale.ROOT)));
        }
    }

    /** {@code key:value}, quoting values that contain whitespace. */
    private static String qualifier(String key, String value) {
        String v = WHITESPACE.matcher(value).find() ? "\"" + value + "\"" : value;
        return key + ":" + encode(v);
    }

    /**
     * Language slugs may arrive URL-encoded (e.g. "c%23" scraped from trending) or
     * raw (e.g. "c++"); decode first so the final encode is applied exactly once.
     */
    private static String normalizeSlug(String slug) {
        String s = clean(slug);
        if (s == null || "all".equalsIgnoreCase(s)) return null;
        if (s.contains("%")) {
            try {
                s = URLDecoder.decode(s, "UTF-8");
            } catch (IllegalArgumentException | UnsupportedEncodingException ignored) {
                // keep as-is
            }
        }
        return clean(s);
    }

    private static String clean(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String join(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) sb.append('+');
            sb.append(p);
        }
        return sb.toString();
    }
}
