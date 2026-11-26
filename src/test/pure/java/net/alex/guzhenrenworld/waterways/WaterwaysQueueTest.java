package net.alex.guzhenrenworld.waterways;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 水系建图队列测试：任务严格按提交顺序一次一个地跑；仍在排队的任务可被提到队首，已提升的保持提升先后，运行中的与未知的
 * 任务不受影响；踢出与清空回调 {@code dropped} 且不再运行；工作线程是低优先级守护线程；任务抛异常不拖垮队列。
 */
class WaterwaysQueueTest {

    private static final long SECONDS = 10;

    @Test
    void jobsRunOneAtATimeInSubmissionOrder() throws InterruptedException {
        WaterwaysQueue queue = new WaterwaysQueue("test-order");
        Gate gate = Gate.hold(queue);
        List<String> order = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger active = new AtomicInteger();
        AtomicInteger overlap = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(3);
        for (String name : List.of("a", "b", "c")) {
            queue.submit(job(() -> {
                if (active.incrementAndGet() > 1) overlap.incrementAndGet();
                order.add(name);
                active.decrementAndGet();
                done.countDown();
            }));
        }
        assertEquals(3, queue.size());
        gate.release();
        assertTrue(done.await(SECONDS, TimeUnit.SECONDS));
        assertEquals(List.of("a", "b", "c"), order);
        assertEquals(0, overlap.get(), "two jobs never ran at the same time");
    }

    @Test
    void promoteMovesAQueuedJobToTheFront() throws InterruptedException {
        WaterwaysQueue queue = new WaterwaysQueue("test-promote");
        Gate gate = Gate.hold(queue);
        List<String> order = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch done = new CountDownLatch(3);
        WaterwaysQueue.Job a = recorder("a", order, done);
        WaterwaysQueue.Job b = recorder("b", order, done);
        WaterwaysQueue.Job c = recorder("c", order, done);
        queue.submit(a);
        queue.submit(b);
        queue.submit(c);

        assertTrue(queue.promote(c));
        assertFalse(queue.promote(gate.job), "the running job cannot be preempted or promoted");
        assertFalse(queue.promote(recorder("stranger", order, done)), "a job that was never queued is ignored");
        gate.release();
        assertTrue(done.await(SECONDS, TimeUnit.SECONDS));
        assertEquals(List.of("c", "a", "b"), order);
    }

    @Test
    void promotedJobsKeepTheOrderTheyWerePromotedIn() throws InterruptedException {
        WaterwaysQueue queue = new WaterwaysQueue("test-promoted-order");
        Gate gate = Gate.hold(queue);
        List<String> order = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch done = new CountDownLatch(4);
        List<WaterwaysQueue.Job> jobs = new ArrayList<>();
        for (String name : List.of("a", "b", "c", "d")) {
            jobs.add(recorder(name, order, done));
            queue.submit(jobs.getLast());
        }

        assertTrue(queue.promote(jobs.get(3)));
        assertTrue(queue.promote(jobs.get(1)));
        assertFalse(queue.promote(jobs.get(3)), "an already promoted job keeps its place");
        gate.release();
        assertTrue(done.await(SECONDS, TimeUnit.SECONDS));
        assertEquals(List.of("d", "b", "a", "c"), order);
    }

    @Test
    void removeDropsAQueuedJobButNotTheRunningOne() throws InterruptedException {
        WaterwaysQueue queue = new WaterwaysQueue("test-remove");
        Gate gate = Gate.hold(queue);
        List<String> order = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch done = new CountDownLatch(1);
        AtomicInteger dropped = new AtomicInteger();
        WaterwaysQueue.Job removed = new WaterwaysQueue.Job() {

            @Override
            public void run() {
                order.add("removed");
            }

            @Override
            public void dropped() {
                dropped.incrementAndGet();
            }
        };
        queue.submit(removed);
        queue.submit(recorder("kept", order, done));

        assertTrue(queue.remove(removed));
        assertFalse(queue.remove(removed), "a job is dropped once");
        assertFalse(queue.remove(gate.job), "the running job is not in the queue");
        assertEquals(1, dropped.get());
        gate.release();
        assertTrue(done.await(SECONDS, TimeUnit.SECONDS));
        assertEquals(List.of("kept"), order);
    }

    @Test
    void clearDropsEverythingQueuedAndTheQueueWorksAgain() throws InterruptedException {
        WaterwaysQueue queue = new WaterwaysQueue("test-clear");
        Gate gate = Gate.hold(queue);
        AtomicInteger dropped = new AtomicInteger();
        AtomicInteger ran = new AtomicInteger();
        List<WaterwaysQueue.Job> jobs = new ArrayList<>();
        for (int index = 0; index < 3; index++) {
            jobs.add(new WaterwaysQueue.Job() {

                @Override
                public void run() {
                    ran.incrementAndGet();
                }

                @Override
                public void dropped() {
                    dropped.incrementAndGet();
                }
            });
            queue.submit(jobs.getLast());
        }
        assertTrue(queue.promote(jobs.get(2)), "one promoted and two waiting are all cleared");

        queue.clear();
        assertEquals(0, queue.size());
        assertEquals(3, dropped.get());
        gate.release();

        CountDownLatch again = new CountDownLatch(1);
        queue.submit(job(again::countDown));
        assertTrue(again.await(SECONDS, TimeUnit.SECONDS));
        assertEquals(0, ran.get(), "nothing cleared ever ran");
    }

    @Test
    void theWorkerIsALowPriorityDaemonThread() throws InterruptedException {
        WaterwaysQueue queue = new WaterwaysQueue("test-thread");
        AtomicReference<Thread> worker = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        queue.submit(job(() -> {
            worker.set(Thread.currentThread());
            done.countDown();
        }));

        assertTrue(done.await(SECONDS, TimeUnit.SECONDS));
        assertTrue(worker.get().isDaemon());
        assertEquals(Thread.MIN_PRIORITY, worker.get().getPriority());
        assertEquals("test-thread", worker.get().getName());
    }

    @Test
    void aFailingJobDoesNotStopTheJobsBehindIt() throws InterruptedException {
        WaterwaysQueue queue = new WaterwaysQueue("test-failure");
        Gate gate = Gate.hold(queue);
        CountDownLatch done = new CountDownLatch(1);
        queue.submit(job(() -> {
            throw new IllegalStateException("expected by the test");
        }));
        queue.submit(job(done::countDown));
        gate.release();

        assertTrue(done.await(SECONDS, TimeUnit.SECONDS));
    }

    private static WaterwaysQueue.Job job(Runnable run) {
        return new WaterwaysQueue.Job() {

            @Override
            public void run() {
                run.run();
            }

            @Override
            public void dropped() {}
        };
    }

    private static WaterwaysQueue.Job recorder(String name, List<String> order, CountDownLatch done) {
        return job(() -> {
            order.add(name);
            done.countDown();
        });
    }

    /** 占住工作线程的任务：等到 {@link #release} 才结束，让后面提交的任务都留在队列里。 */
    private static final class Gate {

        private final CountDownLatch open = new CountDownLatch(1);
        private WaterwaysQueue.Job job;

        static Gate hold(WaterwaysQueue queue) throws InterruptedException {
            Gate gate = new Gate();
            CountDownLatch started = new CountDownLatch(1);
            gate.job = job(() -> {
                started.countDown();
                try {
                    gate.open.await(SECONDS, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });
            queue.submit(gate.job);
            assertTrue(started.await(SECONDS, TimeUnit.SECONDS));
            return gate;
        }

        void release() {
            open.countDown();
        }
    }
}
