package me.piitex.app;

import java.util.concurrent.*;

public class ThreadPoolManager {

    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutor;

    public ThreadPoolManager() {
        this.executorService = Executors.newCachedThreadPool();
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
    }

    public Future<?> submitTask(Runnable task) {
        if (!executorService.isShutdown()) {
            return executorService.submit(task);
        } else {
            return null;
        }
    }

    public Future<?> submitSchedule(Runnable task, long delay, TimeUnit unit) {
        if (!scheduledExecutor.isShutdown()) {
            return scheduledExecutor.schedule(task, delay, unit);
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