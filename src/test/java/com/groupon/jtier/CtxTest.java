package com.groupon.jtier;

import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class CtxTest {

    private static final Ctx.Key<String> NAME = Ctx.key("name", String.class);


    @Test
    public void testKeyOnChildIsNotOnParent() throws Exception {
        final Ctx root = Ctx.empty();
        final Ctx child = root.with(NAME, "Brian");

        assertThat(child.get(NAME).get()).isEqualTo("Brian");
        assertThat(root.get(NAME)).isEmpty();
    }

    @Test
    public void testExplicitThreadLocalInfection() throws Exception {
        final Ctx root = Ctx.empty();

        try (CtxAttachment i = root.attachToThread()) {
            assertThat(CtxAttachment.isCurrentThreadAttached()).isTrue();
            assertThat(CtxAttachment.currentCtx()).isPresent();

            final Ctx magic = CtxAttachment.currentCtx().get();
            assertThat(magic).isEqualTo(i.getCtx());
        }

        assertThat(CtxAttachment.isCurrentThreadAttached()).isFalse();
    }

    @Test
    public void testThreadLocalNotAllowedWithoutInject() throws Exception {
        assertThat(CtxAttachment.currentCtx()).isEmpty();
    }

    @Test
    public void testCancelOnPeers() throws Exception {
        final Ctx brian = Ctx.empty().with(NAME, "Brian");
        final Ctx eric = brian.with(NAME, "Eric");
        final Ctx keith = brian.with(NAME, "Keith");

        brian.cancel();

        assertThat(brian.isCancelled()).isTrue();
        assertThat(eric.isCancelled()).isTrue();
        assertThat(keith.isCancelled()).isTrue();
    }

    @Test
    public void testCancelOnTree() throws Exception {
        final Ctx tip = Ctx.empty().with(NAME, "Tip");

        /*
        (tip
          (brian
            ((ian
              (panda))
             (cora
               (sprinkle)))))
         */

        final Ctx brian = tip.createChild().with(NAME, "Brian");
        final Ctx ian = brian.createChild().with(NAME, "Ian");
        final Ctx panda = ian.createChild().with(NAME, "Panda");
        final Ctx cora = brian.createChild().with(NAME, "Cora");
        final Ctx sprinkle = cora.createChild().with(NAME, "Sprinkle Kitty");

        brian.cancel();

        assertThat(brian.isCancelled()).isTrue();
        assertThat(ian.isCancelled()).isTrue();
        assertThat(cora.isCancelled()).isTrue();
        assertThat(panda.isCancelled()).isTrue();
        assertThat(sprinkle.isCancelled()).isTrue();
        assertThat(tip.isCancelled()).isFalse();
    }

    @Test
    public void testPropagateFromThread() throws Exception {
        final ExecutorService pool = Ctx.createPropagatingExecutor(Executors.newFixedThreadPool(1));
        final AtomicReference<Boolean> isCurrentThreadAttached = new AtomicReference(false);

        final Runnable command = () -> {
            if (CtxAttachment.isCurrentThreadAttached()) {
                isCurrentThreadAttached.set(true);
            }
        };

        try (CtxAttachment _i = Ctx.empty().attachToThread()) {
            pool.execute(command);
        }
        pool.awaitTermination(1, TimeUnit.SECONDS);
        assertThat(isCurrentThreadAttached.get()).isTrue();
    }
}
