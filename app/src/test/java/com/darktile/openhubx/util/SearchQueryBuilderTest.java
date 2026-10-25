package com.darktile.openhubx.util;

import static org.junit.Assert.assertEquals;

import com.darktile.openhubx.mvp.model.SearchFilter;
import com.darktile.openhubx.util.SearchQueryBuilder.Target;

import org.junit.Test;

public class SearchQueryBuilderTest {

    @Test
    public void textOnly_noFilter() {
        assertEquals("android", SearchQueryBuilder.build("android", null, Target.REPOSITORIES));
        assertEquals("android", SearchQueryBuilder.build("  android ", new SearchFilter(), Target.REPOSITORIES));
    }

    @Test
    public void emptyTextAndEmptyFilter_isEmpty() {
        assertEquals("", SearchQueryBuilder.build(null, new SearchFilter(), Target.REPOSITORIES));
        assertEquals("", SearchQueryBuilder.build("   ", null, Target.USERS));
    }

    @Test
    public void spacesAndSpecialCharsAreEncoded() {
        assertEquals("rx+java", SearchQueryBuilder.build("rx java", null, Target.REPOSITORIES));
        assertEquals("a%26b+%24x", SearchQueryBuilder.build("a&b $x", null, Target.REPOSITORIES));
    }

    @Test
    public void languageAppended() {
        SearchFilter f = new SearchFilter().setLanguage("java");
        assertEquals("http+client+language:java", SearchQueryBuilder.build("http client", f, Target.REPOSITORIES));
    }

    @Test
    public void languageSlugEncodedOnce() {
        assertEquals("x+language:c%2B%2B",
                SearchQueryBuilder.build("x", new SearchFilter().setLanguage("c++"), Target.REPOSITORIES));
        assertEquals("x+language:c%23",
                SearchQueryBuilder.build("x", new SearchFilter().setLanguage("c%23"), Target.REPOSITORIES));
    }

    @Test
    public void allLanguageIgnored() {
        assertEquals("x", SearchQueryBuilder.build("x", new SearchFilter().setLanguage("all"), Target.REPOSITORIES));
    }

    @Test
    public void allRepoQualifiersCompose() {
        SearchFilter f = new SearchFilter()
                .setLanguage("kotlin")
                .setMinStars(100)
                .setPushedAfter("2026-01-31")
                .setForkMode(SearchFilter.ForkMode.INCLUDE)
                .setOwner("@square", true)
                .setTopic("http")
                .setLicense("MIT");
        assertEquals("okhttp+language:kotlin+org:square+stars:%3E100+pushed:%3E2026-01-31"
                        + "+fork:true+topic:http+license:mit",
                SearchQueryBuilder.build("okhttp", f, Target.REPOSITORIES));
    }

    @Test
    public void qualifiersWithoutText() {
        SearchFilter f = new SearchFilter().setMinStars(5000).setForkMode(SearchFilter.ForkMode.ONLY);
        assertEquals("stars:%3E5000+fork:only", SearchQueryBuilder.build("", f, Target.REPOSITORIES));
    }

    @Test
    public void invalidValuesDropped() {
        SearchFilter f = new SearchFilter().setMinStars(0).setPushedAfter("last week").setTopic("  ");
        assertEquals("x", SearchQueryBuilder.build("x", f, Target.REPOSITORIES));
    }

    @Test
    public void valueWithSpaceIsQuoted() {
        SearchFilter f = new SearchFilter().setTopic("machine learning");
        assertEquals("topic:%22machine+learning%22", SearchQueryBuilder.build(null, f, Target.REPOSITORIES));
    }

    @Test
    public void usersTargetKeepsOnlyLanguage() {
        SearchFilter f = new SearchFilter().setLanguage("go").setMinStars(10).setOwner("x", false).setTopic("t");
        assertEquals("rob+language:go", SearchQueryBuilder.build("rob", f, Target.USERS));
    }

    @Test
    public void codeAndIssuesKeepLanguageAndOwner() {
        SearchFilter f = new SearchFilter().setLanguage("java").setOwner("octocat", false)
                .setMinStars(10).setLicense("mit");
        assertEquals("println+language:java+user:octocat",
                SearchQueryBuilder.build("println", f, Target.CODE));
        assertEquals("crash+language:java+user:octocat",
                SearchQueryBuilder.build("crash", f, Target.ISSUES));
    }
}
