package com.groupon.jtier;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DingTest {

    @Test
    public void testKeyOnChildIsNotOnParent() throws Exception {
        Ding root = Ding.empty();
        Ding.Key<String> name = Ding.key("name", String.class);
        Ding child = root.with(name, "Brian");

        assertThat(child.get(name)).isEqualTo("Brian");
        assertThat(root.get(name)).isNull();
    }

    @Test
    public void testExplicitThreadLocalPropagation() throws Exception {
        Ding root = Ding.empty();

        try (Ding child = root.infect()) {
            Ding magic = Ding.summonThreadContext();
            assertThat(magic).isEqualTo(child);
            assertThat(magic).isNotEqualTo(root);
        } // ding.close() called by the try block
    }

    @Test(expected = IllegalStateException.class)
    public void testThreadLocalNotAllowedWithoutInject() throws Exception {
        Ding.summonThreadContext();
    }
}
