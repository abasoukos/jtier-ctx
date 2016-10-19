package com.groupon.jtier;

import org.immutables.value.Value;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

class Life {
    private volatile Optional<Timeout> timeout = Optional.empty();
    private volatile State state = State.ALIVE;
    private final List<Runnable> cancelListeners = new ArrayList<>();


    Life(final Optional<Life> parent) {
        if (parent.isPresent()) {
            final Life plife = parent.get();
            plife.onCancel(this::cancel);
        }
    }

    synchronized void cancel() {
        if (this.state == State.ALIVE) {
            this.state = State.CANCELLED;
            this.cancelListeners.forEach(Runnable::run);
            this.cancelListeners.clear();
        }
    }

    synchronized void startTimeout(final Duration duration, final ScheduledExecutorService scheduler) {
        final ScheduledFuture<?> future = scheduler.schedule(this::cancel, duration.getNano(), TimeUnit.NANOSECONDS);
        final Timeout t = new TimeoutBuilder().future(future).finishAt(duration.addTo(Instant.now())).build();
        final Optional<Timeout> old = this.timeout;
        this.timeout = Optional.of(t);

        // try to cancel as we are replacing the timeout, best effort
        if (old.isPresent()) {
            old.get().future().cancel(false);
        }
    }

    synchronized  Optional<Duration> timeRemaining() {
        return this.timeout.map((d) -> Duration.between(Instant.now(), d.finishAt()));
    }

    synchronized boolean isCancelled() {
        return this.state == State.CANCELLED;
    }

    synchronized void onCancel(final Runnable runnable) {
        if (isCancelled()) {
            runnable.run();
            return;
        }
        this.cancelListeners.add(runnable);
    }

    private enum State {
        ALIVE, CANCELLED
    }

    @Value.Immutable
    @Value.Style(visibility = Value.Style.ImplementationVisibility.PRIVATE)
    abstract static class Timeout {
        abstract Temporal finishAt();
        abstract ScheduledFuture<?> future();
    }

}
