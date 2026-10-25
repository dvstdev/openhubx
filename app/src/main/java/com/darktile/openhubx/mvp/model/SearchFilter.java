package com.darktile.openhubx.mvp.model;

import java.io.Serializable;

/**
 * User-selected search qualifiers (language + advanced filters). Plain Java so the
 * query composition in {@link com.darktile.openhubx.util.SearchQueryBuilder} is unit-testable.
 * A null / blank field means "not set".
 */
public class SearchFilter implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum ForkMode {
        /** Omit the qualifier (GitHub default: forks excluded). */
        DEFAULT,
        /** fork:true -- include forks. */
        INCLUDE,
        /** fork:only -- only forks. */
        ONLY
    }

    private String language;
    private Integer minStars;
    /** yyyy-MM-dd */
    private String pushedAfter;
    private ForkMode forkMode = ForkMode.DEFAULT;
    private String owner;
    private boolean ownerIsOrg;
    private String topic;
    private String license;

    public SearchFilter() {
    }

    public SearchFilter(SearchFilter other) {
        if (other == null) return;
        language = other.language;
        minStars = other.minStars;
        pushedAfter = other.pushedAfter;
        forkMode = other.forkMode;
        owner = other.owner;
        ownerIsOrg = other.ownerIsOrg;
        topic = other.topic;
        license = other.license;
    }

    public String getLanguage() {
        return language;
    }

    public SearchFilter setLanguage(String language) {
        this.language = language;
        return this;
    }

    public Integer getMinStars() {
        return minStars;
    }

    public SearchFilter setMinStars(Integer minStars) {
        this.minStars = minStars;
        return this;
    }

    public String getPushedAfter() {
        return pushedAfter;
    }

    public SearchFilter setPushedAfter(String pushedAfter) {
        this.pushedAfter = pushedAfter;
        return this;
    }

    public ForkMode getForkMode() {
        return forkMode == null ? ForkMode.DEFAULT : forkMode;
    }

    public SearchFilter setForkMode(ForkMode forkMode) {
        this.forkMode = forkMode;
        return this;
    }

    public String getOwner() {
        return owner;
    }

    public boolean isOwnerIsOrg() {
        return ownerIsOrg;
    }

    public SearchFilter setOwner(String owner, boolean isOrg) {
        this.owner = owner;
        this.ownerIsOrg = isOrg;
        return this;
    }

    public String getTopic() {
        return topic;
    }

    public SearchFilter setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public String getLicense() {
        return license;
    }

    public SearchFilter setLicense(String license) {
        this.license = license;
        return this;
    }

    /** Number of qualifiers that are set (regardless of which search type uses them). */
    public int getActiveCount() {
        int count = 0;
        if (!isBlank(language)) count++;
        if (minStars != null && minStars > 0) count++;
        if (!isBlank(pushedAfter)) count++;
        if (getForkMode() != ForkMode.DEFAULT) count++;
        if (!isBlank(owner)) count++;
        if (!isBlank(topic)) count++;
        if (!isBlank(license)) count++;
        return count;
    }

    public boolean isEmpty() {
        return getActiveCount() == 0;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
