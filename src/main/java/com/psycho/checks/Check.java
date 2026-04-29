package com.psycho.checks;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.psycho.Psycho;
import com.psycho.cfg.CheckCfg;
import com.psycho.player.PsychoPlayer;
import com.psycho.utils.Hex;
import com.psycho.utils.Logger;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public abstract class Check {
    protected final PsychoPlayer player;
    public final Psycho plugin;
    @Getter
    private final String name;
    @Getter
    private final CheckCfg cfg;
    @Getter
    private final String cfgPath;
    private long lastVlDecayTime;
    private long lastFlagTime;

    public Check(PsychoPlayer player, String cfgPath, CheckCfg cfg) {
        this.plugin = Psycho.get();
        this.name = getClass().getSimpleName();
        this.cfg = cfg;
        this.cfgPath = cfgPath;
        this.player = player;
        this.lastVlDecayTime = System.currentTimeMillis();
    }

    public static String buildBar(int value, int maxValue) {
        if (maxValue <= 0) {
            return buildBar(0.0);
        }
        return buildBar((double) value / maxValue);
    }

    public static String buildBar(double progress) {
        int totalBars = 20;
        double clamped = Math.max(0.0, Math.min(progress, 1.0));
        int filled = Math.min((int) Math.round(clamped * totalBars), totalBars);

        StringBuilder bar = new StringBuilder("§7[");
        for (int i = 0; i < totalBars; i++) {
            if (i < filled) {
                if (i < totalBars * 0.4) bar.append("§a|");
                else if (i < totalBars * 0.7) bar.append("§e|");
                else bar.append("§c|");
            } else {
                bar.append("§8|");
            }
        }
        bar.append("§7]");
        return bar.toString();
    }

    public final void process(PacketReceiveEvent event) {
        tickVlDecay();
        handle(event);
    }

    protected abstract void handle(PacketReceiveEvent event);

    private void tickVlDecay() {
        long interval = cfg.vlDecayInterval();
        if (interval <= 0) return;

        long now = System.currentTimeMillis();
        long elapsed = now - lastVlDecayTime;

        if (elapsed >= interval) {
            int ticks = (int) (elapsed / interval);
            int currentVl = player.getViolation(name);
            if (currentVl > 0) {
                int newVl = Math.max(0, currentVl - ticks);
                player.setViolation(name, newVl);
            }
            lastVlDecayTime = now;
        }
    }

    public void flag() {
        flag("");
    }

    public void flag(String info) {
        long now = System.currentTimeMillis();
        if (now - lastFlagTime < 500) return;
        lastFlagTime = now;

        int vl = player.getViolation(name) + 1;
        player.addViolation(name, 1);

        lastVlDecayTime = now;

        player.getStats().getFailedChecks().add(name);

        String vlBar = buildBar(vl, cfg.vlThreshold());

        if (vl >= cfg.vlThreshold()) {
            Logger.log("executing punishment");
            Player bukkitPlayer = player.getBukkitPlayer();
            plugin.getConnectionListener().removePlayer(bukkitPlayer.getUniqueId());
            plugin.getScheduler().runSync(() -> {
                Bukkit.dispatchCommand(plugin.getServer().getConsoleSender(),
                        cfg.punishCommand().replace("{player}", bukkitPlayer.getName()));
                Logger.log("§a✓ " + player.getBukkitPlayer().getName() + " punished");
            });
        }

        String message = Hex.translateHexColors(plugin.getMessagesCfg().formatAlert(
                player.getBukkitPlayer().getName(),
                name,
                vlBar,
                vl,
                cfg.vlThreshold(),
                info
        ));

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission("psycho.alerts")) continue;

            PsychoPlayer psychoOnline = plugin.getConnectionListener().getPlayer(online.getUniqueId());

            if (psychoOnline == null) continue;

            if (psychoOnline.isSendAlerts()) {
                online.sendMessage(message);
            }
        }
    }

    public void setback() {
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) return;

        if (player.getLastSafeLocation() != null) {
            plugin.getScheduler().runSync(() -> {
                plugin.getScheduler().teleportAsync(bukkitPlayer, player.getLastSafeLocation());
            });
        }
    }

    public void cancelHits() {
        player.setHitCancelTicks(40);
    }

}
