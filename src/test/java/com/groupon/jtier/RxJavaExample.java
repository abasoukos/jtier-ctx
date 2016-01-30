package com.groupon.jtier;

import org.junit.Test;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class RxJavaExample {

    @Test
    public void testUnsubscribeOnCancel() throws Exception {
        TestScheduler ticker = Schedulers.test();

        Ding ding = Ding.empty();


        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicInteger counter = new AtomicInteger();

        Observable os = Observable.interval(10, TimeUnit.MILLISECONDS, ticker);

        Subscription s = os.subscribe((i) -> counter.getAndIncrement(),
                                      (e) -> failed.set(true),
                                      () -> completed.set(true));

        // unsubscribe when ding is cancelled
        ding.whenCancelled().thenRun(s::unsubscribe);

        ticker.advanceTimeBy(10, TimeUnit.MILLISECONDS);
        ticker.triggerActions();
        assertThat(counter.get()).isEqualTo(1);

        ticker.advanceTimeBy(10, TimeUnit.MILLISECONDS);
        ticker.triggerActions();

        assertThat(counter.get()).isEqualTo(2);

        // should led to unsubscribe
        ding.cancel();

        ticker.advanceTimeBy(10, TimeUnit.MILLISECONDS);
        ticker.triggerActions();

        assertThat(counter.get()).isEqualTo(2);

        assertThat(completed.get()).isFalse();
        assertThat(failed.get()).isFalse();
    }
}
