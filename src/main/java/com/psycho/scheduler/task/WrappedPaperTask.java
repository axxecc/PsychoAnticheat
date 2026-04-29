package com.psycho.scheduler.task;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Paper 平台的 {@link WrappedTask} 实现，包装了 {@link ScheduledTask}。
 */
public class WrappedPaperTask implements WrappedTask {

    private final ScheduledTask task;
    private final boolean async;
    private static final Class<? extends ScheduledTask> ASYNC_TASK_CLASS;
    static {
        Class<? extends ScheduledTask> asyncTaskClass = null;
        try {
            // noinspection unchecked
            asyncTaskClass = (Class<? extends ScheduledTask>) Class.forName("io.papermc.paper.threadedregions.scheduler.FoliaAsyncScheduler.AsyncScheduledTask");
        } catch (ClassNotFoundException e) {
            // ignore
        }
        ASYNC_TASK_CLASS = asyncTaskClass;
    }

    public WrappedPaperTask(ScheduledTask task) {
        this.task = task;
        if (ASYNC_TASK_CLASS == null) {
            this.async = false;
        } else {
            this.async = ASYNC_TASK_CLASS.isAssignableFrom(task.getClass());
        }
    }

    @Override
    public @NotNull Plugin getOwningPlugin() {
        return task.getOwningPlugin();
    }

    @Override
    public void cancel() {
        this.task.cancel();
    }

    @Override
    public boolean isCancelled() {
        return task.isCancelled();
    }

    @Override
    public int getTaskId() {
        return -1;
    }

    @Override
    public boolean isRepeating() {
        return task.isRepeatingTask();
    }

    @Override
    public boolean isAsync() {
        return this.async;
    }
}