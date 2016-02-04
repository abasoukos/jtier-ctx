package com.groupon.jtier;

import org.junit.Test;
import org.skife.clocked.ClockedExecutorService;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeoutTest {

    @Test
    public void testTimeoutCancels() throws Exception {
        ClockedExecutorService clock = new ClockedExecutorService();

        Ctx ctx = Ctx.empty();
        ctx.startTimeout(Duration.ofMillis(100), clock);

        clock.advance(102, TimeUnit.MILLISECONDS);

        assertThat(ctx.isCancelled()).isTrue();
    }

    @Test
    public void testTimeRemaining() throws Exception {
        ClockedExecutorService clock = new ClockedExecutorService();

        Ctx ctx = Ctx.empty();
        ctx.startTimeout(Duration.ofMillis(100), clock);

        Thread.sleep(10);
        Optional<Duration> tr = ctx.getApproximateTimeRemaining();
        assertThat(tr).isPresent();
        Duration d = tr.get();

        assertThat(d.toMillis()).isLessThanOrEqualTo(90);

    }

}
