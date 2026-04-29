package com.psycho.scheduler.task;

import com.psycho.scheduler.impl.PlatformScheduler;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * 统一的任务包装接口，用于控制和管理通过 {@link PlatformScheduler} 调度的任务。
 * <p>
 * 该接口提供了取消任务、查询状态以及获取任务 ID（如果底层支持）的方法。
 * 实现类将根据平台不同包装 Bukkit 的 {@code BukkitTask} 或 Paper 的 {@code ScheduledTask}。
 * </p>
 */
public interface WrappedTask {

    /**
     * 获取拥有此任务的插件。
     *
     * @return 插件实例
     */
    @NotNull Plugin getOwningPlugin();

    /**
     * 取消此任务。
     * <p>
     * 如果任务正在执行，则不会中断当前执行，但会阻止后续执行（如果是重复任务）。
     * </p>
     */
    void cancel();

    /**
     * 检查此任务是否已取消。
     *
     * @return 如果任务已取消返回 {@code true}，否则返回 {@code false}。
     */
    boolean isCancelled();

    /**
     * 获取此任务的任务 ID（如果底层支持）。
     * <p>
     * 注意：在 Paper 平台上，通过实体调度器或区域调度器创建的任务可能没有数值 ID，
     * 此时将返回 -1。传统 Bukkit 调度器创建的任务始终有正数 ID。
     * </p>
     *
     * @return 任务 ID，如果不可用则返回 -1。
     */
    int getTaskId();

    /**
     * 判断此任务是否为重复执行任务（定时器任务）。
     *
     * @return 如果是重复任务返回 {@code true}，否则返回 {@code false}。
     */
    boolean isRepeating();

    /**
     * Whether the task is async or not
     * <p>
     * Async tasks are never run on any world threads, including on Folia
     *
     * @return true if the task is async
     */
    boolean isAsync();
}