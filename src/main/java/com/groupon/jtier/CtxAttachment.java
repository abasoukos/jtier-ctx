package com.groupon.jtier;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

public class CtxAttachment implements AutoCloseable {
    private static ThreadLocal<Optional<CtxAttachment>> ATTACHMENT = ThreadLocal.withInitial(Optional::empty);

    private final CompletableFuture<CtxAttachment> listener;
    private final Ctx ctx;

    CtxAttachment(CompletableFuture<CtxAttachment> listener, Ctx ctx) {
        this.listener = listener;
        this.ctx = ctx;
    }

    static void update(Ctx ctx) {
        Optional<CtxAttachment> infection = ATTACHMENT.get();
        if (infection.isPresent()) {
            ATTACHMENT.set(infection.map((old) -> new CtxAttachment(old.listener, ctx)));
        }
    }

    static boolean isCurrentThreadAttached() {
        return ATTACHMENT.get().isPresent();
    }

    public Ctx getCtx() {
        return ctx;
    }

    static Optional<Ctx> currentCtx() {
        return ATTACHMENT.get().map(CtxAttachment::getCtx);
    }

    public CompletionStage<CtxAttachment> whenDetached() {
        return listener;
    }

    @Override
    public void close() {
        disinfectThread();
    }

    static CtxAttachment attachToCurrentThread(Ctx ctx) {
        CtxAttachment i = new CtxAttachment(new CompletableFuture<>(), ctx);
        ATTACHMENT.set(Optional.of(i));
        return i;
    }

    static void disinfectThread() {
        Optional<CtxAttachment> o = ATTACHMENT.get();
        ATTACHMENT.set(Optional.empty());
        if (o.isPresent()) {
            CtxAttachment i = o.get();
            i.listener.complete(i);
        }
    }

    static Optional<CtxAttachment> getCurrentAttachment() {
        return ATTACHMENT.get();
    }

    /**
     * Creates an ExecutorService which propagates attached contexts.
     *
     * If a context is attached at the time of job submission, that context is saved
     * and attached before execution of the job, then detached after.
     */
    public static ExecutorService wrap(final ExecutorService exec) {
        return AttachingExecutor.infect(exec);
    }
}
