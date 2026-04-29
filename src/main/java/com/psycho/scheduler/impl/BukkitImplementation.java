package com.psycho.scheduler.impl;

import com.psycho.scheduler.task.WrappedBukkitTask;
import com.psycho.scheduler.task.WrappedTask;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class BukkitImplementation implements PlatformScheduler {

    private final Plugin plugin;
    private final BukkitScheduler scheduler;

    public BukkitImplementation(Plugin plugin) {
        this.plugin = plugin;
        this.scheduler = plugin.getServer().getScheduler();
    }

    private WrappedTask wrap(BukkitTask task) {
        return new WrappedBukkitTask(task);
    }

    @Override
    public @NotNull WrappedTask runSync(@NotNull Runnable task) {
        return wrap(scheduler.runTask(plugin, task));
    }

    @Override
    public @NotNull WrappedTask runSync(@NotNull Consumer<WrappedTask> task) {
        return wrap(scheduler.runTask(plugin, () -> task.accept(new WrappedBukkitTask(null))));
    }

    @Override
    public @NotNull WrappedTask runSyncLater(@NotNull Runnable task, long delay) {
        return wrap(scheduler.runTaskLater(plugin, task, delay));
    }

    @Override
    public @NotNull WrappedTask runSyncLater(@NotNull Consumer<WrappedTask> task, long delay) {
        return wrap(scheduler.runTaskLater(plugin, () -> task.accept(new WrappedBukkitTask(null)), delay));
    }

    @Override
    public @NotNull WrappedTask runSyncTimer(@NotNull Runnable task, long delay, long period) {
        return wrap(scheduler.runTaskTimer(plugin, task, delay, period));
    }

    @Override
    public @NotNull WrappedTask runSyncTimer(@NotNull Consumer<WrappedTask> task, long delay, long period) {
        return wrap(scheduler.runTaskTimer(plugin, () -> task.accept(new WrappedBukkitTask(null)), delay, period));
    }

    @Override
    public @NotNull WrappedTask runAsync(@NotNull Runnable task) {
        return wrap(scheduler.runTaskAsynchronously(plugin, task));
    }

    @Override
    public @NotNull WrappedTask runAsync(@NotNull Consumer<WrappedTask> task) {
        return wrap(scheduler.runTaskAsynchronously(plugin, () -> task.accept(new WrappedBukkitTask(null))));
    }

    @Override
    public @NotNull WrappedTask runAsyncLater(@NotNull Runnable task, long delay) {
        return wrap(scheduler.runTaskLaterAsynchronously(plugin, task, delay));
    }

    @Override
    public @NotNull WrappedTask runAsyncLater(@NotNull Consumer<WrappedTask> task, long delay) {
        return wrap(scheduler.runTaskLaterAsynchronously(plugin, () -> task.accept(new WrappedBukkitTask(null)), delay));
    }

    @Override
    public @NotNull WrappedTask runAsyncTimer(@NotNull Runnable task, long delay, long period) {
        return wrap(scheduler.runTaskTimerAsynchronously(plugin, task, delay, period));
    }

    @Override
    public @NotNull WrappedTask runAsyncTimer(@NotNull Consumer<WrappedTask> task, long delay, long period) {
        return wrap(scheduler.runTaskTimerAsynchronously(plugin, () -> task.accept(new WrappedBukkitTask(null)), delay, period));
    }

    // 区域任务退化为同步任务
    @Override
    public @NotNull WrappedTask runAtLocation(@NotNull Location location, @NotNull Consumer<WrappedTask> task) {
        return runSync(task);
    }

    @Override
    public @NotNull WrappedTask runAtLocation(@NotNull Location location, @NotNull Runnable task) {
        return runSync(task);
    }

    @Override
    public @NotNull WrappedTask runAtLocation(@NotNull World world, int chunkX, int chunkZ, @NotNull Consumer<WrappedTask> task) {
        return runSync(task);
    }

    @Override
    public @NotNull WrappedTask runAtLocation(@NotNull World world, int chunkX, int chunkZ, @NotNull Runnable task) {
        return runSync(task);
    }

    @Override
    public @NotNull WrappedTask runAtLocationLater(@NotNull Location location, @NotNull Consumer<WrappedTask> task, long delayTicks) {
        return runSyncLater(task, delayTicks);
    }

    @Override
    public @NotNull WrappedTask runAtLocationLater(@NotNull Location location, @NotNull Runnable task, long delayTicks) {
        return runSyncLater(task, delayTicks);
    }

    @Override
    public @NotNull WrappedTask runAtLocationTimer(@NotNull Location location, @NotNull Consumer<WrappedTask> task, long initialDelayTicks, long periodTicks) {
        return runSyncTimer(task, initialDelayTicks, periodTicks);
    }

    @Override
    public @NotNull WrappedTask runAtLocationTimer(@NotNull Location location, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return runSyncTimer(task, initialDelayTicks, periodTicks);
    }

    // 实体任务退化为同步任务
    @Override
    public @Nullable WrappedTask runAtEntity(@NotNull Entity entity, @NotNull Consumer<WrappedTask> task, @Nullable Runnable retired) {
        return runSync(task);
    }

    @Override
    public @Nullable WrappedTask runAtEntity(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired) {
        return runSync(task);
    }

    @Override
    public @Nullable WrappedTask runAtEntityLater(@NotNull Entity entity, @NotNull Consumer<WrappedTask> task, @Nullable Runnable retired, long delayTicks) {
        return runSyncLater(task, delayTicks);
    }

    @Override
    public @Nullable WrappedTask runAtEntityLater(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired, long delayTicks) {
        return runSyncLater(task, delayTicks);
    }

    @Override
    public @Nullable WrappedTask runAtEntityTimer(@NotNull Entity entity, @NotNull Consumer<WrappedTask> task, @Nullable Runnable retired, long initialDelayTicks, long periodTicks) {
        return runSyncTimer(task, initialDelayTicks, periodTicks);
    }

    @Override
    public @Nullable WrappedTask runAtEntityTimer(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired, long initialDelayTicks, long periodTicks) {
        return runSyncTimer(task, initialDelayTicks, periodTicks);
    }

    @Override
    public @NotNull CompletableFuture<Boolean> teleportAsync(@NotNull Entity entity, @NotNull Location location) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        runSync(() -> {
            boolean success = entity.teleport(location);
            future.complete(success);
        });
        return future;
    }

    @Override
    public void cancelTasks(@NotNull Plugin plugin) {
        scheduler.cancelTasks(plugin);
    }
}