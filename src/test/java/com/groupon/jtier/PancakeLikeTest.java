package com.groupon.jtier;

import org.junit.Test;
import org.skife.clocked.ClockedExecutorService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;


public class PancakeLikeTest {

    @Test
    public void testFoo() throws Exception {
        final Ctx ctx = Ctx.empty();

        final ClockedExecutorService clock = new ClockedExecutorService();

        final CompletableFuture<String> fs = new CompletableFuture<>();
        final CompletableFuture<Integer> fi = new CompletableFuture<>();
        final CompletableFuture<Double> fd = new CompletableFuture<>();

        clock.schedule(() -> fs.complete("string"), 100, TimeUnit.MILLISECONDS);
        clock.schedule(() -> fi.complete(7), 200, TimeUnit.MILLISECONDS);
        clock.schedule(() -> fd.complete(4.2), 300, TimeUnit.MILLISECONDS);

        ctx.onCancel(() -> fs.cancel(false));
        ctx.onCancel(() -> fi.cancel(false));
        ctx.onCancel(() -> fd.cancel(false));

        clock.advance(150, TimeUnit.MILLISECONDS).get();
        ctx.cancel();
        clock.advance(400, TimeUnit.MILLISECONDS).get();

        assertThat(fs).isCompleted();
        assertThat(fi).isCancelled();
        assertThat(fd).isCancelled();
    }
}
