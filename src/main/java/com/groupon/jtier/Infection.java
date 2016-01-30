package com.groupon.jtier;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

public class Infection implements AutoCloseable {
    private static ThreadLocal<Optional<Infection>> INFECTION = ThreadLocal.withInitial(Optional::empty);

    private final CompletableFuture<Infection> listener;
    private final Ctx ctx;

    Infection(CompletableFuture<Infection> listener, Ctx ctx) {
        this.listener = listener;
        this.ctx = ctx;
    }

    static void update(Ctx ctx) {
        Optional<Infection> infection = INFECTION.get();
        if (infection.isPresent()) {
            INFECTION.set(infection.map((old) -> new Infection(old.listener, ctx)));
        }
    }

    public static boolean isCurrentThreadInfected() {
        return INFECTION.get().isPresent();
    }

    public Ctx getCtx() {
        return ctx;
    }

    public static Optional<Ctx> currentCtx() {
        return currentInfection().map(Infection::getCtx);
    }

    public static Optional<Infection> currentInfection() {
        return INFECTION.get();
    }

    public CompletionStage<Infection> whenDetached() {
        return listener;
    }

    @Override
    public void close() {
        disinfectThread();
    }

    static Infection infectThread(Ctx ctx) {
        Infection i = new Infection(new CompletableFuture<>(), ctx);
        INFECTION.set(Optional.of(i));
        return i;
    }

    static void disinfectThread() {
        Optional<Infection> o = INFECTION.get();
        INFECTION.set(Optional.empty());
        if (o.isPresent()) {
            Infection i = o.get();
            i.listener.complete(i);
        }
    }

    static Optional<Infection> getCurrentInfection() {
        return INFECTION.get();
    }

    /**
     * Creates an ExecutorService which propagates attached contexts.
     *
     * If a context is attached at the time of job submission, that context is saved
     * and attached before execution of the job, then detached after.
     */
    static ExecutorService wrap(ExecutorService exec) {
        return AttachingExecutor.infect(exec);
    }
}
