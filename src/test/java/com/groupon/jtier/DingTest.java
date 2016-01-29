package com.groupon.jtier;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DingTest {

    private static final Ding.Key<String> NAME = Ding.key("name", String.class);


    @Test
    public void testKeyOnChildIsNotOnParent() throws Exception {
        Ding root = Ding.empty();
        Ding child = root.with(NAME, "Brian");

        assertThat(child.get(NAME)).isEqualTo("Brian");
        assertThat(root.get(NAME)).isNull();
    }

    @Test
    public void testExplicitThreadLocalInfection() throws Exception {
        Ding root = Ding.empty();

        try (Ding child = root.infectThread()) {
            assertThat(Ding.isCurrentThreadInfected()).isTrue();

            assertThat(Ding.summonThreadContext()).isPresent();

            Ding magic = Ding.summonThreadContext().get();

            assertThat(magic).isEqualTo(child);
        }

        assertThat(Ding.isCurrentThreadInfected()).isFalse();
    }

    @Test
    public void testInfectionIsContagious() throws Exception {
        try (Ding child = Ding.empty().infectThread()) {
            child.with(NAME, "Brian");

            assertThat(Ding.summonThreadContext().get().get(NAME)).isEqualTo("Brian");
        }
    }

    @Test
    public void testThreadLocalNotAllowedWithoutInject() throws Exception {
        assertThat(Ding.summonThreadContext()).isEmpty();
    }

    @Test
    public void testDoubleInfection() throws Exception {
        try (Ding _child = Ding.empty().infectThread()) {
            try (Ding grand = _child.infectThread()) {
                grand.with(NAME, "Ian");

                assertThat(Ding.summonThreadContext().get().get(NAME)).isEqualTo("Ian");
            }
            // closing dis-infects the thread, period
            assertThat(Ding.isCurrentThreadInfected()).isFalse();

        }

        assertThat(Ding.isCurrentThreadInfected()).isFalse();
    }

    @Test
    public void testCancelOnPeers() throws Exception {
        Ding brian = Ding.empty().with(NAME, "Brian");
        Ding eric = brian.with(NAME, "Eric");
        Ding keith = brian.with(NAME, "Keith");

        brian.cancel();

        assertThat(brian.isCancelled()).isTrue();
        assertThat(eric.isCancelled()).isTrue();
        assertThat(keith.isCancelled()).isTrue();
    }

    @Test
    public void testCancelOnTree() throws Exception {
        Ding tip = Ding.empty().with(NAME, "Tip");

        /*
        (tip
          (brian
            ((ian
              (panda))
             (cora
               (sprinkle)))))
         */

        Ding brian = tip.createChild().with(NAME, "Brian");
        Ding ian = brian.createChild().with(NAME, "Ian");
        Ding panda = ian.createChild().with(NAME, "Panda");
        Ding cora = brian.createChild().with(NAME, "Cora");
        Ding sprinkle = cora.createChild().with(NAME, "Sprinkle Kitty");

        brian.cancel();

        assertThat(brian.isCancelled()).isTrue();
        assertThat(ian.isCancelled()).isTrue();
        assertThat(cora.isCancelled()).isTrue();
        assertThat(panda.isCancelled()).isTrue();
        assertThat(sprinkle.isCancelled()).isTrue();
        assertThat(tip.isCancelled()).isFalse();
    }
}
