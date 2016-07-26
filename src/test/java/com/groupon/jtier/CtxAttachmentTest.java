package com.groupon.jtier;

import com.google.common.collect.Lists;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class CtxAttachmentTest {

    @Test
    public void testInfect() throws Exception {
        final ExecutorService pool = CtxAttachment.wrap(Executors.newFixedThreadPool(1));
        final List<Integer> isCurrentThreadAttached = Lists.newArrayList();
        final Runnable command = () -> {
            if (CtxAttachment.isCurrentThreadAttached()) {
                isCurrentThreadAttached.add(1);
            }
        };

        try (CtxAttachment _i = Ctx.empty().attachToThread()) {
            pool.execute(command);
        }
        pool.awaitTermination(1, TimeUnit.SECONDS);
        assertThat(isCurrentThreadAttached).hasSize(1);
    }
}