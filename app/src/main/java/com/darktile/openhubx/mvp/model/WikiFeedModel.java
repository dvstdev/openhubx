package com.darktile.openhubx.mvp.model;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

/**
 * Wiki atom feed model. Previously deserialized via SimpleXML; the SimpleXML
 * converter was dropped in the RxJava 3 modernization, so the atom feed is now
 * parsed manually from the raw response with the platform XmlPullParser.
 * Created by ThirtyDegreesRay on 2017/12/7 10:01:39
 */
public class WikiFeedModel {

    private String id;
    private String title;
    private String updated;
    private ArrayList<WikiModel> wikiList;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    public ArrayList<WikiModel> getWikiList() {
        return wikiList;
    }

    public void setWikiList(ArrayList<WikiModel> wikiList) {
        this.wikiList = wikiList;
    }

    /**
     * Parse a GitHub wiki atom feed. Reads the feed-level id/title/updated and
     * every &lt;entry&gt; into a {@link WikiModel}.
     *
     * @param xml raw atom feed body
     * @return populated feed model
     * @throws XmlPullParserException on malformed XML
     * @throws IOException on read failure
     */
    public static WikiFeedModel fromAtom(String xml) throws XmlPullParserException, IOException {
        WikiFeedModel feed = new WikiFeedModel();
        ArrayList<WikiModel> entries = new ArrayList<>();
        feed.setWikiList(entries);

        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(new StringReader(xml));

        WikiModel current = null;
        boolean inEntry = false;
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            String name = parser.getName();
            if (event == XmlPullParser.START_TAG) {
                if ("entry".equals(name)) {
                    inEntry = true;
                    current = new WikiModel();
                } else if ("id".equals(name)) {
                    String text = parser.nextText();
                    if (inEntry && current != null) {
                        current.setId(text);
                    } else if (feed.getId() == null) {
                        feed.setId(text);
                    }
                } else if ("title".equals(name)) {
                    if (!inEntry && feed.getTitle() == null) {
                        feed.setTitle(parser.nextText());
                    }
                } else if ("updated".equals(name)) {
                    String text = parser.nextText();
                    if (inEntry && current != null) {
                        current.setUpdated(text);
                    } else if (feed.getUpdated() == null) {
                        feed.setUpdated(text);
                    }
                } else if ("published".equals(name)) {
                    if (inEntry && current != null) {
                        current.setPublished(parser.nextText());
                    }
                } else if ("content".equals(name)) {
                    if (inEntry && current != null) {
                        current.setContent(parser.nextText());
                    }
                }
            } else if (event == XmlPullParser.END_TAG && "entry".equals(name)) {
                if (current != null) {
                    entries.add(current);
                }
                inEntry = false;
                current = null;
            }
            event = parser.next();
        }
        return feed;
    }

}
