package net.alex.guzhenrenworld.waterways;

import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicIntegerArray;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 水系并行池测试：并行度为核数减 2 且至少 1；每个下标恰好执行一次；执行线程是自家池的低优先级守护线程而非公共池；取消异常
 * 传回调用方。
 */
class WaterwaysPoolTest {

    @Test
    void parallelismLeavesTwoCoresButNeverDropsBelowOne() {
        assertEquals(Math.max(1, Runtime.getRuntime().availableProcessors() - 2), WaterwaysPool.PARALLELISM);
    }

    @Test
    void everyIndexRunsOnceOnLowPriorityDaemonThreadsOfItsOwnPool() {
        int count = 20_000;
        AtomicIntegerArray visits = new AtomicIntegerArray(count);
        Set<Thread> workers = ConcurrentHashMap.newKeySet();
        WaterwaysPool.forEach(count, index -> {
            visits.incrementAndGet(index);
            workers.add(Thread.currentThread());
        });

        for (int index = 0; index < count; index++) assertEquals(1, visits.get(index), "index " + index);
        assertTrue(workers.size() <= WaterwaysPool.PARALLELISM, workers.size() + " workers");
        for (Thread worker : workers) {
            assertTrue(worker.getName().startsWith("guworld-waterways-pool-"), worker.getName());
            assertTrue(worker.isDaemon());
            assertEquals(Thread.MIN_PRIORITY, worker.getPriority());
        }
    }

    @Test
    void aCancellationInsideTheBodyReachesTheCaller() {
        assertThrows(CancellationException.class, () -> WaterwaysPool.forEach(1_000, index -> {
            if (index == 500) throw new CancellationException("test");
        }));
    }
}
