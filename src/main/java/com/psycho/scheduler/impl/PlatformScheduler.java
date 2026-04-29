package com.psycho.scheduler.impl;

import com.psycho.scheduler.task.WrappedTask;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 统一调度器接口，屏蔽 Bukkit 和 Paper（包括 Folia）平台差异，提供一致的调度方法。
 * <p>
 * 该接口封装了以下类型的任务：
 * <ul>
 *     <li>同步任务（在主线程执行）</li>
 *     <li>同步延迟/重复任务</li>
 *     <li>异步任务（在异步线程执行）</li>
 *     <li>异步延迟/重复任务</li>
 *     <li>区域任务（在指定区块所属的线程执行，Paper 特有，Bukkit 退化为同步任务）</li>
 *     <li>实体任务（在实体所属的线程执行，Paper 特有，Bukkit 退化为同步任务）</li>
 *     <li>异步传送（Paper 使用 {@link Entity#teleportAsync(Location)}，Bukkit 使用同步传送并包装为 Future）</li>
 * </ul>
 * </p>
 * <p>
 * 注意：在 Bukkit 平台上，区域任务和实体任务实际上是在主线程执行的普通同步任务，
 * 因此它们不具备 Paper 平台上的线程安全和实体跟随特性。请在文档中明确此行为。
 * </p>
 */
public interface PlatformScheduler {

    // ==================== 同步任务 ====================

    /**
     * 在下一个服务器 tick 执行一次同步任务。
     * @param task   要执行的任务
     * @return 包装后的任务对象 {@link WrappedTask}
     */
    @NotNull WrappedTask runSync(@NotNull Runnable task);

    /**
     * 在下一个服务器 tick 执行一次同步任务（Consumer 版本，接收 {@link WrappedTask}）。
     *
     * @param task   要执行的任务，参数为当前任务包装对象
     * @return 包装后的任务对象 {@link WrappedTask}
     */
    @NotNull WrappedTask runSync(@NotNull Consumer<WrappedTask> task);

    /**
     * 延迟指定 ticks 后执行一次同步任务。
     *
     * @param task   要执行的任务
     * @param delay  延迟的 tick 数（必须 >=0）
     * @return 包装后的任务对象 {@link WrappedTask}
     */
    @NotNull WrappedTask runSyncLater(@NotNull Runnable task, long delay);

    /**
     * 延迟指定 ticks 后执行一次同步任务（Consumer 版本）。
     *
     * @param task   要执行的任务，参数为当前任务包装对象
     * @param delay  延迟的 tick 数（必须 >=0）
     * @return 包装后的任务对象 {@link WrappedTask}
     */
    @NotNull WrappedTask runSyncLater(@NotNull Consumer<WrappedTask> task, long delay);

    /**
     * 延迟指定 ticks 后开始，以固定周期重复执行同步任务。
     *
     * @param task   要执行的任务
     * @param delay  首次执行的延迟 tick 数（必须 >=0）
     * @param period 重复执行的周期 tick 数（必须 >0）
     * @return 包装后的任务对象 {@link WrappedTask}
     */
    @NotNull WrappedTask runSyncTimer(@NotNull Runnable task, long delay, long period);

    /**
     * 延迟指定 ticks 后开始，以固定周期重复执行同步任务（Consumer 版本）。
     *
     * @param task   要执行的任务，参数为当前任务包装对象
     * @param delay  首次执行的延迟 tick 数（必须 >=0）
     * @param period 重复执行的周期 tick 数（必须 >0）
     * @return 包装后的任务对象 {@link WrappedTask}
     */
    @NotNull WrappedTask runSyncTimer(@NotNull Consumer<WrappedTask> task, long delay, long period);

    // ==================== 异步任务 ====================

    /**
     * 立即执行一次异步任务。
     *
     * @param task   要执行的任务
     * @return 包装后的任务对象 {@link WrappedTask}
     */
    @NotNull WrappedTask runAsync(@NotNull Runnable task);

    /**
     * 立即执行一次异步任务（Consumer 版本）。
     *
     * @param task   要执行的任务，参数为当前任务包装对象
     * @return 包装后的任务对象 {@link WrappedTask}
     */
    @NotNull WrappedTask runAsync(@NotNull Consumer<WrappedTask> task);

    /**
     * 延迟指定 ticks 后执行一次异步任务。
     *
     * @param task   要执行的任务
     * @param delay  延迟的 tick 数（必须 >=0）
     * @return 包装后的任务对象 {@link WrappedTask}
     */
    @NotNull WrappedTask runAsyncLater(@NotNull Runnable task, long delay);

    /**
     * 延迟指定 ticks 后执行一次异步任务（Consumer 版本）。
     *
     * @param task   要执行的任务，参数为当前任务包装对象
     * @param delay  延迟的 tick 数（必须 >=0）
     * @return 包装后的任务对象 {@link WrappedTask}
     */
    @NotNull WrappedTask runAsyncLater(@NotNull Consumer<WrappedTask> task, long delay);

    /**
     * 延迟指定 ticks 后开始，以固定周期重复执行异步任务。
     *
     * @param task   要执行的任务
     * @param delay  首次执行的延迟 tick 数（必须 >=0）
     * @param period 重复执行的周期 tick 数（必须 >0）
     * @return 包装后的任务对象 {@link WrappedTask}
     */
    @NotNull WrappedTask runAsyncTimer(@NotNull Runnable task, long delay, long period);

    /**
     * 延迟指定 ticks 后开始，以固定周期重复执行异步任务（Consumer 版本）。
     *
     * @param task   要执行的任务，参数为当前任务包装对象
     * @param delay  首次执行的延迟 tick 数（必须 >=0）
     * @param period 重复执行的周期 tick 数（必须 >0）
     * @return 包装后的任务对象 {@link WrappedTask}
     */
    @NotNull WrappedTask runAsyncTimer(@NotNull Consumer<WrappedTask> task, long delay, long period);

    // ==================== 区域任务（Paper特有，Bukkit退化为同步任务） ====================

    /**
     * 在指定位置所属的区域线程上执行一次任务（下一个 tick）。
     * <p>
     * 在 Paper 平台上，任务将在该区块所属的线程执行；在 Bukkit 平台上，此方法退化为在主线程执行的同步任务。
     * </p>
     *
     * @param location 位置，用于确定区域
     * @param task     要执行的任务，参数为当前任务包装对象
     * @return 包装后的任务对象 {@link WrappedTask}
     */
    @NotNull WrappedTask runAtLocation(@NotNull Location location, @NotNull Consumer<WrappedTask> task);

    /**
     * 在指定位置所属的区域线程上执行一次任务（下一个 tick），无参数版本。
     *
     * @param location 位置，用于确定区域
     * @param task     要执行的任务
     * @return 包装后的任务对象 {@link WrappedTask}
     */
    @NotNull WrappedTask runAtLocation(@NotNull Location location, @NotNull Runnable task);

    /**
     * 在指定世界和区块坐标所属的区域线程上执行一次任务（下一个 tick）。
     *
     * @param world  世界
     * @param chunkX 区块 X 坐标
     * @param chunkZ 区块 Z 坐标
     * @param task   要执行的任务，参数为当前任务包装对象
     * @return 包装后的任务对象 {@link WrappedTask}
     */
    @NotNull WrappedTask runAtLocation(@NotNull World world, int chunkX, int chunkZ, @NotNull Consumer<WrappedTask> task);

    /**
     * 在指定世界和区块坐标所属的区域线程上执行一次任务（下一个 tick），无参数版本。
     *
     * @param world  世界
     * @param chunkX 区块 X 坐标
     * @param chunkZ 区块 Z 坐标
     * @param task   要执行的任务
     * @return 包装后的任务对象 {@link WrappedTask}
     */
    @NotNull WrappedTask runAtLocation(@NotNull World world, int chunkX, int chunkZ, @NotNull Runnable task);

    /**
     * 在指定位置所属的区域线程上执行一次延迟任务。
     *
     * @param location   位置，用于确定区域
     * @param task       要执行的任务，参数为当前任务包装对象
     * @param delayTicks 延迟的 tick 数（必须 >=0）
     * @return 包装后的任务对象 {@link WrappedTask}
     */
    @NotNull WrappedTask runAtLocationLater(@NotNull Location location, @NotNull Consumer<WrappedTask> task, long delayTicks);

    /**
     * 在指定位置所属的区域线程上执行一次延迟任务，无参数版本。
     *
     * @param location   位置，用于确定区域
     * @param task       要执行的任务
     * @param delayTicks 延迟的 tick 数（必须 >=0）
     * @return 包装后的任务对象 {@link WrappedTask}
     */
    @NotNull WrappedTask runAtLocationLater(@NotNull Location location, @NotNull Runnable task, long delayTicks);

    /**
     * 在指定位置所属的区域线程上执行重复任务。
     *
     * @param location          位置，用于确定区域
     * @param task              要执行的任务，参数为当前任务包装对象
     * @param initialDelayTicks 首次执行的延迟 tick 数（必须 >=0）
     * @param periodTicks       重复执行的周期 tick 数（必须 >0）
     * @return 包装后的任务对象 {@link WrappedTask}
     */
    @NotNull WrappedTask runAtLocationTimer(@NotNull Location location, @NotNull Consumer<WrappedTask> task, long initialDelayTicks, long periodTicks);

    /**
     * 在指定位置所属的区域线程上执行重复任务，无参数版本。
     *
     * @param location          位置，用于确定区域
     * @param task              要执行的任务
     * @param initialDelayTicks 首次执行的延迟 tick 数（必须 >=0）
     * @param periodTicks       重复执行的周期 tick 数（必须 >0）
     * @return 包装后的任务对象 {@link WrappedTask}
     */
    @NotNull WrappedTask runAtLocationTimer(@NotNull Location location, @NotNull Runnable task, long initialDelayTicks, long periodTicks);

    // ==================== 实体任务（Paper特有，Bukkit退化为同步任务） ====================

    /**
     * 在实体所属的线程上执行一次任务（下一个 tick）。
     * <p>
     * 在 Paper 平台上，任务将在该实体当前的区域线程执行，如果实体被移除则会触发 retired 回调；
     * 在 Bukkit 平台上，此方法退化为在主线程执行的同步任务，retired 回调将被忽略（永远不会触发）。
     * </p>
     *
     * @param entity  目标实体
     * @param task    要执行的任务，参数为当前任务包装对象
     * @param retired 当实体被移除且任务尚未执行时的回调（仅在 Paper 平台有效，Bukkit 上被忽略）
     * @return 包装后的任务对象 {@link WrappedTask}，若实体已移除且无法调度则可能返回 {@code null}（Paper 平台），Bukkit 平台总是返回非 null。
     */
    @Nullable WrappedTask runAtEntity(@NotNull Entity entity, @NotNull Consumer<WrappedTask> task, @Nullable Runnable retired);

    /**
     * 在实体所属的线程上执行一次任务（下一个 tick），无参数版本。
     *
     * @param entity  目标实体
     * @param task    要执行的任务
     * @param retired 当实体被移除且任务尚未执行时的回调（仅在 Paper 平台有效，Bukkit 上被忽略）
     * @return 包装后的任务对象 {@link WrappedTask}，若实体已移除且无法调度则可能返回 {@code null}（Paper 平台），Bukkit 平台总是返回非 null。
     */
    @Nullable WrappedTask runAtEntity(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired);

    /**
     * 在实体所属的线程上执行一次延迟任务。
     *
     * @param entity     目标实体
     * @param task       要执行的任务，参数为当前任务包装对象
     * @param retired    当实体被移除且任务尚未执行时的回调（仅在 Paper 平台有效，Bukkit 上被忽略）
     * @param delayTicks 延迟的 tick 数（必须 >=0）
     * @return 包装后的任务对象 {@link WrappedTask}，若实体已移除且无法调度则可能返回 {@code null}（Paper 平台），Bukkit 平台总是返回非 null。
     */
    @Nullable WrappedTask runAtEntityLater(@NotNull Entity entity, @NotNull Consumer<WrappedTask> task, @Nullable Runnable retired, long delayTicks);

    /**
     * 在实体所属的线程上执行一次延迟任务，无参数版本。
     *
     * @param entity     目标实体
     * @param task       要执行的任务
     * @param retired    当实体被移除且任务尚未执行时的回调（仅在 Paper 平台有效，Bukkit 上被忽略）
     * @param delayTicks 延迟的 tick 数（必须 >=0）
     * @return 包装后的任务对象 {@link WrappedTask}，若实体已移除且无法调度则可能返回 {@code null}（Paper 平台），Bukkit 平台总是返回非 null。
     */
    @Nullable WrappedTask runAtEntityLater(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired, long delayTicks);

    /**
     * 在实体所属的线程上执行重复任务。
     *
     * @param entity            目标实体
     * @param task              要执行的任务，参数为当前任务包装对象
     * @param retired           当实体被移除且任务尚未执行时的回调（仅在 Paper 平台有效，Bukkit 上被忽略）
     * @param initialDelayTicks 首次执行的延迟 tick 数（必须 >=0）
     * @param periodTicks       重复执行的周期 tick 数（必须 >0）
     * @return 包装后的任务对象 {@link WrappedTask}，若实体已移除且无法调度则可能返回 {@code null}（Paper 平台），Bukkit 平台总是返回非 null。
     */
    @Nullable WrappedTask runAtEntityTimer(@NotNull Entity entity, @NotNull Consumer<WrappedTask> task, @Nullable Runnable retired, long initialDelayTicks, long periodTicks);

    /**
     * 在实体所属的线程上执行重复任务，无参数版本。
     *
     * @param entity            目标实体
     * @param task              要执行的任务
     * @param retired           当实体被移除且任务尚未执行时的回调（仅在 Paper 平台有效，Bukkit 上被忽略）
     * @param initialDelayTicks 首次执行的延迟 tick 数（必须 >=0）
     * @param periodTicks       重复执行的周期 tick 数（必须 >0）
     * @return 包装后的任务对象 {@link WrappedTask}，若实体已移除且无法调度则可能返回 {@code null}（Paper 平台），Bukkit 平台总是返回非 null。
     */
    @Nullable WrappedTask runAtEntityTimer(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired, long initialDelayTicks, long periodTicks);

    // ==================== 异步传送 ====================

    /**
     * 异步传送实体到目标位置。
     * <p>
     * 在 Paper 平台上，直接调用 {@link Entity#teleportAsync(Location)} 返回一个 {@link CompletableFuture}；
     * 在 Bukkit 平台上，调度一个同步任务执行普通传送，并在完成后完成 Future。
     * </p>
     *
     * @param entity   要传送的实体
     * @param location 目标位置
     * @return 一个 {@link CompletableFuture}，在传送完成后返回 {@code true} 表示成功，{@code false} 表示失败。
     *         在 Bukkit 平台上，Future 将始终在传送后完成（同步传送可能失败，此时返回 false）。
     */
    @NotNull CompletableFuture<Boolean> teleportAsync(@NotNull Entity entity, @NotNull Location location);

    // ==================== 取消任务 ====================

    /**
     * 取消指定插件拥有的所有任务。
     *
     * @param plugin 目标插件
     */
    void cancelTasks(@NotNull Plugin plugin);
}