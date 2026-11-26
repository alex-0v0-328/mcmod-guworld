package net.alex.guzhenrenworld.waterways;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

/**
 * 水系规划与剖面求解的并行计算池：专用 {@link ForkJoinPool}，不占公共池，免得建图把区块生成和别的并行流饿死。
 * <ul>
 *   <li>并行度 {@link #PARALLELISM} 为可用核数减 2（至少 1），留两个核给服务端主线程与区块生成；工作线程为低优先级
 *       （{@link Thread#MIN_PRIORITY}）守护线程，空闲 60 秒自行退出。</li>
 *   <li>{@link #forEach} 从任意线程调用：并行流跑在池内线程上，调用线程阻塞等待，规划阶段抛出的
 *       {@link java.util.concurrent.CancellationException} 照常传回调用方。</li>
 * </ul>
 */

final class WaterwaysPool {

    static final int PARALLELISM = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);

    private static final AtomicInteger WORKERS = new AtomicInteger();
    private static final ForkJoinPool POOL = new ForkJoinPool(PARALLELISM, WaterwaysPool::newWorker, null, false);

    private WaterwaysPool() {}

    static void forEach(int count, IntConsumer body) {
        POOL.invoke(ForkJoinTask.adapt(() -> IntStream.range(0, count).parallel().forEach(body)));
    }

    private static ForkJoinWorkerThread newWorker(ForkJoinPool pool) {
        ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
        worker.setName("guworld-waterways-pool-" + WORKERS.incrementAndGet());
        worker.setDaemon(true);
        worker.setPriority(Thread.MIN_PRIORITY);
        return worker;
    }
}
