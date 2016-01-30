package com.groupon.jtier;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class Infection implements AutoCloseable {
    private static ThreadLocal<Optional<Infection>> INFECTION = ThreadLocal.withInitial(Optional::empty);

    private final CompletableFuture<Infection> listener;
    private final Ding ding;

    Infection(CompletableFuture<Infection> listener, Ding ding) {
        this.listener = listener;
        this.ding = ding;
    }

    static void update(Ding ding) {
        Optional<Infection> infection = INFECTION.get();
        if (infection.isPresent()) {
            INFECTION.set(infection.map((old) -> new Infection(old.listener, ding)));
        }
    }

    static boolean isCurrentThreadInfected() {
        return INFECTION.get().isPresent();
    }

    public Ding getDing() {
        return ding;
    }

    public static Optional<Ding> ding() {
        return INFECTION.get().map(Infection::getDing);
    }

    public CompletionStage<Infection> whenCured() {
        return listener;
    }

    @Override
    public void close() {
        cure();
    }

    static Infection infectThread(Ding ding) {
        Infection i = new Infection(new CompletableFuture<>(), ding);
        INFECTION.set(Optional.of(i));
        return i;
    }

    static void cure() {
        Optional<Infection> o = INFECTION.get();
        INFECTION.set(Optional.empty());
        if (o.isPresent()) {
            Infection i = o.get();
            i.listener.complete(i);
        }
    }

    static Optional<Infection> get() {
        return INFECTION.get();
    }
}
