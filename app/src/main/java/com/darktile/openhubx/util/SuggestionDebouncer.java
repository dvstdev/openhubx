package com.darktile.openhubx.util;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;

/** Flat (non-growing) debounce for live search suggestions. */
public final class SuggestionDebouncer {

    public static final long DEBOUNCE_MS = 350;

    private SuggestionDebouncer() {
    }

    /**
     * Emits the latest text once input has been quiet for {@link #DEBOUNCE_MS},
     * skipping repeats of the same text.
     */
    public static Observable<String> debounce(Observable<String> textChanges, Scheduler scheduler) {
        return textChanges
                .map(s -> s == null ? "" : s.trim())
                .debounce(DEBOUNCE_MS, TimeUnit.MILLISECONDS, scheduler)
                .distinctUntilChanged();
    }
}
