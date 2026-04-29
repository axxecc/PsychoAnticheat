package com.psycho;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.psycho.cfg.MessagesCfg;
import com.psycho.hologram.Holograms;
import com.psycho.listeners.CheckListener;
import com.psycho.listeners.ConnectionListener;
import com.psycho.player.PsychoPlayer;
import com.psycho.scheduler.PlatformDetector;
import com.psycho.scheduler.impl.PlatformScheduler;
import com.psycho.services.CheckService;
import com.psycho.services.CommandService;
import com.psycho.services.ConfigService;
import com.psycho.services.MlModelService;
import com.psycho.services.PlayerTrackerService;
import com.psycho.utils.Logger;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class Psycho extends JavaPlugin {
    private static Psycho instance;
    @Getter
    private ConnectionListener connectionListener;
    @Getter
    private CheckService checkService;
    @Getter
    private ConfigService configService;
    private CommandService commandService;
    @Getter
    private PlayerTrackerService playerTrackerService;
    private CheckListener checkListener;
    private Holograms holograms;
    @Getter
    private MlModelService mlModelService;
    private PlatformDetector scheduler;

    public static Psycho get() {
        return instance;
    }

    private void create() {
        instance = this;
        scheduler = new PlatformDetector(this);
        connectionListener = new ConnectionListener();
        checkService = new CheckService();
        configService = new ConfigService(this);
        mlModelService = new MlModelService(this);
        commandService = new CommandService(this);
        playerTrackerService = new PlayerTrackerService();
        checkListener = new CheckListener();
        holograms = new Holograms(this);

        checkService.initialize();
    }

    @Override
    public void onEnable() {
        create();
        saveDefaultConfig();

        ensureMlResources();
        mlModelService.load();

        PacketEvents.getAPI().init();
        PacketEvents.getAPI().load();

        // reg. commands
        Objects.requireNonNull(getCommand("psycho")).setExecutor(commandService);
        Objects.requireNonNull(getCommand("psycho")).setTabCompleter(commandService);

        // reg. bukkit event listeners
        getServer().getPluginManager().registerEvents(connectionListener, this);

        // reg. packetevents listeners
        PacketEvents.getAPI().getEventManager().registerListener(checkListener, PacketListenerPriority.NORMAL);

        getServer().getGlobalRegionScheduler().run(this, task-> {
            getServer().dispatchCommand(getServer().getConsoleSender(), "psycho reload");
        });

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (connectionListener.getPlayer(online.getUniqueId()) == null) {
                connectionListener.getPlayers().put(online.getUniqueId(), new PsychoPlayer(online));
            }
            playerTrackerService.trackJoin(connectionListener.getPlayer(online.getUniqueId()));
        }

        holograms.start();
        Logger.log("Psycho successfully loaded");
    }

    private void ensureMlResources() {
        File mlDir = new File(getDataFolder(), "ml");
        if (!mlDir.exists() && !mlDir.mkdirs()) {
            Logger.log("Failed to create ml directory at " + mlDir.getAbsolutePath());
            return;
        }

        ensureMlResource("ml/model.bin");
        ensureMlResource("ml/normalizer.bin");
    }

    private void ensureMlResource(String resourcePath) {
        File targetFile = new File(getDataFolder(), resourcePath);
        if (targetFile.exists()) {
            return;
        }

        if (getResource(resourcePath) == null) {
            Logger.log("Missing bundled ML resource: " + resourcePath);
            return;
        }

        saveResource(resourcePath, false);
        Logger.log("Installed ML resource: " + resourcePath);
    }

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
    }

    @Override
    public void onDisable() {
        try {
            if (holograms != null) holograms.stop();
        } catch (Exception e) {
            Logger.log("Error stopping holograms: " + e.getMessage());
        }
        if (mlModelService != null) {
            mlModelService.unload();
        }
        Logger.log("Psycho disabled");
        try {
            PacketEvents.getAPI().terminate();
        } catch (Exception e) {
            Logger.log("Error terminating PacketEvents: " + e.getMessage());
        }
        if (scheduler != null) {
            scheduler.getScheduler().cancelTasks(this);
        }
    }

    public MessagesCfg getMessagesCfg() {
        return configService.getMessagesCfg();
    }

    public Holograms getNametagManager() {
        return holograms;
    }

    public PlatformScheduler getScheduler() {
        return scheduler.getScheduler();
    }
}
