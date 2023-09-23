package server.concurrency;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

public class ThreadPoolFactory {
    private static final ExecutorService fixedThreadPool = Executors.newFixedThreadPool(10);
    private static final ForkJoinPool forkJoinPool = new ForkJoinPool(15);

    public static ExecutorService getFixedThreadPool() {
        return fixedThreadPool;
    }

    public static ForkJoinPool getForkJoinPool() {
        return forkJoinPool;
    }
}
