package com.groupon.jtier;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class InfectingExecutor extends AbstractExecutorService {

    private final ExecutorService target;

    private InfectingExecutor(ExecutorService target) {
        this.target = target;
    }

    public static ExecutorService infect(ExecutorService target) {
        return new InfectingExecutor(target);
    }

    @Override
    public void shutdown() {
        target.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return target.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return target.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return target.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return target.awaitTermination(timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        if (Infection.isCurrentThreadInfected()) {
            Infection infection = Infection.get().get();
            target.execute(() -> {
                try (Infection _i = infection.getDing().infectThread()) {
                    command.run();
                }
            });
        }
        else {
            target.execute(command);
        }
    }
}
