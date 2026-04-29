package com.psycho.scheduler;

import com.psycho.scheduler.impl.BukkitImplementation;
import com.psycho.scheduler.impl.PaperImplementation;
import com.psycho.scheduler.impl.PlatformScheduler;
import lombok.Getter;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * 平台检测器，用于确定当前服务器运行在哪个平台上，并提供对应的 {@link PlatformScheduler} 实例。
 * <p>
 * 每个插件应创建自己的 {@code PlatformDetector} 实例，构造函数需要传入插件实例。
 * </p>
 */
public final class PlatformDetector {
    private final Plugin plugin;
    private final ImplementationType implementationType;
    /**
     * -- GETTER --
     *  获取当前平台的统一调度器实例。
     *
     * @return 平台对应的 {@link PlatformScheduler} 实现。
     */
    @Getter
    private final PlatformScheduler scheduler;

    /**
     * 创建平台检测器实例，自动检测平台并初始化调度器。
     *
     * @param plugin 你的插件实例，用于获取服务器信息及后续任务调度。
     */
    public PlatformDetector(@NotNull Plugin plugin) {
        this.plugin = plugin;
        // 根据类名查找实现类型
        ImplementationType foundType = ImplementationType.BUKKIT;

        for (ImplementationType type : ImplementationType.values()) {
            // 该服务器的实现不适用
            if (!type.selfCheck()) continue;
            // 找到的实现匹配
            foundType = type;
            break;
        }

        this.implementationType = foundType;

        this.scheduler = createSchedulerDirectly(this.implementationType);

        Logger logger = this.plugin.getLogger();
        logger.severe("*******************************");
        logger.severe("Server PlatformDetector is" + foundType);
        logger.severe("*******************************");
    }

    private PlatformScheduler createSchedulerDirectly(ImplementationType type) {
        if (type == ImplementationType.PAPER) {
            return new PaperImplementation(plugin);
        } else {
            return new BukkitImplementation(plugin);
        }
    }

    public boolean isPaper() {
        return this.implementationType == ImplementationType.PAPER;
    }

    public boolean isBukkit() {
        return this.implementationType == ImplementationType.BUKKIT;
    }

    /**
     * 获取当前服务器平台。
     *
     * @return 服务器平台枚举。
     */
    public ImplementationType getPlatform() {
        return this.implementationType;
    }

}