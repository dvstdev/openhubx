package com.darktile.openhubx.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Pure rules for search history: normalization, case-insensitive dedup with
 * newest-first ordering, the size cap, LIKE escaping for suggestions, and parsing
 * of the legacy "$$"-joined preference string.
 */
public final class SearchHistoryPolicy {

    public static final int MAX_PER_TYPE = 50;
    public static final int MAX_QUERY_LENGTH = 256;
    /** Separator used by the old preference-backed history. */
    public static final String LEGACY_SEPARATOR = "$$";

    private SearchHistoryPolicy() {
    }

    /** Trimmed query, or null when it should not be recorded. Any character (incl. '$') is kept. */
    public static String normalize(String query) {
        if (query == null) return null;
        String q = query.trim();
        if (q.isEmpty() || q.length() > MAX_QUERY_LENGTH) return null;
        return q;
    }

    /**
     * Returns a new list with {@code query} at the front, any case-insensitive
     * duplicate removed, capped at {@code cap}. Input is newest first.
     */
    public static List<String> push(List<String> newestFirst, String query, int cap) {
        List<String> source = new ArrayList<>();
        String q = normalize(query);
        if (q != null) source.add(q);
        if (newestFirst != null) source.addAll(newestFirst);
        return dedupAndCap(source, cap);
    }

    /** Keeps the first occurrence (newest) of each query, case-insensitively, up to cap. */
    public static List<String> dedupAndCap(List<String> newestFirst, int cap) {
        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (newestFirst == null || cap <= 0) return result;
        for (String raw : newestFirst) {
            String q = normalize(raw);
            if (q == null) continue;
            if (seen.add(q.toLowerCase(Locale.ROOT))) {
                result.add(q);
                if (result.size() >= cap) break;
            }
        }
        return result;
    }

    /** Parses the legacy newest-first "$$"-joined preference value. */
    public static List<String> parseLegacy(String raw, int cap) {
        List<String> items = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return items;
        int start = 0;
        while (true) {
            int idx = raw.indexOf(LEGACY_SEPARATOR, start);
            if (idx < 0) {
                items.add(raw.substring(start));
                break;
            }
            items.add(raw.substring(start, idx));
            start = idx + LEGACY_SEPARATOR.length();
        }
        return dedupAndCap(items, cap);
    }

    /** SQL LIKE pattern matching {@code text} anywhere, with '\' as the escape character. */
    public static String containsPattern(String text) {
        String t = text == null ? "" : text.trim();
        StringBuilder sb = new StringBuilder("%");
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '%' || c == '_' || c == '\\') sb.append('\\');
            sb.append(c);
        }
        return sb.append('%').toString();
    }
}
