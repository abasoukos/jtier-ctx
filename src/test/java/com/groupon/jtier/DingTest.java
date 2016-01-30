package com.groupon.jtier;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DingTest {

    private static final Ding.Key<String> NAME = Ding.key("name", String.class);


    @Test
    public void testKeyOnChildIsNotOnParent() throws Exception {
        Ding root = Ding.empty();
        Ding child = root.with(NAME, "Brian");

        assertThat(child.get(NAME).get()).isEqualTo("Brian");
        assertThat(root.get(NAME)).isEmpty();
    }

    @Test
    public void testExplicitThreadLocalInfection() throws Exception {
        Ding root = Ding.empty();

        try (Attachment i = root.attachToThread()) {
            assertThat(Attachment.isCurrentThreadAttached()).isTrue();
            assertThat(Attachment.currentExchange()).isPresent();

            Ding magic = Attachment.currentExchange().get();
            assertThat(magic).isEqualTo(i.getDing());
        }

        assertThat(Attachment.isCurrentThreadAttached()).isFalse();
    }

    @Test
    public void testInfectionIsContagious() throws Exception {
        try (Attachment child = Ding.empty().attachToThread()) {
            child.getDing().with(NAME, "Brian");

            assertThat(Attachment.currentExchange().get().get(NAME).get()).isEqualTo("Brian");
        }
    }

    @Test
    public void testThreadLocalNotAllowedWithoutInject() throws Exception {
        assertThat(Attachment.currentExchange()).isEmpty();
    }

    @Test
    public void testDoubleInfection() throws Exception {
        try (Attachment i = Ding.empty().attachToThread()) {
            try (Attachment i2 = i.getDing().attachToThread()) {
                i2.getDing().with(NAME, "Ian");

                assertThat(Attachment.currentExchange().get().get(NAME).get()).isEqualTo("Ian");
            }
            // closing dis-infects the thread, period
            assertThat(Attachment.isCurrentThreadAttached()).isFalse();

        }

        assertThat(Attachment.isCurrentThreadAttached()).isFalse();
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
