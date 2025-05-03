package custom;

import java.util.concurrent.BlockingQueue;

class Worker implements Runnable {

    private final BlockingQueue<Runnable> queue;
    private final CustomThreadPoolExecutor pool;
    private Thread thread;

    Worker(BlockingQueue<Runnable> queue, CustomThreadPoolExecutor pool) {
        this.queue = queue;
        this.pool  = pool;
    }

    void setThread(Thread thread) {
        this.thread = thread; }



    Thread.State getState() {
        return thread.getState(); }

    void interrupt() {
        thread.interrupt(); }

    @Override
    public void run() {
        try {
            while (true) {
                if (pool.isShutdown() && queue.isEmpty()) break;

                Runnable task = queue.poll(pool.getKeepAliveTime(), pool.getTimeUnit());
                if (task != null) {
                    System.out.printf("[Worker] %s executes %s%n",
                            Thread.currentThread().getName(), task);
                    task.run();
                    pool.afterExecute();        // уведомляем пул
                } else if (pool.shouldTerminateThisWorker()) {
                    System.out.printf("[Worker] %s idle timeout, stopping.%n",
                            Thread.currentThread().getName());
                    break;
                }
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } finally {
            pool.onWorkerExit(this);
            System.out.printf("[Worker] %s terminated.%n", Thread.currentThread().getName());
        }
    }
}