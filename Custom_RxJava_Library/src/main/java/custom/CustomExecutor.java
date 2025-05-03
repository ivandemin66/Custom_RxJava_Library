package custom;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

public interface CustomExecutor extends Executor {

    @Override
    void execute(Runnable command);

    <T> Future<T> submit(Callable<T> task);

    /** мягкое завершение — ждём выполнения очереди */
    void shutdown();

    /** резкое завершение — пытаемся отменить задачи */
    void shutdownNow();
}