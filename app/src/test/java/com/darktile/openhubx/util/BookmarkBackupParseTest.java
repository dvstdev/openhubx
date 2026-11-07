package com.darktile.openhubx.util;

import com.google.gson.JsonPrimitive;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Import must read any export: other languages, Android versions, odd rows. */
public class BookmarkBackupParseTest {

    private static final String REPO = "{\"id\":42,\"name\":\"openhubx\",\"ownerLogin\":\"dvstdev\"}";

    private static String file(String markTime) {
        return "{\"version\":1,\"bookmarks\":[{\"id\":\"a\",\"type\":\"repo\",\"repoId\":42,"
                + "\"markTime\":" + markTime + ",\"ownerLoginId\":\"someone-else\"}],"
                + "\"repos\":[" + REPO + "],\"users\":[]}";
    }

    @Test
    public void readsDatesFromAnyPhone() {
        String[] dates = {
                "\"Nov 6, 2026 7:12:00 PM\"",          // Gson default, older Android
                "\"Nov 6, 2026, 7:12:00\u202FPM\"",    // Android 14+ (narrow no-break space)
                "\"2026-11-06T19:12:00Z\"",            // ISO 8601
                "1793949120000",                        // epoch millis
                "\"6 nov. 2026 19:12:00\"",             // French phone
        };
        for (String d : dates) {
            BookmarkBackup.Parsed p = BookmarkBackup.parse(file(d));
            assertEquals(d, 1, p.bookmarks.size());
            Calendar c = Calendar.getInstance(TimeZone.getDefault());
            c.setTime(p.bookmarks.get(0).getMarkTime());
            assertEquals(d, 2026, c.get(Calendar.YEAR));
            assertEquals(d, Calendar.NOVEMBER, c.get(Calendar.MONTH));
        }
    }

    @Test
    public void badDateFallsBackToNow() {
        BookmarkBackup.Parsed p = BookmarkBackup.parse(file("\"not a date\""));
        assertEquals(1, p.bookmarks.size());
        assertTrue(p.bookmarks.get(0).getMarkTime().getTime() > new Date().getTime() - 60_000);
    }

    @Test
    public void toleratesBomBareArrayAndBadRows() {
        String json = "\uFEFF[{\"type\":\"user\",\"userId\":\"octocat\"},"
                + "{\"type\":\"repo\"},"                // no repoId: unusable
                + "\"junk\","                           // not an object: unusable
                + "{\"repoId\":\"7\",\"extra\":true}]"; // string id, no type, unknown field
        BookmarkBackup.Parsed p = BookmarkBackup.parse(json);
        assertEquals(2, p.bookmarks.size());
        assertEquals(2, p.unusable);
        assertEquals("repo", p.bookmarks.get(1).getType());
        assertEquals(Long.valueOf(7), p.bookmarks.get(1).getRepoId());
        assertNotNull("user bookmark gets a placeholder user row", p.users.get("octocat"));
        assertTrue(p.placeholderUsers.contains("octocat"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonBackup() {
        BookmarkBackup.parse("{\"hello\":\"world\"}");
    }

    @Test
    public void exportWritesMillisAndKeepsText() {
        BookmarkBackup.Backup b = new BookmarkBackup.Backup();
        com.darktile.openhubx.dao.Bookmark bm = new com.darktile.openhubx.dao.Bookmark("x");
        bm.setType("repo");
        bm.setRepoId(42L);
        bm.setMarkTime(new Date(1793949120123L));
        b.bookmarks.add(bm);
        String json = BookmarkBackup.toJson(b);
        assertTrue(json, json.contains("\"markTimeMs\":1793949120123"));
        assertTrue(json, json.contains("\"markTime\":\""));
        // millis win over the text, so the exact time comes back
        String withRepo = json.replace("\"repos\":[]", "\"repos\":[" + REPO + "]");
        assertEquals(1793949120123L,
                BookmarkBackup.parse(withRepo).bookmarks.get(0).getMarkTime().getTime());
    }

    @Test
    public void nullDateIsNull() {
        assertEquals(null, BookmarkBackup.date(new JsonPrimitive("")));
    }
}
