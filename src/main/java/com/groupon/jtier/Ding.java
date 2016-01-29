package com.groupon.jtier;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

public class Ding implements AutoCloseable {

    private static final ThreadLocal<Optional<Ding>> INFECTION = ThreadLocal.withInitial(Optional::empty);

    private final Life life;
    private final Map<Key<?>, Object> values;

    private Ding(Life life, Map<Key<?>, Object> values) {
        this.life = life;
        this.values = values;
    }

    public void cancel() {
        life.cancel();
    }

    public void finish() {
        life.finish();
    }

    public boolean isCancelled() {
        return life.isCancelled();
    }

    public boolean isFinished() {
        return life.isFinished();
    }

    public boolean isAlive() {
        return life.isAlive();
    }

    public static Ding empty() {
        return new Ding(new Life(Optional.empty()), ImmutableMap.of());
    }

    public Ding infectThread() {
        INFECTION.set(Optional.of(this));
        return this;
    }

    /**
     * alias for this.disinfect() to implement AutoCloseable
     */
    public void close() {
        disinfect();
    }

    /**
     * Un-infect the thread
     */
    public void disinfect() {
        INFECTION.set(Optional.empty());
    }

    public static Optional<Ding> summonThreadContext() {
        return INFECTION.get();
    }

    public <T> Ding with(Key<T> key, T value) {
        Map<Key<?>, Object> next = new HashMap<>();
        next.putAll(this.values);
        next.put(key, value);

        Ding child = new Ding(life, next);
        if (isCurrentThreadInfected()) {
            return child.infectThread();
        }
        else {
            return child;
        }
    }

    public Ding createChild() {
        // TODO create new notification channel
        return new Ding(new Life(Optional.of(life)), values);
    }

    public <T> T get(Key<T> key) {
        return key.cast(values.get(key));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ding ding = (Ding) o;
        return Objects.equal(life, ding.life) &&
                Objects.equal(values, ding.values);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(life, values);
    }

    public static <T> Key<T> key(String name, Class<T> type) {
        return new Key<>(type, name);
    }

    public static boolean isCurrentThreadInfected() {
        return INFECTION.get().isPresent();
    }

    public void startTimeout(Duration duration, ScheduledExecutorService scheduler) {
        life.startTimeout(duration, scheduler);
    }

    public Optional<Duration> getApproximateTimeRemaining() {
        return life.timeRemaining();
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
