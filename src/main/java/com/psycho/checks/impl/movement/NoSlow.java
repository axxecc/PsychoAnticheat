package com.psycho.checks.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.psycho.cfg.CheckCfg;
import com.psycho.checks.Check;
import com.psycho.player.PsychoPlayer;
import com.psycho.utils.buffer.VlBuffer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.potion.PotionEffectType;

// best noslow check in 2026
public class NoSlow extends Check {
    public NoSlow(PsychoPlayer player, String cfgPath, CheckCfg cfg) {
        super(player, cfgPath, cfg);
    }

    private final VlBuffer buffer = new VlBuffer();
    private long lastUsingItem = 0;
    private boolean usingItem = false;
    private double threshold = 0;

    @Override
    protected void handle(PacketReceiveEvent event) {
        if (!getCfg().enabled() || player.getBukkitPlayer().getGameMode() == GameMode.CREATIVE || player.getBukkitPlayer().isFlying() || player.getBukkitPlayer().isInsideVehicle()) return;

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION) {
            long now = System.currentTimeMillis();
            double xd = player.getDeltaX();
            double yd = player.getDeltaY();
            double zd = player.getDeltaZ();
            double speed = Math.sqrt(xd * xd + zd * zd);

            if (now - player.getLastDamageTime() < 1000L) {
                return;
            }

            if (now - lastUsingItem < 800) {
                return;
            }

            if (player.getBukkitPlayer().hasPotionEffect(PotionEffectType.DOLPHINS_GRACE)) {
                return;
            }

            if (Math.abs(yd) > 0.01) {
                threshold = 0.3;
            } else {
                threshold = 0.14;
            }

            double speedMultiplier = 1.0;

            if (player.getBukkitPlayer().hasPotionEffect(PotionEffectType.SPEED)) {
                int level = player.getBukkitPlayer()
                        .getPotionEffect(PotionEffectType.SPEED)
                        .getAmplifier();

                speedMultiplier += 0.2 * (level + 1);
            }

            threshold *= speedMultiplier;

            Location loc = player.getBukkitPlayer().getLocation();
            plugin.getScheduler().runAtLocation(loc, () -> {
                if (isOnIce(loc)) {
                    threshold = 0.2;
                    if (Math.abs(yd) > 0.01) {
                        threshold = 0.5;
                    }
                }
            });

            if (player.getBukkitPlayer().isHandRaised()) {
                if (!usingItem) {
                    lastUsingItem = now;
                }
                usingItem = true;
                if (speed > threshold) {
                    buffer.fail(1);
                    if (buffer.getVl() >= 10) {
                        flag();
                        setback();
                    }
                } else {
                    buffer.decay(2);
                }
            } else {
                usingItem = false;
                buffer.setVl(0);
            }
        }
    }

    private boolean isOnIce(Location loc) {
        var world = loc.getWorld();
        if (world == null) return false;

        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        int y = loc.getBlockY();

        for (int i = 0; i < 3; i++) {
            Block b = world.getBlockAt(x, y - i, z);
            Material type = b.getType();

            if (type == Material.ICE || type == Material.PACKED_ICE ||
                    type == Material.BLUE_ICE || type == Material.FROSTED_ICE) {
                return true;
            }

            if (b.getType().isSolid()) break;
        }

        return false;
    }
}
