package com.groupon.jtier;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class AttachingExecutor extends AbstractExecutorService {

    private final ExecutorService target;

    private AttachingExecutor(ExecutorService target) {
        this.target = target;
    }

    public static ExecutorService infect(ExecutorService target) {
        return new AttachingExecutor(target);
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
        if (Attachment.isCurrentThreadAttached()) {
            Attachment infection = Attachment.get().get();
            target.execute(() -> {
                try (Attachment _i = infection.getDing().attachToThread()) {
                    command.run();
                }
            });
        }
        else {
            target.execute(command);
        }
    }
}
