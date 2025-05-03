package org.example;


import custom.CustomThreadFactory;
import custom.CustomThreadPoolExecutor;
import custom.RejectionPolicy;

import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        CustomThreadPoolExecutor pool = new CustomThreadPoolExecutor(
                2, 4, 5, TimeUnit.SECONDS,
                5, 1,
                RejectionPolicy.ABORT,
                new CustomThreadFactory("MyPool")
        );

        // имитационные задачи
        for (int i = 0; i < 12; i++) {
            int id = i;
            pool.execute(() -> {
                System.out.printf("[Task %d] started%n", id);
                try { Thread.sleep(3000); } catch (InterruptedException ignored) { }
                System.out.printf("[Task %d] finished%n", id);
            });
        }

        // ждём и завершаем
        Thread.sleep(15000);
        pool.shutdown();
    }
}