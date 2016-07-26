package com.groupon.jtier;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

public class Ctx {
    private final Life life;
    private final Map<Key<?>, Object> values;

    private Ctx(final Life life, final Map<Key<?>, Object> values) {
        this.life = life;
        this.values = values;
    }

    public static Ctx empty() {
        return new Ctx(new Life(Optional.empty()), ImmutableMap.of());
    }

    public static Optional<Ctx> fromThread() {

        return CtxAttachment.currentCtx();

    }

    public static <T> Key<T> key(final String name, final Class<T> type) {
        return new Key<>(type, name);
    }

    /**
     * Creates an ExecutorService which propagates attached contexts.
     *
     * If a context is attached at the time of job submission, that context is saved and attached
     * before execution of the job, then detached after.
     */
    public static ExecutorService createPropagatingExecutor(final ExecutorService exec) {
        return AttachingExecutor.infect(exec);
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

    public CtxAttachment attachToThread() {
        return CtxAttachment.attachToCurrentThread(this);
    }

    public <T> Ctx with(final Key<T> key, final T value) {
        final Map<Key<?>, Object> next = new HashMap<>();
        next.putAll(this.values);
        next.put(key, value);

        return new Ctx(life, next);
    }

    public Ctx createChild() {
        return new Ctx(new Life(Optional.of(life)), values);
    }

    public <T> Optional<T> get(final Key<T> key) {
        if (values.containsKey(key)) {
            return Optional.of(key.cast(values.get(key)));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Ctx ctx = (Ctx) o;
        return Objects.equal(life, ctx.life) &&
                Objects.equal(values, ctx.values);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(life, values);
    }

    public void startTimeout(final Duration duration, final ScheduledExecutorService scheduler) {
        life.startTimeout(this, duration, scheduler);
    }

    public Optional<Duration> getApproximateTimeRemaining() {
        return life.timeRemaining();
    }

    public CompletionStage<Ctx> whenCancelled() {
        return life.whenCanceled();
    }

    public CompletionStage<Ctx> whenFinished() {
        return life.whenFinished();
    }

    public static final class Key<T> {
        private final Class<T> type;
        private final String name;

        private Key(final Class<T> type, final String name) {
            this.type = type;
            this.name = name;
        }

        public T cast(final Object obj) {
            return type.cast(obj);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final Key<?> key = (Key<?>) o;
            return Objects.equal(type, key.type) &&
                    Objects.equal(name, key.name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(type, name);
        }
    }
}
