package com.groupon.jtier;

import org.immutables.value.Value;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

class Life {

    private final Optional<Life> parent;
    private volatile State state = State.ALIVE;

    private final AtomicReference<Optional<Timeout>> timeout = new AtomicReference<>(Optional.empty());

    Life(Optional<Life> parent) {
        this.parent = parent;
    }

    void cancel() {
        if (state == State.ALIVE) {
            state = State.CANCELLED;
        }
    }

    void finish() {
        if (state == State.ALIVE) {
            state = State.FINISHED;
        }
    }

    boolean isCancelled() {
        return state == State.CANCELLED || parent.map(Life::isCancelled).orElse(false);
    }

    boolean isFinished() {
        return state == State.FINISHED || parent.map(Life::isFinished).orElse(false);
    }

    boolean isAlive() {
        return state == State.ALIVE || parent.map(Life::isAlive).orElse(false);
    }

    void startTimeout(Duration duration, ScheduledExecutorService scheduler) {
        ScheduledFuture<?> future = scheduler.schedule((Runnable) this::cancel,
                                                       duration.getNano(),
                                                       TimeUnit.NANOSECONDS);
        Timeout t = new TimeoutBuilder().future(future)
                                        .finishAt(duration.addTo(Instant.now()))
                                        .build();
        Optional<Timeout> old = timeout.getAndSet(Optional.of(t));
        if (old.isPresent()) {
            // try to cancel as we are replacing the timeout, best effort
            old.get().future().cancel(false);
        }
    }

    Optional<Duration> timeRemaining() {
        return timeout.get().map((d) -> Duration.between(Instant.now(), d.finishAt()));
    }

    private enum State {
        ALIVE, CANCELLED, FINISHED
    }

    @Value.Immutable
    @Value.Style(visibility = Value.Style.ImplementationVisibility.PRIVATE)
    abstract static class Timeout {

        abstract Temporal finishAt();

        abstract ScheduledFuture<?> future();
    }

}
