package com.groupon.jtier;

import org.immutables.value.Value;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

class Life {

    private final Optional<Life> parent;
    private volatile State state = State.ALIVE;

    private final AtomicReference<Optional<Timeout>> timeout = new AtomicReference<>(Optional.empty());
    private final CompletableFuture<Ding> cancel = new CompletableFuture<>();
    private final CompletableFuture<Ding> finished = new CompletableFuture<>();

    Life(Optional<Life> parent) {
        this.parent = parent;
        if (parent.isPresent()) {
            Life plife = parent.get();
            plife.whenCanceled().thenAccept(cancel::complete);
            plife.whenFinished().thenAccept(finished::complete);
        }
    }

    void cancel(Ding canceledDing) {
        if (state == State.ALIVE) {
            state = State.CANCELLED;
            cancel.complete(canceledDing);
        }
    }

    void finish(Ding finishedDing) {
        if (state == State.ALIVE) {
            state = State.FINISHED;
            finished.complete(finishedDing);
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

    void startTimeout(Ding timerDing, Duration duration, ScheduledExecutorService scheduler) {
        ScheduledFuture<?> future = scheduler.schedule(() -> cancel(timerDing),
                                                       duration.getNano(),
                                                       TimeUnit.NANOSECONDS);
        Timeout t = new TimeoutBuilder().future(future)
                                        .finishAt(duration.addTo(Instant.now()))
                                        .build();
        Optional<Timeout> old = timeout.getAndSet(Optional.of(t));

        // try to cancel as we are replacing the timeout, best effort
        if (old.isPresent()) {
            old.get().future().cancel(false);
        }
    }

    Optional<Duration> timeRemaining() {
        return timeout.get().map((d) -> Duration.between(Instant.now(), d.finishAt()));
    }

    public CompletionStage<Ding> whenCanceled() {
        return cancel;
    }

    public CompletionStage<Ding> whenFinished() {
        return finished;
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
