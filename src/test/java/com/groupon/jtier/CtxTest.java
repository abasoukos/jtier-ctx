package com.groupon.jtier;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CtxTest {

    private static final Ctx.Key<String> NAME = Ctx.key("name", String.class);


    @Test
    public void testKeyOnChildIsNotOnParent() throws Exception {
        Ctx root = Ctx.empty();
        Ctx child = root.with(NAME, "Brian");

        assertThat(child.get(NAME).get()).isEqualTo("Brian");
        assertThat(root.get(NAME)).isEmpty();
    }

    @Test
    public void testExplicitThreadLocalInfection() throws Exception {
        Ctx root = Ctx.empty();

        try (CtxAttachment i = root.attachToThread()) {
            assertThat(CtxAttachment.isCurrentThreadAttached()).isTrue();
            assertThat(CtxAttachment.currentCtx()).isPresent();

            Ctx magic = CtxAttachment.currentCtx().get();
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
        Ctx brian = Ctx.empty().with(NAME, "Brian");
        Ctx eric = brian.with(NAME, "Eric");
        Ctx keith = brian.with(NAME, "Keith");

        brian.cancel();

        assertThat(brian.isCancelled()).isTrue();
        assertThat(eric.isCancelled()).isTrue();
        assertThat(keith.isCancelled()).isTrue();
    }

    @Test
    public void testCancelOnTree() throws Exception {
        Ctx tip = Ctx.empty().with(NAME, "Tip");

        /*
        (tip
          (brian
            ((ian
              (panda))
             (cora
               (sprinkle)))))
         */

        Ctx brian = tip.createChild().with(NAME, "Brian");
        Ctx ian = brian.createChild().with(NAME, "Ian");
        Ctx panda = ian.createChild().with(NAME, "Panda");
        Ctx cora = brian.createChild().with(NAME, "Cora");
        Ctx sprinkle = cora.createChild().with(NAME, "Sprinkle Kitty");

        brian.cancel();

        assertThat(brian.isCancelled()).isTrue();
        assertThat(ian.isCancelled()).isTrue();
        assertThat(cora.isCancelled()).isTrue();
        assertThat(panda.isCancelled()).isTrue();
        assertThat(sprinkle.isCancelled()).isTrue();
        assertThat(tip.isCancelled()).isFalse();
    }
}
