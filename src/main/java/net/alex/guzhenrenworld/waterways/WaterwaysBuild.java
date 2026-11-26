package net.alex.guzhenrenworld.waterways;

import com.mojang.logging.LogUtils;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;

/**
 * 单个维度的一次后台建图在 {@link WaterwaysQueue} 中的生命周期，结果类型 {@code T}（实际为 {@link WaterwaysIndex}）。
 * <ul>
 *   <li>{@link #start} 只提交一次并返回同一个 {@link CompletableFuture}；任何结局（建成、被取消、失败）都会用结果或回退值
 *       完成它，等在 {@link CompletableFuture#join()} 上的区块生成线程因而不会挂死。</li>
 *   <li>{@link #cancel} 置取消标志，让运行中的建图在下一次 {@link BooleanSupplier} 检查处抛出
 *       {@link CancellationException} 并回退；仍在排队的任务则直接踢出队列、立即以回退值完成。取消后再 {@link #start}
 *       不再入队，直接回退。</li>
 *   <li>{@link #promote} 把仍在排队的任务提到队首（玩家进入该维度时调用）；已在运行或已完成则无事发生。</li>
 *   <li>建图抛出 {@link CancellationException} 记一行信息，其他异常记错误日志，两种都回退。</li>
 * </ul>
 */

final class WaterwaysBuild<T> implements WaterwaysQueue.Job {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final WaterwaysQueue queue;
    private final String label;
    private final Function<BooleanSupplier, T> work;
    private final Supplier<T> fallback;
    private final AtomicReference<CompletableFuture<T>> started = new AtomicReference<>();
    private volatile boolean cancelled;

    WaterwaysBuild(WaterwaysQueue queue, String label, Function<BooleanSupplier, T> work, Supplier<T> fallback) {
        this.queue = queue;
        this.label = label;
        this.work = work;
        this.fallback = fallback;
    }

    CompletableFuture<T> start() {
        CompletableFuture<T> existing = started.get();
        if (existing != null) return existing;
        CompletableFuture<T> fresh = new CompletableFuture<>();
        if (!started.compareAndSet(null, fresh)) return started.get();
        if (cancelled) {
            fresh.complete(fallback.get());
            return fresh;
        }
        try {
            queue.submit(this);
            if (cancelled) queue.remove(this);
        } catch (RuntimeException | Error exception) {
            LOGGER.error("Waterways of {} could not start; the region stays uncarved", label, exception);
            fresh.complete(fallback.get());
        }
        return fresh;
    }

    void promote() {
        queue.promote(this);
    }

    void cancel() {
        cancelled = true;
        queue.remove(this);
    }

    boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void run() {
        CompletableFuture<T> future = started.get();
        if (cancelled) {
            future.complete(fallback.get());
            return;
        }
        T result;
        try {
            result = work.apply(this::isCancelled);
        } catch (CancellationException exception) {
            LOGGER.info("Waterways of {} cancelled with its dimension", label);
            result = fallback.get();
        } catch (RuntimeException | Error exception) {
            LOGGER.error("Waterways of {} failed; the region stays uncarved", label, exception);
            result = fallback.get();
        }
        future.complete(result);
    }

    @Override
    public void dropped() {
        LOGGER.info("Waterways of {} dropped from the queue before it started", label);
        started.get().complete(fallback.get());
    }
}
