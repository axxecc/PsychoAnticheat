package com.psycho.scheduler.impl;

import com.psycho.scheduler.task.WrappedPaperTask;
import com.psycho.scheduler.task.WrappedTask;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PaperImplementation implements PlatformScheduler {

    private final Plugin plugin;
    private final AsyncScheduler asyncScheduler;
    private final RegionScheduler regionScheduler;
    private final GlobalRegionScheduler globalScheduler;

    public PaperImplementation(Plugin plugin) {
        this.plugin = plugin;
        this.asyncScheduler = plugin.getServer().getAsyncScheduler();
        this.regionScheduler = plugin.getServer().getRegionScheduler();
        this.globalScheduler = plugin.getServer().getGlobalRegionScheduler();
    }

    private WrappedTask wrap(ScheduledTask task) {
        return new WrappedPaperTask(task);
    }

    // ==================== 同步任务 ====================

    @Override
    public @NotNull WrappedTask runSync(@NotNull Runnable task) {
        return wrap(globalScheduler.run(plugin, scheduledTask -> task.run()));
    }

    @Override
    public @NotNull WrappedTask runSync(@NotNull Consumer<WrappedTask> task) {
        return wrap(globalScheduler.run(plugin, scheduledTask -> task.accept(new WrappedPaperTask(scheduledTask))));
    }

    @Override
    public @NotNull WrappedTask runSyncLater(@NotNull Runnable task, long delay) {
        return wrap(globalScheduler.runDelayed(plugin, scheduledTask -> task.run(), delay));
    }

    @Override
    public @NotNull WrappedTask runSyncLater(@NotNull Consumer<WrappedTask> task, long delay) {
        return wrap(globalScheduler.runDelayed(plugin, scheduledTask -> task.accept(new WrappedPaperTask(scheduledTask)), delay));
    }

    @Override
    public @NotNull WrappedTask runSyncTimer(@NotNull Runnable task, long delay, long period) {
        return wrap(globalScheduler.runAtFixedRate(plugin, scheduledTask -> task.run(), delay, period));
    }

    @Override
    public @NotNull WrappedTask runSyncTimer(@NotNull Consumer<WrappedTask> task, long delay, long period) {
        return wrap(globalScheduler.runAtFixedRate(plugin, scheduledTask -> task.accept(new WrappedPaperTask(scheduledTask)), delay, period));
    }

    // ==================== 异步任务 ====================

    @Override
    public @NotNull WrappedTask runAsync(@NotNull Runnable task) {
        return wrap(asyncScheduler.runNow(plugin, scheduledTask -> task.run()));
    }

    @Override
    public @NotNull WrappedTask runAsync(@NotNull Consumer<WrappedTask> task) {
        return wrap(asyncScheduler.runNow(plugin, scheduledTask -> task.accept(new WrappedPaperTask(scheduledTask))));
    }

    @Override
    public @NotNull WrappedTask runAsyncLater(@NotNull Runnable task, long delay) {
        long delayMillis = delay * 50;
        return wrap(asyncScheduler.runDelayed(plugin, scheduledTask -> task.run(), delayMillis, TimeUnit.MILLISECONDS));
    }

    @Override
    public @NotNull WrappedTask runAsyncLater(@NotNull Consumer<WrappedTask> task, long delay) {
        long delayMillis = delay * 50;
        return wrap(asyncScheduler.runDelayed(plugin, scheduledTask -> task.accept(new WrappedPaperTask(scheduledTask)), delayMillis, TimeUnit.MILLISECONDS));
    }

    @Override
    public @NotNull WrappedTask runAsyncTimer(@NotNull Runnable task, long delay, long period) {
        long delayMillis = delay * 50;
        long periodMillis = period * 50;
        return wrap(asyncScheduler.runAtFixedRate(plugin, scheduledTask -> task.run(), delayMillis, periodMillis, TimeUnit.MILLISECONDS));
    }

    @Override
    public @NotNull WrappedTask runAsyncTimer(@NotNull Consumer<WrappedTask> task, long delay, long period) {
        long delayMillis = delay * 50;
        long periodMillis = period * 50;
        return wrap(asyncScheduler.runAtFixedRate(plugin, scheduledTask -> task.accept(new WrappedPaperTask(scheduledTask)), delayMillis, periodMillis, TimeUnit.MILLISECONDS));
    }

    // ==================== 区域任务 ====================

    @Override
    public @NotNull WrappedTask runAtLocation(@NotNull Location location, @NotNull Consumer<WrappedTask> task) {
        return wrap(regionScheduler.run(plugin, location, scheduledTask -> task.accept(new WrappedPaperTask(scheduledTask))));
    }

    @Override
    public @NotNull WrappedTask runAtLocation(@NotNull Location location, @NotNull Runnable task) {
        return wrap(regionScheduler.run(plugin, location, scheduledTask -> task.run()));
    }

    @Override
    public @NotNull WrappedTask runAtLocation(@NotNull World world, int chunkX, int chunkZ, @NotNull Consumer<WrappedTask> task) {
        return wrap(regionScheduler.run(plugin, world, chunkX, chunkZ, scheduledTask -> task.accept(new WrappedPaperTask(scheduledTask))));
    }

    @Override
    public @NotNull WrappedTask runAtLocation(@NotNull World world, int chunkX, int chunkZ, @NotNull Runnable task) {
        return wrap(regionScheduler.run(plugin, world, chunkX, chunkZ, scheduledTask -> task.run()));
    }

    @Override
    public @NotNull WrappedTask runAtLocationLater(@NotNull Location location, @NotNull Consumer<WrappedTask> task, long delayTicks) {
        return wrap(regionScheduler.runDelayed(plugin, location, scheduledTask -> task.accept(new WrappedPaperTask(scheduledTask)), delayTicks));
    }

    @Override
    public @NotNull WrappedTask runAtLocationLater(@NotNull Location location, @NotNull Runnable task, long delayTicks) {
        return wrap(regionScheduler.runDelayed(plugin, location, scheduledTask -> task.run(), delayTicks));
    }

    @Override
    public @NotNull WrappedTask runAtLocationTimer(@NotNull Location location, @NotNull Consumer<WrappedTask> task, long initialDelayTicks, long periodTicks) {
        return wrap(regionScheduler.runAtFixedRate(plugin, location, scheduledTask -> task.accept(new WrappedPaperTask(scheduledTask)), initialDelayTicks, periodTicks));
    }

    @Override
    public @NotNull WrappedTask runAtLocationTimer(@NotNull Location location, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return wrap(regionScheduler.runAtFixedRate(plugin, location, scheduledTask -> task.run(), initialDelayTicks, periodTicks));
    }

    // ==================== 实体任务 ====================

    @Override
    public @Nullable WrappedTask runAtEntity(@NotNull Entity entity, @NotNull Consumer<WrappedTask> task, @Nullable Runnable retired) {
        EntityScheduler entityScheduler = entity.getScheduler();
        ScheduledTask scheduled = entityScheduler.run(plugin, stask -> task.accept(new WrappedPaperTask(stask)), retired);
        return scheduled != null ? new WrappedPaperTask(scheduled) : null;
    }

    @Override
    public @Nullable WrappedTask runAtEntity(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired) {
        EntityScheduler entityScheduler = entity.getScheduler();
        ScheduledTask scheduled = entityScheduler.run(plugin, stask -> task.run(), retired);
        return scheduled != null ? new WrappedPaperTask(scheduled) : null;
    }

    @Override
    public @Nullable WrappedTask runAtEntityLater(@NotNull Entity entity, @NotNull Consumer<WrappedTask> task, @Nullable Runnable retired, long delayTicks) {
        EntityScheduler entityScheduler = entity.getScheduler();
        ScheduledTask scheduled = entityScheduler.runDelayed(plugin, stask -> task.accept(new WrappedPaperTask(stask)), retired, delayTicks);
        return scheduled != null ? new WrappedPaperTask(scheduled) : null;
    }

    @Override
    public @Nullable WrappedTask runAtEntityLater(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired, long delayTicks) {
        EntityScheduler entityScheduler = entity.getScheduler();
        ScheduledTask scheduled = entityScheduler.runDelayed(plugin, stask -> task.run(), retired, delayTicks);
        return scheduled != null ? new WrappedPaperTask(scheduled) : null;
    }

    @Override
    public @Nullable WrappedTask runAtEntityTimer(@NotNull Entity entity, @NotNull Consumer<WrappedTask> task, @Nullable Runnable retired, long initialDelayTicks, long periodTicks) {
        EntityScheduler entityScheduler = entity.getScheduler();
        ScheduledTask scheduled = entityScheduler.runAtFixedRate(plugin, stask -> task.accept(new WrappedPaperTask(stask)), retired, initialDelayTicks, periodTicks);
        return scheduled != null ? new WrappedPaperTask(scheduled) : null;
    }

    @Override
    public @Nullable WrappedTask runAtEntityTimer(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired, long initialDelayTicks, long periodTicks) {
        EntityScheduler entityScheduler = entity.getScheduler();
        ScheduledTask scheduled = entityScheduler.runAtFixedRate(plugin, stask -> task.run(), retired, initialDelayTicks, periodTicks);
        return scheduled != null ? new WrappedPaperTask(scheduled) : null;
    }

    // ==================== 异步传送 ====================

    @Override
    public @NotNull CompletableFuture<Boolean> teleportAsync(@NotNull Entity entity, @NotNull Location location) {
        return entity.teleportAsync(location);
    }

    // ==================== 取消任务 ====================

    @Override
    public void cancelTasks(@NotNull Plugin plugin) {
        globalScheduler.cancelTasks(plugin);
        asyncScheduler.cancelTasks(plugin);
    }
}