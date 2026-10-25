package com.darktile.openhubx.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchHistoryPolicyTest {

    @Test
    public void pushPutsNewestFirstAndDedups() {
        List<String> current = Arrays.asList("b", "a", "c");
        assertEquals(Arrays.asList("a", "b", "c"), SearchHistoryPolicy.push(current, "a", 50));
        assertEquals(Arrays.asList("d", "b", "a", "c"), SearchHistoryPolicy.push(current, "d", 50));
    }

    @Test
    public void dedupIsCaseInsensitiveAndKeepsNewestSpelling() {
        assertEquals(Arrays.asList("RxJava", "b"),
                SearchHistoryPolicy.push(Arrays.asList("rxjava", "b"), "RxJava", 50));
    }

    @Test
    public void capDropsOldest() {
        List<String> current = new ArrayList<>();
        for (int i = 0; i < SearchHistoryPolicy.MAX_PER_TYPE; i++) current.add("q" + i);
        List<String> next = SearchHistoryPolicy.push(current, "new", SearchHistoryPolicy.MAX_PER_TYPE);
        assertEquals(SearchHistoryPolicy.MAX_PER_TYPE, next.size());
        assertEquals("new", next.get(0));
        assertEquals("q" + (SearchHistoryPolicy.MAX_PER_TYPE - 2), next.get(next.size() - 1));
    }

    @Test
    public void dollarQueriesKept() {
        assertEquals(Arrays.asList("price $5", "a"),
                SearchHistoryPolicy.push(Arrays.asList("a"), " price $5 ", 50));
    }

    @Test
    public void blankAndTooLongIgnored() {
        assertNull(SearchHistoryPolicy.normalize("   "));
        assertNull(SearchHistoryPolicy.normalize(null));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= SearchHistoryPolicy.MAX_QUERY_LENGTH; i++) sb.append('x');
        assertEquals(Arrays.asList("a"), SearchHistoryPolicy.push(Arrays.asList("a"), sb.toString(), 50));
    }

    @Test
    public void parseLegacy() {
        assertEquals(Arrays.asList("c", "b", "a"), SearchHistoryPolicy.parseLegacy("c$$b$$a$$b$$", 50));
        assertEquals(Arrays.asList("one"), SearchHistoryPolicy.parseLegacy("one", 50));
        assertEquals(new ArrayList<String>(), SearchHistoryPolicy.parseLegacy("", 50));
        assertEquals(Arrays.asList("c", "b"), SearchHistoryPolicy.parseLegacy("c$$b$$a", 2));
    }

    @Test
    public void likePatternEscapesWildcards() {
        assertEquals("%java%", SearchHistoryPolicy.containsPattern(" java "));
        assertEquals("%50\\%\\_x\\\\%", SearchHistoryPolicy.containsPattern("50%_x\\"));
    }
}
