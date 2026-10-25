package com.darktile.openhubx.util;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class SuggestionDebouncerTest {

    @Test
    public void emitsOnlyAfterFlatQuietPeriod() {
        TestScheduler scheduler = new TestScheduler();
        PublishSubject<String> input = PublishSubject.create();
        TestObserver<String> out = SuggestionDebouncer.debounce(input, scheduler).test();

        input.onNext("a");
        scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);
        input.onNext("ab");
        scheduler.advanceTimeBy(SuggestionDebouncer.DEBOUNCE_MS - 1, TimeUnit.MILLISECONDS);
        out.assertNoValues();
        scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);
        out.assertValues("ab");

        // Same delay again (flat, not growing); repeated text is suppressed.
        input.onNext("ab ");
        scheduler.advanceTimeBy(SuggestionDebouncer.DEBOUNCE_MS, TimeUnit.MILLISECONDS);
        input.onNext("abc");
        scheduler.advanceTimeBy(SuggestionDebouncer.DEBOUNCE_MS, TimeUnit.MILLISECONDS);
        out.assertValues("ab", "abc");
    }
}
