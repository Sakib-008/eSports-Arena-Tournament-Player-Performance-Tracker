package com.esports.arena.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;


public class DataRefreshService {
    private static final int REFRESH_INTERVAL_SECONDS = 10; // Reduced from 3 to 10 seconds for less lag
    private static final int CORE_POOL_SIZE = 2; // Reduced from 4 to 2 threads
    private static final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(CORE_POOL_SIZE);
    private static final Map<String, Runnable> refreshTasks = new HashMap<>();
    private static ScheduledFuture<?> refreshSchedule = null;
    private static boolean isRunning = false;

    /**
     * Register a refresh task with a unique identifier to avoid duplicate tasks
     * @param taskId Unique identifier for the task
     * @param task The runnable task to execute
     */
    public static void registerRefreshTask(String taskId, Runnable task) {
        synchronized (refreshTasks) {
            refreshTasks.put(taskId, task);
        }
    }

    /**
     * Unregister a refresh task
     * @param taskId The task identifier to remove
     */
    public static void unregisterRefreshTask(String taskId) {
        synchronized (refreshTasks) {
            refreshTasks.remove(taskId);
        }
    }

    /**
     * Start the refresh service
     */
    public static void start() {
        if (!isRunning) {
            isRunning = true;
            refreshSchedule = scheduler.scheduleAtFixedRate(() -> {
                synchronized (refreshTasks) {
                    refreshTasks.values().forEach(task -> {
                        try {
                            task.run();
                        } catch (Exception e) {
                            System.err.println("Error in refresh task: " + e.getMessage());
                        }
                    });
                }
            }, REFRESH_INTERVAL_SECONDS, REFRESH_INTERVAL_SECONDS, TimeUnit.SECONDS);
        }
    }

    /**
     * Stop the refresh service
     */
    public static void stop() {
        isRunning = false;
        if (refreshSchedule != null) {
            refreshSchedule.cancel(false);
            refreshSchedule = null;
        }
    }

    /**
     * Pause the refresh service temporarily
     */
    public static void pause() {
        if (refreshSchedule != null) {
            refreshSchedule.cancel(false);
            refreshSchedule = null;
            isRunning = false;
        }
    }

    /**
     * Resume the refresh service after pause
     */
    public static void resume() {
        if (!isRunning) {
            start();
        }
    }

    /**
     * Safe UI update from background thread
     * @param runnable The UI update code to run on JavaFX thread
     */
    public static void updateUI(Runnable runnable) {
        Platform.runLater(runnable);
    }

    /**
     * Execute a task asynchronously with UI callback on completion
     * @param task The task to execute
     * @param onSuccess Callback when task succeeds
     * @param onError Callback when task fails
     */
    public static <T> void executeAsync(TaskExecutor<T> task, 
                                       SuccessCallback<T> onSuccess,
                                       ErrorCallback onError) {
        scheduler.execute(() -> {
            try {
                T result = task.execute();
                if (onSuccess != null) {
                    Platform.runLater(() -> onSuccess.onSuccess(result));
                }
            } catch (Exception e) {
                if (onError != null) {
                    Platform.runLater(() -> onError.onError(e));
                } else {
                    System.err.println("Async task error: " + e.getMessage());
                }
            }
        });
    }

    // Functional interfaces for async operations
    @FunctionalInterface
    public interface TaskExecutor<T> {
        T execute() throws Exception;
    }

    @FunctionalInterface
    public interface SuccessCallback<T> {
        void onSuccess(T result);
    }

    @FunctionalInterface
    public interface ErrorCallback {
        void onError(Exception e);
    }
}
