package com.darktile.openhubx.mvp.model;

import com.google.gson.annotations.SerializedName;

/** One item of GET search/code. */
public class CodeSearchItem {

    private String name;
    private String path;
    private String sha;
    /** Contents API url of the file at the matched commit. */
    private String url;
    @SerializedName("html_url") private String htmlUrl;
    private Repository repository;

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getSha() {
        return sha;
    }

    public String getUrl() {
        return url;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public Repository getRepository() {
        return repository;
    }
}
