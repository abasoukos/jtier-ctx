package com.groupon.jtier;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class LifeEventsTest {

    @Test
    public void testCancelEventFires() throws Exception {
        Ctx d = Ctx.empty();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean canceled = new AtomicBoolean(false);

        d.whenCancelled().thenAccept((_d) -> {
            canceled.set(true);
            latch.countDown();
        });

        d.cancel();

        assertThat(latch.await(20, TimeUnit.MILLISECONDS)).isTrue();
        assertThat(canceled.get()).isTrue();
    }

    @Test
    public void testCancelParentCancelsChildren() throws Exception {
        Ctx p = Ctx.empty();
        Ctx c = p.createChild();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean canceled = new AtomicBoolean(false);

        c.whenCancelled().thenAccept((canceledDing) -> {
            canceled.set(true);
            assertThat(canceledDing).isSameAs(p);
            latch.countDown();
        });

        p.cancel();

        assertThat(latch.await(20, TimeUnit.MILLISECONDS)).isTrue();
        assertThat(canceled.get()).isTrue();
    }

    @Test
    public void testFinishEventFires() throws Exception {
        Ctx d = Ctx.empty();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean canceled = new AtomicBoolean(false);

        d.whenFinished().thenAccept((_d) -> {
            canceled.set(true);
            latch.countDown();
        });

        d.finish();

        assertThat(latch.await(20, TimeUnit.MILLISECONDS)).isTrue();
        assertThat(canceled.get()).isTrue();
    }

    @Test
    public void testFinishParentCancelsChildren() throws Exception {
        Ctx p = Ctx.empty();
        Ctx c = p.createChild();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean canceled = new AtomicBoolean(false);

        c.whenFinished().thenAccept((canceledDing) -> {
            canceled.set(true);
            assertThat(canceledDing).isSameAs(p);
            latch.countDown();
        });

        p.finish();

        assertThat(latch.await(20, TimeUnit.MILLISECONDS)).isTrue();
        assertThat(canceled.get()).isTrue();
    }

}
