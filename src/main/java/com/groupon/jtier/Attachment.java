package com.groupon.jtier;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class Attachment implements AutoCloseable {
    private static ThreadLocal<Optional<Attachment>> INFECTION = ThreadLocal.withInitial(Optional::empty);

    private final CompletableFuture<Attachment> listener;
    private final Ding ding;

    Attachment(CompletableFuture<Attachment> listener, Ding ding) {
        this.listener = listener;
        this.ding = ding;
    }

    static void update(Ding ding) {
        Optional<Attachment> infection = INFECTION.get();
        if (infection.isPresent()) {
            INFECTION.set(infection.map((old) -> new Attachment(old.listener, ding)));
        }
    }

    static boolean isCurrentThreadAttached() {
        return INFECTION.get().isPresent();
    }

    public Ding getDing() {
        return ding;
    }

    public static Optional<Ding> currentExchange() {
        return INFECTION.get().map(Attachment::getDing);
    }

    public CompletionStage<Attachment> whenDetached() {
        return listener;
    }

    @Override
    public void close() {
        detach();
    }

    static Attachment attachToThread(Ding ding) {
        Attachment i = new Attachment(new CompletableFuture<>(), ding);
        INFECTION.set(Optional.of(i));
        return i;
    }

    static void detach() {
        Optional<Attachment> o = INFECTION.get();
        INFECTION.set(Optional.empty());
        if (o.isPresent()) {
            Attachment i = o.get();
            i.listener.complete(i);
        }
    }

    static Optional<Attachment> get() {
        return INFECTION.get();
    }
}
