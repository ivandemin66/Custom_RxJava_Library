package custom;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Кастомный пул потоков с параметрами core/max, queueSize, keepAliveTime и minSpareThreads.
 */
public class CustomThreadPoolExecutor implements CustomExecutor {

    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private final int minSpareThreads;
    private final RejectionPolicy rejection;
    private final CustomThreadFactory threadFactory;

    /** Очередь задач. */
    private final BlockingQueue<Runnable> queue;
    /** Набор воркеров (а не Thread!). */
    private final Set<Worker> workers = ConcurrentHashMap.newKeySet();
    /** Счётчик отправленных (ещё не завершённых) задач. */
    private final AtomicInteger submittedTasks = new AtomicInteger();
    private volatile boolean shutdown;

    /* ── конструктор ───────────────────────────────────────────────────── */

    public CustomThreadPoolExecutor(
            int corePoolSize,
            int maxPoolSize,
            long keepAliveTime,
            TimeUnit timeUnit,
            int queueSize,
            int minSpareThreads,
            RejectionPolicy rejection,
            CustomThreadFactory threadFactory) {

        if (corePoolSize <= 0 || maxPoolSize < corePoolSize)
            throw new IllegalArgumentException("Неверные размеры пула");

        this.corePoolSize    = corePoolSize;
        this.maxPoolSize     = maxPoolSize;
        this.keepAliveTime   = keepAliveTime;
        this.timeUnit        = timeUnit;
        this.minSpareThreads = minSpareThreads;
        this.rejection       = rejection;
        this.threadFactory   = threadFactory;

        this.queue = new ArrayBlockingQueue<>(queueSize);

        /* стартуем базовые core‑воркеры */
        for (int i = 0; i < corePoolSize; i++) addWorker();
    }

    /* ── CustomExecutor API ────────────────────────────────────────────── */

    @Override
    public void execute(Runnable command) {
        Objects.requireNonNull(command);
        if (shutdown) reject(command);

        if (queue.offer(command)) {
            System.out.printf("[Pool] Task accepted into queue: %s%n", command);
            ensureSpareThreads();
        } else {
            // очередь полна, пытаемся расширить пул
            if (workers.size() < maxPoolSize) {
                addWorker();
                queue.offer(command);
            } else {
                reject(command);
            }
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        CompletableFuture<T> cf = new CompletableFuture<>();
        execute(() -> {
            try { cf.complete(task.call()); }
            catch (Throwable t) { cf.completeExceptionally(t); }
        });
        return cf;
    }

    @Override
    public void shutdown() { shutdown = true; }

    @Override
    public void shutdownNow() {
        shutdown = true;
        workers.forEach(Worker::interrupt);  // ← без кастов
        queue.clear();
    }


    boolean isShutdown()             { return shutdown; }
    long     getKeepAliveTime()      { return keepAliveTime; }
    TimeUnit getTimeUnit()           { return timeUnit; }

    /** после успешного выполнения задачи */
    void afterExecute() { submittedTasks.decrementAndGet(); }

    /** Поток должен завершиться, если простаивает дольше keepAlive и превышает corePoolSize. */
    boolean shouldTerminateThisWorker() {
        return workers.size() > corePoolSize;
    }

    void onWorkerExit(Worker w) {
        workers.remove(w); }

    /** Создаём нового Worker + поток, запоминаем их связь. */
    private void addWorker() {
        Worker w = new Worker(queue, this);
        Thread t = threadFactory.newThread(w);
        w.setThread(t);         // связь Worker↔Thread
        workers.add(w);
        t.start();
    }

    /** Поддерживаем резерв свободных потоков. */
    private void ensureSpareThreads() {
        long idle = workers.stream()
                .filter(w -> w.getState() == Thread.State.WAITING)
                .count();
        if (idle < minSpareThreads && workers.size() < maxPoolSize) addWorker();
    }

    /** Обрабатываем переполнение очереди. */
    private void reject(Runnable task) {
        System.out.printf("[Rejected] Task %s was rejected due to overload!%n", task);
        switch (rejection) {
            case ABORT          -> throw new RejectedExecutionException();
            case CALLER_RUNS    -> task.run();
            case DISCARD_OLDEST -> { queue.poll(); queue.offer(task); }
            case DISCARD        -> { /* игнор */ }
        }
    }
}