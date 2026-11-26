package net.alex.guzhenrenworld.waterways;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 单个维度建图生命周期测试：只提交一次；排队中的建图可被提到别的建图前面；取消排队中的建图立即以回退值放行等待者且不跑
 * 建图；取消运行中的建图在下一次检查处停下并回退；清空队列、建图失败同样回退，没有任何结局会让等待者挂死。
 */
class WaterwaysBuildTest {

    private static final long SECONDS = 10;
    private static final String FALLBACK = "fallback";

    @Test
    void startSubmitsOnceAndReturnsTheSameFuture() throws Exception {
        WaterwaysQueue queue = new WaterwaysQueue("test-build-once");
        AtomicInteger runs = new AtomicInteger();
        WaterwaysBuild<String> build = new WaterwaysBuild<>(queue, "once", cancelled -> {
            runs.incrementAndGet();
            return "built";
        }, () -> FALLBACK);

        CompletableFuture<String> first = build.start();
        assertSame(first, build.start());
        assertEquals("built", first.get(SECONDS, TimeUnit.SECONDS));
        assertEquals(1, runs.get());
    }

    @Test
    void promotedBuildsRunBeforeTheOnesAheadOfThemInTheQueue() throws Exception {
        WaterwaysQueue queue = new WaterwaysQueue("test-build-promote");
        CountDownLatch open = hold(queue);
        List<String> order = Collections.synchronizedList(new ArrayList<>());
        List<CompletableFuture<String>> futures = new ArrayList<>();
        List<WaterwaysBuild<String>> builds = new ArrayList<>();
        for (String name : List.of("overworld-like", "the-players", "other")) {
            WaterwaysBuild<String> build = new WaterwaysBuild<>(queue, name, cancelled -> {
                order.add(name);
                return name;
            }, () -> FALLBACK);
            builds.add(build);
            futures.add(build.start());
        }

        builds.get(1).promote();
        open.countDown();
        for (CompletableFuture<String> future : futures) future.get(SECONDS, TimeUnit.SECONDS);
        assertEquals(List.of("the-players", "overworld-like", "other"), order);
    }

    @Test
    void cancellingAQueuedBuildReleasesItsWaitersAtOnceAndNeverRunsIt() throws Exception {
        WaterwaysQueue queue = new WaterwaysQueue("test-build-cancel-queued");
        CountDownLatch open = hold(queue);
        AtomicInteger runs = new AtomicInteger();
        WaterwaysBuild<String> build = new WaterwaysBuild<>(queue, "queued", cancelled -> {
            runs.incrementAndGet();
            return "built";
        }, () -> FALLBACK);
        CompletableFuture<String> future = build.start();
        assertFalse(future.isDone(), "the queue is busy, so the build waits");

        build.cancel();
        assertEquals(FALLBACK, future.get(SECONDS, TimeUnit.SECONDS));
        assertEquals(0, queue.size());
        open.countDown();
        assertEquals(0, runs.get());
    }

    @Test
    void cancellingARunningBuildStopsItAtTheNextCheckAndFallsBack() throws Exception {
        WaterwaysQueue queue = new WaterwaysQueue("test-build-cancel-running");
        CountDownLatch started = new CountDownLatch(1);
        WaterwaysBuild<String> build = new WaterwaysBuild<>(queue, "running", cancelled -> {
            started.countDown();
            while (!cancelled.getAsBoolean()) Thread.onSpinWait();
            throw new CancellationException("running");
        }, () -> FALLBACK);
        CompletableFuture<String> future = build.start();
        assertTrue(started.await(SECONDS, TimeUnit.SECONDS));

        build.cancel();
        assertEquals(FALLBACK, future.get(SECONDS, TimeUnit.SECONDS));
        assertTrue(build.isCancelled());
    }

    @Test
    void aBuildCancelledBeforeItStartsNeverEntersTheQueue() throws Exception {
        WaterwaysQueue queue = new WaterwaysQueue("test-build-cancel-early");
        CountDownLatch open = hold(queue);
        WaterwaysBuild<String> build = new WaterwaysBuild<>(queue, "early", cancelled -> "built", () -> FALLBACK);

        build.cancel();
        CompletableFuture<String> future = build.start();
        assertEquals(FALLBACK, future.get(SECONDS, TimeUnit.SECONDS));
        assertEquals(0, queue.size());
        open.countDown();
    }

    @Test
    void clearingTheQueueReleasesTheWaitersOfEveryQueuedBuild() throws Exception {
        WaterwaysQueue queue = new WaterwaysQueue("test-build-clear");
        CountDownLatch open = hold(queue);
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (int index = 0; index < 3; index++) {
            futures.add(new WaterwaysBuild<>(queue, "clear-" + index, cancelled -> "built", () -> FALLBACK).start());
        }

        queue.clear();
        for (CompletableFuture<String> future : futures) assertEquals(FALLBACK, future.get(SECONDS, TimeUnit.SECONDS));
        open.countDown();
    }

    @Test
    void aFailingBuildFallsBackInsteadOfLeavingItsWaitersHanging() throws Exception {
        WaterwaysQueue queue = new WaterwaysQueue("test-build-failure");
        WaterwaysBuild<String> build = new WaterwaysBuild<>(queue, "failing", cancelled -> {
            throw new IllegalStateException("expected by the test");
        }, () -> FALLBACK);

        assertEquals(FALLBACK, build.start().get(SECONDS, TimeUnit.SECONDS));
    }

    @Test
    void aBuildWaitsItsTurnBehindARunningOne() throws Exception {
        WaterwaysQueue queue = new WaterwaysQueue("test-build-wait");
        CountDownLatch open = hold(queue);
        CompletableFuture<String> future = new WaterwaysBuild<>(queue, "waiting", cancelled -> "built",
                () -> FALLBACK).start();

        try {
            future.get(200, TimeUnit.MILLISECONDS);
            throw new AssertionError("the build ran while another one held the queue");
        } catch (TimeoutException expected) {
            open.countDown();
        }
        assertEquals("built", future.get(SECONDS, TimeUnit.SECONDS));
    }

    /** 占住队列工作线程直到返回的闩被打开，让之后启动的建图都留在队列里。 */
    private static CountDownLatch hold(WaterwaysQueue queue) throws InterruptedException {
        CountDownLatch open = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(1);
        queue.submit(new WaterwaysQueue.Job() {

            @Override
            public void run() {
                started.countDown();
                try {
                    open.await(SECONDS, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }

            @Override
            public void dropped() {}
        });
        assertTrue(started.await(SECONDS, TimeUnit.SECONDS));
        return open;
    }
}
