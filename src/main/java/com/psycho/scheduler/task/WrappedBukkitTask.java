package com.psycho.scheduler.task;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

/**
 * Bukkit 平台的 {@link WrappedTask} 实现，包装了 {@link BukkitTask}。
 */
public class WrappedBukkitTask implements WrappedTask {

    private final BukkitTask task;

    public WrappedBukkitTask(BukkitTask task) {
        this.task = task;
    }

    @Override
    public @NotNull Plugin getOwningPlugin() {
        return this.task.getOwner();
    }

    @Override
    public void cancel() {
        this.task.cancel();
    }

    @Override
    public boolean isCancelled() {
        return this.task.isCancelled();
    }

    @Override
    public int getTaskId() {
        return this.task.getTaskId();
    }

    @Override
    public boolean isRepeating() {
        // BukkitTask 没有直接方法判断是否重复，可以通过判断 period 是否为 0 或通过内部状态。
        // 但无法从 API 获取 period，所以返回 false 作为默认。
        return false;
    }

    @Override
    public boolean isAsync() {
        return !this.task.isSync();
    }
}