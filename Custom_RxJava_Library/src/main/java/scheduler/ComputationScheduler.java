package scheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ComputationScheduler implements Scheduler {
    private final ExecutorService pool =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Override public void execute(Runnable task) { pool.execute(task); }
    @Override public void shutdown() { pool.shutdown(); }
}