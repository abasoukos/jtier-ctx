package com.groupon.jtier;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class LifeEventsTest {

    @Test
    public void testCancelEventFires() throws Exception {
        final Ctx d = Ctx.empty();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean canceled = new AtomicBoolean(false);

        d.onCancel(() -> {
            canceled.set(true);
            latch.countDown();
        });

        d.cancel();

        assertThat(latch.await(20, TimeUnit.MILLISECONDS)).isTrue();
        assertThat(canceled.get()).isTrue();
    }

    @Test
    public void testCancelParentCancelsChildren() throws Exception {
        final Ctx p = Ctx.empty();
        final Ctx c = p.createChild();

        final AtomicBoolean canceled = new AtomicBoolean(false);

        c.onCancel(() -> canceled.set(true));

        p.cancel();
        assertThat(canceled.get()).isTrue();
    }

    @Test
    public void testOnCancelAfterCancelExecutesImmediately() throws Exception {
        final Ctx c = Ctx.empty();
        c.cancel();
        final boolean[] canceled = {false};
        c.onCancel(() -> canceled[0] = true );
        assertThat(canceled[0]).isTrue();
    }
}
