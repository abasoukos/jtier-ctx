package com.groupon.jtier;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

class AttachingExecutor extends AbstractExecutorService {

    private final ExecutorService target;

    private AttachingExecutor(final ExecutorService target) {
        this.target = target;
    }

    static ExecutorService infect(final ExecutorService target) {
        return new AttachingExecutor(target);
    }

    @Override
    public void shutdown() {
        this.target.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return this.target.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return this.target.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return this.target.isTerminated();
    }

    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        return this.target.awaitTermination(timeout, unit);
    }

    @Override
    public void execute(final Runnable command) {
        final Optional<Ctx> oc = Ctx.fromThread();
        if (oc.isPresent()) {
            this.target.execute(() -> {
                try (Ctx ignored = oc.get().attachToThread()) {
                    command.run();
                }
            });
        }
        else {
            this.target.execute(command);
        }
    }
}
