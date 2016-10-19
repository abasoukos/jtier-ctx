package com.groupon.jtier;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

public class Ctx implements AutoCloseable {

    private static final ThreadLocal<Optional<Ctx>> ATTACHED = ThreadLocal.withInitial(Optional::empty);

    private final Life life;
    private final Map<Key<?>, Object> values;

    private final List<Runnable> attachListeners = new CopyOnWriteArrayList<>();
    private final List<Runnable> detachListeners = new CopyOnWriteArrayList<>();

    private Ctx(final Life life, final Map<Key<?>, Object> values) {
        this.life = life;
        this.values = values;
    }

    public static Ctx empty() {
        return new Ctx(new Life(Optional.empty()), ImmutableMap.of());
    }

    public static Optional<Ctx> fromThread() {
        return ATTACHED.get();
    }

    /**
     * Forcibly detach
     */
    public static void cleanThread() {
        final Optional<Ctx> oc = ATTACHED.get();
        oc.ifPresent(Ctx::close);
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

    public Ctx attachToThread() {
        ATTACHED.set(Optional.of(this));
        this.attachListeners.forEach(Runnable::run);
        return this;
    }

    public void runAttached(final Runnable r) {
        try (Ctx ignored = attachToThread()) {
            r.run();
        }
    }

    public <T> Ctx with(final Key<T> key, final T value) {
        final Map<Key<?>, Object> next = new HashMap<>();
        next.putAll(this.values);
        next.put(key, value);

        return new Ctx(this.life, next);
    }

    public Ctx createChild() {
        return new Ctx(new Life(Optional.of(this.life)), this.values);
    }

    public <T> Optional<T> get(final Key<T> key) {
        if (this.values.containsKey(key)) {
            return Optional.of(key.cast(this.values.get(key)));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Ctx ctx = (Ctx) o;
        return Objects.equal(this.life, ctx.life) &&
                Objects.equal(this.values, ctx.values);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.life, this.values);
    }

    public void startTimeout(final Duration duration, final ScheduledExecutorService scheduler) {
        this.life.startTimeout(duration, scheduler);
    }

    public Optional<Duration> getApproximateTimeRemaining() {
        return this.life.timeRemaining();
    }

    @Override
    public void close() {
        final Optional<Ctx> o = ATTACHED.get();
        if (o.isPresent()) {
            final Ctx attached = o.get();
            if (attached != this) {
                // TODO write test for this
                throw new IllegalStateException("Attempt to detach different context from current thread");
            }
            ATTACHED.set(Optional.empty());
            this.detachListeners.forEach(Runnable::run);
        } else {
            throw new IllegalStateException("Attempt to detach context from unattached thread");
        }
    }


    public void cancel() {
        this.life.cancel();
    }

    public boolean isCancelled() {
        return this.life.isCancelled();
    }

    public void onDetach(final Runnable runnable) {
        this.detachListeners.add(runnable);
    }

    public void onAttach(final Runnable runnable) {
        this.attachListeners.add(runnable);
    }

    public void onCancel(final Runnable runnable) {
        this.life.onCancel(runnable);
    }


    public static final class Key<T> {
        private final Class<T> type;
        private final String name;

        private Key(final Class<T> type, final String name) {
            this.type = type;
            this.name = name;
        }

        public T cast(final Object obj) {
            return this.type.cast(obj);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final Key<?> key = (Key<?>) o;
            return Objects.equal(this.type, key.type) &&
                    Objects.equal(this.name, key.name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.type, this.name);
        }
    }
}
