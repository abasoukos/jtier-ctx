package com.groupon.jtier;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.util.Optional;

public class Ding implements AutoCloseable {

    private static final Ding BACKGROUND = new Ding(Optional.empty(), ImmutableMap.of(), false);
    private static final ThreadLocal<Ding> CURRENT = ThreadLocal.withInitial(Ding::empty);

    private final ImmutableMap<Key<?>, Object> values;
    private final transient Optional<Ding> parent;
    private final boolean global;

    /**
     * TODO Parent is to be used for lifecycle only, it should be refactored away once ding lifecycle is grokked better
     */
    private Ding(Optional<Ding> parent, ImmutableMap<Key<?>, Object> values, boolean infected) {
        this.parent = parent;
        this.values = values;
        this.global = infected;
        // TODO hook into lifecycle notifications of parent
    }

    public static Ding empty() {
        return BACKGROUND;
    }

    public Ding infect() {
        Ding child = new Ding(Optional.of(this), values, true);
        CURRENT.set(child);
        return child;
    }

    public void close() {
        if (global) {
            CURRENT.set(this.parent.get());
        }
        else {
            throw new IllegalStateException("No thread local ding to close!");
        }
    }

    public static Ding summonThreadContext() {
        if (CURRENT.get().global) {
            return CURRENT.get();
        }
        else {
            throw new IllegalStateException("No thread local ding has been opened!");
        }
    }

    public <T> Ding with(Key<T> key, T value) {
        ImmutableMap<Key<?>, Object> next = ImmutableMap.<Key<?>, Object>builder().putAll(values)
                                                                                  .put(key, value)
                                                                                  .build();
        Ding child = new Ding(Optional.of(this), next, global);
        if (global) {
            CURRENT.set(child);
        }
        return child;
    }

    public <T> T get(Key<T> key) {
        return key.cast(values.get(key));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ding ding = (Ding) o;
        return global == ding.global &&
                Objects.equal(values, ding.values) &&
                Objects.equal(parent, ding.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(values, parent, global);
    }

    public static <T> Key<T> key(String name, Class<T> type) {
        return new Key<>(type, name);
    }

    public static final class Key<T> {
        private final Class<T> type;
        private final String name;

        private Key(Class<T> type, String name) {
            this.type = type;
            this.name = name;
        }

        public T cast(Object obj) {
            return type.cast(obj);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key<?> key = (Key<?>) o;
            return Objects.equal(type, key.type) &&
                    Objects.equal(name, key.name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(type, name);
        }
    }
}
