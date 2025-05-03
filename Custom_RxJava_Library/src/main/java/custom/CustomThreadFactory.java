package custom;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class CustomThreadFactory implements ThreadFactory {
    private final AtomicInteger counter = new AtomicInteger();
    private final String poolName;

    public CustomThreadFactory(String poolName) { this.poolName = poolName; }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, poolName + "-worker-" + counter.incrementAndGet());
        System.out.printf("[ThreadFactory] Creating new thread: %s%n", t.getName());
        return t;
    }
}