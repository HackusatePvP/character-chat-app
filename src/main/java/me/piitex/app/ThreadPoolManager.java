package me.piitex.app;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ThreadPoolManager {

    private final ExecutorService executorService;

    public ThreadPoolManager() {
        // Using a cached thread pool for flexible background tasks.
        // It creates new threads as needed and reuses idle ones.
        this.executorService = Executors.newCachedThreadPool();
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public Future<?> submitTask(Runnable task) {
        if (!executorService.isShutdown()) {
            return executorService.submit(task); // Use submit()
        } else {
            return null; // Or throw an IllegalStateException
        }
    }


    public void shutdown() {
        executorService.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a reasonable time for tasks to complete their execution
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ie) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}