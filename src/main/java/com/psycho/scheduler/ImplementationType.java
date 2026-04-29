package com.psycho.scheduler;

import java.util.function.Supplier;

/**
 * 该枚举用于确定服务器实现类型
 * <p>
 * 枚举按优先级排序。第一个通过所有测试的实现将被使用
 * <p>
* 如果服务器不支持任务消费者，则被视为"遗留"服务器。此功能于 1.13.2 版本中添加
 * 这意味着即使是1.13.1服务器也被视为遗留服务器，因为它们不支持高度重要的支持
 * 关于不支持此功能服务器上任务消费者启用方法行为的更多信息
 */
@SuppressWarnings("SpellCheckingInspection")
public enum ImplementationType {

    PAPER (
            "PaperImplementation",
            new Supplier[0],
            "com.destroystokyo.paper.PaperConfig"
    ),
    BUKKIT (
            "BukkitImplementation",
            new Supplier[0],
            "org.spigotmc.SpigotConfig"
    );

    private final String implementationClassName;
    private final Supplier<Boolean>[] tests;
    private final String[] classNames;

    ImplementationType(String implementationClassName, Supplier<Boolean>[] tests, String... classNames) {
        this.implementationClassName = implementationClassName;
        this.tests = tests;
        this.classNames = classNames;
    }

    public String getImplementationClassName() {
        return implementationClassName;
    }

    public Supplier<Boolean>[] getTests() {
        return tests;
    }

    public String[] getClassNames() {
        return classNames;
    }

    public boolean selfCheck() {
        // Run self-tests
        for (Supplier<Boolean> test : this.getTests()) {
            if (!test.get()) return false;
        }

        // Self-test for class names
        String[] classNames = this.getClassNames();

        // Check if any of the class names are present
        for (String className : classNames) {
            try {
                // Try to load the class
                Class.forName(className);

                // Found the server type, remember that and break the loop
                return true;
            } catch (ClassNotFoundException ignored) {}
        }

        return false;
    }
}

