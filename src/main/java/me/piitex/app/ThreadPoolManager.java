package me.piitex.app;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ThreadPoolManager {

    private final ExecutorService executorService;

    public ThreadPoolManager() {
        this.executorService = Executors.newCachedThreadPool();
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public Future<?> submitTask(Runnable task) {
        if (!executorService.isShutdown()) {
            return executorService.submit(task);
        } else {
            return null;
        }
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ie) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}