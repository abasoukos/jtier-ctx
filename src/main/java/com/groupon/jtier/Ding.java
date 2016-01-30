package com.groupon.jtier;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;

public class Ding {
    private final Life life;
    private final Map<Key<?>, Object> values;

    private Ding(Life life, Map<Key<?>, Object> values) {
        this.life = life;
        this.values = values;
    }

    public void cancel() {
        life.cancel(this);
    }

    public void finish() {
        life.finish(this);
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

    public Attachment attachToThread() {
        return Attachment.attachToThread(this);
    }

    public <T> Ding with(Key<T> key, T value) {
        Map<Key<?>, Object> next = new HashMap<>();
        next.putAll(this.values);
        next.put(key, value);

        Ding child = new Ding(life, next);
        if (Attachment.isCurrentThreadAttached()) {
            Attachment.update(child);
        }
        return child;
    }

    public Ding createChild() {
        return new Ding(new Life(Optional.of(life)), values);
    }

    public <T> Optional<T> get(Key<T> key) {
        if (values.containsKey(key)) {
            return Optional.of(key.cast(values.get(key)));
        }
        else {
            return Optional.empty();
        }
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

    public void startTimeout(Duration duration, ScheduledExecutorService scheduler) {
        life.startTimeout(this, duration, scheduler);
    }

    public Optional<Duration> getApproximateTimeRemaining() {
        return life.timeRemaining();
    }

    public CompletionStage<Ding> whenCancelled() {
        return life.whenCanceled();
    }

    public CompletionStage<Ding> whenFinished() {
        return life.whenFinished();
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
