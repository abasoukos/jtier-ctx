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
        final TestScheduler ticker = Schedulers.test();

        final Ctx ctx = Ctx.empty();

        final AtomicBoolean failed = new AtomicBoolean(false);
        final AtomicBoolean completed = new AtomicBoolean(false);
        final AtomicInteger counter = new AtomicInteger();

        final Observable os = Observable.interval(10, TimeUnit.MILLISECONDS, ticker);

        final Subscription s = os.subscribe((i) -> counter.getAndIncrement(),
                                            (e) -> failed.set(true),
                                            () -> completed.set(true));

        // unsubscribe when currentExchange is cancelled
        ctx.onCancel(s::unsubscribe);

        // receive first event
        ticker.advanceTimeBy(10, TimeUnit.MILLISECONDS);
        ticker.triggerActions();
        assertThat(counter.get()).isEqualTo(1);

        // receive second event
        ticker.advanceTimeBy(10, TimeUnit.MILLISECONDS);
        ticker.triggerActions();
        assertThat(counter.get()).isEqualTo(2);

        // should led to unsubscribe, not receive future elements
        ctx.cancel();

        // advance time enough to trigger third event, but we should not get it
        ticker.advanceTimeBy(10, TimeUnit.MILLISECONDS);
        ticker.triggerActions();
        assertThat(counter.get()).isEqualTo(2);

        // verify it was not completed or failed
        assertThat(completed.get()).isFalse();
        assertThat(failed.get()).isFalse();
    }
}
