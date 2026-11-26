package net.alex.guzhenrenworld.waterways;

import com.mojang.logging.LogUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.slf4j.Logger;

/**
 * 水系建图的单线程任务队列：任一时刻至多一个维度在建图，其余排队等候，避免新存档里五个维度同时抢满 CPU。
 * <ul>
 *   <li>唯一的工作线程为低优先级（{@link Thread#MIN_PRIORITY}）守护线程，有任务时才起、队列排空即退出，下次提交再起；
 *       某个任务抛出异常只记录日志，不影响后面的任务。</li>
 *   <li>{@link #submit} 排到队尾；{@link #promote} 把仍在排队的任务提到队首，已提到队首的任务之间保持提升先后；正在运行
 *       或已离队的任务不受影响（运行中的任务不被抢占）。</li>
 *   <li>{@link #remove} 与 {@link #clear} 把排队中的任务踢出队列并回调 {@link Job#dropped}，让等着它结果的线程不会一直
 *       挂起。{@link #SHARED} 是模组全局共用的那一条队列，停服与单人切世界时由 {@link WaterwaysRuntime#clear} 清空。</li>
 * </ul>
 */

final class WaterwaysQueue {

    private static final Logger LOGGER = LogUtils.getLogger();
    /** 全局共用队列：所有维度的水系建图都经它依次执行。 */
    static final WaterwaysQueue SHARED = new WaterwaysQueue("guworld-waterways");

    private final String name;
    private final Deque<Job> urgent = new ArrayDeque<>();
    private final Deque<Job> waiting = new ArrayDeque<>();
    private boolean working;

    WaterwaysQueue(String name) {
        this.name = name;
    }

    synchronized void submit(Job job) {
        waiting.addLast(job);
        if (working) return;
        working = true;
        Thread worker = new Thread(this::drain, name);
        worker.setDaemon(true);
        worker.setPriority(Thread.MIN_PRIORITY);
        worker.start();
    }

    synchronized boolean promote(Job job) {
        if (!waiting.remove(job)) return false;
        urgent.addLast(job);
        return true;
    }

    boolean remove(Job job) {
        synchronized (this) {
            if (!urgent.remove(job) && !waiting.remove(job)) return false;
        }
        job.dropped();
        return true;
    }

    void clear() {
        List<Job> removed;
        synchronized (this) {
            removed = new ArrayList<>(urgent);
            removed.addAll(waiting);
            urgent.clear();
            waiting.clear();
        }
        removed.forEach(Job::dropped);
    }

    synchronized int size() {
        return urgent.size() + waiting.size();
    }

    private void drain() {
        for (Job job = next(); job != null; job = next()) {
            try {
                job.run();
            } catch (RuntimeException | Error exception) {
                LOGGER.error("A waterways job of {} failed", name, exception);
            }
        }
    }

    private synchronized Job next() {
        Job job = urgent.pollFirst();
        if (job == null) job = waiting.pollFirst();
        if (job == null) working = false;
        return job;
    }

    /** 队列里的一项任务；{@link #dropped} 在任务未运行就被踢出队列时回调。 */
    interface Job extends Runnable {

        void dropped();
    }
}
